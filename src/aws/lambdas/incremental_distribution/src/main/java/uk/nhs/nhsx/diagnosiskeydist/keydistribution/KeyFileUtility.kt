package uk.nhs.nhsx.diagnosiskeydist.keydistribution

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object KeyFileUtility {
    private const val EXPORT_BIN = "export.bin"
    private const val EXPORT_SIG = "export.sig"

    fun writeToFile(file: File, fileContent: ByteArray) {
        FileOutputStream(file).use { it.write(fileContent) }
    }

    fun zipFiles(zipFile: File, binFile: File, sigFile: File) {
        FileOutputStream(zipFile).use {
            ZipOutputStream(it).use { zipOut ->
                addFileToZip(zipOut, EXPORT_BIN, binFile)
                addFileToZip(zipOut, EXPORT_SIG, sigFile)
            }
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, zipEntryName: String, zipEntryFile: File) {
        FileInputStream(zipEntryFile).use { inputStream ->
            val zipEntry = ZipEntry(zipEntryName)
            zipOut.putNextEntry(zipEntry)
            inputStream.copyTo(zipOut)
        }
    }
}
