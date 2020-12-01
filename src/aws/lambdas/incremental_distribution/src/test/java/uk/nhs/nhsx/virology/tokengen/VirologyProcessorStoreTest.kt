package uk.nhs.nhsx.virology.tokengen

import com.google.common.io.ByteSource
import com.google.common.io.Files.asByteSource
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.http.Consts
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.S3Storage

class VirologyProcessorStoreTest {

    private val bucketName = BucketName.of("bucket")
    private val locatorSlot = slot<S3Storage.Locator>()
    private val contentTypeSlot = slot<ContentType>()
    private val bytesSlot = slot<ByteSource>()
    private val s3Storage = mockk<S3Storage> {
        every { upload(capture(locatorSlot), capture(contentTypeSlot), capture(bytesSlot)) } just Runs
    }
    private val store = VirologyProcessorStore(s3Storage, bucketName)

    @Test
    fun `stores csv file`() {
        store.storeCsv(CtaTokensCsv("file.csv", "file-content"))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("file.csv"))
        assertThat(contentTypeSlot.captured.charset).isEqualTo(Consts.UTF_8)
        assertThat(contentTypeSlot.captured.mimeType).isEqualTo("text/csv")
        assertThat(bytesSlot.captured.asCharSource(Charsets.UTF_8).read()).isEqualTo("file-content")
    }

    @Test
    fun `stores zip file`() {
        val zipFile = createTempFile("file", ".zip")
        store.storeZip(CtaTokensZip("file.zip", zipFile))

        verify(exactly = 1) { s3Storage.upload(any(), any(), any()) }
        assertThat(locatorSlot.captured.bucket).isEqualTo(BucketName.of("bucket"))
        assertThat(locatorSlot.captured.key).isEqualTo(ObjectKey.of("file.zip"))
        assertThat(contentTypeSlot.captured.mimeType).isEqualTo("application/zip")
        assertThat(bytesSlot.captured.read()).isEqualTo(asByteSource(zipFile).read())
    }
}