package ca.quantumform.mileage.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class TripLedgerServiceTest {
    @Test
    fun summarySeparatesBusinessPersonalAndReviewKilometres() {
        val snapshot = LedgerSnapshot(
            trips = listOf(
                trip("1", 10.0, TripPurpose.Business),
                trip("2", 7.5, TripPurpose.Personal),
                trip("3", 4.25, TripPurpose.NeedsReview)
            )
        )

        val summary = TripLedgerService.summary(snapshot)

        assertEquals(3, summary.totalTrips)
        assertEquals(1, summary.needsReview)
        assertEquals(10.0, summary.businessKilometres)
        assertEquals(7.5, summary.personalKilometres)
        assertEquals(4.25, summary.mixedKilometres)
    }

    @Test
    fun csvExportQuotesCellsForCraStyleLogbook() {
        val snapshot = LedgerSnapshot(
            vehicles = listOf(Vehicle("vehicle-1", "Work vehicle")),
            trips = listOf(
                trip("1", 12.34, TripPurpose.Business, destination = "Client, Warehouse")
            )
        )

        val csv = CsvMileageExporter.export(snapshot)

        assertTrue(csv.contains("date,origin,destination,vehicle,job,category,purpose,kilometres"))
        assertTrue(csv.contains("\"Client, Warehouse\""))
        assertTrue(csv.contains("Work vehicle"))
    }

    @Test
    fun reviewActionsCanClassifyAndDeleteTrips() {
        val snapshot = LedgerSnapshot(trips = listOf(trip("1", 8.0, TripPurpose.NeedsReview)))

        val classified = TripLedgerService.updateTripPurpose(
            snapshot = snapshot,
            tripId = "1",
            purpose = TripPurpose.Business,
            reviewNote = "Client visit",
            jobLabel = "Warehouse install",
            category = "Client work"
        )

        assertEquals(TripPurpose.Business, classified.trips.single().purpose)
        assertEquals("Warehouse install", classified.trips.single().jobLabel)
        assertEquals("Client work", classified.trips.single().category)
        assertEquals("Client visit", classified.trips.single().adjustmentNote)

        val empty = TripLedgerService.deleteTrip(classified, "1")
        assertEquals(0, empty.trips.size)
    }

    @Test
    fun deletingVehiclePreservesTripsAndClearsVehicleReference() {
        val snapshot = LedgerSnapshot(
            vehicles = listOf(Vehicle("vehicle-1", "Work vehicle")),
            trips = listOf(trip("1", 8.0, TripPurpose.Business))
        )

        val updated = TripLedgerService.deleteVehicle(snapshot, "vehicle-1")

        assertEquals(emptyList(), updated.vehicles)
        assertEquals(1, updated.trips.size)
        assertEquals(null, updated.trips.single().vehicleId)
    }

    @Test
    fun tripsCanBeFilteredByDateRangeForReview() {
        val snapshot = LedgerSnapshot(
            trips = listOf(
                trip("1", 8.0, TripPurpose.NeedsReview, startedAt = "2026-05-20T16:00:00Z"),
                trip("2", 9.0, TripPurpose.NeedsReview, startedAt = "2026-05-25T16:00:00Z"),
                trip("3", 10.0, TripPurpose.NeedsReview, startedAt = "2026-05-30T16:00:00Z")
            )
        )

        val filtered = TripLedgerService.tripsInDateRange(snapshot, "2026-05-21", "2026-05-29")

        assertEquals(listOf("2"), filtered.map { it.id })
    }

    private fun trip(
        id: String,
        kilometres: Double,
        purpose: TripPurpose,
        destination: String = "Client",
        startedAt: String = "2026-05-25T16:00:00Z"
    ) = TripLeg(
        id = id,
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(startedAt).plus(30.minutes),
        originLabel = "Office",
        destinationLabel = destination,
        kilometres = kilometres,
        source = TripSource.Manual,
        vehicleId = "vehicle-1",
        purpose = purpose,
        evidenceNote = "Manual entry"
    )
}
