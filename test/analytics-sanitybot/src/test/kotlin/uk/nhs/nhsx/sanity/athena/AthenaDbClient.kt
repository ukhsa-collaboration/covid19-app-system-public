package uk.nhs.nhsx.sanity.athena

interface AsyncDbClient {
    fun submitQuery(sqlQuery: String): QueryId
    fun queryResults(queryId: QueryId): QueryResult<Unit>
}

data class QueryId(val id: String)

sealed class QueryResult<T> {
    class Waiting<T> : QueryResult<T>()
    data class Finished<T>(val results: T) : QueryResult<T>()
    data class Error<T>(val message: String) : QueryResult<T>()
}
