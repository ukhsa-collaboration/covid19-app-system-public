package uk.nhs.nhsx.testhelper.assertions

import strikt.api.Assertion

val <K, V> Assertion.Builder<Map.Entry<K, V>>.key get() = get("key") { key }
val <K, V> Assertion.Builder<Map.Entry<K, V>>.value get() = get("key") { value }
