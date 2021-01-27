package uk.nhs.nhsx.core.aws.s3

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class MetaHeaderTest {

    @Test
    fun s3headerName() {
        assertThat(MetaHeader("key", "value").asS3MetaName(), equalTo("key"))
    }


    @Test
    fun httpHeaderName() {
        assertThat(MetaHeader("key", "value").asHttpHeaderName(), equalTo("X-Amz-Meta-key"))
    }
}