package uk.nhs.nhsx.diagnosiskeydist

import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

object ZipFileUtility {
    private const val TEMP_DIR = "DistributionServiceTest"

    fun extractZipFileToTempLocation(file: File): File {
        val destDir = Files.createTempDirectory(TEMP_DIR).toFile()
        val buffer = ByteArray(1024)
        val zipStream = ZipInputStream(FileInputStream(file))
        var zipEntry = zipStream.nextEntry
        while (zipEntry != null) {
            val newFile = File(destDir, zipEntry.name)
            val outputStream = FileOutputStream(newFile)
            var length: Int
            while (zipStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.close()
            zipEntry = zipStream.nextEntry
        }
        zipStream.close()
        return destDir
    }

    fun extractZipFileToTempLocation(file: File, password:String): File {
        val destDir = Files.createTempDirectory(TEMP_DIR).toFile()

        val zipFile = ZipFile (file)
        if (zipFile.isEncrypted) {
            zipFile.setPassword(password.toCharArray())
        }
        zipFile.extractAll(destDir.absolutePath)
        return destDir
    }
}
