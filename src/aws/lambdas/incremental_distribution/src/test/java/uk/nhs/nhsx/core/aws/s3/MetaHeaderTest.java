package uk.nhs.nhsx.core.aws.s3;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MetaHeaderTest {

    @Test
    public void s3headerName() throws Exception {
        assertThat(new MetaHeader("key", "value").asS3MetaName(), equalTo("key"));
    }

    @Test
    public void httpHeaderName() throws Exception {
        assertThat(new MetaHeader("key", "value").asHttpHeaderName(), equalTo("X-Amz-Meta-key"));
    }
}