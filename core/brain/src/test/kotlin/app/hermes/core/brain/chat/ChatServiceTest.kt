package app.hermes.core.brain.chat

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.BrainErrorKind
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.ChatMessage
import app.hermes.core.brain.fake.FakeProvider
import app.hermes.core.model.ChatRole
import app.hermes.core.model.MemoryCategory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChatServiceTest {

    private val ctx = ChatContext(BrainContext(nowMillis = 0, zoneId = "UTC", language = "en"))
    private val history = listOf(ChatMessage(ChatRole.USER, "hi"))

    @Test
    fun `reply maps content and usage from the provider`() = runTest {
        val service = ChatService(FakeProvider.single("Hello there"))
        val reply = (service.reply(history, ctx) as BrainResult.Ok).value
        assertThat(reply.content).isEqualTo("Hello there")
        assertThat(reply.usage).isEqualTo(FakeProvider.DEFAULT_USAGE)
    }

    @Test
    fun `reply surfaces a transport failure`() = runTest {
        val service = ChatService(FakeProvider.failing(BrainErrorKind.AUTH, "bad key"))
        val result = service.reply(history, ctx)
        assertThat(result).isInstanceOf(BrainResult.Failure::class.java)
    }

    @Test
    fun `injected retrieval context does not break the call`() = runTest {
        val grounded = ChatContext(
            brain = ctx.brain,
            retrievedFacts = listOf(RetrievedFact("diet", "vegetarian", MemoryCategory.PREFERENCES)),
            recentFollowups = listOf(RetrievedFollowup("Call bank", dueAtMillis = 100, isDateOnly = false)),
        )
        val service = ChatService(FakeProvider.single("ok"))
        val reply = (service.reply(history, grounded) as BrainResult.Ok).value
        assertThat(reply.content).isEqualTo("ok")
    }
}
