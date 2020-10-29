package uk.nhs.nhsx.core.csvupload.s3;

import com.amazonaws.services.s3.model.S3Object;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.diagnosiskeydist.FakeS3;
import uk.nhs.nhsx.highriskpostcodesupload.RiskLevel;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class FakeCsvUploadServiceS3 extends FakeS3 implements AwsS3 {

    @Override
    public Optional<S3Object> getObject(String bucketName, String key) {
        var riskLevel = new RiskLevel("yellow",
            Collections.singletonMap("en", "PostCodes"),
            Collections.singletonMap("en", "PostCodes"),
            Collections.singletonMap("en", "PostCodes"),
            Collections.singletonMap("en", "PostCodes"),
            Collections.singletonMap("en", "PostCodes")
        );

        var riskLevelMap = Map.of(
            "EN.Tier1", riskLevel,
            "EN.Tier2", riskLevel,
            "EN.Tier3", riskLevel,
            "WA.Tier1", riskLevel,
            "WA.Tier2", riskLevel,
            "WA.Tier3", riskLevel
        );

        String json = Jackson.toJson(riskLevelMap);
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(json.getBytes()));
        return Optional.of(s3Object);
    }

}
