package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.shouldEqual
import org.junit.Test
import java.util.concurrent.TimeUnit

class AsyncPollerSpec {

    private val testScope = TestCoroutineScope()

    @Test
    fun `should invoke the provided function`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(60, testScope) // minimum

        poller.start { numberOfInvocations++ }

        numberOfInvocations shouldEqual 1
    }

    @Test
    fun `should poll at the specified interval`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(120, testScope) // greater than minimum

        poller.start { numberOfInvocations++ }

        testScope.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        numberOfInvocations shouldEqual 4
    }

    @Test
    fun `should stop and reset poll`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(60, testScope)

        poller.start { numberOfInvocations++ }
        poller.reset()
        numberOfInvocations shouldEqual 1 // poller should not yet be started

        testScope.advanceTimeBy(TimeUnit.MINUTES.toMillis(5))

        numberOfInvocations shouldEqual 6
    }

    @Test
    fun `should not throw exception when reset poller before start`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(60, testScope)

        poller.reset()
        testScope.advanceTimeBy(TimeUnit.MINUTES.toMillis(5))

        numberOfInvocations shouldEqual 0
    }

    @Test
    fun `should set delay to minimum if less than minimum`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(30, testScope) // minimum is 60s

        poller.start { numberOfInvocations++ }

        testScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(59))

        numberOfInvocations shouldEqual 1
    }

    @Test
    fun `should set delay to minimum for negative`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(-1, testScope) // minimum is 60s

        poller.start { numberOfInvocations++ }

        testScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(59))

        numberOfInvocations shouldEqual 1
    }
}
