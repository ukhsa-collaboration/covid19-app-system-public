package uk.nhs.nhsx.analyticsedge.persistence

import com.amazonaws.services.athena.AmazonAthena
import com.amazonaws.services.athena.model.ResultConfiguration
import com.amazonaws.services.athena.model.StartQueryExecutionRequest
import uk.nhs.nhsx.core.aws.s3.BucketName

class AthenaAsyncDbClient(private val athena: AmazonAthena) : AsyncDbClient {

    override fun submitQueryWithOutputLocation(sqlQuery: String, outputBucket: BucketName, prefix: String) {
        athena.startQueryExecution(
            StartQueryExecutionRequest()
                .withResultConfiguration(
                    ResultConfiguration().withOutputLocation("s3://${outputBucket.value}/$prefix")
                )
                .withQueryString(sqlQuery)
        )
    }
}
