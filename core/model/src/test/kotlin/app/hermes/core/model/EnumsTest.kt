package app.hermes.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnumsTest {

    @Test
    fun `known wire values round-trip`() {
        assertThat(MemoryCategory.fromWire("health")).isEqualTo(MemoryCategory.HEALTH)
        assertThat(Criticality.fromWire("high")).isEqualTo(Criticality.HIGH)
        assertThat(DocumentKind.fromWire("id-document")).isEqualTo(DocumentKind.ID_DOCUMENT)
        assertThat(MemoryCategory.HEALTH.wire).isEqualTo("health")
    }

    @Test
    fun `unknown wire value degrades to fallback, never crashes`() {
        // Deliberately opposite to the old zod contract (unknown enum = error).
        assertThat(Criticality.fromWire("urgent")).isEqualTo(Criticality.NORMAL)
        assertThat(MemoryCategory.fromWire("astrology")).isEqualTo(MemoryCategory.MISC)
        assertThat(NoteStatus.fromWire(null)).isEqualTo(NoteStatus.RAW)
        assertThat(ProjectStatus.fromWire("")).isEqualTo(ProjectStatus.ACTIVE)
    }

    @Test
    fun `nullable billing period stays null when absent but degrades when unknown`() {
        assertThat(BillingPeriod.fromWire(null)).isNull()
        assertThat(BillingPeriod.fromWire("weekly")).isEqualTo(BillingPeriod.MONTHLY)
        assertThat(BillingPeriod.fromWire("yearly")).isEqualTo(BillingPeriod.YEARLY)
    }
}
