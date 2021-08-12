package uk.nhs.nhsx.core.aws.s3

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MetaHeaderTest {

    @Test
    fun s3headerName() {
        expectThat(MetaHeader("key", "value").asS3MetaName()).isEqualTo("key")
    }


    @Test
    fun httpHeaderName() {
        expectThat(MetaHeader("key", "value").asHttpHeaderName()).isEqualTo("X-Amz-Meta-key")
    }
}
