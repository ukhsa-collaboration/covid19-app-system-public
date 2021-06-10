package uk.nhs.nhsx.virology.tokengen

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.diagnosiskeydist.ZipFileUtility
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult.*
import java.io.File

class VirologyProcessorExportsTest {

    private val csvContent = """
            cta token, test result, test end date
            pesddgrq, POSITIVE, 2020-10-06T00:00:00Z
            gve9v72v, POSITIVE, 2020-10-06T00:00:00Z
            fveeqkrn, POSITIVE, 2020-10-06T00:00:00Z
        """.trimIndent()
    private val virologyProcessorExports = VirologyProcessorExports(System.getProperty("java.io.tmpdir") + File.separator)

    @Test
    fun `generates csv`() {
        val csv = virologyProcessorExports.csvFrom(
            listOf(
                CtaToken.of("pesddgrq"),
                CtaToken.of("gve9v72v"),
                CtaToken.of("fveeqkrn")
            ),
            Positive, TestEndDate.of(2020, 10, 6)
        )

        assertThat(csv).isEqualTo(csvContent)
    }

    @Test
    fun `generates zip file without password protection`() {
        val zipFile = virologyProcessorExports.zipFrom(
            "not-protected-file",
            listOf(CtaTokensCsv("file-name", csvContent))
        )
        assertThat(zipFile.extension).isEqualTo("zip")

        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile)
        val csvFile = File(workingDir, "file-name.csv")
        val csv = csvFile.readText(Charsets.UTF_8)
        assertThat(csv).isEqualTo(csvContent)
    }

    @Test
    fun `generates zip file with multiple entries`() {
        val zipFile = virologyProcessorExports.zipFrom(
            "file-name",
            listOf(CtaTokensCsv("20200101", csvContent),CtaTokensCsv("20200102", csvContent),CtaTokensCsv("20200103", csvContent)),
            "password"
        )
        assertThat(zipFile.extension).isEqualTo("zip")
        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile,"password")

        readFileAndAssertContent(workingDir,"20200101.csv",csvContent)
        readFileAndAssertContent(workingDir,"20200102.csv",csvContent)
        readFileAndAssertContent(workingDir,"20200103.csv",csvContent)
    }


    @Test
    fun `generate multiple zip files with same content`() {
        val zipFile1 = virologyProcessorExports.zipFrom(
            "zip1",
            listOf(CtaTokensCsv("20200101", csvContent)),
            "password"
        )

        val zipFile2 = virologyProcessorExports.zipFrom(
            "zip2",
            listOf(CtaTokensCsv("20200101", csvContent)),
            "password"
        )
        assertThat(zipFile1.extension).isEqualTo("zip")
        assertThat(zipFile2.extension).isEqualTo("zip")

        val workingDirZip1 = ZipFileUtility.extractZipFileToTempLocation(zipFile1,"password")
        readFileAndAssertContent(workingDirZip1,"20200101.csv",csvContent)

        val workingDirZip2 = ZipFileUtility.extractZipFileToTempLocation(zipFile2,"password")
        readFileAndAssertContent(workingDirZip2,"20200101.csv",csvContent)

    }

    @Test
    fun `generates zip file with password protection`() {
        val zipFile = virologyProcessorExports.zipFrom(
            "file-name",
            listOf(CtaTokensCsv("file-name", csvContent)),
            "password"
        )
        assertThat(zipFile.extension).isEqualTo("zip")

        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile,"password")
        val csvFile = File(workingDir, "file-name.csv")
        val csv = csvFile.readText(Charsets.UTF_8)
        assertThat(csv).isEqualTo(csvContent)
    }

    private fun readFileAndAssertContent(workingDir:File, filename:String, content:String) {
        val csvFile = File(workingDir, filename)
        val csv = csvFile.readText(Charsets.UTF_8)
        assertThat(csv).isEqualTo(content)
    }

}
