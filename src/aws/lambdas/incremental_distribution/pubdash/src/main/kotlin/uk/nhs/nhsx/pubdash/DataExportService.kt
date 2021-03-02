package uk.nhs.nhsx.pubdash

import org.apache.http.Consts
import org.apache.http.entity.ContentType
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.pubdash.datasets.AnalyticsDataSet

class DataExportService(private val bucketName: BucketName,
                        private val s3Storage: S3Storage,
                        private val analyticsDataSet: AnalyticsDataSet) {

    fun export() {
        uploadToS3(analyticsDataSet.countryAgnosticDataset())
        uploadToS3(analyticsDataSet.countrySpecificDataset())
    }

    private fun uploadToS3(csvS3Object: CsvS3Object) {
        s3Storage.upload(
            Locator.of(bucketName, csvS3Object.objectKey()),
            ContentType.create("text/csv", Consts.UTF_8),
            ByteArraySource.fromUtf8String(csvS3Object.csv())
        )
    }
}
