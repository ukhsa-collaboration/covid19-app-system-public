package uk.nhs.nhsx.analyticssubmission

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.RecordingEvents

class PostDistrictLaReplacerTest {

    private val events = RecordingEvents()

    @Test
    fun `looks up key with post district and local authority`() {
        val result = PostDistrictLaReplacer.replacePostDistrictLA(
            "CV23",
            "E07000151",
            events
        )

        assertThat(result).isEqualTo(PostDistrictPair("CV21_CV23", "E07000151"))
        events.none()
    }

    @Test
    fun `looks up key with post district and null local authority`() {
        val result = PostDistrictLaReplacer.replacePostDistrictLA(
            "CV23",
            null,
            events
        )

        assertThat(result).isEqualTo(PostDistrictPair("CV23", null))
        events.none()
    }

    @Test
    fun `looks up key with post district null and null local authority`() {
        val result = PostDistrictLaReplacer.replacePostDistrictLA(
            null,
            null,
            events
        )

        assertThat(result).isEqualTo(PostDistrictPair("UNKNOWN", "UNKNOWN"))
        events.containsExactly(InfoEvent::class)
    }
}
