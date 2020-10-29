package uk.nhs.nhsx.core.csvupload;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.DistributionSignature;
import uk.nhs.nhsx.core.signature.SigningHeaders;
import uk.nhs.nhsx.highriskpostcodesupload.RiskLevel;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodesCsvParser;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodesV2;

import java.io.IOException;
import java.util.Map;

import static uk.nhs.nhsx.core.aws.s3.Sources.byteSourceFor;

public class CsvUploadService {

    private final BucketName bucketName;
    private final ObjectKey distributionObjKeyName;
    private final ObjectKey distributionV2ObjKeyName;
    private final ObjectKey rawObjKeyName;
    private final ObjectKey metaDataObjKeyName;
    private final DatedSigner signer;
    private final AwsS3 s3Client;
    private final AwsCloudFront awsCloudFront;
    private final String cloudFrontDistributionId;
    private final String cloudFrontInvalidationPattern;

    public CsvUploadService(BucketName bucketName,
                            ObjectKey distributionObjKeyName,
                            ObjectKey distributionV2ObjKeyName,
                            ObjectKey rawObjKeyName,
                            ObjectKey metaDataObjKeyName,
                            DatedSigner signer,
                            AwsS3 s3Client,
                            AwsCloudFront awsCloudFront,
                            String cloudFrontDistributionId,
                            String cloudFrontInvalidationPattern) {
        this.bucketName = bucketName;
        this.distributionObjKeyName = distributionObjKeyName;
        this.distributionV2ObjKeyName = distributionV2ObjKeyName;
        this.rawObjKeyName = rawObjKeyName;
        this.metaDataObjKeyName = metaDataObjKeyName;
        this.signer = signer;
        this.s3Client = s3Client;
        this.awsCloudFront = awsCloudFront;
        this.cloudFrontDistributionId = cloudFrontDistributionId;
        this.cloudFrontInvalidationPattern = cloudFrontInvalidationPattern;
    }

    public void upload(String csv) {
        var riskLevels = retrievePostDistrictRiskLevels();

        uploadPostDistrictsV1(csv, riskLevels);
        uploadPostDistrictsV2(csv, riskLevels);

        awsCloudFront.invalidateCache(cloudFrontDistributionId, cloudFrontInvalidationPattern);
    }

    private void uploadPostDistrictsV1(String csv, Map<String, RiskLevel> riskLevels) {
        var riskyPostCodes = RiskyPostCodesCsvParser.parse(csv, riskLevels);
        var byteSource = byteSourceFor(Jackson.toJson(riskyPostCodes));
        var signatureResult = signer.sign(new DistributionSignature(byteSource));

        s3Client.upload(
            S3Storage.Locator.of(bucketName, distributionObjKeyName),
            ContentType.APPLICATION_JSON,
            byteSource,
            SigningHeaders.fromDatedSignature(signatureResult)
        );

        s3Client.upload(
            S3Storage.Locator.of(bucketName, rawObjKeyName),
            ContentType.TEXT_PLAIN,
            byteSourceFor(csv)
        );
    }

    private void uploadPostDistrictsV2(String csv, Map<String, RiskLevel> riskLevels) {
        var riskyPostCodesV2 = new RiskyPostCodesV2(RiskyPostCodesCsvParser.parseV2(csv, riskLevels), riskLevels);
        var byteSourceV2 = byteSourceFor(Jackson.toJson(riskyPostCodesV2));
        var signatureResultV2 = signer.sign(new DistributionSignature(byteSourceV2));

        s3Client.upload(
            S3Storage.Locator.of(bucketName, distributionV2ObjKeyName),
            ContentType.APPLICATION_JSON,
            byteSourceV2,
            SigningHeaders.fromDatedSignature(signatureResultV2)
        );
    }

    private Map<String, RiskLevel> retrievePostDistrictRiskLevels() {
        return s3Client
            .getObject(this.bucketName.value, this.metaDataObjKeyName.value)
            .map(this::convertS3ObjectToRiskLevels)
            .orElseThrow(() -> new RuntimeException(
                "Missing post district metadata. Bucket: " + this.bucketName.value +
                    " does not have key: " + this.metaDataObjKeyName.value
            ));
    }

    private Map<String, RiskLevel> convertS3ObjectToRiskLevels(S3Object s3Object) {
        try (S3ObjectInputStream s3inputStream = s3Object.getObjectContent()) {
            return Jackson.readJson(s3inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
