package uk.nhs.nhsx.virology.tokengen

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.java.extension
import strikt.java.text
import uk.nhs.nhsx.diagnosiskeydist.ZipFileUtility
import uk.nhs.nhsx.domain.CtaToken
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult.Positive
import java.io.File
import java.nio.file.Path

class VirologyProcessorExportsTest {

    private val csvContent = """
            cta token, test result, test end date
            pesddgrq, POSITIVE, 2020-10-06T00:00:00Z
            gve9v72v, POSITIVE, 2020-10-06T00:00:00Z
            fveeqkrn, POSITIVE, 2020-10-06T00:00:00Z
        """.trimIndent()

    @Test
    fun `generates csv`(@TempDir tempDir: Path) {
        val csv = VirologyProcessorExports(tempDir).csvFrom(
            tokens = listOf(
                CtaToken.of("pesddgrq"),
                CtaToken.of("gve9v72v"),
                CtaToken.of("fveeqkrn")
            ),
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 10, 6)
        )

        expectThat(csv).isEqualTo(csvContent)
    }

    @Test
    fun `generates zip file without password protection`(@TempDir tempDir: Path) {
        val zipFile = VirologyProcessorExports(tempDir).zipFrom(
            zipFileName = "not-protected-file",
            ctaTokensCsv = listOf(CtaTokensCsv("file-name", csvContent))
        )

        expectThat(zipFile).extension.isEqualTo("zip")

        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile)
        val csvFile = File(workingDir, "file-name.csv")

        expectThat(csvFile).text().isEqualTo(csvContent)
    }

    @Test
    fun `generates zip file with multiple entries`(@TempDir tempDir: Path) {
        val zipFile = VirologyProcessorExports(tempDir).zipFrom(
            zipFileName = "file-name",
            ctaTokensCsv = listOf(
                CtaTokensCsv("20200101", csvContent),
                CtaTokensCsv("20200102", csvContent),
                CtaTokensCsv("20200103", csvContent)
            ),
            zipFilePassword = "password"
        )
        expectThat(zipFile).extension.isEqualTo("zip")

        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile, "password")

        expectThat(File(workingDir, "20200101.csv")).text().isEqualTo(csvContent)
        expectThat(File(workingDir, "20200102.csv")).text().isEqualTo(csvContent)
        expectThat(File(workingDir, "20200103.csv")).text().isEqualTo(csvContent)
    }

    @Test
    fun `generate multiple zip files with same content`(@TempDir tempDir: Path) {
        val exports = VirologyProcessorExports(tempDir)

        val zipFile1 = exports.zipFrom(
            zipFileName = "zip1",
            ctaTokensCsv = listOf(CtaTokensCsv("20200101", csvContent)),
            zipFilePassword = "password"
        )

        val zipFile2 = exports.zipFrom(
            zipFileName = "zip2",
            ctaTokensCsv = listOf(CtaTokensCsv("20200101", csvContent)),
            zipFilePassword = "password"
        )

        expectThat(zipFile1).extension.isEqualTo("zip")
        expectThat(zipFile2).extension.isEqualTo("zip")

        val workingDirZip1 = ZipFileUtility.extractZipFileToTempLocation(zipFile1, "password")
        expectThat(File(workingDirZip1, "20200101.csv")).text().isEqualTo(csvContent)

        val workingDirZip2 = ZipFileUtility.extractZipFileToTempLocation(zipFile2, "password")
        expectThat(File(workingDirZip2, "20200101.csv")).text().isEqualTo(csvContent)
    }

    @Test
    fun `generates zip file with password protection`(@TempDir tempDir: Path) {
        val zipFile = VirologyProcessorExports(tempDir).zipFrom(
            zipFileName = "file-name",
            ctaTokensCsv = listOf(CtaTokensCsv("file-name", csvContent)),
            zipFilePassword = "password"
        )

        expectThat(zipFile).extension.isEqualTo("zip")

        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile, "password")
        val csvFile = File(workingDir, "file-name.csv")

        expectThat(csvFile).text().isEqualTo(csvContent)
    }
}
