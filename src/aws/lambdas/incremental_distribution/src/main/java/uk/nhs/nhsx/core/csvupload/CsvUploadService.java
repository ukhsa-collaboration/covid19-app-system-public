package uk.nhs.nhsx.core.csvupload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.signature.DatedSignature;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.DistributionSignature;
import uk.nhs.nhsx.core.signature.SigningHeaders;
import uk.nhs.nhsx.highriskpostcodesupload.RiskLevel;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodes;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodesCsvParser;
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodesV2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.nhs.nhsx.core.aws.s3.Sources.byteSourceFor;

public class CsvUploadService {

    private final BucketName bucketName;
    private final ObjectKey distributionObjKeyName;
    private final ObjectKey distributionV2ObjKeyName;
    private final ObjectKey rawObjKeyName;
    private final DatedSigner signer;
    private final S3Storage s3Client;
    private final AwsCloudFront awsCloudFront;
    private final String cloudFrontDistributionId;
    private final String cloudFrontInvalidationPattern;
    private final RiskyPostCodesCsvParser parser;

    public CsvUploadService(BucketName bucketName,
                            ObjectKey distributionObjKeyName,
                            ObjectKey distributionV2ObjKeyName,
                            ObjectKey rawObjKeyName,
                            DatedSigner signer,
                            S3Storage s3Client,
                            AwsCloudFront awsCloudFront,
                            String cloudFrontDistributionId,
                            String cloudFrontInvalidationPattern) {
        this.bucketName = bucketName;
        this.distributionObjKeyName = distributionObjKeyName;
        this.distributionV2ObjKeyName = distributionV2ObjKeyName;
        this.rawObjKeyName = rawObjKeyName;
        this.signer = signer;
        this.s3Client = s3Client;
        this.awsCloudFront = awsCloudFront;
        this.cloudFrontDistributionId = cloudFrontDistributionId;
        this.cloudFrontInvalidationPattern = cloudFrontInvalidationPattern;
        this.parser = new RiskyPostCodesCsvParser();
    }

    public void upload(String csv) {
        RiskyPostCodes riskyPostCodes = RiskyPostCodesCsvParser.parse(csv);
        ByteSource byteSource = byteSourceFor(Jackson.toJson(riskyPostCodes));
        DatedSignature signatureResult = signer.sign(new DistributionSignature(byteSource));

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


        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("highriskpostcodesupload/metadata.json");
        Map<String, RiskLevel> riskLevels;
        try {
            riskLevels = Jackson.readJson(resourceAsStream, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RiskyPostCodesV2 riskyPostCodesV2 = new RiskyPostCodesV2(RiskyPostCodesCsvParser.parseV2(csv), riskLevels);

        ByteSource byteSourceV2 = byteSourceFor(Jackson.toJson(riskyPostCodesV2));
        DatedSignature signatureResultV2 = signer.sign(new DistributionSignature(byteSourceV2));

        s3Client.upload(
            S3Storage.Locator.of(bucketName, distributionV2ObjKeyName),
            ContentType.APPLICATION_JSON,
            byteSourceV2,
            SigningHeaders.fromDatedSignature(signatureResultV2)
        );

        awsCloudFront.invalidateCache(cloudFrontDistributionId, cloudFrontInvalidationPattern);
    }

}
