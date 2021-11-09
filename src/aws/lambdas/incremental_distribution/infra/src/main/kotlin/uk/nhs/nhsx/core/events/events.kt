package uk.nhs.nhsx.core.events

/**
 * Event interface for all structured logs
 */
abstract class Event(
    private val category: EventCategory,
    private val metadata: Set<Pair<String, Any>> = emptySet()
) {

    constructor(
        category: EventCategory,
        vararg metadata: Pair<String, Any>
    ) : this(category, metadata.toSet())

    fun category() = category

    fun metadata() = metadata
}

enum class EventCategory {
    Error, Warning, Info, Metric, Operational, Audit
}

interface Events : (Event) -> Unit
