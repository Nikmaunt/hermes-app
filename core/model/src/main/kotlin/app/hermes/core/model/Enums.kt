package app.hermes.core.model

/**
 * An enum persisted as its [wire] TEXT value.
 *
 * Storage is additive: an unknown wire value read back from the DB degrades to a
 * known [WireEnumCompanion.fallback] instead of crashing. This is DELIBERATELY the
 * opposite of the old Hermes Lens zod API contract, where an unknown enum was an
 * error that forced a fallback to cache. Rationale: there is no second service to
 * protect here — the DB is the only source — so a value we don't recognize (e.g. a
 * criticality written by a newer app version, or a hand-edited import) must NOT
 * lose the row. We degrade to a safe known value and keep going.
 */
interface WireEnum {
    val wire: String
}

/** Central TEXT -> enum mapping. Every enum below maps through here — the one place
 *  the "unknown -> known fallback, never crash" policy lives. */
inline fun <reified E> enumFromWire(wire: String?, fallback: E): E
    where E : Enum<E>, E : WireEnum =
    enumValues<E>().firstOrNull { it.wire == wire } ?: fallback

enum class MemoryCategory(override val wire: String) : WireEnum {
    IDENTITY("identity"),
    PREFERENCES("preferences"),
    HEALTH("health"),
    ROUTINES("routines"),
    PLANS("plans"),
    RELATIONSHIPS("relationships"),
    FINANCE("finance"),
    MISC("misc"),
    ;

    companion object {
        fun fromWire(wire: String?): MemoryCategory = enumFromWire(wire, MISC)
    }
}

enum class MemorySource(override val wire: String) : WireEnum {
    CAPTURE("capture"),
    INFERRED("inferred"),
    IMPORT("import"),
    NOTIFICATION("notification"),
    GMAIL("gmail"),
    ;

    companion object {
        fun fromWire(wire: String?): MemorySource = enumFromWire(wire, CAPTURE)
    }
}

enum class NoteSource(override val wire: String) : WireEnum {
    TEXT("text"),
    VOICE("voice"),
    SHARE_TEXT("share_text"),
    SHARE_FILE("share_file"),
    NOTIFICATION("notification"),
    GMAIL("gmail"),
    ;

    companion object {
        fun fromWire(wire: String?): NoteSource = enumFromWire(wire, TEXT)
    }
}

enum class NoteStatus(override val wire: String) : WireEnum {
    RAW("raw"),
    REVIEW("review"),
    FILED("filed"),
    DISCARDED("discarded"),
    ;

    companion object {
        fun fromWire(wire: String?): NoteStatus = enumFromWire(wire, RAW)
    }
}

enum class Sensitivity(override val wire: String) : WireEnum {
    NORMAL("normal"),
    SENSITIVE("sensitive"),
    ;

    companion object {
        fun fromWire(wire: String?): Sensitivity = enumFromWire(wire, NORMAL)
    }
}

enum class Criticality(override val wire: String) : WireEnum {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    ;

    companion object {
        fun fromWire(wire: String?): Criticality = enumFromWire(wire, NORMAL)
    }
}

enum class FollowupStatus(override val wire: String) : WireEnum {
    PENDING("pending"),
    DONE("done"),
    SNOOZED("snoozed"),
    CANCELLED("cancelled"),
    ;

    companion object {
        fun fromWire(wire: String?): FollowupStatus = enumFromWire(wire, PENDING)
    }
}

enum class DocumentKind(override val wire: String) : WireEnum {
    CONTRACT("contract"),
    SUBSCRIPTION("subscription"),
    INSURANCE("insurance"),
    ID_DOCUMENT("id-document"),
    ;

    companion object {
        fun fromWire(wire: String?): DocumentKind = enumFromWire(wire, CONTRACT)
    }
}

enum class BillingPeriod(override val wire: String) : WireEnum {
    MONTHLY("monthly"),
    YEARLY("yearly"),
    ;

    companion object {
        /** Nullable: billingPeriod is optional. A present-but-unknown value degrades to MONTHLY. */
        fun fromWire(wire: String?): BillingPeriod? = wire?.let { enumFromWire(it, MONTHLY) }
    }
}

enum class ProjectStatus(override val wire: String) : WireEnum {
    ACTIVE("active"),
    PAUSED("paused"),
    DONE("done"),
    ARCHIVED("archived"),
    ;

    companion object {
        fun fromWire(wire: String?): ProjectStatus = enumFromWire(wire, ACTIVE)
    }
}

enum class HabitCadence(override val wire: String) : WireEnum {
    DAILY("daily"),
    WEEKLY("weekly"),
    CUSTOM("custom"),
    ;

    companion object {
        fun fromWire(wire: String?): HabitCadence = enumFromWire(wire, DAILY)
    }
}

enum class TransactionSource(override val wire: String) : WireEnum {
    MANUAL("manual"),
    NOTIFICATION("notification"),
    GMAIL("gmail"),
    IMPORT("import"),
    ;

    companion object {
        fun fromWire(wire: String?): TransactionSource = enumFromWire(wire, MANUAL)
    }
}

enum class ChatRole(override val wire: String) : WireEnum {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    ;

    companion object {
        fun fromWire(wire: String?): ChatRole = enumFromWire(wire, USER)
    }
}

enum class SpendKind(override val wire: String) : WireEnum {
    EXTRACTION("extraction"),
    BRIEF("brief"),
    CONSOLIDATION("consolidation"),
    CHAT("chat"),
    MODELS_PROBE("models_probe"),
    ;

    companion object {
        fun fromWire(wire: String?): SpendKind = enumFromWire(wire, EXTRACTION)
    }
}
