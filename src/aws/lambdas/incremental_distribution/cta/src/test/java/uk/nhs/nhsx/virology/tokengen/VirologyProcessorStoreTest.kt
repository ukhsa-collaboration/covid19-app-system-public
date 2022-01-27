package uk.nhs.nhsx.virology.tokengen

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.ContentType
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.testhelper.assertions.asString
import uk.nhs.nhsx.testhelper.assertions.captured
import uk.nhs.nhsx.testhelper.assertions.withCaptured
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class VirologyProcessorStoreTest {

    private val bucketName = BucketName.of("bucket")
    private val locator = slot<Locator>()
    private val contentType = slot<ContentType>()
    private val bytes = slot<ByteArraySource>()
    private val awsS3 = mockk<AwsS3> {
        every {
            upload(
                capture(locator),
                capture(contentType),
                capture(bytes)
            )
        } just runs
    }
    private val store = VirologyProcessorStore(awsS3, bucketName)

    @Test
    fun `stores csv file`() {
        store.storeCsv(CtaTokensCsv("file.csv", "file-content"))

        verify(exactly = 1) { awsS3.upload(any(), any(), any()) }

        expectThat(locator).withCaptured {
            get(Locator::bucket).isEqualTo(BucketName.of("bucket"))
            get(Locator::key).isEqualTo(ObjectKey.of("file.csv"))
        }

        expectThat(contentType).captured.isEqualTo(ContentType.of("text/csv"))
        expectThat(bytes).captured.asString().isEqualTo("file-content")
    }

    @Test
    fun `stores zip file`(@TempDir tempDir: Path) {
        val zipFile = tempDir.toFile()
            .run { File(this, "file.zip") }
            .apply { createNewFile() }

        store.storeZip(CtaTokensZip("file.zip", zipFile))

        verify(exactly = 1) { awsS3.upload(any(), any(), any()) }

        expectThat(locator).withCaptured {
            get(Locator::bucket).isEqualTo(BucketName.of("bucket"))
            get(Locator::key).isEqualTo(ObjectKey.of("file.zip"))
        }

        expectThat(contentType).captured.isEqualTo(ContentType.of("application/zip"))
        expectThat(bytes).captured.get { toArray() }.isEqualTo(Files.readAllBytes(zipFile.toPath()))
    }
}
