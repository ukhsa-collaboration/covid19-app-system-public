package uk.nhs.nhsx.testhelper.mocks

import com.amazonaws.services.s3.model.S3Object
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodeTestData.tierMetadata
import java.io.ByteArrayInputStream
import java.util.*

class FakeCsvUploadServiceS3 : FakeS3(), AwsS3 {
    override fun getObject(locator: Locator) = Optional.of(S3Object().apply {
        setObjectContent(ByteArrayInputStream(Json.toJson(tierMetadata).toByteArray()))
    })
}
