package app.hermes.core.brain.fake

import app.hermes.core.brain.BrainErrorKind
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.CompletionRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeProviderTest {

    private val anyRequest = CompletionRequest(messages = emptyList())

    @Test
    fun `single returns the same completion on every call`() = runTest {
        val provider = FakeProvider.single("same")
        val first = (provider.complete(anyRequest) as BrainResult.Ok).value.text
        val second = (provider.complete(anyRequest) as BrainResult.Ok).value.text
        assertThat(first).isEqualTo("same")
        assertThat(second).isEqualTo("same")
    }

    @Test
    fun `sequence advances per call and repeats the last element`() = runTest {
        val provider = FakeProvider.sequence("a", "b")
        val texts = List(3) { (provider.complete(anyRequest) as BrainResult.Ok).value.text }
        assertThat(texts).containsExactly("a", "b", "b").inOrder()
    }

    @Test
    fun `failing returns a leak-free transport failure`() = runTest {
        val provider = FakeProvider.failing(BrainErrorKind.RATE_LIMIT, "slow down")
        val result = provider.complete(anyRequest)
        assertThat(result).isInstanceOf(BrainResult.Failure::class.java)
        assertThat((result as BrainResult.Failure).kind).isEqualTo(BrainErrorKind.RATE_LIMIT)
    }
}
