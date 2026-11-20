package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class AppListing(
    @DocumentId val id: String = "",
    val developerId: String = "",
    val developerName: String = "",
    val title: String = "",
    val tagline: String = "",
    val description: String = "",
    val appType: String = StoreConstants.TYPE_APP,
    val category: String = "Tools",
    val versionName: String = "1.0.0",
    val sizeMb: Double = 24.0,
    val ageRating: String = "Everyone",
    val accentColorHex: String = "#00875A",
    val iconPreset: String = "bolt",
    val iconUrl: String? = null,
    val downloadUrl: String? = null,
    val packageName: String? = null,
    val tags: List<String> = emptyList(),
    val releaseNotes: String = "",
    val published: Boolean = true,
    val starterCatalog: Boolean = false,
    val downloadsCount: Long = 0L,
    val ratingSum: Long = 0L,
    val ratingCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val averageRating: Double
        get() = if (ratingCount > 0L) ratingSum.toDouble() / ratingCount.toDouble() else 0.0

    @get:Exclude
    val isGame: Boolean
        get() = appType == StoreConstants.TYPE_GAME
}

@Keep
@IgnoreExtraProperties
data class DeveloperProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studioName: String = "",
    val tagline: String = "",
    val bio: String = "",
    val specialty: String = "Indie Software",
    val avatarPreset: String = "rocket",
    val websiteUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateDeveloperAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studioName: String = "",
    val contactEmail: String = "",
    val supportUrl: String? = null,
    val termsAccepted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InstalledApp(
    @DocumentId val id: String = "",
    val appId: String = "",
    val title: String = "",
    val developerName: String = "",
    val category: String = "Tools",
    val appType: String = StoreConstants.TYPE_APP,
    val installedVersion: String = "1.0.0",
    val sizeMb: Double = 24.0,
    val accentColorHex: String = "#00875A",
    val iconPreset: String = "bolt",
    val iconUrl: String? = null,
    val downloadUrl: String? = null,
    @ServerTimestamp val installedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppReview(
    @DocumentId val id: String = "",
    val appId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AppDraftInput(
    val existingId: String? = null,
    val title: String,
    val tagline: String,
    val description: String,
    val appType: String,
    val category: String,
    val versionName: String,
    val sizeMb: Double,
    val ageRating: String,
    val accentColorHex: String,
    val iconPreset: String,
    val iconUrl: String?,
    val downloadUrl: String?,
    val packageName: String?,
    val tags: List<String>,
    val releaseNotes: String,
    val published: Boolean
)

data class DeveloperRegistrationInput(
    val studioName: String,
    val tagline: String,
    val bio: String,
    val specialty: String,
    val avatarPreset: String,
    val websiteUrl: String?,
    val contactEmail: String,
    val supportUrl: String?
)

object StoreConstants {
    const val TYPE_GAME = "game"
    const val TYPE_APP = "app"

    val gameCategories = listOf(
        "Action",
        "Arcade",
        "Puzzle",
        "Racing",
        "RPG",
        "Strategy"
    )

    val appCategories = listOf(
        "Productivity",
        "Tools",
        "Music & Audio",
        "Photography",
        "Education",
        "Social",
        "Finance",
        "Health & Fitness"
    )

    val allCategories = gameCategories + appCategories

    val ageRatings = listOf(
        "Everyone",
        "Everyone 10+",
        "Teen",
        "Mature 17+"
    )

    val specialties = listOf(
        "Indie Games",
        "Productivity Tools",
        "Creative Apps",
        "Audio & Music",
        "Developer Utilities",
        "Education & Learning"
    )

    val iconPresets = listOf(
        "gamepad",
        "rocket",
        "bolt",
        "music",
        "camera",
        "palette",
        "shield",
        "book",
        "chart",
        "compass",
        "code",
        "heart"
    )

    val accentPaletteHex = listOf(
        "#00875A",
        "#2563EB",
        "#EA580C",
        "#7C3AED",
        "#DB2777",
        "#0891B2",
        "#D97706",
        "#0F766E"
    )

    fun categoriesForType(appType: String): List<String> =
        if (appType == TYPE_GAME) gameCategories else appCategories
}

data class StarterSeedSpec(
    val docId: String,
    val title: String,
    val developerName: String,
    val tagline: String,
    val description: String,
    val appType: String,
    val category: String,
    val versionName: String,
    val sizeMb: Double,
    val ageRating: String,
    val accentColorHex: String,
    val iconPreset: String,
    val tags: List<String>,
    val releaseNotes: String,
    val packageName: String
)

object StarterCatalogFactory {
    val entries: List<StarterSeedSpec> = listOf(
        StarterSeedSpec(
            docId = "starter_neon_drift",
            title = "Neon Drift Horizon",
            developerName = "Velocity Labs",
            tagline = "Zero-gravity canyon racing at 120fps",
            description = "Master precision drift boosts across twilight skyways, customize hovering street racers, and climb weekly time-trial leaderboards. Designed with crisp tactile touch steering and offline campaign stages.",
            appType = StoreConstants.TYPE_GAME,
            category = "Racing",
            versionName = "2.4.1",
            sizeMb = 186.0,
            ageRating = "Everyone 10+",
            accentColorHex = "#0891B2",
            iconPreset = "rocket",
            tags = listOf("racing", "arcade", "offline", "120fps"),
            releaseNotes = "Added Solar Canyon circuit, improved drift slipstream tuning, and reduced package footprint.",
            packageName = "io.velocitylabs.neondrift"
        ),
        StarterSeedSpec(
            docId = "starter_aether_tactics",
            title = "Aether Grid Tactics",
            developerName = "Monolith Indie",
            tagline = "Turn-based mech strategy on floating islands",
            description = "Command a squad of modular clockwork mechs across procedural hex battlegrounds. Chain terrain combos, forecast enemy artillery, and unlock 18 pilot loadouts in bite-sized 10-minute tactical missions.",
            appType = StoreConstants.TYPE_GAME,
            category = "Strategy",
            versionName = "1.8.0",
            sizeMb = 142.0,
            ageRating = "Everyone 10+",
            accentColorHex = "#7C3AED",
            iconPreset = "shield",
            tags = listOf("strategy", "roguelite", "turn-based"),
            releaseNotes = "New Frost Spire biome, 3 new support drones, and quick undo move button.",
            packageName = "io.monolithindie.aethergrid"
        ),
        StarterSeedSpec(
            docId = "starter_prism_shift",
            title = "Prism Shift",
            developerName = "Lumen Craft",
            tagline = "Relaxing optical beam & mirror puzzles",
            description = "Bend polychromatic light rays through rotating crystal prisms to illuminate 160 handcrafted geometric sanctuaries. No timers, adaptive ambient soundtrack, and colorblind-friendly light patterns.",
            appType = StoreConstants.TYPE_GAME,
            category = "Puzzle",
            versionName = "3.1.2",
            sizeMb = 64.0,
            ageRating = "Everyone",
            accentColorHex = "#00875A",
            iconPreset = "compass",
            tags = listOf("puzzle", "minimalist", "zen"),
            releaseNotes = "Added 20 Mirror Echo bonus puzzles and haptic rotation feedback.",
            packageName = "dev.lumencraft.prismshift"
        ),
        StarterSeedSpec(
            docId = "starter_starbound_blade",
            title = "Starbound Blade",
            developerName = "Ember Fox Interactive",
            tagline = "Fast-paced pixel action RPG adventure",
            description = "Explore ruined orbital citadels, parry laser guardians, and forge elemental relic blades in a handcrafted pixel-art action RPG built for fluid mobile combat and boss rush challenges.",
            appType = StoreConstants.TYPE_GAME,
            category = "RPG",
            versionName = "1.5.4",
            sizeMb = 215.0,
            ageRating = "Teen",
            accentColorHex = "#EA580C",
            iconPreset = "gamepad",
            tags = listOf("rpg", "action", "pixel-art"),
            releaseNotes = "Introduced Boss Rush arena and customizable on-screen control layout.",
            packageName = "com.emberfox.starboundblade"
        ),
        StarterSeedSpec(
            docId = "starter_focus_deck",
            title = "FocusDeck Planner",
            developerName = "Northstar Works",
            tagline = "Time-blocked daily workspace & habit loops",
            description = "Plan deep work sprints, organize project milestones, and review weekly focus trends in one distraction-free calendar deck. Supports Pomodoro intervals, markdown notes, and crisp dark mode.",
            appType = StoreConstants.TYPE_APP,
            category = "Productivity",
            versionName = "4.2.0",
            sizeMb = 28.0,
            ageRating = "Everyone",
            accentColorHex = "#2563EB",
            iconPreset = "bolt",
            tags = listOf("planner", "focus", "tasks"),
            releaseNotes = "New weekly focus heatmap and faster gesture rescheduling.",
            packageName = "co.northstarworks.focusdeck"
        ),
        StarterSeedSpec(
            docId = "starter_wave_synth",
            title = "WaveLab Pocket Synth",
            developerName = "Klangwerk Audio",
            tagline = "4-track groovebox & analog synthesizer",
            description = "Sketch beats, layer lush wavetable chords, and tweak real-time tape delay filters wherever inspiration strikes. Export lossless WAV loops and build custom drum kits on the go.",
            appType = StoreConstants.TYPE_APP,
            category = "Music & Audio",
            versionName = "2.0.3",
            sizeMb = 52.0,
            ageRating = "Everyone",
            accentColorHex = "#DB2777",
            iconPreset = "music",
            tags = listOf("synth", "beats", "audio"),
            releaseNotes = "Added granular reverb module and MIDI clock export.",
            packageName = "audio.klangwerk.wavelab"
        ),
        StarterSeedSpec(
            docId = "starter_chroma_cam",
            title = "ChromaFilm Raw Studio",
            developerName = "Aperture Nine",
            tagline = "Analog film emulation & precision color grading",
            description = "Bring classic medium-format color science to your photography with non-destructive HSL curves, halation controls, and custom LUT presets tailored for golden hour and street scenes.",
            appType = StoreConstants.TYPE_APP,
            category = "Photography",
            versionName = "1.9.1",
            sizeMb = 41.0,
            ageRating = "Everyone",
            accentColorHex = "#D97706",
            iconPreset = "camera",
            tags = listOf("photo", "film", "editor"),
            releaseNotes = "Added 4 monochrome silver-gelatin profiles and split-tone curves.",
            packageName = "studio.aperturenine.chromafilm"
        ),
        StarterSeedSpec(
            docId = "starter_dev_bench",
            title = "PacketBench Utilities",
            developerName = "Kite Terminal",
            tagline = "JSON inspector, regex lab & color tokens",
            description = "An essential pocket Swiss-army knife for engineers and designers. Format JSON payloads, test regular expressions live, inspect contrast ratios, and convert Unix timestamps offline.",
            appType = StoreConstants.TYPE_APP,
            category = "Tools",
            versionName = "3.0.0",
            sizeMb = 16.0,
            ageRating = "Everyone",
            accentColorHex = "#0F766E",
            iconPreset = "code",
            tags = listOf("developer", "utilities", "offline"),
            releaseNotes = "Added Base64/JWT inspector and WCAG AAA contrast checker.",
            packageName = "dev.kiteterminal.packetbench"
        )
    )
}
