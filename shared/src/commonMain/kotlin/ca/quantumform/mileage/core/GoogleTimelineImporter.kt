package ca.quantumform.mileage.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Instant

data class TimelineImportResult(
    val importedTrips: List<TripLeg>,
    val skippedSegments: Int,
    val warning: String? = null
)

object GoogleTimelineImporter {
    private val drivingTypes = setOf(
        "IN_PASSENGER_VEHICLE",
        "DRIVING",
        "MOTORCYCLING",
        "IN_VEHICLE"
    )

    fun parse(jsonText: String): TimelineImportResult {
        val root = Json.parseToJsonElement(jsonText).jsonObject
        val timelineObjects = root["timelineObjects"]?.jsonArray.orEmpty()
        val semanticSegments = root["semanticSegments"]?.jsonArray.orEmpty()

        val imported = mutableListOf<TripLeg>()
        var skipped = 0

        for (item in timelineObjects) {
            val segment = item.jsonObject["activitySegment"]?.jsonObject
            if (segment == null) {
                skipped += 1
                continue
            }
            parseActivitySegment(segment)?.let { imported += it } ?: run { skipped += 1 }
        }

        for (item in semanticSegments) {
            parseSemanticSegment(item.jsonObject)?.let { imported += it } ?: run { skipped += 1 }
        }

        val warning = if (timelineObjects.isEmpty() && semanticSegments.isEmpty()) {
            "No supported Timeline activity segments were found."
        } else {
            null
        }
        return TimelineImportResult(imported.sortedByDescending { it.startedAt }, skipped, warning)
    }

    private fun parseActivitySegment(segment: JsonObject): TripLeg? {
        val activityType = segment["activityType"]?.text()?.uppercase()
        if (activityType !in drivingTypes) return null

        val duration = segment["duration"]?.jsonObject ?: return null
        val startedAt = duration["startTimestamp"]?.instant() ?: return null
        val endedAt = duration["endTimestamp"]?.instant() ?: return null
        if (endedAt <= startedAt) return null

        val origin = segment["startLocation"]?.jsonObject?.routePoint() ?: return null
        val destination = segment["endLocation"]?.jsonObject?.routePoint() ?: return null
        val distanceKm = segment["distance"]?.jsonPrimitive?.doubleOrNull?.div(1000.0)
            ?: haversineKilometres(origin, destination)

        return importedTrip(
            startedAt = startedAt,
            endedAt = endedAt,
            origin = origin,
            destination = destination,
            kilometres = distanceKm,
            evidence = "Imported from Google Timeline activity segment: $activityType"
        )
    }

    private fun parseSemanticSegment(segment: JsonObject): TripLeg? {
        val startedAt = segment["startTime"]?.instant() ?: return null
        val endedAt = segment["endTime"]?.instant() ?: return null
        if (endedAt <= startedAt) return null

        val activityType = segment["activity"]?.jsonObject
            ?.get("topCandidate")?.jsonObject
            ?.get("type")?.text()
            ?.uppercase()
        if (activityType !in drivingTypes) return null

        val path = segment["timelinePath"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonObject["point"]?.geoPoint() }
        if (path.size < 2) return null

        val origin = path.first()
        val destination = path.last()
        val distanceKm = path.zipWithNext().sumOf { (from, to) -> haversineKilometres(from, to) }

        return importedTrip(
            startedAt = startedAt,
            endedAt = endedAt,
            origin = origin,
            destination = destination,
            kilometres = distanceKm,
            evidence = "Imported from Google Timeline semantic segment: $activityType"
        )
    }

    private fun importedTrip(
        startedAt: Instant,
        endedAt: Instant,
        origin: RoutePoint,
        destination: RoutePoint,
        kilometres: Double,
        evidence: String
    ): TripLeg {
        return TripLeg(
            id = stableTimelineId(startedAt, endedAt, origin, destination),
            startedAt = startedAt,
            endedAt = endedAt,
            originLabel = origin.label,
            destinationLabel = destination.label,
            kilometres = (kilometres * 10.0).toInt() / 10.0,
            source = TripSource.GoogleTimelineImport,
            vehicleId = null,
            purpose = TripPurpose.NeedsReview,
            evidenceNote = evidence,
            adjustmentNote = "Review imported trip before claiming it."
        )
    }

    private fun stableTimelineId(
        startedAt: Instant,
        endedAt: Instant,
        origin: RoutePoint,
        destination: RoutePoint
    ): String {
        val raw = "${startedAt}|${endedAt}|${origin.latitude}|${origin.longitude}|${destination.latitude}|${destination.longitude}"
        val suffix = raw.fold(0) { acc, char -> (acc * 31) + char.code }.toUInt().toString(16)
        return "timeline-$suffix"
    }

    private fun JsonObject.routePoint(): RoutePoint? {
        val lat = this["latitudeE7"]?.jsonPrimitive?.doubleOrNull?.div(10_000_000.0)
            ?: this["latitude"]?.jsonPrimitive?.doubleOrNull
            ?: return null
        val lon = this["longitudeE7"]?.jsonPrimitive?.doubleOrNull?.div(10_000_000.0)
            ?: this["longitude"]?.jsonPrimitive?.doubleOrNull
            ?: return null
        return RoutePoint(label = "${lat.formatCoordinate()}, ${lon.formatCoordinate()}", latitude = lat, longitude = lon)
    }

    private fun JsonElement.geoPoint(): RoutePoint? {
        val text = text()
        if (!text.startsWith("geo:")) return null
        val parts = text.removePrefix("geo:").split(",")
        if (parts.size < 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        return RoutePoint(label = "${lat.formatCoordinate()}, ${lon.formatCoordinate()}", latitude = lat, longitude = lon)
    }

    private fun JsonElement.instant(): Instant? {
        return try {
            Instant.parse(text())
        } catch (error: Exception) {
            null
        }
    }

    private fun JsonElement.text(): String {
        return (this as? JsonPrimitive)?.content.orEmpty()
    }

    private fun Double.formatCoordinate(): String {
        return (this * 100000.0).toInt().let { rounded -> (rounded / 100000.0).toString() }
    }

    private fun haversineKilometres(from: RoutePoint, to: RoutePoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = degreesToRadians(to.latitude - from.latitude)
        val dLon = degreesToRadians(to.longitude - from.longitude)
        val fromLat = degreesToRadians(from.latitude)
        val toLat = degreesToRadians(to.latitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(fromLat) * cos(toLat) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun degreesToRadians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0
}
