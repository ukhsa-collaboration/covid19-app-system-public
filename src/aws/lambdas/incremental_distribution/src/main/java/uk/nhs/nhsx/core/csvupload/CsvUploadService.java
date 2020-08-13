package uk.nhs.nhsx.core.csvupload;

import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.signature.DatedSignature;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.DistributionSignature;
import uk.nhs.nhsx.core.signature.SigningHeaders;

import static uk.nhs.nhsx.core.aws.s3.Sources.byteSourceFor;

public class CsvUploadService {

    private final BucketName bucketName;
    private final ObjectKey distributionObjKeyName;
    private final DatedSigner signer;
    private final S3Storage s3Client;
    private final AwsCloudFront awsCloudFront;
    private final String cloudFrontDistributionId;
    private final String cloudFrontInvalidationPattern;
    private final CsvToJsonParser parser;

    public CsvUploadService(BucketName bucketName,
                            ObjectKey distributionObjKeyName,
                            DatedSigner signer,
                            S3Storage s3Client,
                            AwsCloudFront awsCloudFront,
                            String cloudFrontDistributionId,
                            String cloudFrontInvalidationPattern,
                            CsvToJsonParser parser) {
        this.bucketName = bucketName;
        this.distributionObjKeyName = distributionObjKeyName;
        this.signer = signer;
        this.s3Client = s3Client;
        this.awsCloudFront = awsCloudFront;
        this.cloudFrontDistributionId = cloudFrontDistributionId;
        this.cloudFrontInvalidationPattern = cloudFrontInvalidationPattern;
        this.parser = parser;
    }

    public void upload(String csv) {
        ByteSource byteSource = byteSourceFor(parser.toJson(csv));
        DatedSignature signatureResult = signer.sign(new DistributionSignature(byteSource));

        s3Client.upload(
            S3Storage.Locator.of(bucketName, distributionObjKeyName),
            ContentType.APPLICATION_JSON,
            byteSource,
            SigningHeaders.fromDatedSignature(signatureResult)
        );
        awsCloudFront.invalidateCache(cloudFrontDistributionId, cloudFrontInvalidationPattern);
    }
}
