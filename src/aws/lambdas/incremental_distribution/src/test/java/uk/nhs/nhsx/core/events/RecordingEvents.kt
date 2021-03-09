package uk.nhs.nhsx.core.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
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
        assertThat(received.toList().map { it::class }, equalTo(events.toList()))
    }

    fun contains(vararg events: KClass<*>) {
        events.forEach { e ->
            assertThat(received.toList().map { it::class }, hasElement(e))
        }
    }
}
