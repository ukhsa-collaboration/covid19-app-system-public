package uk.nhs.nhsx.analyticsevents

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
        assertThat(payload, present())
    }

    @Test
    fun `empty json returns empty`() {
        val payload = PayloadValidator().maybeValidPayload("{}")
        assertThat(payload, absent())
    }

    @Test
    fun `invalid json returns empty`() {
        val payload = PayloadValidator().maybeValidPayload("!@£$%^")
        assertThat(payload, absent())
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
        assertThat(payload, absent())
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
        assertThat(payload, absent())
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
        assertThat(payload, absent())
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
        assertThat(payload, absent())
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
        assertThat(payload, absent())
    }

}
