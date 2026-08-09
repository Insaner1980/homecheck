package com.finnvek.homecheck.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataMutationGateTest {
    @Test fun `concurrent mutation waits until active operation finishes`() =
        runTest {
            val gate = DataMutationGate()
            val active = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var secondEntered = false

            val first =
                async {
                    gate.withLock {
                        active.complete(Unit)
                        release.await()
                    }
                }
            active.await()
            val second = async { gate.withLock { secondEntered = true } }

            yield()
            assertFalse(secondEntered)
            release.complete(Unit)
            first.await()
            second.await()
            assertTrue(secondEntered)
        }

    @Test fun `nested operation in the same coroutine reuses the active lock`() =
        runTest {
            val gate = DataMutationGate()
            val events = mutableListOf<String>()

            gate.withLock {
                events += "outer"
                gate.withLock { events += "inner" }
            }

            assertEquals(listOf("outer", "inner"), events)
        }

    @Test fun `competitor cannot enter between nested steps of an outer operation`() =
        runTest {
            val gate = DataMutationGate()
            val betweenSteps = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()

            val outer =
                async {
                    gate.withLock {
                        events += "outer-start"
                        gate.withLock { events += "nested-step" }
                        betweenSteps.complete(Unit)
                        release.await()
                        events += "outer-end"
                    }
                }
            betweenSteps.await()
            val competitor = async { gate.withLock { events += "competitor" } }

            yield()
            assertEquals(listOf("outer-start", "nested-step"), events)
            release.complete(Unit)
            outer.await()
            competitor.await()
            assertEquals(listOf("outer-start", "nested-step", "outer-end", "competitor"), events)
        }

    @Test fun `inherited marker child waits behind an already queued competitor`() =
        runTest {
            val gate = DataMutationGate()
            val outerReady = CompletableDeferred<Unit>()
            val releaseOuter = CompletableDeferred<Unit>()
            val startChild = CompletableDeferred<Unit>()
            val competitorEntered = CompletableDeferred<Unit>()
            val releaseCompetitor = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            lateinit var child: Deferred<Unit>
            lateinit var detachedScope: CoroutineScope

            val outer =
                async {
                    gate.withLock {
                        detachedScope = CoroutineScope(currentCoroutineContext() + SupervisorJob())
                        child =
                            detachedScope.async {
                                startChild.await()
                                gate.withLock { events += "child" }
                            }
                        outerReady.complete(Unit)
                        releaseOuter.await()
                        events += "outer-end"
                    }
                }
            outerReady.await()
            val competitor =
                async {
                    gate.withLock {
                        events += "competitor"
                        competitorEntered.complete(Unit)
                        releaseCompetitor.await()
                    }
                }
            yield()
            startChild.complete(Unit)
            yield()
            assertTrue(events.isEmpty())

            releaseOuter.complete(Unit)
            competitorEntered.await()
            assertEquals(listOf("outer-end", "competitor"), events)
            releaseCompetitor.complete(Unit)
            outer.await()
            competitor.await()
            child.await()
            assertEquals(listOf("outer-end", "competitor", "child"), events)
            detachedScope.cancel()
        }

    @Test fun `cancelling lock owner releases a queued caller`() =
        runTest {
            val gate = DataMutationGate()
            val ownerEntered = CompletableDeferred<Unit>()
            var waiterEntered = false
            val owner =
                launch {
                    gate.withLock {
                        ownerEntered.complete(Unit)
                        awaitCancellation()
                    }
                }
            ownerEntered.await()
            val waiter = async { gate.withLock { waiterEntered = true } }
            yield()
            assertFalse(waiterEntered)

            owner.cancelAndJoin()
            waiter.await()
            assertTrue(waiterEntered)
        }

    @Test fun `cancelled non cancellable waiter never enters critical action`() =
        runTest {
            val gate = DataMutationGate()
            val ownerEntered = CompletableDeferred<Unit>()
            val releaseOwner = CompletableDeferred<Unit>()
            var cancelledActionEntered = false
            val owner =
                launch {
                    gate.withLock {
                        ownerEntered.complete(Unit)
                        releaseOwner.await()
                    }
                }
            ownerEntered.await()
            val waiter =
                launch {
                    gate.withNonCancellableLock {
                        cancelledActionEntered = true
                    }
                }
            yield()

            waiter.cancelAndJoin()
            releaseOwner.complete(Unit)
            owner.join()
            assertFalse(cancelledActionEntered)

            var nextCallerEntered = false
            gate.withLock { nextCallerEntered = true }
            assertTrue(nextCallerEntered)
        }
}
