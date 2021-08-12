package uk.nhs.nhsx.analyticsevents

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class PayloadValidatorTest {

    @Test
    fun `valid payload returns value`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "A1"
                },
                "events": [
                    {
                        "type": "exposure_window",
                        "version": 1,
                        "payload": {
                            "date": "2020-08-24T21:59:00Z",
                            "infectiousness": "high|none|standard",
                            "scanInstances": [
                                {
                                    "minimumAttenuation": 1,
                                    "secondsSinceLastScan": 5,
                                    "typicalAttenuation": 2
                                }
                            ],
                            "riskScore": "FIXME: sample int value (range?) or string value (enum?)"
                        }
                    }
                ]
            }
        """.trimIndent()
        )

        expectThat(payload).isNotNull()
    }

    @Test
    fun `payload field of objects in events array can receive any valid json`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "A1"
                },
                "events": [
                    {
                        "type": "exposure_window",
                        "version": 1,
                        "payload": {
                            "randomField1": 1,
                            "randomField2": "abc",
                            "randomField3": true,
                            "randomField4": [1, 2, 3],
                            "randomField5": {
                                "nested": "data"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()
        )

        expectThat(payload).isNotNull()

        val events = payload!!["events"] as List<*>
        expectThat(events).hasSize(1)

        val eventPayload = (events[0] as Map<*, *>)["payload"] as Map<*, *>
        expectThat(eventPayload["randomField1"]).isEqualTo(1)
        expectThat(eventPayload["randomField2"]).isEqualTo("abc")
        expectThat(eventPayload["randomField3"]).isEqualTo(true)
        expectThat(eventPayload["randomField4"]).isEqualTo(listOf(1, 2, 3))
        expectThat(eventPayload["randomField5"]).isEqualTo(mapOf("nested" to "data"))
    }

    @Test
    fun `metadata can accept optional local authority`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "A1",
                    "localAuthority" : "E09000012"
                },
                "events": [
                    {
                        "type": "exposure_window",
                        "version": 1,
                        "payload": {"any": "data"}
                    }
                ]
            }
        """.trimIndent()
        )

        expectThat(payload).isNotNull()
        val metadata = payload!!["metadata"] as Map<*, *>
        expectThat(metadata["localAuthority"]).isEqualTo("E09000012")
    }


    @Test
    fun `empty json returns empty`() {
        val payload = PayloadValidator().maybeValidPayload("{}")

        expectThat(payload).isNull()
    }

    @Test
    fun `invalid json returns empty`() {
        val payload = PayloadValidator().maybeValidPayload("!@£$%^")

        expectThat(payload).isNull()
    }

    @Test
    fun `missing fields in json returns empty`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)"
                },
                "events": [
                    {
                        "type": "exposure_window",
                        "version": 1,
                        "payload": {
                            "date": "2020-08-24T21:59:00Z",
                            "infectiousness": "high|none|standard",
                            "scanInstances": [
                                {
                                    "minimumAttenuation": 1,
                                    "secondsSinceLastScan": 5,
                                    "typicalAttenuation": 2
                                }
                            ],
                            "riskScore": "FIXME: sample int value (range?) or string value (enum?)"
                        }
                    }
                ]
            }
        """.trimIndent()
        )

        expectThat(payload).isNull()
    }

    @Test
    fun `missing events payload returns empty`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "A1"
                }
            }
        """.trimIndent()
        )

        expectThat(payload).isNull()
    }

    @Test
    fun `empty events payload returns empty`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "A1"
                },
                "events": [
                    {}
                ]
            }
        """.trimIndent()
        )

        expectThat(payload).isNull()
    }

    @Test
    fun `invalid metadata type returns empty`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": "not supposed to be a string",
                "events": [
                    {}
                ]
            }
        """.trimIndent()
        )

        expectThat(payload).isNull()
    }

    @Test
    fun `invalid events type returns empty`() {
        val payload = PayloadValidator().maybeValidPayload(
            """
            {
                "metadata": {
                    "operatingSystemVersion": "iPhone OS 13.5.1 (17F80)",
                    "latestApplicationVersion": "3.0",
                    "deviceModel": "iPhone11,2",
                    "postalDistrict": "A1"
                },
                "events": "not supposed to be a string"
            }
        """.trimIndent()
        )

        expectThat(payload).isNull()
    }
}
