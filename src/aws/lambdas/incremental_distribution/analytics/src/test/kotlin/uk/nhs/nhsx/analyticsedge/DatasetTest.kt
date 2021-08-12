package uk.nhs.nhsx.analyticsedge

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DatasetTest {

    @Test
    fun `expected filename`() {
        val prefix = "some-prefix"
        expectThat(Dataset.Aggregate.filename(prefix)).isEqualTo("some-prefix-app-aggregate.csv")
        expectThat(Dataset.Poster.filename(prefix)).isEqualTo("some-prefix-app-posters.csv")
        expectThat(Dataset.Isolation.filename(prefix)).isEqualTo("some-prefix-app-isolation.csv")
        expectThat(Dataset.Enpic.filename(prefix)).isEqualTo("some-prefix-app-enpic.csv")
        expectThat(Dataset.Adoption.filename(prefix)).isEqualTo("some-prefix-app-adoption.csv")
    }
}
