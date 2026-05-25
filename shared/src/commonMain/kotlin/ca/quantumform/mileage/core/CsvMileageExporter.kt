package ca.quantumform.mileage.core

object CsvMileageExporter {
    fun export(snapshot: LedgerSnapshot): String {
        val header = listOf(
            "date",
            "origin",
            "destination",
            "vehicle",
            "purpose",
            "kilometres",
            "source",
            "evidence_note",
            "adjustment_note"
        ).joinToString(",")

        val rows = snapshot.trips.sortedBy { it.startedAt }.map { trip ->
            val vehicle = snapshot.vehicles.firstOrNull { it.id == trip.vehicleId }?.displayName.orEmpty()
            listOf(
                trip.startedAt.toString().take(10),
                trip.originLabel,
                trip.destinationLabel,
                vehicle,
                trip.purpose.name,
                formatKilometres(trip.kilometres),
                trip.source.name,
                trip.evidenceNote,
                trip.adjustmentNote.orEmpty()
            ).joinToString(",") { csvCell(it) }
        }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun formatKilometres(value: Double): String {
        val rounded = (value * 100.0).toLong() / 100.0
        return rounded.toString()
    }

    private fun csvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
