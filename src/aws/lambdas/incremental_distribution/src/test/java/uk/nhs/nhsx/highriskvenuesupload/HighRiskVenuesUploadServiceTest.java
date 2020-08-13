package uk.nhs.nhsx.highriskvenuesupload;

import com.amazonaws.services.s3.model.S3Object;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.highriskpostcodesupload.TestDatedSigner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.nhs.nhsx.TestData.RISKY_VENUES_UPLOAD_PAYLOAD;
import static uk.nhs.nhsx.TestData.STORED_RISKY_VENUES_UPLOAD_PAYLOAD;
import static uk.nhs.nhsx.core.signature.SigningHeadersTest.matchesMeta;

public class HighRiskVenuesUploadServiceTest {

    private static final BucketName s3BucketName = BucketName.of("some-bucket");
    private static final ObjectKey s3ObjKeyName = ObjectKey.of("some-key");
    private static final String cloudFrontDistId = "some-dist-id";
    private static final String cloudFrontInvPattern = "some-pattern";

    private final FakeS3Storage s3 = new FakeS3Storage();
    private final AwsCloudFront awsCloudFront = mock(AwsCloudFront.class);
    private final HighRiskVenueCsvParser parser = new HighRiskVenueCsvParser();

    private final HighRiskVenuesUploadConfig config = new HighRiskVenuesUploadConfig(
        s3BucketName, s3ObjKeyName,
        cloudFrontDistId, cloudFrontInvPattern
    );

    private final TestDatedSigner testSigner = new TestDatedSigner("date");

    private final HighRiskVenuesUploadService service = new HighRiskVenuesUploadService(
        config, testSigner, s3, awsCloudFront, parser
    );

    @Test
    public void uploadsCsv() throws IOException {
        service.upload(RISKY_VENUES_UPLOAD_PAYLOAD);

        verifyHappyPath(STORED_RISKY_VENUES_UPLOAD_PAYLOAD);
    }

    @Test
    public void uploadsEmptyCsv() throws IOException {
        service.upload("# venue_id, start_time, end_time");

        verifyHappyPath("{\"venues\":[]}");
    }

    @Test
    public void uploadsWhenS3ObjectDoesNotExist() throws IOException {
        service.upload(RISKY_VENUES_UPLOAD_PAYLOAD);

        verifyHappyPath(STORED_RISKY_VENUES_UPLOAD_PAYLOAD);
    }

    @Test
    public void validationErrorIfNoBody() {
        VenuesUploadResult result = service.upload(null);

        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError);
        assertThat(result.message).isEqualTo("validation error: No payload");

        assertThat(s3.count).isEqualTo(0);
        verifyNoInteractions(awsCloudFront);
    }

    @Test
    public void validationErrorIfEmptyBody() {
        VenuesUploadResult result = service.upload("");

        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError);
        assertThat(result.message).isEqualTo("validation error: No payload");

        assertThat(s3.count).isEqualTo(0);
        verifyNoInteractions(awsCloudFront);
    }

    @Test
    public void validationErrorIfWhitespaceBody() {
        VenuesUploadResult result = service.upload("    ");

        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError);
        assertThat(result.message).isEqualTo("validation error: No payload");
        assertThat(s3.count).isEqualTo(0);
        verifyNoInteractions(awsCloudFront);
    }

    @Test
    public void validationErrorIfInvalidHeader() {
        VenuesUploadResult result = service.upload("# start_time, venue_id, end_time");

        assertThat(result.type).isEqualTo(VenuesUploadResult.ResultType.ValidationError);
        assertThat(result.message).isEqualTo("validation error: Invalid header");

        assertThat(s3.count).isEqualTo(0);
        verifyNoInteractions(awsCloudFront);
    }

    private void verifyHappyPath(String payload) throws IOException {
        assertThat(s3.count, equalTo(1));
        assertThat(s3.bucket, equalTo(s3BucketName));
        assertThat(s3.name, equalTo(s3ObjKeyName));
        assertThat(s3.bytes.read(), equalTo(payload.getBytes(StandardCharsets.UTF_8)));

        MatcherAssert.assertThat(s3.meta, matchesMeta(testSigner.keyId, "AAECAwQ=", "date"));

        verify(awsCloudFront, times(1)).invalidateCache(cloudFrontDistId, cloudFrontInvPattern);
    }

}