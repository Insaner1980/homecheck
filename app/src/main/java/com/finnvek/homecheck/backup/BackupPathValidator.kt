package com.finnvek.homecheck.backup

object BackupPathValidator {
    private const val MAX_PATH_LENGTH = 240
    private val safeSegment = Regex("[A-Za-z0-9._-]+")

    fun isSafe(path: String): Boolean {
        val hasInvalidRoot = path.startsWith('/') || '\\' in path || ':' in path
        if (path.isBlank() || path.length > MAX_PATH_LENGTH || hasInvalidRoot) return false
        val segments = path.split('/')
        return segments.all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".." && safeSegment.matches(segment)
        }
    }
}
