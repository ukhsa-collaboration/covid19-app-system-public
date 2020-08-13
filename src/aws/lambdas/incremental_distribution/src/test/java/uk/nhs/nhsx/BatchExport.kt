package uk.nhs.nhsx

import batchZipCreation.Exposure.*
import uk.nhs.nhsx.diagnosiskeydist.ZipFileUtility
import java.io.File
import java.io.InputStream
import java.nio.file.Files

object BatchExport {

    private const val BIN_FILE_HEADER = "EK Export v1    "

    fun tekExportFromZipFile(file: File): TemporaryExposureKeyExport {
        val workingDir = ZipFileUtility.extractZipFileToTempLocation(file)
        val binFile = File(workingDir, "export.bin")
        val bytes = Files.readAllBytes(binFile.toPath())
        val payloadBytes = ByteArray(bytes.size - BIN_FILE_HEADER.toByteArray().size)
        System.arraycopy(bytes, BIN_FILE_HEADER.toByteArray().size, payloadBytes, 0, payloadBytes.size)
        return TemporaryExposureKeyExport.parseFrom(payloadBytes)
    }

    fun tekListFromZipFile(file: File): List<TemporaryExposureKey>
        = tekExportFromZipFile(file).keysList

    fun tekSignatureListFromZipFile(file: File): TEKSignatureList {
        val workingDir = ZipFileUtility.extractZipFileToTempLocation(file)
        val sigFile = File(workingDir, "export.sig")
        val bytes = Files.readAllBytes(sigFile.toPath())
        return TEKSignatureList.parseFrom(bytes)
    }

    fun tekExportFrom(inputStream: InputStream): TemporaryExposureKeyExport {
        val file = createTempFile()
        file.outputStream().use { inputStream.copyTo(it) }
        return tekExportFromZipFile(file)
    }

}