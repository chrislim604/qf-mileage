package ca.quantumform.mileage.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CraReviewRulesTest {
    @Test
    fun actualBusinessLegsClaimsOnlyBusinessKilometres() {
        val decision = CraReviewRules.decisionFor(
            legs = listOf(
                leg("home", "dinner", 8.0, TripPurpose.Personal),
                leg("dinner", "client", 6.0, TripPurpose.Business),
                leg("client", "home", 12.0, TripPurpose.Business)
            ),
            treatment = CraTreatment.ActualBusinessLegs,
            userNote = null
        )

        assertEquals(18.0, decision.claimableKilometres)
        assertEquals(CraTreatment.ActualBusinessLegs, decision.selectedTreatment)
    }

    @Test
    fun mixedTripsRequireUserNotesBeforeAdjustedExport() {
        val decision = CraReviewRules.decisionFor(
            legs = listOf(leg("home", "client", 15.0, TripPurpose.Mixed)),
            treatment = CraTreatment.SplitMixedTrip,
            userNote = ""
        )

        assertTrue(decision.noteRequired)
        assertEquals(0.0, decision.claimableKilometres)
    }

    private fun leg(origin: String, destination: String, kilometres: Double, purpose: TripPurpose) = TripLeg(
        id = "$origin-$destination",
        startedAt = Instant.parse("2026-05-25T16:00:00Z"),
        endedAt = Instant.parse("2026-05-25T16:30:00Z"),
        originLabel = origin,
        destinationLabel = destination,
        kilometres = kilometres,
        source = TripSource.Manual,
        vehicleId = "vehicle-1",
        purpose = purpose,
        evidenceNote = "Test evidence"
    )
}
