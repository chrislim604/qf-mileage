package ca.quantumform.mileage.core

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MileageAppStatus(
    val displayName: String,
    val version: String,
    val entitlement: AppEntitlement
)

@Serializable
enum class AppEntitlement(val label: String) {
    FreeWithAds("Free with ads"),
    FreeAdRemoved("Free app, ads removed"),
    PaidAdFree("Paid ad-free")
}

@Serializable
data class Vehicle(
    val id: String,
    val displayName: String,
    val bluetoothDeviceIds: Set<String> = emptySet()
)

@Serializable
data class PlaceTag(
    val id: String,
    val label: String,
    val kind: PlaceKind,
    val approved: Boolean
)

@Serializable
enum class PlaceKind {
    Home,
    Office,
    Warehouse,
    Client,
    Supplier,
    Personal,
    Custom
}

@Serializable
data class TripLeg(
    val id: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val originLabel: String,
    val destinationLabel: String,
    val kilometres: Double,
    val source: TripSource,
    val vehicleId: String?,
    val purpose: TripPurpose,
    val evidenceNote: String,
    val adjustmentNote: String? = null
)

@Serializable
enum class TripSource {
    Manual,
    GoogleTimelineImport,
    TripAssist,
    GoogleRoutesReconstruction
}

@Serializable
enum class TripPurpose {
    Business,
    Personal,
    Mixed,
    NeedsReview
}

@Serializable
data class RouteDistanceRequest(
    val origin: RoutePoint,
    val destination: RoutePoint,
    val waypoints: List<RoutePoint> = emptyList()
)

@Serializable
data class RoutePoint(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class RouteDistanceResult(
    val kilometres: Double,
    val provider: RouteProvider,
    val confidence: DistanceConfidence,
    val evidenceSummary: String
)

@Serializable
enum class RouteProvider {
    GoogleRoutes,
    AppleMapKit,
    Manual
}

@Serializable
enum class DistanceConfidence {
    High,
    Medium,
    Low,
    UserOverride
}

@Serializable
data class BackupManifest(
    val schemaVersion: Int,
    val createdAt: Instant,
    val encrypted: Boolean,
    val tripCount: Int,
    val vehicleCount: Int,
    val placeCount: Int
)

@Serializable
data class LedgerSnapshot(
    val vehicles: List<Vehicle> = emptyList(),
    val places: List<PlaceTag> = emptyList(),
    val trips: List<TripLeg> = emptyList()
)
