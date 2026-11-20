package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDraftInput
import com.example.data.AppListing
import com.example.data.AppReview
import com.example.data.DeveloperProfile
import com.example.data.DeveloperRegistrationInput
import com.example.data.InstalledApp
import com.example.data.PrivateDeveloperAccount
import com.example.data.StoreRepository
import com.example.data.UiState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CatalogSortOption(val label: String) {
    POPULAR("Popular"),
    TOP_RATED("Top Rated"),
    NEWEST("Newest"),
    SMALLEST("Compact")
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    private val repository: StoreRepository,
    val currentUserId: String,
    val currentUserDisplayName: String,
    val currentUserEmail: String
) : ViewModel() {

    private companion object {
        const val TAG = "StoreVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    val publishedAppsState: StateFlow<UiState<List<AppListing>>> =
        repository.observePublishedApps()
            .map<List<AppListing>, UiState<List<AppListing>>> { list ->
                val sorted = list.sortedWith(
                    compareByDescending<AppListing> { it.downloadsCount }
                        .thenByDescending { it.averageRating }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
                UiState.Success(sorted)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing published apps", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load store catalog"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myDeveloperAppsState: StateFlow<UiState<List<AppListing>>> =
        repository.observeDeveloperApps(currentUserId)
            .map<List<AppListing>, UiState<List<AppListing>>> { list ->
                val sorted = list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                UiState.Success(sorted)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing developer apps", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load your software"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val libraryState: StateFlow<UiState<List<InstalledApp>>> =
        repository.observeUserLibrary(currentUserId)
            .map<List<InstalledApp>, UiState<List<InstalledApp>>> { list ->
                val sorted = list.sortedByDescending { it.updatedAt ?: it.installedAt ?: Timestamp(0, 0) }
                UiState.Success(sorted)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user library", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load your library"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myDeveloperProfileState: StateFlow<UiState<DeveloperProfile?>> =
        repository.observeDeveloperProfile(currentUserId)
            .map<DeveloperProfile?, UiState<DeveloperProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing developer profile", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load developer profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myPrivateDeveloperAccount: StateFlow<PrivateDeveloperAccount?> =
        repository.observePrivateDeveloperAccount(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing private developer account", firestoreError ?: error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    private val _selectedAppId = MutableStateFlow<String?>(null)
    val selectedAppId: StateFlow<String?> = _selectedAppId.asStateFlow()

    val selectedAppReviewsState: StateFlow<UiState<List<AppReview>>> =
        _selectedAppId
            .flatMapLatest { appId ->
                if (appId.isNullOrBlank()) {
                    flowOf<UiState<List<AppReview>>>(UiState.Success(emptyList()))
                } else {
                    repository.observeAppReviews(appId)
                        .map<List<AppReview>, UiState<List<AppReview>>> { reviews ->
                            UiState.Success(
                                reviews.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                            )
                        }
                        .catch { error ->
                            val firestoreError = unwrapFirestoreException(error)
                            if (firestoreError == null && error is CancellationException) throw error
                            val rootError = firestoreError ?: error
                            Log.w(TAG, "Error observing reviews for $appId", rootError)
                            emit(UiState.Error(rootError.message ?: "Unable to load reviews"))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGameCategory = MutableStateFlow<String?>(null)
    val selectedGameCategory: StateFlow<String?> = _selectedGameCategory.asStateFlow()

    private val _selectedAppCategory = MutableStateFlow<String?>(null)
    val selectedAppCategory: StateFlow<String?> = _selectedAppCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(CatalogSortOption.POPULAR)
    val sortOption: StateFlow<CatalogSortOption> = _sortOption.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _viewedStudioProfile = MutableStateFlow<DeveloperProfile?>(null)
    val viewedStudioProfile: StateFlow<DeveloperProfile?> = _viewedStudioProfile.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGameCategory(category: String?) {
        _selectedGameCategory.value = category
    }

    fun selectAppCategory(category: String?) {
        _selectedAppCategory.value = category
    }

    fun updateSortOption(option: CatalogSortOption) {
        _sortOption.value = option
    }

    fun selectAppForDetail(appId: String?) {
        _selectedAppId.value = appId
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun dismissStudioProfileSheet() {
        _viewedStudioProfile.value = null
    }

    fun inspectDeveloperStudio(developerId: String, fallbackName: String) {
        viewModelScope.launch {
            val result = repository.getDeveloperProfileOnce(developerId)
            result.onSuccess { profile ->
                _viewedStudioProfile.value = profile ?: DeveloperProfile(
                    id = developerId,
                    userId = developerId,
                    studioName = fallbackName,
                    tagline = "Verified PulseStore Publisher",
                    bio = "Independent creator distributing software on PulseStore.",
                    specialty = "Indie Software",
                    avatarPreset = "rocket"
                )
            }.onFailure {
                _snackbarMessage.value = "Could not load studio profile."
            }
        }
    }

    fun seedStarterCatalog() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedStarterCatalog()
            _isBusy.value = false
            result.onSuccess { count ->
                _snackbarMessage.value = if (count > 0) {
                    "Loaded $count featured games & apps into PulseStore."
                } else {
                    "Storefront catalog is already active."
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Failed to initialize storefront catalog."
            }
        }
    }

    fun startDownloadOrUpdate(app: AppListing, isUpdate: Boolean) {
        if (_downloadProgress.value.containsKey(app.id)) return
        viewModelScope.launch {
            _downloadProgress.update { it + (app.id to 0.1f) }
            val steps = listOf(0.28f, 0.54f, 0.79f, 0.96f)
            for (step in steps) {
                delay(180L)
                _downloadProgress.update { it + (app.id to step) }
            }
            val result = if (isUpdate) {
                repository.updateInstalledApp(app)
            } else {
                repository.installApp(app)
            }
            _downloadProgress.update { it - app.id }
            result.onSuccess {
                _snackbarMessage.value = if (isUpdate) {
                    "Updated ${app.title} to v${app.versionName}"
                } else {
                    "Installed ${app.title} to your Library"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Download failed. Please try again."
            }
        }
    }

    fun uninstallFromLibrary(appId: String, title: String) {
        viewModelScope.launch {
            val result = repository.uninstallApp(appId)
            result.onSuccess {
                _snackbarMessage.value = "Uninstalled $title"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not uninstall $title."
            }
        }
    }

    fun submitReview(
        appId: String,
        rating: Int,
        comment: String,
        existingReview: AppReview?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitReview(
                appId = appId,
                authorName = currentUserDisplayName,
                rating = rating,
                comment = comment,
                existingReview = existingReview
            )
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = if (existingReview == null) "Review posted!" else "Review updated!"
                onDone()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Unable to submit review."
            }
        }
    }

    fun deleteReview(appId: String, review: AppReview) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteReview(appId, review)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Review removed"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Unable to remove review."
            }
        }
    }

    fun saveDeveloperAccount(
        input: DeveloperRegistrationInput,
        isUpdate: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.registerOrUpdateDeveloperAccount(input, isUpdate)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = if (isUpdate) {
                    "Developer studio profile updated!"
                } else {
                    "Developer account created! Ready to upload software."
                }
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Failed to save developer profile."
            }
        }
    }

    fun saveSoftwareListing(
        input: AppDraftInput,
        studioName: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveAppListing(input, studioName)
            _isBusy.value = false
            result.onSuccess { id ->
                _snackbarMessage.value = if (input.existingId == null) {
                    if (input.published) "Published ${input.title} to PulseStore!" else "Saved ${input.title} as draft."
                } else {
                    "Updated ${input.title} (v${input.versionName})"
                }
                onSuccess(id)
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Unable to save software listing."
            }
        }
    }

    fun toggleAppPublished(app: AppListing) {
        viewModelScope.launch {
            val next = !app.published
            val result = repository.setAppPublished(app.id, next)
            result.onSuccess {
                _snackbarMessage.value = if (next) {
                    "${app.title} is now live in the store"
                } else {
                    "${app.title} moved to Drafts"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Unable to change visibility."
            }
        }
    }

    fun deleteSoftwareListing(app: AppListing) {
        viewModelScope.launch {
            val result = repository.deleteAppListing(app.id)
            result.onSuccess {
                _snackbarMessage.value = "Deleted ${app.title}"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Unable to delete software."
            }
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
