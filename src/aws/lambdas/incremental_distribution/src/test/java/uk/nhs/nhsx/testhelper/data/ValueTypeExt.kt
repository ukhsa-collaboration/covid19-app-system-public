package uk.nhs.nhsx.testhelper.data

import dev.forkhandles.values.Value
import kotlin.reflect.jvm.isAccessible

inline fun <reified T : Value<*>> constructWith(value: String) = T::class.constructors.first().let {
    it.isAccessible = true
    it.call(value)
}
