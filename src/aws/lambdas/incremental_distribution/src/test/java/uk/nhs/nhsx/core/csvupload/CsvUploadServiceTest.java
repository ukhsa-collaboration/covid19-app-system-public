package uk.nhs.nhsx.core.csvupload;

import org.junit.Test;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.csvupload.s3.FakeCsvUploadServiceS3;
import uk.nhs.nhsx.highriskpostcodesupload.TestDatedSigner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class CsvUploadServiceTest {

    private static final BucketName s3BucketName = BucketName.of("some-bucket");
    private static final ObjectKey s3ObjKeyName = ObjectKey.of("some-key");
    private static final ObjectKey s3V2ObjKeyName = ObjectKey.of("some-key-v2");
    private static final ObjectKey s3RawObjKeyName = ObjectKey.of("raw/risky-post-districts");
    private static final ObjectKey s3MetaDataObjKeyName = ObjectKey.of("tier-metadata");

    private static final String cloudFrontDistId = "some-dist-id";
    private static final String cloudFrontInvPattern = "some-pattern";

    private final FakeCsvUploadServiceS3 s3 = new FakeCsvUploadServiceS3();
    private final AwsCloudFront awsCloudFront = mock(AwsCloudFront.class);
    private final TestDatedSigner testSigner = new TestDatedSigner("date");
    private final CsvUploadService uploadService = new CsvUploadService(
        s3BucketName,
        s3ObjKeyName,
        s3V2ObjKeyName,
        s3RawObjKeyName,
        s3MetaDataObjKeyName,
        testSigner,
        s3,
        awsCloudFront,
        cloudFrontDistId,
        cloudFrontInvPattern
    );

    @Test
    public void testRawFileUploadedToS3Bucket() {
        uploadService.upload("" +
            "# postal_district_code, risk_indicator\n" +
            "\"CODE1\", \"H\"\n" +
            "\"CODE2\", \"M\"\n" +
            "\"CODE3\", \"L\""
        );

        assertThat(s3.count, equalTo(3));

    }

}

