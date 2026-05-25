package ca.quantumform.mileage.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

        assertTrue(csv.contains("date,origin,destination,vehicle,purpose,kilometres"))
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
            reviewNote = "Client visit"
        )

        assertEquals(TripPurpose.Business, classified.trips.single().purpose)
        assertEquals("Client visit", classified.trips.single().adjustmentNote)

        val empty = TripLedgerService.deleteTrip(classified, "1")
        assertEquals(0, empty.trips.size)
    }

    private fun trip(
        id: String,
        kilometres: Double,
        purpose: TripPurpose,
        destination: String = "Client"
    ) = TripLeg(
        id = id,
        startedAt = Instant.parse("2026-05-25T16:00:00Z"),
        endedAt = Instant.parse("2026-05-25T16:30:00Z"),
        originLabel = "Office",
        destinationLabel = destination,
        kilometres = kilometres,
        source = TripSource.Manual,
        vehicleId = "vehicle-1",
        purpose = purpose,
        evidenceNote = "Manual entry"
    )
}
