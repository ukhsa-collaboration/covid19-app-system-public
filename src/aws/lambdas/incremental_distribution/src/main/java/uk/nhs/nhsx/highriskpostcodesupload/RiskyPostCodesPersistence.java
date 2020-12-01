package uk.nhs.nhsx.highriskpostcodesupload;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.DistributionSignature;
import uk.nhs.nhsx.core.signature.SigningHeaders;

import java.io.IOException;
import java.util.Map;

import static uk.nhs.nhsx.core.aws.s3.Sources.byteSourceFor;

public class RiskyPostCodesPersistence {

    private final BucketName bucketName;
    private final ObjectKey distributionObjKeyName;
    private final ObjectKey distributionV2ObjKeyName;
    private final ObjectKey backupJsonKeyName;
    private final ObjectKey rawCsvKeyName;
    private final ObjectKey metaDataObjKeyName;
    private final DatedSigner signer;
    private final AwsS3 s3Client;

    public RiskyPostCodesPersistence(BucketName bucketName,
                                     ObjectKey distributionObjKeyName,
                                     ObjectKey distributionV2ObjKeyName,
                                     ObjectKey backupJsonKeyName,
                                     ObjectKey rawCsvKeyName,
                                     ObjectKey metaDataObjKeyName,
                                     DatedSigner signer,
                                     AwsS3 s3Client) {
        this.bucketName = bucketName;
        this.distributionObjKeyName = distributionObjKeyName;
        this.distributionV2ObjKeyName = distributionV2ObjKeyName;
        this.backupJsonKeyName = backupJsonKeyName;
        this.rawCsvKeyName = rawCsvKeyName;
        this.metaDataObjKeyName = metaDataObjKeyName;
        this.signer = signer;
        this.s3Client = s3Client;
    }

    public void uploadToBackup(String json) {
        s3Client.upload(
            S3Storage.Locator.of(bucketName, backupJsonKeyName),
            ContentType.APPLICATION_JSON,
            byteSourceFor(json)
        );
    }

    public void uploadToRaw(String csv) {
        s3Client.upload(
            S3Storage.Locator.of(bucketName, rawCsvKeyName),
            ContentType.TEXT_PLAIN,
            byteSourceFor(csv)
        );
    }

    public void uploadPostDistrictsVersion1(String riskyPostCodes) {
        uploadPostDistrictsVersion(riskyPostCodes, distributionObjKeyName);
    }

    public void uploadPostDistrictsVersion2(String riskyPostCodes) {
        uploadPostDistrictsVersion(riskyPostCodes, distributionV2ObjKeyName);
    }

    private void uploadPostDistrictsVersion(String riskyPostCodes, ObjectKey objectKey) {
        var byteSource = byteSourceFor(riskyPostCodes);
        var signatureResult = signer.sign(new DistributionSignature(byteSource));

        s3Client.upload(
            S3Storage.Locator.of(bucketName, objectKey),
            ContentType.APPLICATION_JSON,
            byteSource,
            SigningHeaders.fromDatedSignature(signatureResult)
        );
    }

    public Map<String, Map<String, Object>> retrievePostDistrictRiskLevels() {
        return s3Client
            .getObject(this.bucketName.value, this.metaDataObjKeyName.value)
            .map(this::convertS3ObjectToRiskLevels)
            .orElseThrow(() -> new RuntimeException(
                "Missing post district metadata. Bucket: " + this.bucketName.value +
                    " does not have key: " + this.metaDataObjKeyName.value
            ));
    }

    private Map<String, Map<String, Object>> convertS3ObjectToRiskLevels(S3Object s3Object) {
        try (S3ObjectInputStream s3inputStream = s3Object.getObjectContent()) {
            return Jackson.readJson(s3inputStream, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}