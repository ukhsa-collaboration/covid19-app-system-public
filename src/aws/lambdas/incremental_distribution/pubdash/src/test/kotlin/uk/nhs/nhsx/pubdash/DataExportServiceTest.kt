package uk.nhs.nhsx.pubdash

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.apache.http.Consts
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.pubdash.datasets.AnalyticsDataSet
import uk.nhs.nhsx.pubdash.datasets.CountryAgnosticDataset
import uk.nhs.nhsx.pubdash.datasets.CountryAgnosticRow
import uk.nhs.nhsx.pubdash.datasets.CountrySpecificDataset
import uk.nhs.nhsx.pubdash.datasets.CountrySpecificRow
import java.time.LocalDate

class DataExportServiceTest {

    private val locatorSlot = mutableListOf<Locator>()
    private val contentTypeSlot = mutableListOf<ContentType>()
    private val bytesSlot = mutableListOf<ByteArraySource>()
    private val s3Storage = mockk<S3Storage> {
        every { upload(capture(locatorSlot), capture(contentTypeSlot), capture(bytesSlot)) } just Runs
    }
    private val bucketName = BucketName.of("bucket")

    private val analyticsDataSet = object : AnalyticsDataSet {
        override fun countryAgnosticDataset(): CountryAgnosticDataset =
            CountryAgnosticDataset(
                listOf(CountryAgnosticRow(LocalDate.parse("2020-11-01"), 1, 2, 3))
            )

        override fun countrySpecificDataset(): CountrySpecificDataset =
            CountrySpecificDataset(
                listOf(CountrySpecificRow(LocalDate.parse("2020-11-01"), "lang 1", "lang 2", 1, 2, 3, 4, 5))
            )
    }

    @Test
    fun `exports to s3`() {
        DataExportService(bucketName, s3Storage, analyticsDataSet).export()

        verify(exactly = 2) { s3Storage.upload(any(), any(), any()) }

        assertThat(locatorSlot[0].bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot[1].bucket).isEqualTo(BucketName.of("bucket"))

        assertThat(locatorSlot[0].key).isEqualTo(ObjectKey.of("data/covid19_app_country_agnostic_dataset.csv"))
        assertThat(locatorSlot[1].key).isEqualTo(ObjectKey.of("data/covid19_app_country_specific_dataset.csv"))

        assertThat(contentTypeSlot[0].charset).isEqualTo(Consts.UTF_8)
        assertThat(contentTypeSlot[1].charset).isEqualTo(Consts.UTF_8)

        assertThat(contentTypeSlot[0].mimeType).isEqualTo("text/csv")
        assertThat(contentTypeSlot[1].mimeType).isEqualTo("text/csv")

        assertThat(String(bytesSlot[0].toArray())).contains(""""2020-11-01",1,2,3""")
        assertThat(String(bytesSlot[1].toArray())).contains(""""2020-11-01","lang 1","lang 2",1,2,3,4,5""")
    }
}
