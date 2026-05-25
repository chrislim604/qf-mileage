package ca.quantumform.mileage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.quantumform.mileage.core.AppEntitlement
import ca.quantumform.mileage.core.CsvMileageExporter
import ca.quantumform.mileage.core.LedgerSnapshot
import ca.quantumform.mileage.core.MileageAppStatus
import ca.quantumform.mileage.core.TripLedgerService
import ca.quantumform.mileage.core.TripLeg
import ca.quantumform.mileage.core.TripPurpose
import ca.quantumform.mileage.core.TripSource
import ca.quantumform.mileage.core.Vehicle
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LocalLedgerStore(this)
        setContent {
            QfMileageApp(store = store)
        }
    }
}

@Composable
private fun QfMileageApp(store: LocalLedgerStore) {
    var snapshot by remember { mutableStateOf(store.load()) }
    var exportedPath by remember { mutableStateOf<String?>(null) }
    val status = MileageAppStatus(
        displayName = "QF Mileage",
        version = "0.2.0",
        entitlement = AppEntitlement.FreeWithAds
    )

    fun persist(next: LedgerSnapshot) {
        snapshot = next
        store.save(next)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Header(status)
                SummaryPanel(snapshot)
                AddVehiclePanel(snapshot) { persist(TripLedgerService.upsertVehicle(snapshot, it)) }
                AddTripPanel(snapshot) { persist(TripLedgerService.upsertTrip(snapshot, it)) }
                ReviewQueue(snapshot)
                TripList(snapshot)
                ExportPanel(exportedPath = exportedPath) {
                    exportedPath = store.exportCsv(CsvMileageExporter.export(snapshot)).absolutePath
                }
            }
        }
    }
}

@Composable
private fun Header(status: MileageAppStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = status.displayName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Local trip ledger MVP - Version ${status.version} - ${status.entitlement.label}")
    }
}

@Composable
private fun SummaryPanel(snapshot: LedgerSnapshot) {
    val summary = TripLedgerService.summary(snapshot)
    Section(title = "Summary") {
        Text("Trips: ${summary.totalTrips}")
        Text("Needs review: ${summary.needsReview}")
        Text("Business km: ${summary.businessKilometres}")
        Text("Personal km: ${summary.personalKilometres}")
        Text("Review/mixed km: ${summary.mixedKilometres}")
    }
}

@Composable
private fun AddVehiclePanel(snapshot: LedgerSnapshot, onSave: (Vehicle) -> Unit) {
    var name by remember { mutableStateOf("") }
    Section(title = "Vehicles") {
        snapshot.vehicles.forEach { Text("- ${it.displayName}") }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Vehicle name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                onSave(Vehicle(id = "vehicle-${Clock.System.now().toEpochMilliseconds()}", displayName = name.trim()))
                name = ""
            }
        ) {
            Text("Add vehicle")
        }
    }
}

@Composable
private fun AddTripPanel(snapshot: LedgerSnapshot, onSave: (TripLeg) -> Unit) {
    var origin by remember { mutableStateOf("Home office") }
    var destination by remember { mutableStateOf("") }
    var kilometres by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf(TripPurpose.NeedsReview) }
    var note by remember { mutableStateOf("Manual entry") }
    var selectedVehicleId by remember(snapshot.vehicles) { mutableStateOf(snapshot.vehicles.firstOrNull()?.id) }

    Section(title = "Add manual trip") {
        VehicleButtons(
            vehicles = snapshot.vehicles,
            selectedVehicleId = selectedVehicleId,
            onChange = { selectedVehicleId = it }
        )
        OutlinedTextField(origin, { origin = it }, label = { Text("Origin") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(destination, { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = kilometres,
            onValueChange = { kilometres = it },
            label = { Text("Kilometres driven") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        PurposeButtons(purpose = purpose, onChange = { purpose = it })
        OutlinedTextField(note, { note = it }, label = { Text("Evidence / review note") }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = destination.isNotBlank() && kilometres.toDoubleOrNull() != null,
            onClick = {
                val now = Clock.System.now()
                onSave(
                    TripLeg(
                        id = "trip-${now.toEpochMilliseconds()}",
                        startedAt = now.minus(30.minutes),
                        endedAt = now,
                        originLabel = origin.trim(),
                        destinationLabel = destination.trim(),
                        kilometres = kilometres.toDouble(),
                        source = TripSource.Manual,
                        vehicleId = selectedVehicleId,
                        purpose = purpose,
                        evidenceNote = note.trim().ifBlank { "Manual entry" },
                        adjustmentNote = if (purpose == TripPurpose.Mixed || purpose == TripPurpose.NeedsReview) note else null
                    )
                )
                destination = ""
                kilometres = ""
                purpose = TripPurpose.NeedsReview
                note = "Manual entry"
            }
        ) {
            Text("Save trip")
        }
    }
}

@Composable
private fun VehicleButtons(vehicles: List<Vehicle>, selectedVehicleId: String?, onChange: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vehicle")
        vehicles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { vehicle ->
                    Button(onClick = { onChange(vehicle.id) }) {
                        Text(if (selectedVehicleId == vehicle.id) "* ${vehicle.displayName}" else vehicle.displayName)
                    }
                }
            }
        }
    }
}

@Composable
private fun PurposeButtons(purpose: TripPurpose, onChange: (TripPurpose) -> Unit) {
    val options = listOf(TripPurpose.Business, TripPurpose.Personal, TripPurpose.Mixed, TripPurpose.NeedsReview)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Purpose")
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    Button(onClick = { onChange(option) }) {
                        Text(if (purpose == option) "* ${option.name}" else option.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQueue(snapshot: LedgerSnapshot) {
    val reviewTrips = snapshot.trips.filter { it.purpose == TripPurpose.NeedsReview || it.purpose == TripPurpose.Mixed }
    Section(title = "Review queue") {
        if (reviewTrips.isEmpty()) {
            Text("No trips need review.")
        } else {
            reviewTrips.forEach {
                Text("${it.originLabel} to ${it.destinationLabel} - ${it.kilometres} km - ${it.purpose.name}")
                Text("Note: ${it.adjustmentNote ?: it.evidenceNote}")
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TripList(snapshot: LedgerSnapshot) {
    Section(title = "Trip ledger") {
        if (snapshot.trips.isEmpty()) {
            Text("No trips yet.")
        } else {
            snapshot.trips.forEach {
                val vehicleName = snapshot.vehicles.firstOrNull { vehicle -> vehicle.id == it.vehicleId }?.displayName ?: "No vehicle"
                Text("${it.startedAt.toString().take(10)} - ${it.originLabel} to ${it.destinationLabel}")
                Text("${it.kilometres} km - ${vehicleName} - ${it.purpose.name} - ${it.source.name}")
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExportPanel(exportedPath: String?, onExport: () -> Unit) {
    Section(title = "Export") {
        Text("Generate a local CRA-style CSV logbook from the current ledger.")
        Button(onClick = onExport) {
            Text("Export CSV")
        }
        exportedPath?.let {
            Text("Saved to $it")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}
