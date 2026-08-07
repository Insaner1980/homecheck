package com.finnvek.homecheck.backup

object BackupPathValidator {
    private val safeSegment = Regex("[A-Za-z0-9._-]+")

    fun isSafe(path: String): Boolean {
        if (path.isBlank() || path.length > 240 || path.startsWith('/') || '\\' in path || ':' in path) return false
        val segments = path.split('/')
        return segments.all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".." && safeSegment.matches(segment)
        }
    }
}
