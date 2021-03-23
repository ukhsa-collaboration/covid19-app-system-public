package uk.nhs.nhsx.virology.tokengen

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyFileUtility
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import java.io.File

class VirologyProcessorExports(private var tempDirPath: String = System.getProperty("java.io.tmpdir") + File.separator) {

    fun csvFrom(tokens: List<CtaToken>, testResult: TestResult, testEndDate: TestEndDate): String = tokens
        .map { """${it.value}, ${testResult.wireValue}, ${TestEndDate.show(testEndDate)}""" }
        .joinToString(prefix = "cta token, test result, test end date\n", separator = "\n") { it }

    fun zipFrom(zipFileName: String, ctaTokensCsv: List<CtaTokensCsv>, zipFilePassword: String? = null): File {
        val zipFileEntries = ctaTokensCsv.map {
            val csvFile = File.createTempFile(it.filename, ".csv")
            KeyFileUtility.writeToFile(csvFile, it.content.toByteArray())
            ZipFileEntry(csvFile, it.filename)
        }

        return zipFile(zipFileName, zipFileEntries, zipFilePassword)
    }

    private fun zipFile(zipFileName: String, zipEntryFile: List<ZipFileEntry>, password: String?): File {
        val filename = "$tempDirPath$zipFileName.zip"

        val zipFile = when {
            password != null -> ZipFile(filename, password.toCharArray())
            else -> ZipFile(filename)
        }

        zipEntryFile.forEach {
            zipFile.addFile(it.file, zipParametersFrom(it, password))
        }

        return zipFile.file
    }

    private fun zipParametersFrom(it: ZipFileEntry, password: String?) = ZipParameters().apply {
        fileNameInZip = it.filename
        if (password != null) {
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
        }
    }
}

data class ZipFileEntry(val file: File, val filename: String)
