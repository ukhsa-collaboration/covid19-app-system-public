package uk.nhs.nhsx.core.signature;

import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.collection.IsArrayWithSize;
import org.junit.jupiter.api.Test;
import uk.nhs.nhsx.core.aws.s3.MetaHeader;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SigningHeadersTest {
    @Test
    public void toHeaders() throws Exception {

        MetaHeader[] headers = SigningHeaders.fromDatedSignature(
                new DatedSignature(
                        new DatedSignature.SignatureDate(
                                "somedate", Instant.EPOCH
                        ),
                        new Signature(
                                KeyId.of("some-key"),
                                SigningAlgorithmSpec.ECDSA_SHA_256,
                                new byte[]{48, 49, 50}
                        )
                )
        );

        assertThat(headers, matchesMeta(KeyId.of("some-key"), "MDEy", "somedate"));
    }

    public static Matcher<MetaHeader[]> matchesMeta(KeyId keyId, String signature, String date) {
        return new TypeSafeDiagnosingMatcher<MetaHeader[]>() {
            @Override
            protected boolean matchesSafely(MetaHeader[] headers, Description description) {
                assertThat(headers, IsArrayWithSize.arrayWithSize(2));
                assertThat(headers[0].asS3MetaName(), equalTo("Signature"));
                assertThat(headers[0].value, equalTo(String.format("keyId=\"%s\",signature=\"%s\"", keyId.value, signature)));

                assertThat(headers[1].asS3MetaName(), equalTo("Signature-Date"));
                assertThat(headers[1].value, equalTo(date));
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("matches expected headers for a dated signature");
            }
        };
    }
}