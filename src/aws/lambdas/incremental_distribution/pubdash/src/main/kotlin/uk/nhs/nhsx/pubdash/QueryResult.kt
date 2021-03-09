package uk.nhs.nhsx.pubdash

sealed class QueryResult<T> {
    class Waiting<T> : QueryResult<T>()
    data class Finished<T>(val results: T) : QueryResult<T>()
    data class Error<T>(val message: String) : QueryResult<T>()
}
