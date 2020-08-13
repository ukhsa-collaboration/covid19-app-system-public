package uk.nhs.nhsx.highriskvenuesupload;

import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.MetaHeader;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.s3.Sources;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.DistributionSignature;
import uk.nhs.nhsx.core.signature.SigningHeaders;

public class HighRiskVenuesUploadService {

    private final HighRiskVenuesUploadConfig config;
    private final DatedSigner signer;
    private final S3Storage s3Client;
    private final AwsCloudFront awsCloudFront;
    private final HighRiskVenueCsvParser parser;

    public HighRiskVenuesUploadService(HighRiskVenuesUploadConfig config,
                                       DatedSigner awsSigner,
                                       S3Storage s3Client,
                                       AwsCloudFront awsCloudFront,
                                       HighRiskVenueCsvParser parser) {
        this.config = config;
        this.signer = awsSigner;
        this.s3Client = s3Client;
        this.awsCloudFront = awsCloudFront;
        this.parser = parser;
    }

    public VenuesUploadResult upload(String csv) {
        VenuesParsingResult parsingResult = parser.toJson(csv);

        return parsingResult
            .failureMaybe()
            .map(VenuesUploadResult::validationError)
            .orElseGet(() -> uploadMaybe(parsingResult.jsonOrThrow()));
    }

    private VenuesUploadResult uploadMaybe(String json) {
        ByteSource bytes = Sources.byteSourceFor(json);

        MetaHeader[] headers = SigningHeaders.fromDatedSignature(signer.sign(new DistributionSignature(bytes)));

        s3Client.upload(config.locator, ContentType.APPLICATION_JSON, bytes, headers);

        awsCloudFront.invalidateCache(config.cloudFrontDistId, config.cloudFrontInvalidationPattern);

        return VenuesUploadResult.ok();
    }

}
