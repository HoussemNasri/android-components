/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoResult

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GeckoResultTest {

    @Test
    fun awaitWithResult() = runBlockingTest {
        val result = GeckoResult.fromValue(42).await()
        assertEquals(42, result)
    }

    @Test(expected = IllegalStateException::class)
    fun awaitWithException() = runBlockingTest {
        GeckoResult.fromException<Unit>(IllegalStateException()).await()
    }

    @Test
    fun fromResult() = runBlockingTest {
        val result = launchGeckoResult { 42 }

        result.then<Int> {
            assertEquals(42, it)
            GeckoResult.fromValue(null)
        }.await()
    }

    @Test
    fun fromException() = runBlockingTest {
        val result = launchGeckoResult { throw IllegalStateException() }

        result.then<Unit>(
            {
                assertTrue("Invalid branch", false)
                GeckoResult.fromValue(null)
            },
            {
                assertTrue(it is IllegalStateException)
                GeckoResult.fromValue(null)
            }
        ).await()
    }

    @Test
    fun asCancellableOperation() = runBlockingTest {
        val geckoResult: GeckoResult<Int> = mock()
        val op = geckoResult.asCancellableOperation()

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromValue(false))
        assertFalse(op.cancel().await())

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromValue(null))
        assertFalse(op.cancel().await())

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromValue(true))
        assertTrue(op.cancel().await())

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromException(IllegalStateException()))
        try {
            op.cancel().await()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
    }
}
