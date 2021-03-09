package uk.nhs.nhsx.diagnosiskeydist.keydistribution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KeyFileUtility {

    private static final String EXPORT_BIN = "export.bin";
    private static final String EXPORT_SIG = "export.sig";

    public static void writeToFile(File file, byte[] fileContent) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(fileContent);
        }
    }

    public static void zipFiles(File zipFile, File binFile, File sigFile) throws IOException{
        try (FileOutputStream outputStream = new FileOutputStream(zipFile)) {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                addFileToZip(zipOut, EXPORT_BIN, binFile);
                addFileToZip(zipOut, EXPORT_SIG, sigFile);
            }
        }
    }

    public static void addFileToZip(ZipOutputStream zipOut, String zipEntryName, File zipEntryFile) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(zipEntryFile)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = inputStream.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }
}
