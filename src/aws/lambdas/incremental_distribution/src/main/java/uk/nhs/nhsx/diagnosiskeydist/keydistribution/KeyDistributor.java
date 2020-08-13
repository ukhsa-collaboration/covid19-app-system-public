package uk.nhs.nhsx.diagnosiskeydist.keydistribution;

import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface KeyDistributor {

    void distribute(BucketName name, ObjectKey key, File binFile,
                    File sigFile) throws IOException, NoSuchAlgorithmException;

}
