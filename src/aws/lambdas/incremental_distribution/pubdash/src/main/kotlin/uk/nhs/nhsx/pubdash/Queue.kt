package uk.nhs.nhsx.pubdash

data class QueryId(val id: String)
enum class Dataset { Agnostic, Country, LocalAuthority }
data class QueueMessage(val queryId: QueryId, val dataset: Dataset)
