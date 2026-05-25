package ca.quantumform.mileage

import android.content.Context
import ca.quantumform.mileage.core.LedgerSnapshot
import ca.quantumform.mileage.core.TripLeg
import ca.quantumform.mileage.core.TripPurpose
import ca.quantumform.mileage.core.TripSource
import ca.quantumform.mileage.core.Vehicle
import java.io.File
import kotlin.time.Instant
import org.json.JSONArray
import org.json.JSONObject

class LocalLedgerStore(context: Context) {
    private val file = File(context.filesDir, "qf-mileage-ledger.json")

    fun load(): LedgerSnapshot {
        if (!file.exists()) {
            return LedgerSnapshot(vehicles = listOf(defaultVehicle()))
        }

        val root = JSONObject(file.readText())
        val vehicles = root.optJSONArray("vehicles").toVehicles()
        val trips = root.optJSONArray("trips").toTrips()

        return LedgerSnapshot(
            vehicles = vehicles.ifEmpty { listOf(defaultVehicle()) },
            trips = trips
        )
    }

    fun save(snapshot: LedgerSnapshot) {
        val root = JSONObject()
            .put("vehicles", snapshot.vehicles.toJsonVehicles())
            .put("trips", snapshot.trips.toJsonTrips())
        file.writeText(root.toString(2))
    }

    fun exportCsv(csv: String): File {
        val export = File(file.parentFile, "qf-mileage-logbook.csv")
        export.writeText(csv)
        return export
    }

    private fun defaultVehicle() = Vehicle(
        id = "vehicle-default",
        displayName = "Default vehicle"
    )

    private fun JSONArray?.toVehicles(): List<Vehicle> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            Vehicle(
                id = item.getString("id"),
                displayName = item.getString("displayName"),
                bluetoothDeviceIds = item.optJSONArray("bluetoothDeviceIds").toStringSet()
            )
        }
    }

    private fun JSONArray?.toTrips(): List<TripLeg> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            TripLeg(
                id = item.getString("id"),
                startedAt = Instant.parse(item.getString("startedAt")),
                endedAt = Instant.parse(item.getString("endedAt")),
                originLabel = item.getString("originLabel"),
                destinationLabel = item.getString("destinationLabel"),
                kilometres = item.getDouble("kilometres"),
                source = TripSource.valueOf(item.getString("source")),
                vehicleId = item.optString("vehicleId").ifBlank { null },
                purpose = TripPurpose.valueOf(item.getString("purpose")),
                evidenceNote = item.getString("evidenceNote"),
                adjustmentNote = item.optString("adjustmentNote").ifBlank { null }
            )
        }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return List(length()) { getString(it) }.toSet()
    }

    private fun List<Vehicle>.toJsonVehicles(): JSONArray {
        val array = JSONArray()
        forEach { vehicle ->
            array.put(
                JSONObject()
                    .put("id", vehicle.id)
                    .put("displayName", vehicle.displayName)
                    .put("bluetoothDeviceIds", JSONArray(vehicle.bluetoothDeviceIds.toList()))
            )
        }
        return array
    }

    private fun List<TripLeg>.toJsonTrips(): JSONArray {
        val array = JSONArray()
        forEach { trip ->
            array.put(
                JSONObject()
                    .put("id", trip.id)
                    .put("startedAt", trip.startedAt.toString())
                    .put("endedAt", trip.endedAt.toString())
                    .put("originLabel", trip.originLabel)
                    .put("destinationLabel", trip.destinationLabel)
                    .put("kilometres", trip.kilometres)
                    .put("source", trip.source.name)
                    .put("vehicleId", trip.vehicleId.orEmpty())
                    .put("purpose", trip.purpose.name)
                    .put("evidenceNote", trip.evidenceNote)
                    .put("adjustmentNote", trip.adjustmentNote.orEmpty())
            )
        }
        return array
    }
}
