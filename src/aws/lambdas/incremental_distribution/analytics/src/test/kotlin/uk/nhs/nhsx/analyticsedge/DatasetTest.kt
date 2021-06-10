package uk.nhs.nhsx.analyticsedge

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DatasetTest {

    @Test
    fun `expected filename`() {
        val prefix = "some-prefix"
        assertThat(Dataset.Aggregate.filename(prefix)).isEqualTo("some-prefix-app-aggregate.csv")
        assertThat(Dataset.Poster.filename(prefix)).isEqualTo("some-prefix-app-posters.csv")
        assertThat(Dataset.Isolation.filename(prefix)).isEqualTo("some-prefix-app-isolation.csv")
        assertThat(Dataset.Enpic.filename(prefix)).isEqualTo("some-prefix-app-enpic.csv")
        assertThat(Dataset.Adoption.filename(prefix)).isEqualTo("some-prefix-app-adoption.csv")
    }
}
