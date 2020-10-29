package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.services.s3.model.S3Object
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.csvupload.s3.FakeCsvUploadServiceS3
import java.io.InputStreamReader


class RiskPostCodesV2Test {

    private val bucketName = "some-bucket-name"
    private val objectKey = ObjectKey.of("some-object-key")
    private val s3Client = FakeCsvUploadServiceS3()

    @Test
    fun `deserialize json`() {
        val s3Object: S3Object = s3Client.getObject(bucketName, objectKey.toString()).get()
        assertThat(s3Object.objectContent).isNotNull()

        val resourceAsStream = s3Object.objectContent
        val reader = InputStreamReader(resourceAsStream)
        val riskLevels = ObjectMapper().readValue(reader, object : TypeReference<Map<String, RiskLevel>>() {})
        assertThat(riskLevels.keys).contains("EN.Tier1", "EN.Tier2", "EN.Tier3", "WA.Tier1", "WA.Tier2", "WA.Tier3")
    }

}