package ca.quantumform.mileage.core

data class ReviewQueueSummary(
    val totalTrips: Int,
    val needsReview: Int,
    val businessKilometres: Double,
    val personalKilometres: Double,
    val mixedKilometres: Double
)

object TripLedgerService {
    fun summary(snapshot: LedgerSnapshot): ReviewQueueSummary {
        return ReviewQueueSummary(
            totalTrips = snapshot.trips.size,
            needsReview = snapshot.trips.count { it.purpose == TripPurpose.NeedsReview || it.purpose == TripPurpose.Mixed },
            businessKilometres = snapshot.trips
                .filter { it.purpose == TripPurpose.Business }
                .sumOf { it.kilometres },
            personalKilometres = snapshot.trips
                .filter { it.purpose == TripPurpose.Personal }
                .sumOf { it.kilometres },
            mixedKilometres = snapshot.trips
                .filter { it.purpose == TripPurpose.Mixed || it.purpose == TripPurpose.NeedsReview }
                .sumOf { it.kilometres }
        )
    }

    fun upsertTrip(snapshot: LedgerSnapshot, trip: TripLeg): LedgerSnapshot {
        val trips = snapshot.trips.filterNot { it.id == trip.id } + trip
        return snapshot.copy(trips = trips.sortedByDescending { it.startedAt })
    }

    fun upsertVehicle(snapshot: LedgerSnapshot, vehicle: Vehicle): LedgerSnapshot {
        val vehicles = snapshot.vehicles.filterNot { it.id == vehicle.id } + vehicle
        return snapshot.copy(vehicles = vehicles.sortedBy { it.displayName.lowercase() })
    }

    fun updateTripPurpose(
        snapshot: LedgerSnapshot,
        tripId: String,
        purpose: TripPurpose,
        reviewNote: String,
        jobLabel: String? = null,
        category: String? = null
    ): LedgerSnapshot {
        val trips = snapshot.trips.map { trip ->
            if (trip.id != tripId) {
                trip
            } else {
                trip.copy(
                    purpose = purpose,
                    jobLabel = jobLabel?.ifBlank { null } ?: trip.jobLabel,
                    category = category?.ifBlank { null } ?: trip.category,
                    adjustmentNote = reviewNote.ifBlank { trip.adjustmentNote }
                )
            }
        }
        return snapshot.copy(trips = trips)
    }

    fun deleteTrip(snapshot: LedgerSnapshot, tripId: String): LedgerSnapshot {
        return snapshot.copy(trips = snapshot.trips.filterNot { it.id == tripId })
    }

    fun tripsInDateRange(snapshot: LedgerSnapshot, fromDate: String?, toDate: String?): List<TripLeg> {
        return snapshot.trips.filter { trip ->
            val tripDate = trip.startedAt.toString().take(10)
            val afterStart = fromDate.isNullOrBlank() || tripDate >= fromDate
            val beforeEnd = toDate.isNullOrBlank() || tripDate <= toDate
            afterStart && beforeEnd
        }
    }
}
