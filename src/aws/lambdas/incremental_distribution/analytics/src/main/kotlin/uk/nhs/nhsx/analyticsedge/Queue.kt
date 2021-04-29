package uk.nhs.nhsx.analyticsedge

data class QueryId(val id: String)
enum class Dataset { Adoption, Aggregate, Enpic, Isolation, Poster }
data class QueueMessage(val queryId: QueryId, val dataset: Dataset)
