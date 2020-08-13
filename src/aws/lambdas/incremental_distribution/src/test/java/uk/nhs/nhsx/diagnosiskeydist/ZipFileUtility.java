package uk.nhs.nhsx.diagnosiskeydist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileUtility {

    private static final String TEMP_DIR = "DistributionServiceTest";

    public static File extractZipFileToTempLocation(File file) throws IOException {
        File destDir = Files.createTempDirectory(TEMP_DIR).toFile();

        byte[] buffer = new byte[1024];
        ZipInputStream zipStream = new ZipInputStream(new FileInputStream(file));
        ZipEntry zipEntry = zipStream.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(destDir, zipEntry.getName());
            FileOutputStream outputStream = new FileOutputStream(newFile);
            int length;
            while ((length = zipStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            zipEntry = zipStream.getNextEntry();
        }
        zipStream.close();

        return destDir;
    }
}
