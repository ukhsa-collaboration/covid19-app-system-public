package uk.nhs.nhsx.virology

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.virology.CtaExchangeRejectionEvent.UnprocessableVirologyCtaExchange

class VirologyEventsTest {

    @Test
    fun `unprocessable virology cta exchange event is info`() {
        val event = UnprocessableVirologyCtaExchange(Exception())
        expectThat(event).get { category() }.isEqualTo(EventCategory.Info)
    }

}
