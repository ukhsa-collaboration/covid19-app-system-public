package uk.nhs.nhsx.core.aws.s3

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

class MetaHeaderTest {

    @Test
    fun s3headerName() {
        MatcherAssert.assertThat(MetaHeader("key", "value").asS3MetaName(), CoreMatchers.equalTo("key"))
    }


    @Test
    fun httpHeaderName() {
        MatcherAssert.assertThat(MetaHeader("key", "value").asHttpHeaderName(), CoreMatchers.equalTo("X-Amz-Meta-key"))
    }
}
