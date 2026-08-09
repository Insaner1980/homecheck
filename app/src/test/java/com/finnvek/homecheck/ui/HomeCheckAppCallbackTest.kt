package com.finnvek.homecheck.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCheckAppCallbackTest {
    @Test fun `notification handling invokes the latest callback`() {
        val invocations = mutableListOf<String>()
        val callback = NotificationHandledCallback { invocations += "stale" }

        callback.update { invocations += "latest" }
        callback()

        assertEquals(listOf("latest"), invocations)
    }
}
