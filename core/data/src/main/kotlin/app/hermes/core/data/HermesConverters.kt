package app.hermes.core.data

import androidx.room.TypeConverter
import app.hermes.core.model.BillingPeriod
import app.hermes.core.model.ChatRole
import app.hermes.core.model.Criticality
import app.hermes.core.model.DocumentKind
import app.hermes.core.model.FollowupStatus
import app.hermes.core.model.HabitCadence
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.MemorySource
import app.hermes.core.model.NoteSource
import app.hermes.core.model.NoteStatus
import app.hermes.core.model.ProjectStatus
import app.hermes.core.model.Sensitivity
import app.hermes.core.model.SpendKind
import app.hermes.core.model.TransactionSource

/**
 * The one place enums cross the DB boundary. Every enum is stored as its TEXT `wire`
 * value and read back through the central `fromWire` mapping, which degrades an
 * unknown value to a known fallback instead of crashing (see [app.hermes.core.model.WireEnum]).
 * Storage stays additive: a value written by a newer app version never loses the row.
 */
class HermesConverters {
    @TypeConverter fun memoryCategoryToWire(value: MemoryCategory): String = value.wire

    @TypeConverter fun memoryCategoryFromWire(value: String): MemoryCategory = MemoryCategory.fromWire(value)

    @TypeConverter fun memorySourceToWire(value: MemorySource): String = value.wire

    @TypeConverter fun memorySourceFromWire(value: String): MemorySource = MemorySource.fromWire(value)

    @TypeConverter fun noteSourceToWire(value: NoteSource): String = value.wire

    @TypeConverter fun noteSourceFromWire(value: String): NoteSource = NoteSource.fromWire(value)

    @TypeConverter fun noteStatusToWire(value: NoteStatus): String = value.wire

    @TypeConverter fun noteStatusFromWire(value: String): NoteStatus = NoteStatus.fromWire(value)

    @TypeConverter fun sensitivityToWire(value: Sensitivity): String = value.wire

    @TypeConverter fun sensitivityFromWire(value: String): Sensitivity = Sensitivity.fromWire(value)

    @TypeConverter fun criticalityToWire(value: Criticality): String = value.wire

    @TypeConverter fun criticalityFromWire(value: String): Criticality = Criticality.fromWire(value)

    @TypeConverter fun followupStatusToWire(value: FollowupStatus): String = value.wire

    @TypeConverter fun followupStatusFromWire(value: String): FollowupStatus = FollowupStatus.fromWire(value)

    @TypeConverter fun documentKindToWire(value: DocumentKind): String = value.wire

    @TypeConverter fun documentKindFromWire(value: String): DocumentKind = DocumentKind.fromWire(value)

    @TypeConverter fun projectStatusToWire(value: ProjectStatus): String = value.wire

    @TypeConverter fun projectStatusFromWire(value: String): ProjectStatus = ProjectStatus.fromWire(value)

    @TypeConverter fun habitCadenceToWire(value: HabitCadence): String = value.wire

    @TypeConverter fun habitCadenceFromWire(value: String): HabitCadence = HabitCadence.fromWire(value)

    @TypeConverter fun transactionSourceToWire(value: TransactionSource): String = value.wire

    @TypeConverter fun transactionSourceFromWire(value: String): TransactionSource = TransactionSource.fromWire(value)

    @TypeConverter fun chatRoleToWire(value: ChatRole): String = value.wire

    @TypeConverter fun chatRoleFromWire(value: String): ChatRole = ChatRole.fromWire(value)

    @TypeConverter fun spendKindToWire(value: SpendKind): String = value.wire

    @TypeConverter fun spendKindFromWire(value: String): SpendKind = SpendKind.fromWire(value)

    @TypeConverter fun billingPeriodToWire(value: BillingPeriod?): String? = value?.wire

    @TypeConverter fun billingPeriodFromWire(value: String?): BillingPeriod? = BillingPeriod.fromWire(value)
}
