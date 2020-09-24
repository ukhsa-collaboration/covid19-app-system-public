package uk.nhs.nhsx.core.csvupload;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodesCsvParser;
import uk.nhs.nhsx.highriskpostcodesupload.TestDatedSigner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.mock;

public class CsvUploadServiceTest {
    private static final BucketName s3BucketName = BucketName.of("some-bucket");
    private static final ObjectKey s3ObjKeyName = ObjectKey.of("some-key");
    private static final ObjectKey s3RawObjKeyName = ObjectKey.of("raw/risky-post-districts");

    private static final String cloudFrontDistId = "some-dist-id";
    private static final String cloudFrontInvPattern = "some-pattern";

    private final FakeS3Storage s3 = new FakeS3Storage();
    private final AwsCloudFront awsCloudFront = mock(AwsCloudFront.class);
    private final RiskyPostCodesCsvParser parser = new RiskyPostCodesCsvParser();
    private final TestDatedSigner testSigner = new TestDatedSigner("date");

    @Test
    public void testRawFileUploadedToS3Bucket() {
        CsvUploadService uploadService = new CsvUploadService(
            s3BucketName,
            s3ObjKeyName,
            s3RawObjKeyName,
            testSigner,
            s3,
            awsCloudFront,
            cloudFrontDistId,
            cloudFrontInvPattern,
            parser
        );

        uploadService.upload( "" +
            "# postal_district_code, risk_indicator\n" +
            "\"CODE1\", \"H\"\n" +
            "\"CODE2\", \"M\"\n" +
            "\"CODE3\", \"L\"");

        assertThat(s3.count, equalTo(2));
        assertThat(s3.name, equalTo(s3RawObjKeyName));

    }

}
