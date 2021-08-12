package uk.nhs.nhsx.core.events

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Simple recording events that can be used in tests
 */
class RecordingEvents : Events, Iterable<Event> {
    private val received = CopyOnWriteArrayList<Event>()

    override fun iterator() = received.iterator()

    override fun invoke(event: Event) {
        println("received event: $event")
        received += event
    }
}
