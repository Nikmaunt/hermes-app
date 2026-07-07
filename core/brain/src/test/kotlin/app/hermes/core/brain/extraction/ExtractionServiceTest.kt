package app.hermes.core.brain.extraction

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.BrainErrorKind
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.Completion
import app.hermes.core.brain.CompletionRequest
import app.hermes.core.brain.ProviderDescriptor
import app.hermes.core.brain.TokenUsage
import app.hermes.core.brain.fake.FakeProvider
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExtractionServiceTest {

    private val ctx = BrainContext(
        nowMillis = Instant.parse("2026-07-07T12:00:00Z").toEpochMilli(),
        zoneId = "Europe/Warsaw",
        language = "en",
    )
    private val validNote = """{"memoryFacts":[{"category":"health","topic":"eyes","fact":"eye exam"}]}"""
    private val allRejectedNote = """{"followups":[{"title":"no date"}]}"""

    private fun request(text: String = "note") = ExtractionRequest(text, ctx)

    @Test
    fun `happy path returns Extracted on the first attempt`() = runTest {
        val service = ExtractionService(FakeProvider.single(validNote))
        val outcome = (service.extract(request()) as BrainResult.Ok).value
        assertThat(outcome).isInstanceOf(ExtractionOutcome.Extracted::class.java)
        val extracted = outcome as ExtractionOutcome.Extracted
        assertThat(extracted.attempts).isEqualTo(1)
        assertThat(extracted.result.memoryFacts).hasSize(1)
    }

    @Test
    fun `an itemless reply is Empty`() = runTest {
        val service = ExtractionService(FakeProvider.single("{}"))
        val outcome = (service.extract(request()) as BrainResult.Ok).value
        assertThat(outcome).isInstanceOf(ExtractionOutcome.Empty::class.java)
    }

    @Test
    fun `an over-budget note is not sent to the model`() = runTest {
        var calls = 0
        val provider = FakeProvider { _, _ ->
            calls++
            BrainResult.Ok(Completion(validNote, TokenUsage.NONE))
        }
        val service = ExtractionService(provider, maxNoteChars = 5)
        val outcome = (service.extract(request("way too long a note")) as BrainResult.Ok).value
        assertThat(outcome).isInstanceOf(ExtractionOutcome.TooLarge::class.java)
        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun `a transport failure is surfaced, not swallowed`() = runTest {
        val service = ExtractionService(FakeProvider.failing(BrainErrorKind.NETWORK, "offline"))
        val result = service.extract(request())
        assertThat(result).isInstanceOf(BrainResult.Failure::class.java)
        assertThat((result as BrainResult.Failure).kind).isEqualTo(BrainErrorKind.NETWORK)
    }

    @Test
    fun `a malformed reply is retried and can recover`() = runTest {
        val service = ExtractionService(FakeProvider.sequence("not json at all", validNote))
        val outcome = (service.extract(request()) as BrainResult.Ok).value as ExtractionOutcome.Extracted
        assertThat(outcome.attempts).isEqualTo(2)
        assertThat(outcome.result.memoryFacts).hasSize(1)
        // usage is summed across both attempts
        assertThat(outcome.usage).isEqualTo(FakeProvider.DEFAULT_USAGE + FakeProvider.DEFAULT_USAGE)
    }

    @Test
    fun `a malformed reply twice is Unusable and keeps the latest raw text`() = runTest {
        val service = ExtractionService(FakeProvider.sequence("first junk", "second junk"))
        val outcome = (service.extract(request()) as BrainResult.Ok).value
        assertThat(outcome).isInstanceOf(ExtractionOutcome.Unusable::class.java)
        assertThat((outcome as ExtractionOutcome.Unusable).rawText).isEqualTo("second junk")
    }

    @Test
    fun `an all-rejected first attempt is retried and can recover`() = runTest {
        val service = ExtractionService(FakeProvider.sequence(allRejectedNote, validNote))
        val outcome = (service.extract(request()) as BrainResult.Ok).value as ExtractionOutcome.Extracted
        assertThat(outcome.attempts).isEqualTo(2)
        assertThat(outcome.result.memoryFacts).hasSize(1)
    }

    @Test
    fun `all-rejected twice still surfaces rejected items for review, not Unusable`() = runTest {
        val service = ExtractionService(FakeProvider.sequence(allRejectedNote, allRejectedNote))
        val outcome = (service.extract(request()) as BrainResult.Ok).value
        assertThat(outcome).isInstanceOf(ExtractionOutcome.Extracted::class.java)
        val extracted = outcome as ExtractionOutcome.Extracted
        assertThat(extracted.result.acceptedCount).isEqualTo(0)
        assertThat(extracted.result.rejected).isNotEmpty()
    }

    @Test
    fun `if the retry transport fails, the first structured attempt is kept`() = runTest {
        val provider = FakeProvider { _, i ->
            if (i == 0) {
                BrainResult.Ok(Completion(allRejectedNote, FakeProvider.DEFAULT_USAGE))
            } else {
                BrainResult.Failure(BrainErrorKind.TIMEOUT, "slow")
            }
        }
        val service = ExtractionService(provider)
        val outcome = (service.extract(request()) as BrainResult.Ok).value
        assertThat(outcome).isInstanceOf(ExtractionOutcome.Extracted::class.java)
        val extracted = outcome as ExtractionOutcome.Extracted
        assertThat(extracted.result.rejected).isNotEmpty()
        // retry gave no usage, so total equals the first attempt only
        assertThat(extracted.usage).isEqualTo(FakeProvider.DEFAULT_USAGE)
    }

    @Test
    fun `extraction requests JSON at temperature zero`() = runTest {
        var seen: CompletionRequest? = null
        val provider = object : app.hermes.core.brain.BrainProvider {
            override val descriptor = ProviderDescriptor("probe", "probe-1")
            override suspend fun complete(request: CompletionRequest): BrainResult<Completion> {
                seen = request
                return BrainResult.Ok(Completion(validNote, TokenUsage.NONE))
            }
        }
        ExtractionService(provider).extract(request())
        assertThat(seen!!.temperature).isEqualTo(ExtractionService.EXTRACTION_TEMPERATURE)
        assertThat(seen!!.responseFormat).isInstanceOf(app.hermes.core.brain.ResponseFormat.Json::class.java)
    }
}
