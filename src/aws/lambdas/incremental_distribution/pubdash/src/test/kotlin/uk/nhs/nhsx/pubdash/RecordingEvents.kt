package uk.nhs.nhsx.pubdash

import org.assertj.core.api.Assertions.assertThat
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Simple recording events that can be used in tests
 */
class RecordingEvents : Events, Iterable<Event> {
    private val received = CopyOnWriteArrayList<Event>()

    override fun iterator() = received.iterator()

    override fun invoke(clazz: Class<*>, event: Event) {
        println("received event: $event")
        received += event
    }

    fun clear() = received.clear()

    fun containsNoEvents() = containsExactly()

    fun containsExactly(vararg events: KClass<*>) {
        assertThat(received.toList().map { it::class }).isEqualTo(events.toList())
    }

    fun contains(vararg events: KClass<out Event>) {
        assertThat(received.toList().map { it::class }).containsAll(events.toMutableList())
    }
}
