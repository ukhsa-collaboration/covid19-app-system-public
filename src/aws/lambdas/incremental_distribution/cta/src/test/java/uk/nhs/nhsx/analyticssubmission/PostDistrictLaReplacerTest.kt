package uk.nhs.nhsx.analyticssubmission

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.assertions.containsExactly
import uk.nhs.nhsx.testhelper.assertions.isEmpty

class PostDistrictLaReplacerTest {

    private val events = RecordingEvents()

    @Test
    fun `looks up key with post district and local authority`() {
        val result = PostDistrictLaReplacer.replacePostDistrictLA(
            "CV23",
            "E07000151",
            events
        )

        expectThat(result).isEqualTo(PostDistrictPair("CV21_CV23", "E07000151"))
        expectThat(events).isEmpty()
    }

    @Test
    fun `looks up key with post district and null local authority`() {
        val result = PostDistrictLaReplacer.replacePostDistrictLA(
            "CV23",
            null,
            events
        )

        expectThat(result).isEqualTo(PostDistrictPair("CV23", null))
        expectThat(events).isEmpty()
    }

    @Test
    fun `looks up key with post district null and null local authority`() {
        val result = PostDistrictLaReplacer.replacePostDistrictLA(
            null,
            null,
            events
        )

        expectThat(result).isEqualTo(PostDistrictPair("UNKNOWN", "UNKNOWN"))
        expectThat(events).containsExactly(InfoEvent::class)
    }
}
