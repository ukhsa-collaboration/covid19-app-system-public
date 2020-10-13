package uk.nhs.nhsx.virology.tokengen

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.diagnosiskeydist.ZipFileUtility
import uk.nhs.nhsx.virology.CtaToken
import java.io.File

class VirologyProcessorExportsTest {

    private val csvContent = """
            cta token, test result, test end date
            pesddgrq, POSITIVE, 2020-10-06T00:00:00Z
            gve9v72v, POSITIVE, 2020-10-06T00:00:00Z
            fveeqkrn, POSITIVE, 2020-10-06T00:00:00Z
        """.trimIndent()

    @Test
    fun `generates csv`() {
        val csv = VirologyProcessorExports.csvFrom(
            listOf(
                CtaToken.of("pesddgrq"),
                CtaToken.of("gve9v72v"),
                CtaToken.of("fveeqkrn")
            ),
            "POSITIVE", "2020-10-06T00:00:00Z"
        )

        assertThat(csv).isEqualTo(csvContent)
    }

    @Test
    fun `generates zip file`() {
        val zipFile = VirologyProcessorExports.zipFrom(
            "file-name",
            CtaTokensCsv("file-name", csvContent)
        )
        assertThat(zipFile.extension).isEqualTo("zip")

        val workingDir = ZipFileUtility.extractZipFileToTempLocation(zipFile)
        val csvFile = File(workingDir, "file-name.csv")
        val csv = csvFile.readText(Charsets.UTF_8)
        assertThat(csv).isEqualTo(csvContent)
    }
}