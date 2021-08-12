package uk.nhs.nhsx.core

import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.string
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.value
import uk.nhs.nhsx.core.aws.dynamodb.TableName
import uk.nhs.nhsx.core.aws.s3.BucketName

object EnvironmentKeys {
    val SUBMISSION_BUCKET_NAME = value("SUBMISSION_BUCKET_NAME", BucketName)
    val BUCKET_NAME = value("BUCKET_NAME", BucketName)
    val DISTRIBUTION_ID = string("DISTRIBUTION_ID")
    val DISTRIBUTION_INVALIDATION_PATTERN = string("DISTRIBUTION_INVALIDATION_PATTERN")
    val SUBMISSIONS_TOKENS_TABLE = value("submission_tokens_table", TableName)
    val SUBMISSION_STORE = value("SUBMISSION_STORE", BucketName)
    val SSM_CIRCUIT_BREAKER_BASE_NAME = string("SSM_CIRCUIT_BREAKER_BASE_NAME")
}
