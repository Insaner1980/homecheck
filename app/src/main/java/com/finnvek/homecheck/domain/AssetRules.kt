package com.finnvek.homecheck.domain

object AssetLimitPolicy {
    const val FREE_ASSET_LIMIT = 3

    fun canCreate(
        currentCount: Int,
        isPremium: Boolean,
    ): Boolean = isPremium || currentCount < FREE_ASSET_LIMIT
}

data class AssetSearchDocument(
    val name: String,
    val manufacturer: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val location: String? = null,
    val category: String? = null,
) {
    fun matches(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return listOf(name, manufacturer, modelNumber, serialNumber, location, category)
            .any { value -> value?.contains(needle, ignoreCase = true) == true }
    }
}
