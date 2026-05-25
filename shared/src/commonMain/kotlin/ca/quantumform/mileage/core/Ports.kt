package ca.quantumform.mileage.core

interface RouteDistanceProvider {
    val provider: RouteProvider
    suspend fun calculate(request: RouteDistanceRequest): RouteDistanceResult
}

interface BackupRepository {
    suspend fun createEncryptedBackup(): BackupManifest
    suspend fun restoreEncryptedBackup(backupId: String): BackupManifest
}

interface ErpNextMileageExporter {
    suspend fun exportTrips(trips: List<TripLeg>): ExportResult
}

data class ExportResult(
    val exportedCount: Int,
    val destination: String,
    val evidenceSummary: String
)
