package com.finnvek.homecheck.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

object BackupCodec {
    const val SUPPORTED_SCHEMA_VERSION = 1
    private val json = Json { prettyPrint = true }

    fun encode(backup: HomeCheckBackup): String = json.encodeToString(
        JsonElement.serializer(),
        buildJsonObject {
            put("schemaVersion", backup.schemaVersion)
            put("exportedAtEpochMillis", backup.exportedAtEpochMillis)
            put("assets", buildJsonArray { backup.assets.forEach { add(it.toJson()) } })
            put("attachments", buildJsonArray { backup.attachments.forEach { add(it.toJson()) } })
            put("tasks", buildJsonArray { backup.tasks.forEach { add(it.toJson()) } })
            put("history", buildJsonArray { backup.history.forEach { add(it.toJson()) } })
        },
    )

    fun decode(value: String): HomeCheckBackup {
        val root = json.parseToJsonElement(value).jsonObject
        val version = root.requiredInt("schemaVersion")
        if (version != SUPPORTED_SCHEMA_VERSION) throw UnsupportedBackupVersionException(version)
        return HomeCheckBackup(
            schemaVersion = version,
            exportedAtEpochMillis = root.requiredLong("exportedAtEpochMillis"),
            assets = root.requiredArray("assets").map { it.jsonObject.toAsset() },
            attachments = root.requiredArray("attachments").map { it.jsonObject.toAttachment() },
            tasks = root.requiredArray("tasks").map { it.jsonObject.toTask() },
            history = root.requiredArray("history").map { it.jsonObject.toHistory() },
        )
    }
}

private fun BackupAsset.toJson() = buildJsonObject {
    put("id", id); put("name", name); put("createdAt", createdAt); put("updatedAt", updatedAt)
    putNullable("category", category); putNullable("location", location); putNullable("manufacturer", manufacturer)
    putNullable("modelNumber", modelNumber); putNullable("serialNumber", serialNumber); putNullable("purchaseDate", purchaseDate)
    putNullable("retailer", retailer); putNullable("warrantyExpirationDate", warrantyExpirationDate); putNullable("notes", notes)
}

private fun BackupAttachment.toJson() = buildJsonObject {
    put("id", id); put("assetId", assetId); put("type", type); put("displayName", displayName)
    put("mimeType", mimeType); put("archivePath", archivePath); put("createdAt", createdAt)
}

private fun BackupTask.toJson() = buildJsonObject {
    put("id", id); put("assetId", assetId); put("title", title); put("dueDate", dueDate); putNullable("notes", notes)
    putNullable("recurrenceInterval", recurrenceInterval); putNullable("recurrenceUnit", recurrenceUnit)
    put("reminderEnabled", reminderEnabled); put("createdAt", createdAt); put("updatedAt", updatedAt)
}

private fun BackupHistory.toJson() = buildJsonObject {
    put("id", id); put("assetId", assetId); putNullable("sourceTaskId", sourceTaskId); put("title", title)
    put("completedAt", completedAt); putNullable("note", note)
}

private fun JsonObject.toAsset() = BackupAsset(
    id = requiredString("id"), name = requiredString("name"), createdAt = requiredLong("createdAt"), updatedAt = requiredLong("updatedAt"),
    category = optionalString("category"), location = optionalString("location"), manufacturer = optionalString("manufacturer"),
    modelNumber = optionalString("modelNumber"), serialNumber = optionalString("serialNumber"), purchaseDate = optionalString("purchaseDate"),
    retailer = optionalString("retailer"), warrantyExpirationDate = optionalString("warrantyExpirationDate"), notes = optionalString("notes"),
)

private fun JsonObject.toAttachment() = BackupAttachment(
    id = requiredString("id"), assetId = requiredString("assetId"), type = requiredString("type"),
    displayName = requiredString("displayName"), mimeType = requiredString("mimeType"),
    archivePath = requiredString("archivePath"), createdAt = requiredLong("createdAt"),
)

private fun JsonObject.toTask() = BackupTask(
    id = requiredString("id"), assetId = requiredString("assetId"), title = requiredString("title"), dueDate = requiredString("dueDate"),
    notes = optionalString("notes"), recurrenceInterval = optionalInt("recurrenceInterval"), recurrenceUnit = optionalString("recurrenceUnit"),
    reminderEnabled = this["reminderEnabled"]?.jsonPrimitive?.boolean ?: true,
    createdAt = requiredLong("createdAt"), updatedAt = requiredLong("updatedAt"),
)

private fun JsonObject.toHistory() = BackupHistory(
    id = requiredString("id"), assetId = requiredString("assetId"), sourceTaskId = optionalString("sourceTaskId"),
    title = requiredString("title"), completedAt = requiredLong("completedAt"), note = optionalString("note"),
)

private fun JsonObject.requiredString(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.requiredInt(key: String) = getValue(key).jsonPrimitive.int
private fun JsonObject.requiredLong(key: String) = getValue(key).jsonPrimitive.long
private fun JsonObject.requiredArray(key: String): JsonArray = getValue(key).jsonArray
private fun JsonObject.optionalString(key: String) = get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull
private fun JsonObject.optionalInt(key: String) = get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull?.toIntOrNull()

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Int?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
}

