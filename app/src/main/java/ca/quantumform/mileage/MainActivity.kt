package ca.quantumform.mileage

import android.content.Intent
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import ca.quantumform.mileage.core.AppEntitlement
import ca.quantumform.mileage.core.CsvMileageExporter
import ca.quantumform.mileage.core.GoogleTimelineImporter
import ca.quantumform.mileage.core.LedgerSnapshot
import ca.quantumform.mileage.core.MileageAppStatus
import ca.quantumform.mileage.core.PlaceKind
import ca.quantumform.mileage.core.PlaceTag
import ca.quantumform.mileage.core.TimelineImportResult
import ca.quantumform.mileage.core.TripLedgerService
import ca.quantumform.mileage.core.TripLeg
import ca.quantumform.mileage.core.TripPurpose
import ca.quantumform.mileage.core.TripSource
import ca.quantumform.mileage.core.Vehicle
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LocalLedgerStore(this)
        setContent {
            QfMileageApp(
                store = store,
                importTimelineFile = { uri -> importTimelineFile(uri) },
                isDeviceLocationEnabled = { isDeviceLocationEnabled() },
                openDeviceLocationSettings = { openDeviceLocationSettings() },
                openTimelineSettings = { openTimelineSettings() },
                shareFile = { file, mimeType -> shareFile(file, mimeType) }
            )
        }
    }

    private fun importTimelineFile(uri: Uri): TimelineImportResult {
        val jsonText = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return TimelineImportResult(emptyList(), 0, "Could not read the selected Timeline file.")
        return GoogleTimelineImporter.parse(jsonText)
    }

    private fun isDeviceLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun openDeviceLocationSettings() {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun openTimelineSettings() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myactivity.google.com/activitycontrols/location"))
        startActivity(intent)
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", file)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share QF Mileage export"))
    }
}

@Composable
private fun QfMileageApp(
    store: LocalLedgerStore,
    importTimelineFile: (Uri) -> TimelineImportResult,
    isDeviceLocationEnabled: () -> Boolean,
    openDeviceLocationSettings: () -> Unit,
    openTimelineSettings: () -> Unit,
    shareFile: (File, String) -> Unit
) {
    var snapshot by remember { mutableStateOf(store.load()) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var deviceLocationEnabled by remember { mutableStateOf(isDeviceLocationEnabled()) }
    var reviewFromDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var reviewToDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedTab by remember { mutableStateOf(AppTab.Trips) }
    var themeMode by remember { mutableStateOf(store.loadThemeMode()) }
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.FollowSystem -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val status = MileageAppStatus(
        displayName = "QF Mileage",
        version = BuildConfig.VERSION_NAME,
        entitlement = AppEntitlement.FreeWithAds
    )

    fun persist(next: LedgerSnapshot) {
        snapshot = next
        store.save(next)
    }

    fun persistThemeMode(next: ThemeMode) {
        themeMode = next
        store.saveThemeMode(next)
    }

    val timelineImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = try {
                importTimelineFile(uri)
            } catch (error: Exception) {
                TimelineImportResult(emptyList(), 0, "Timeline import failed: ${error.message ?: "unknown error"}")
            }
            val beforeCount = snapshot.trips.size
            val next = TripLedgerService.importTimelineTrips(snapshot, result.importedTrips)
            persist(next)
            val addedCount = next.trips.size - beforeCount
            importMessage = result.warning ?: "Imported $addedCount new review trips. Skipped ${result.skippedSegments} unsupported segments."
        }
    }

    MaterialTheme(colorScheme = if (useDarkTheme) quantumFormDarkScheme() else quantumFormLightScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Header(status)
                AppTabs(selectedTab = selectedTab, onSelect = { selectedTab = it })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    when (selectedTab) {
                        AppTab.Trips -> {
                            SummaryPanel(snapshot)
                            AddTripPanel(snapshot) { persist(TripLedgerService.upsertTrip(snapshot, it)) }
                            TripList(
                                snapshot = snapshot,
                                onDelete = { persist(TripLedgerService.deleteTrip(snapshot, it)) }
                            )
                        }

                        AppTab.Review -> {
                            DateRangePanel(
                                fromDate = reviewFromDate,
                                toDate = reviewToDate,
                                onFromDateChange = { reviewFromDate = it },
                                onToDateChange = { reviewToDate = it }
                            )
                            ReviewQueue(
                                snapshot = snapshot,
                                fromDate = reviewFromDate,
                                toDate = reviewToDate,
                                onClassify = { tripId, purpose, jobLabel, category ->
                                    persist(
                                        TripLedgerService.updateTripPurpose(
                                            snapshot = snapshot,
                                            tripId = tripId,
                                            purpose = purpose,
                                            reviewNote = "Reviewed in QF Mileage",
                                            jobLabel = jobLabel,
                                            category = category
                                        )
                                    )
                                },
                                onDelete = { persist(TripLedgerService.deleteTrip(snapshot, it)) }
                            )
                        }

                        AppTab.Import -> {
                            ImportPanel(
                                importMessage = importMessage,
                                deviceLocationEnabled = deviceLocationEnabled,
                                onRefreshTimelineStatus = {
                                    deviceLocationEnabled = isDeviceLocationEnabled()
                                    importMessage = "Device Location is ${if (deviceLocationEnabled) "on" else "off"}. Open Timeline settings to confirm Google Timeline is enabled for this account and device."
                                },
                                onOpenTimelineSettings = openTimelineSettings,
                                onOpenDeviceLocationSettings = openDeviceLocationSettings,
                                onImportTimeline = { timelineImportLauncher.launch(arrayOf("application/json", "text/*")) }
                            )
                        }

                        AppTab.Places -> {
                            PlacesPanel(
                                snapshot = snapshot,
                                onSave = { persist(TripLedgerService.upsertPlace(snapshot, it)) },
                                onDelete = { persist(TripLedgerService.deletePlace(snapshot, it)) },
                                onApplyTags = { persist(TripLedgerService.applyApprovedPlaceTags(snapshot)) }
                            )
                        }

                        AppTab.Vehicles -> {
                            AddVehiclePanel(
                                snapshot = snapshot,
                                onSave = { persist(TripLedgerService.upsertVehicle(snapshot, it)) },
                                onDelete = { persist(TripLedgerService.deleteVehicle(snapshot, it)) }
                            )
                        }

                        AppTab.Export -> {
                            ExportPanel(exportMessage = exportMessage) {
                                val csvFile = store.exportCsv(CsvMileageExporter.export(snapshot))
                                shareFile(csvFile, "text/csv")
                                exportMessage = "CSV ready: ${csvFile.name}"
                            }
                            BackupPanel(
                                exportMessage = exportMessage,
                                onPlainBackup = {
                                    val backupFile = store.exportBackup(snapshot)
                                    shareFile(backupFile, "application/json")
                                    exportMessage = "Backup ready: ${backupFile.name}"
                                },
                                onEncryptedBackup = {
                                    val backupFile = store.exportEncryptedBackup(snapshot)
                                    shareFile(backupFile, "application/octet-stream")
                                    exportMessage = "Encrypted backup ready: ${backupFile.name}"
                                },
                                onRestoreLatest = {
                                    val result = try {
                                        store.restoreLatestEncryptedBackup()
                                    } catch (error: Exception) {
                                        BackupRestoreResult(null, "Encrypted restore failed: ${error.message ?: "unknown error"}")
                                    }
                                    result.snapshot?.let { snapshot = it }
                                    exportMessage = result.message
                                }
                            )
                        }

                        AppTab.Settings -> {
                            SettingsPanel(themeMode = themeMode, onThemeModeChange = { persistThemeMode(it) })
                        }
                    }
                }
            }
        }
    }
}

private enum class AppTab(val label: String) {
    Trips("Trips"),
    Review("Review"),
    Import("Import"),
    Places("Places"),
    Vehicles("Vehicles"),
    Export("Export"),
    Settings("Settings")
}

private val QuantumFormGold = Color(0xFFC58A2F)
private val QuantumFormBlack = Color(0xFF111111)
private val QuantumFormWhite = Color(0xFFFFFFFF)
private val QuantumFormPaper = Color(0xFFF4F7F6)
private val QuantumFormLine = Color(0xFFD9E2E4)
private val QuantumFormMuted = Color(0xFF5F6D73)
private val QuantumFormDarkSurface = Color(0xFF202C31)

private fun quantumFormLightScheme() = lightColorScheme(
    primary = QuantumFormGold,
    onPrimary = QuantumFormBlack,
    primaryContainer = Color(0xFFF2E4CF),
    onPrimaryContainer = QuantumFormBlack,
    secondary = QuantumFormBlack,
    onSecondary = QuantumFormWhite,
    secondaryContainer = QuantumFormLine,
    onSecondaryContainer = QuantumFormBlack,
    background = QuantumFormPaper,
    onBackground = QuantumFormBlack,
    surface = QuantumFormWhite,
    onSurface = QuantumFormBlack,
    surfaceVariant = QuantumFormLine,
    onSurfaceVariant = QuantumFormMuted,
    outline = QuantumFormMuted,
    outlineVariant = QuantumFormLine
)

private fun quantumFormDarkScheme() = darkColorScheme(
    primary = QuantumFormGold,
    onPrimary = QuantumFormBlack,
    primaryContainer = QuantumFormGold,
    onPrimaryContainer = QuantumFormBlack,
    secondary = QuantumFormWhite,
    onSecondary = QuantumFormBlack,
    secondaryContainer = QuantumFormDarkSurface,
    onSecondaryContainer = QuantumFormWhite,
    background = QuantumFormBlack,
    onBackground = QuantumFormWhite,
    surface = QuantumFormBlack,
    onSurface = QuantumFormWhite,
    surfaceVariant = QuantumFormDarkSurface,
    onSurfaceVariant = Color(0xFFE8EFF0),
    outline = Color(0xFF8FA0A6),
    outlineVariant = QuantumFormDarkSurface
)

@Composable
private fun AppTabs(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    val tabs = AppTab.entries
    PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                text = { Text(tab.label) }
            )
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
private fun AddVehiclePanel(snapshot: LedgerSnapshot, onSave: (Vehicle) -> Unit, onDelete: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Section(title = "Vehicles") {
        if (snapshot.vehicles.isEmpty()) {
            Text("No vehicles yet.")
        } else {
            snapshot.vehicles.forEach { vehicle ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(vehicle.displayName, modifier = Modifier.weight(1f))
                    Button(onClick = { onDelete(vehicle.id) }) {
                        Text("Delete")
                    }
                }
            }
        }
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
    val now = LocalDateTime.now()
    var tripDate by remember { mutableStateOf(now.toLocalDate().toString()) }
    var startTime by remember { mutableStateOf(now.minusMinutes(30).toLocalTime().formatTime()) }
    var endTime by remember { mutableStateOf(now.toLocalTime().formatTime()) }
    var origin by remember { mutableStateOf("Home office") }
    var destination by remember { mutableStateOf("") }
    var kilometres by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf(TripPurpose.NeedsReview) }
    var jobLabel by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Manual entry") }
    var selectedVehicleId by remember(snapshot.vehicles) { mutableStateOf(snapshot.vehicles.firstOrNull()?.id) }
    val parsedStart = parseLocalInstant(tripDate, startTime)
    val parsedEnd = parseLocalInstant(tripDate, endTime)

    Section(title = "Add manual trip") {
        VehicleButtons(
            vehicles = snapshot.vehicles,
            selectedVehicleId = selectedVehicleId,
            onChange = { selectedVehicleId = it }
        )
        OutlinedTextField(tripDate, { tripDate = it }, label = { Text("Trip date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(startTime, { startTime = it }, label = { Text("Start time (HH:MM)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(endTime, { endTime = it }, label = { Text("End time (HH:MM)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(origin, { origin = it }, label = { Text("Start point") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(destination, { destination = it }, label = { Text("End point") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = kilometres,
            onValueChange = { kilometres = it },
            label = { Text("Kilometres driven") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        PurposeButtons(purpose = purpose, onChange = { purpose = it })
        OutlinedTextField(jobLabel, { jobLabel = it }, label = { Text("Job / client") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Evidence / review note") }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = destination.isNotBlank() &&
                kilometres.toDoubleOrNull() != null &&
                parsedStart != null &&
                parsedEnd != null &&
                parsedEnd >= parsedStart,
            onClick = {
                val startedAt = parsedStart ?: Clock.System.now()
                val endedAt = parsedEnd ?: startedAt
                onSave(
                    TripLeg(
                        id = "trip-${Clock.System.now().toEpochMilliseconds()}",
                        startedAt = startedAt,
                        endedAt = endedAt,
                        originLabel = origin.trim(),
                        destinationLabel = destination.trim(),
                        kilometres = kilometres.toDouble(),
                        source = TripSource.Manual,
                        vehicleId = selectedVehicleId,
                        purpose = purpose,
                        jobLabel = jobLabel.trim().ifBlank { null },
                        category = category.trim().ifBlank { null },
                        evidenceNote = note.trim().ifBlank { "Manual entry" },
                        adjustmentNote = if (purpose == TripPurpose.Mixed || purpose == TripPurpose.NeedsReview) note else null
                    )
                )
                destination = ""
                kilometres = ""
                jobLabel = ""
                category = ""
                purpose = TripPurpose.NeedsReview
                note = "Manual entry"
            }
        ) {
            Text("Save trip")
        }
        if (parsedStart == null || parsedEnd == null) {
            Text("Use date YYYY-MM-DD and times HH:MM.")
        } else if (parsedEnd < parsedStart) {
            Text("End time must be after start time.")
        }
    }
}

@Composable
private fun DateRangePanel(
    fromDate: String,
    toDate: String,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit
) {
    Section(title = "Review date range") {
        OutlinedTextField(
            value = fromDate,
            onValueChange = onFromDateChange,
            label = { Text("From date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = toDate,
            onValueChange = onToDateChange,
            label = { Text("To date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
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
private fun ReviewQueue(
    snapshot: LedgerSnapshot,
    fromDate: String,
    toDate: String,
    onClassify: (String, TripPurpose, String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val reviewTrips = TripLedgerService.tripsInDateRange(snapshot, fromDate, toDate)
        .filter { it.purpose == TripPurpose.NeedsReview || it.purpose == TripPurpose.Mixed }
    Section(title = "Review queue") {
        if (reviewTrips.isEmpty()) {
            Text("No trips need review.")
        } else {
            reviewTrips.forEach {
                var jobLabel by remember(it.id, it.jobLabel) { mutableStateOf(it.jobLabel.orEmpty()) }
                var category by remember(it.id, it.category) { mutableStateOf(it.category.orEmpty()) }
                Text("${it.originLabel} to ${it.destinationLabel} - ${it.kilometres} km - ${it.purpose.name}")
                Text("${it.startedAt.toString().take(16)} to ${it.endedAt.toString().take(16)}")
                Text("Note: ${it.adjustmentNote ?: it.evidenceNote}")
                OutlinedTextField(jobLabel, { jobLabel = it }, label = { Text("Job / client") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { onClassify(it.id, TripPurpose.Business, jobLabel, category) }) {
                        Text("Business")
                    }
                    Button(onClick = { onClassify(it.id, TripPurpose.Personal, jobLabel, category) }) {
                        Text("Personal")
                    }
                    Button(onClick = { onDelete(it.id) }) {
                        Text("Delete")
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ImportPanel(
    importMessage: String?,
    deviceLocationEnabled: Boolean,
    onRefreshTimelineStatus: () -> Unit,
    onOpenTimelineSettings: () -> Unit,
    onOpenDeviceLocationSettings: () -> Unit,
    onImportTimeline: () -> Unit
) {
    Section(title = "Timeline import") {
        Text("Device Location: ${if (deviceLocationEnabled) "On" else "Off"}")
        Text("Google Timeline status must be confirmed in Google settings; Android does not expose a public app API to read or enable it directly.")
        Button(onClick = onOpenTimelineSettings) {
            Text("Open Timeline settings")
        }
        if (!deviceLocationEnabled) {
            Button(onClick = onOpenDeviceLocationSettings) {
                Text("Open Android Location settings")
            }
        }
        Button(onClick = onRefreshTimelineStatus) {
            Text("Refresh status")
        }
        Text("Import a Google Timeline or Takeout JSON file. Imported driving segments land in Review so every trip can be classified before export.")
        Button(onClick = onImportTimeline) {
            Text("Import Timeline JSON")
        }
        importMessage?.let {
            Text(it)
        }
    }
}

@Composable
private fun PlacesPanel(
    snapshot: LedgerSnapshot,
    onSave: (PlaceTag) -> Unit,
    onDelete: (String) -> Unit,
    onApplyTags: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var matchLabel by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(PlaceKind.Client) }
    val suggestions = TripLedgerService.suggestFrequentPlaces(snapshot)

    Section(title = "Saved places") {
        if (snapshot.places.isEmpty()) {
            Text("No saved places yet.")
        } else {
            snapshot.places.forEach { place ->
                Text("${place.kind.name}: ${place.label}")
                Text("Matches: ${(place.matchLabel ?: place.label)} - ${if (place.approved) "Approved" else "Pending"}")
                Button(onClick = { onDelete(place.id) }) {
                    Text("Delete place")
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        OutlinedTextField(label, { label = it }, label = { Text("Display label") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = matchLabel,
            onValueChange = { matchLabel = it },
            label = { Text("Match trip label") },
            modifier = Modifier.fillMaxWidth()
        )
        PlaceKindButtons(kind = kind, onChange = { kind = it })
        Button(
            enabled = label.isNotBlank(),
            onClick = {
                onSave(
                    PlaceTag(
                        id = "place-${Clock.System.now().toEpochMilliseconds()}",
                        label = label.trim(),
                        kind = kind,
                        approved = true,
                        matchLabel = matchLabel.trim().ifBlank { label.trim() }
                    )
                )
                label = ""
                matchLabel = ""
                kind = PlaceKind.Client
            }
        ) {
            Text("Add approved place")
        }
        Button(onClick = onApplyTags, enabled = snapshot.places.any { it.approved }) {
            Text("Apply approved places")
        }
    }

    Section(title = "Frequent place suggestions") {
        if (suggestions.isEmpty()) {
            Text("No repeated trip endpoints to suggest yet.")
        } else {
            suggestions.forEach { suggestion ->
                Text("${suggestion.matchLabel} - ${suggestion.tripCount} visits")
                Button(
                    onClick = {
                        onSave(
                            PlaceTag(
                                id = "place-${Clock.System.now().toEpochMilliseconds()}",
                                label = suggestion.matchLabel,
                                kind = PlaceKind.Custom,
                                approved = true,
                                matchLabel = suggestion.matchLabel
                            )
                        )
                    }
                ) {
                    Text("Approve suggestion")
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PlaceKindButtons(kind: PlaceKind, onChange: (PlaceKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Place kind")
        PlaceKind.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    Button(onClick = { onChange(option) }) {
                        Text(if (kind == option) "* ${option.name}" else option.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripList(snapshot: LedgerSnapshot, onDelete: (String) -> Unit) {
    Section(title = "Trip ledger") {
        if (snapshot.trips.isEmpty()) {
            Text("No trips yet.")
        } else {
            snapshot.trips.forEach {
                val vehicleName = snapshot.vehicles.firstOrNull { vehicle -> vehicle.id == it.vehicleId }?.displayName ?: "No vehicle"
                Text("${it.startedAt.toString().take(10)} - ${it.originLabel} to ${it.destinationLabel}")
                Text("${it.kilometres} km - ${vehicleName} - ${it.purpose.name} - ${it.source.name}")
                if (!it.jobLabel.isNullOrBlank() || !it.category.isNullOrBlank()) {
                    Text("Job: ${it.jobLabel.orEmpty()} - Category: ${it.category.orEmpty()}")
                }
                Button(onClick = { onDelete(it.id) }) {
                    Text("Delete trip")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExportPanel(exportMessage: String?, onExport: () -> Unit) {
    Section(title = "Export") {
        Text("Generate and share a CRA-style CSV logbook from the current ledger.")
        Button(onClick = onExport) {
            Text("Share CSV")
        }
        exportMessage?.let {
            Text(it)
        }
    }
}

@Composable
private fun BackupPanel(
    exportMessage: String?,
    onPlainBackup: () -> Unit,
    onEncryptedBackup: () -> Unit,
    onRestoreLatest: () -> Unit
) {
    Section(title = "Backup") {
        Text("Create a Drive-ready encrypted archive or a plain local JSON backup for debugging.")
        Button(onClick = onEncryptedBackup) {
            Text("Share encrypted backup")
        }
        Button(onClick = onRestoreLatest) {
            Text("Restore latest encrypted backup")
        }
        Button(onClick = onPlainBackup) {
            Text("Share JSON backup")
        }
        exportMessage?.let {
            Text(it)
        }
    }
}

@Composable
private fun SettingsPanel(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Section(title = "Appearance") {
        Text("Theme")
        ThemeMode.entries.forEach { option ->
            Button(onClick = { onThemeModeChange(option) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (themeMode == option) "* ${option.label}" else option.label)
            }
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

private fun parseLocalInstant(date: String, time: String): Instant? {
    return try {
        val localDate = LocalDate.parse(date.trim())
        val localTime = LocalTime.parse(time.trim())
        val localDateTime = LocalDateTime.of(localDate, localTime)
        // Keep the typed ledger date stable for CSV exports and date-range review.
        Instant.fromEpochMilliseconds(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli())
    } catch (error: Exception) {
        null
    }
}

private fun LocalTime.formatTime(): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
