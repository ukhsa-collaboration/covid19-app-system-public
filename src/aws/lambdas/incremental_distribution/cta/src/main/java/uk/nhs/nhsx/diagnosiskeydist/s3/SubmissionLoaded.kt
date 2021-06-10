package uk.nhs.nhsx.diagnosiskeydist.s3

import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory.Info
import uk.nhs.nhsx.core.events.EventCategory.Warning

data class SubmissionLoaded(val bucket: BucketName, val objectKey: String) : Event(Info)
data class SubmissionMissing(val bucket: BucketName, val objectKey: String) : Event(Warning)
