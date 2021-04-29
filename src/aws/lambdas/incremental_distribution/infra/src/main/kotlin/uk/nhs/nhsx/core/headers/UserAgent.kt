package uk.nhs.nhsx.core.headers

import uk.nhs.nhsx.core.headers.MobileAppVersion.Unknown
import uk.nhs.nhsx.core.headers.MobileAppVersion.Version

data class UserAgent(val appVersion: MobileAppVersion, val os: MobileOS?, val osVersion: MobileOSVersion?) {

    companion object {
        fun of(value: String): UserAgent = UserAgent(value.mobileAppVersion(), value.os(), value.osVersion())

        private fun String.mobileAppVersion(): MobileAppVersion =
            try {
                val versionParts = parseVersion()
                when (versionParts.size) {
                    3 -> Version(major = versionParts[0], minor = versionParts[1], patch = versionParts[2])
                    2 -> Version(major = versionParts[0], minor = versionParts[1])
                    else -> Unknown
                }
            } catch (e: Exception) {
                Unknown
            }

        private fun String.os(): MobileOS? =
            try {
                split(",").firstOrNull { it.startsWith("p=") }?.drop(2)?.let(MobileOS::valueOf)
            } catch (e: Exception) {
                null
            }

        private fun String.osVersion(): MobileOSVersion? =
            try {
                split(",").firstOrNull { it.startsWith("o=") }?.drop(2)?.let(MobileOSVersion::of)
            } catch (e: Exception) {
                null
            }

        private fun String.parseVersion(): List<Int> =
            split(",")
                .filter { it.startsWith("v=") }
                .map { it.split("=").last() }
                .firstOrNull().orEmpty()
                .split(".")
                .map { it.toIntOrNull() }
                .toList()
                .emptyIfNullIsPresent()

        private fun List<Int?>.emptyIfNullIsPresent(): List<Int> =
            when {
                any { it == null } -> emptyList()
                else -> filterNotNull()
            }

    }
}



