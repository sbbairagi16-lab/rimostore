package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    // --- Real-time Streams ---

    fun observePublishedApps(): Flow<List<AppListing>> =
        db.collection("apps")
            .whereEqualTo("published", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppListing::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowErrorIfActive(error, OperationType.LIST, "apps")
                throw error
            }

    fun observeDeveloperApps(developerId: String): Flow<List<AppListing>> =
        db.collection("apps")
            .whereEqualTo("developerId", developerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppListing::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowErrorIfActive(error, OperationType.LIST, "apps", developerId)
                throw error
            }

    fun observeUserLibrary(userId: String): Flow<List<InstalledApp>> =
        db.collection("users")
            .document(userId)
            .collection("library")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    InstalledApp::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowErrorIfActive(error, OperationType.LIST, "users/$userId/library", userId)
                throw error
            }

    fun observeDeveloperProfile(userId: String): Flow<DeveloperProfile?> =
        db.collection("developer_profiles")
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        DeveloperProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowErrorIfActive(error, OperationType.GET, "developer_profiles/$userId")
                throw error
            }

    fun observePrivateDeveloperAccount(userId: String): Flow<PrivateDeveloperAccount?> =
        db.collection("developers")
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PrivateDeveloperAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowErrorIfActive(error, OperationType.GET, "developers/$userId", userId)
                throw error
            }

    fun observeAppReviews(appId: String): Flow<List<AppReview>> =
        db.collection("apps")
            .document(appId)
            .collection("reviews")
            .whereEqualTo("appId", appId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppReview::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowErrorIfActive(error, OperationType.LIST, "apps/$appId/reviews")
                throw error
            }

    suspend fun getDeveloperProfileOnce(developerId: String): Result<DeveloperProfile?> {
        val path = "developer_profiles/$developerId"
        return try {
            requireUserId()
            require(developerId.isNotBlank()) { "Developer ID is required" }
            val snap = db.collection("developer_profiles").document(developerId).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(
                    DeveloperProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    // --- Developer Account Registration & Profile Management ---

    suspend fun registerOrUpdateDeveloperAccount(
        input: DeveloperRegistrationInput,
        isUpdate: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "developers/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val studioName = input.studioName.trim()
            val tagline = input.tagline.trim()
            val bio = input.bio.trim()
            val specialty = input.specialty.trim()
            val email = input.contactEmail.trim()
            val website = input.websiteUrl?.trim()?.takeIf { it.isNotEmpty() }
            val support = input.supportUrl?.trim()?.takeIf { it.isNotEmpty() }

            require(studioName.length in 2..60) { "Studio name must be 2–60 characters." }
            require(tagline.length <= 120) { "Tagline must be 120 characters or less." }
            require(bio.length <= 600) { "Bio must be 600 characters or less." }
            require(specialty.length in 1..40) { "Please select a valid specialty." }
            require(email.length in 3..120 && email.contains("@")) {
                "Please enter a valid contact email."
            }
            require(website == null || (website.startsWith("https://") && website.length <= 300)) {
                "Website URL must start with https://"
            }
            require(support == null || (support.startsWith("https://") && support.length <= 300)) {
                "Support URL must start with https://"
            }

            val privateRef = db.collection("developers").document(activeUid)
            val publicRef = db.collection("developer_profiles").document(activeUid)
            val batch = db.batch()

            if (isUpdate) {
                val privateUpdate = mutableMapOf<String, Any>(
                    "studioName" to studioName,
                    "contactEmail" to email,
                    "termsAccepted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (support != null) privateUpdate["supportUrl"] = support

                val publicUpdate = mutableMapOf<String, Any>(
                    "studioName" to studioName,
                    "tagline" to tagline,
                    "bio" to bio,
                    "specialty" to specialty,
                    "avatarPreset" to input.avatarPreset,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (website != null) publicUpdate["websiteUrl"] = website

                batch.update(privateRef, privateUpdate)
                batch.update(publicRef, publicUpdate)
            } else {
                val privateCreate = buildMap<String, Any?> {
                    put("userId", activeUid)
                    put("studioName", studioName)
                    put("contactEmail", email)
                    put("supportUrl", support)
                    put("termsAccepted", true)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }

                val publicCreate = buildMap<String, Any?> {
                    put("userId", activeUid)
                    put("studioName", studioName)
                    put("tagline", tagline)
                    put("bio", bio)
                    put("specialty", specialty)
                    put("avatarPreset", input.avatarPreset)
                    put("websiteUrl", website)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }

                batch.set(privateRef, privateCreate)
                batch.set(publicRef, publicCreate)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Developer Software Upload / Edit / Toggle / Delete ---

    suspend fun saveAppListing(
        input: AppDraftInput,
        developerName: String
    ): Result<String> {
        val isEdit = !input.existingId.isNullOrBlank()
        var path = if (isEdit) "apps/${input.existingId}" else "apps"
        return try {
            val uid = requireUserId()
            val title = input.title.trim()
            val tagline = input.tagline.trim()
            val description = input.description.trim()
            val version = input.versionName.trim()
            val releaseNotes = input.releaseNotes.trim()
            val devName = developerName.trim()
            val iconUrl = input.iconUrl?.trim()?.takeIf { it.isNotEmpty() }
            val downloadUrl = input.downloadUrl?.trim()?.takeIf { it.isNotEmpty() }
            val pkgName = input.packageName?.trim()?.takeIf { it.isNotEmpty() }
            val cleanTags = input.tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().take(6)

            require(devName.length in 2..60) { "Developer studio name must be 2–60 characters." }
            require(title.length in 2..50) { "Software title must be 2–50 characters." }
            require(tagline.length in 4..120) { "Short tagline must be 4–120 characters." }
            require(description.length in 10..2000) { "Description must be 10–2000 characters." }
            require(input.appType in listOf(StoreConstants.TYPE_GAME, StoreConstants.TYPE_APP)) {
                "Invalid software type."
            }
            require(input.category in StoreConstants.allCategories) { "Select a valid category." }
            require(version.length in 1..24) { "Version name must be 1–24 characters." }
            require(input.sizeMb in 1.0..4096.0) { "Size must be between 1 MB and 4096 MB." }
            require(input.ageRating in StoreConstants.ageRatings) { "Select a valid age rating." }
            require(Regex("^#[0-9A-Fa-f]{6}$").matches(input.accentColorHex)) { "Invalid accent color." }
            require(iconUrl == null || (iconUrl.startsWith("https://") && iconUrl.length <= 500)) {
                "Artwork URL must start with https://"
            }
            require(downloadUrl == null || (downloadUrl.startsWith("https://") && downloadUrl.length <= 500)) {
                "Release download URL must start with https://"
            }
            require(pkgName == null || pkgName.length <= 120) { "Package name is too long." }
            require(releaseNotes.length <= 600) { "Release notes must be 600 characters or less." }

            val existingId = input.existingId
            if (!existingId.isNullOrBlank()) {
                val appId = existingId
                val docRef = db.collection("apps").document(appId)
                path = docRef.path
                val updatePayload = mutableMapOf<String, Any>(
                    "developerName" to devName,
                    "title" to title,
                    "tagline" to tagline,
                    "description" to description,
                    "appType" to input.appType,
                    "category" to input.category,
                    "versionName" to version,
                    "sizeMb" to input.sizeMb,
                    "ageRating" to input.ageRating,
                    "accentColorHex" to input.accentColorHex,
                    "iconPreset" to input.iconPreset,
                    "tags" to cleanTags,
                    "releaseNotes" to releaseNotes,
                    "published" to input.published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (iconUrl != null) updatePayload["iconUrl"] = iconUrl
                if (downloadUrl != null) updatePayload["downloadUrl"] = downloadUrl
                if (pkgName != null) updatePayload["packageName"] = pkgName

                docRef.update(updatePayload).await()
                Result.success(appId)
            } else {
                val docRef = db.collection("apps").document()
                path = docRef.path
                val createPayload = buildMap<String, Any?> {
                    put("developerId", uid)
                    put("developerName", devName)
                    put("title", title)
                    put("tagline", tagline)
                    put("description", description)
                    put("appType", input.appType)
                    put("category", input.category)
                    put("versionName", version)
                    put("sizeMb", input.sizeMb)
                    put("ageRating", input.ageRating)
                    put("accentColorHex", input.accentColorHex)
                    put("iconPreset", input.iconPreset)
                    put("iconUrl", iconUrl)
                    put("downloadUrl", downloadUrl)
                    put("packageName", pkgName)
                    put("tags", cleanTags)
                    put("releaseNotes", releaseNotes)
                    put("published", input.published)
                    put("starterCatalog", false)
                    put("downloadsCount", 0L)
                    put("ratingSum", 0L)
                    put("ratingCount", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }

                docRef.set(createPayload).await()
                Result.success(docRef.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun setAppPublished(appId: String, published: Boolean): Result<Unit> {
        val path = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID is required." }
            db.collection("apps").document(appId).update(
                mapOf(
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAppListing(appId: String): Result<Unit> {
        val path = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID is required." }
            db.collection("apps").document(appId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- User Download / Update / Uninstall Library Actions ---

    suspend fun installApp(app: AppListing): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/library/${app.id}"
        return try {
            val activeUid = requireUserId()
            require(app.id.isNotBlank()) { "App ID is required." }
            val libRef = db.collection("users").document(activeUid).collection("library").document(app.id)
            val appRef = db.collection("apps").document(app.id)

            val libPayload = buildMap<String, Any?> {
                put("appId", app.id)
                put("title", app.title)
                put("developerName", app.developerName)
                put("category", app.category)
                put("appType", app.appType)
                put("installedVersion", app.versionName)
                put("sizeMb", app.sizeMb)
                put("accentColorHex", app.accentColorHex)
                put("iconPreset", app.iconPreset)
                put("iconUrl", app.iconUrl?.takeIf { it.isNotBlank() })
                put("downloadUrl", app.downloadUrl?.takeIf { it.isNotBlank() })
                put("installedAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }.filterValues { it != null }

            val batch = db.batch()
            batch.set(libRef, libPayload)
            batch.update(
                appRef,
                mapOf(
                    "downloadsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateInstalledApp(app: AppListing): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/library/${app.id}"
        return try {
            val activeUid = requireUserId()
            require(app.id.isNotBlank()) { "App ID is required." }
            val libRef = db.collection("users").document(activeUid).collection("library").document(app.id)
            val updateMap = mutableMapOf<String, Any>(
                "installedVersion" to app.versionName,
                "title" to app.title,
                "developerName" to app.developerName,
                "sizeMb" to app.sizeMb,
                "accentColorHex" to app.accentColorHex,
                "iconPreset" to app.iconPreset,
                "category" to app.category,
                "appType" to app.appType,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            app.downloadUrl?.takeIf { it.isNotBlank() }?.let { updateMap["downloadUrl"] = it }
            app.iconUrl?.takeIf { it.isNotBlank() }?.let { updateMap["iconUrl"] = it }
            libRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun uninstallApp(appId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/library/$appId"
        return try {
            val activeUid = requireUserId()
            require(appId.isNotBlank()) { "App ID is required." }
            db.collection("users").document(activeUid).collection("library").document(appId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Reviews & Ratings ---

    suspend fun submitReview(
        appId: String,
        authorName: String,
        rating: Int,
        comment: String,
        existingReview: AppReview?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "apps/$appId/reviews/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val cleanComment = comment.trim()
            val cleanAuthor = authorName.trim().ifBlank { "PulseStore User" }.take(60)
            require(appId.isNotBlank()) { "App ID is required." }
            require(rating in 1..5) { "Rating must be between 1 and 5 stars." }
            require(cleanComment.length in 2..600) { "Review comment must be 2–600 characters." }

            val reviewRef = db.collection("apps").document(appId).collection("reviews").document(activeUid)
            val appRef = db.collection("apps").document(appId)
            val batch = db.batch()

            if (existingReview == null) {
                batch.set(
                    reviewRef,
                    mapOf(
                        "appId" to appId,
                        "userId" to activeUid,
                        "authorName" to cleanAuthor,
                        "rating" to rating,
                        "comment" to cleanComment,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    appRef,
                    mapOf(
                        "ratingSum" to FieldValue.increment(rating.toLong()),
                        "ratingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    reviewRef,
                    mapOf(
                        "rating" to rating,
                        "comment" to cleanComment,
                        "authorName" to cleanAuthor,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val delta = (rating - existingReview.rating).toLong()
                if (delta != 0L) {
                    batch.update(
                        appRef,
                        mapOf(
                            "ratingSum" to FieldValue.increment(delta),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(appId: String, existingReview: AppReview): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "apps/$appId/reviews/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val reviewRef = db.collection("apps").document(appId).collection("reviews").document(activeUid)
            val appRef = db.collection("apps").document(appId)
            val batch = db.batch()
            batch.delete(reviewRef)
            batch.update(
                appRef,
                mapOf(
                    "ratingSum" to FieldValue.increment(-existingReview.rating.toLong()),
                    "ratingCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- One-Tap Starter Showcase Seeder (real Firestore docs) ---

    suspend fun seedStarterCatalog(): Result<Int> {
        val path = "apps"
        return try {
            val uid = requireUserId()
            val existingPublished = db.collection("apps")
                .whereEqualTo("published", true)
                .limit(1)
                .get()
                .await()
            if (!existingPublished.isEmpty) {
                return Result.success(0)
            }

            val batch = db.batch()
            for (spec in StarterCatalogFactory.entries) {
                val docRef = db.collection("apps").document(spec.docId)
                val payload = mapOf(
                    "developerId" to uid,
                    "developerName" to spec.developerName,
                    "title" to spec.title,
                    "tagline" to spec.tagline,
                    "description" to spec.description,
                    "appType" to spec.appType,
                    "category" to spec.category,
                    "versionName" to spec.versionName,
                    "sizeMb" to spec.sizeMb,
                    "ageRating" to spec.ageRating,
                    "accentColorHex" to spec.accentColorHex,
                    "iconPreset" to spec.iconPreset,
                    "packageName" to spec.packageName,
                    "tags" to spec.tags,
                    "releaseNotes" to spec.releaseNotes,
                    "published" to true,
                    "starterCatalog" to true,
                    "downloadsCount" to 0L,
                    "ratingSum" to 0L,
                    "ratingCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(StarterCatalogFactory.entries.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun logFlowErrorIfActive(
        error: Throwable,
        operation: OperationType,
        path: String,
        expectedUid: String? = null
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val currentUid = auth.currentUser?.uid
        val shouldLog = currentUid != null && (expectedUid == null || currentUid == expectedUid)
        if (shouldLog) {
            handleFirestoreError(firestoreError ?: error, operation, path)
        }
    }
}
