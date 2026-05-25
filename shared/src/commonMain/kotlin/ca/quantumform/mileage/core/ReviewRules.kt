package ca.quantumform.mileage.core

data class CraReviewDecision(
    val claimableKilometres: Double,
    val selectedTreatment: CraTreatment,
    val noteRequired: Boolean,
    val auditSummary: String
)

enum class CraTreatment {
    ActualBusinessLegs,
    IntendedBusinessRoute,
    SplitMixedTrip,
    ExcludedPersonal
}

object CraReviewRules {
    fun decisionFor(legs: List<TripLeg>, treatment: CraTreatment, userNote: String?): CraReviewDecision {
        require(legs.isNotEmpty()) { "At least one trip leg is required for CRA review." }

        val noteRequired = treatment != CraTreatment.ActualBusinessLegs || legs.any { it.purpose == TripPurpose.Mixed }
        if (noteRequired && userNote.isNullOrBlank()) {
            return CraReviewDecision(
                claimableKilometres = 0.0,
                selectedTreatment = treatment,
                noteRequired = true,
                auditSummary = "User note required before this mixed or adjusted trip can be exported."
            )
        }

        val claimable = when (treatment) {
            CraTreatment.ActualBusinessLegs -> legs
                .filter { it.purpose == TripPurpose.Business }
                .sumOf { it.kilometres }
            CraTreatment.IntendedBusinessRoute,
            CraTreatment.SplitMixedTrip -> legs
                .filter { it.purpose != TripPurpose.Personal }
                .sumOf { it.kilometres }
            CraTreatment.ExcludedPersonal -> 0.0
        }

        return CraReviewDecision(
            claimableKilometres = claimable,
            selectedTreatment = treatment,
            noteRequired = noteRequired,
            auditSummary = userNote?.takeIf { it.isNotBlank() } ?: "Actual business legs only."
        )
    }
}
