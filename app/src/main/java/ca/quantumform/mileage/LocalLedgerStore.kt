package ca.quantumform.mileage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ca.quantumform.mileage.core.BackupManifest
import ca.quantumform.mileage.core.LedgerSnapshot
import ca.quantumform.mileage.core.TripLeg
import ca.quantumform.mileage.core.TripPurpose
import ca.quantumform.mileage.core.TripSource
import ca.quantumform.mileage.core.Vehicle
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.time.Clock
import kotlin.time.Instant
import org.json.JSONArray
import org.json.JSONObject

class LocalLedgerStore(context: Context) {
    private val backupKeyAlias = "qf-mileage-backup-key"
    private val file = File(context.filesDir, "qf-mileage-ledger.json")
    private val encryptedBackupFile = File(context.filesDir, "qf-mileage-backup.qfmbackup")
    private val preferences = context.getSharedPreferences("qf-mileage-settings", Context.MODE_PRIVATE)

    fun load(): LedgerSnapshot {
        if (!file.exists()) {
            return LedgerSnapshot(vehicles = listOf(defaultVehicle()))
        }

        val root = try {
            JSONObject(file.readText())
        } catch (error: Exception) {
            recoverCorruptLedger()
            return LedgerSnapshot(vehicles = listOf(defaultVehicle()))
        }
        return root.toLedgerSnapshot()
    }

    fun save(snapshot: LedgerSnapshot) {
        file.writeText(snapshot.toJson().toString(2))
    }

    fun loadThemeMode(): ThemeMode {
        return ThemeMode.entries.firstOrNull { it.name == preferences.getString("themeMode", ThemeMode.FollowSystem.name) }
            ?: ThemeMode.FollowSystem
    }

    fun saveThemeMode(themeMode: ThemeMode) {
        preferences.edit().putString("themeMode", themeMode.name).apply()
    }

    fun exportCsv(csv: String): File {
        val export = File(file.parentFile, "qf-mileage-logbook.csv")
        export.writeText(csv)
        return export
    }

    fun exportBackup(snapshot: LedgerSnapshot): File {
        val export = File(file.parentFile, "qf-mileage-backup.json")
        export.writeText(snapshot.toJson().toString(2))
        return export
    }

    fun exportEncryptedBackup(snapshot: LedgerSnapshot): File {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, backupKey())
        val ledgerJson = snapshot.toJson().toString()
        val encryptedPayload = cipher.doFinal(ledgerJson.toByteArray(Charsets.UTF_8))
        val manifest = snapshot.toBackupManifest(encrypted = true)
        val archive = JSONObject()
            .put("manifest", manifest.toJson())
            .put("algorithm", "AES/GCM/NoPadding")
            .put("keyStoreAlias", backupKeyAlias)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("payload", Base64.encodeToString(encryptedPayload, Base64.NO_WRAP))

        encryptedBackupFile.writeText(archive.toString(2))
        return encryptedBackupFile
    }

    fun restoreLatestEncryptedBackup(): BackupRestoreResult {
        if (!encryptedBackupFile.exists()) {
            return BackupRestoreResult(null, "No encrypted backup has been created on this device.")
        }

        val archive = JSONObject(encryptedBackupFile.readText())
        val cipher = Cipher.getInstance(archive.getString("algorithm"))
        val iv = Base64.decode(archive.getString("iv"), Base64.NO_WRAP)
        val payload = Base64.decode(archive.getString("payload"), Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, backupKey(), GCMParameterSpec(128, iv))

        val ledgerJson = cipher.doFinal(payload).toString(Charsets.UTF_8)
        val snapshot = JSONObject(ledgerJson).toLedgerSnapshot()
        save(snapshot)
        val manifest = archive.getJSONObject("manifest")
        return BackupRestoreResult(snapshot, "Restored encrypted backup from ${manifest.getString("createdAt")}.")
    }

    private fun recoverCorruptLedger() {
        val recovered = File(file.parentFile, "qf-mileage-ledger.corrupt-${System.currentTimeMillis()}.json")
        file.renameTo(recovered)
    }

    private fun defaultVehicle() = Vehicle(
        id = "vehicle-default",
        displayName = "Default vehicle"
    )

    private fun backupKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(backupKeyAlias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keySpec = KeyGenParameterSpec.Builder(
            backupKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

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
                jobLabel = item.optString("jobLabel").ifBlank { null },
                category = item.optString("category").ifBlank { null },
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
                    .put("jobLabel", trip.jobLabel.orEmpty())
                    .put("category", trip.category.orEmpty())
                    .put("evidenceNote", trip.evidenceNote)
                    .put("adjustmentNote", trip.adjustmentNote.orEmpty())
            )
        }
        return array
    }

    private fun LedgerSnapshot.toJson(): JSONObject {
        return JSONObject()
            .put("vehicles", vehicles.toJsonVehicles())
            .put("trips", trips.toJsonTrips())
    }

    private fun JSONObject.toLedgerSnapshot(): LedgerSnapshot {
        val vehicles = optJSONArray("vehicles").toVehicles()
        val trips = optJSONArray("trips").toTrips()
        return LedgerSnapshot(
            vehicles = vehicles.ifEmpty { listOf(defaultVehicle()) },
            trips = trips
        )
    }

    private fun LedgerSnapshot.toBackupManifest(encrypted: Boolean): BackupManifest {
        return BackupManifest(
            schemaVersion = 1,
            createdAt = Clock.System.now(),
            encrypted = encrypted,
            tripCount = trips.size,
            vehicleCount = vehicles.size,
            placeCount = places.size
        )
    }

    private fun BackupManifest.toJson(): JSONObject {
        return JSONObject()
            .put("schemaVersion", schemaVersion)
            .put("createdAt", createdAt.toString())
            .put("encrypted", encrypted)
            .put("tripCount", tripCount)
            .put("vehicleCount", vehicleCount)
            .put("placeCount", placeCount)
    }
}

enum class ThemeMode(val label: String) {
    FollowSystem("Follow system"),
    Light("Light"),
    Dark("Dark")
}

data class BackupRestoreResult(
    val snapshot: LedgerSnapshot?,
    val message: String
)
