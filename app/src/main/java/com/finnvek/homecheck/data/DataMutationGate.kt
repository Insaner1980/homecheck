package com.finnvek.homecheck.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@Singleton
class DataMutationGate
    @Inject
    constructor() {
        private val mutex = Mutex()

        suspend fun <T> withLock(action: suspend () -> T): T {
            val activeGate = coroutineContext[GateHeld]
            if (activeGate?.gate === this && activeGate.ownerJob === coroutineContext[Job]) return action()
            return mutex.withLock { withFreshOwner(action) }
        }

        suspend fun <T> withNonCancellableLock(action: suspend () -> T): T {
            coroutineContext.ensureActive()
            val activeGate = coroutineContext[GateHeld]
            if (activeGate?.gate === this && activeGate.ownerJob === coroutineContext[Job]) {
                return withContext(NonCancellable) { withFreshOwner(action) }
            }
            return mutex.withLock {
                coroutineContext.ensureActive()
                withContext(NonCancellable) { withFreshOwner(action) }
            }
        }

        private suspend fun <T> withFreshOwner(action: suspend () -> T): T {
            val held = GateHeld(this@DataMutationGate)
            return withContext(held) {
                held.ownerJob = coroutineContext[Job]
                action()
            }
        }

        private class GateHeld(
            val gate: DataMutationGate,
            var ownerJob: Job? = null,
        ) : AbstractCoroutineContextElement(GateHeld) {
            companion object : CoroutineContext.Key<GateHeld>
        }
    }
