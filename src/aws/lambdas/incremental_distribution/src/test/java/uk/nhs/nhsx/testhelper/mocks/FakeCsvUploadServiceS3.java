package uk.nhs.nhsx.testhelper.mocks;

import com.amazonaws.services.s3.model.S3Object;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodeTestData;
import uk.nhs.nhsx.testhelper.mocks.FakeS3;

import java.io.ByteArrayInputStream;
import java.util.Optional;

public class FakeCsvUploadServiceS3 extends FakeS3 implements AwsS3 {

    @Override
    public Optional<S3Object> getObject(String bucketName, String key) {
        String json = Jackson.toJson(RiskyPostCodeTestData.INSTANCE.getTierMetadata());
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(json.getBytes()));
        return Optional.of(s3Object);
    }

}
