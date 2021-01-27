package uk.nhs.nhsx.diagnosiskeydist.keydistribution;

import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;

import java.io.File;
import java.io.IOException;

public class SaveToFileKeyDistributor implements KeyDistributor {

    private final File distributionOutputDir;

    public SaveToFileKeyDistributor(File distributionOutputDir) {
        this.distributionOutputDir = distributionOutputDir;
    }

    @Override
    public void distribute(BucketName name, ObjectKey key, File binFile,
                           File sigFile) throws IOException {

        File zipFile = new File(distributionOutputDir, key.value);
        zipFile.getParentFile().mkdirs();

        KeyFileUtility.zipFiles(zipFile, binFile, sigFile);
    }
}
