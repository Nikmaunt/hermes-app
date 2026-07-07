package app.hermes.core.brain.extraction

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.ChatMessage
import app.hermes.core.model.ChatRole
import java.time.Instant
import java.time.ZoneId

/**
 * Builds the extraction messages. Kept minimal in M0 — the real prompt is filled in M1;
 * what matters here is the CONTRACT: JSON-only, the fixed set of keys, dates as
 * {date|dateTime}, no ids/timestamps/source, and the current time / output language
 * anchored from [BrainContext]. The hardened variant appends the concrete violations
 * so the retry is corrective, not a blind re-ask (§6).
 */
internal object ExtractionPrompts {

    fun initial(request: ExtractionRequest): List<ChatMessage> = listOf(
        ChatMessage(ChatRole.SYSTEM, systemPrompt(request.context)),
        ChatMessage(ChatRole.USER, request.noteText),
    )

    fun hardened(request: ExtractionRequest, violations: List<Violation>): List<ChatMessage> {
        val problems = violations.joinToString(separator = "\n") { "- ${it.field}: ${it.detail}" }
        val system = systemPrompt(request.context) +
            "\n\nYour previous reply was rejected for these problems:\n" + problems +
            "\nReturn corrected, valid JSON only."
        return listOf(
            ChatMessage(ChatRole.SYSTEM, system),
            ChatMessage(ChatRole.USER, request.noteText),
        )
    }

    private fun systemPrompt(context: BrainContext): String {
        val now = Instant.ofEpochMilli(context.nowMillis).atZone(ZoneId.of(context.zoneId))
        val language = context.language ?: "the note's own language"
        return buildString {
            append("You are Hermes's extraction engine. Read the user's note and reply with ")
            append("ONLY a JSON object with keys: summary, memoryFacts, followups, people, ")
            append("projects, decisions, habits, documents, transactions.\n")
            append("Current date-time in the user's zone: ").append(now).append(".\n")
            append("Resolve relative dates against it. Emit dates as {\"date\":\"YYYY-MM-DD\"} ")
            append("or {\"dateTime\":\"YYYY-MM-DDTHH:MM\"}. A followup MUST have a date.\n")
            append("Do NOT invent ids, timestamps, or a source field.\n")
            append("Write summary in ").append(language).append(".")
        }
    }
}
