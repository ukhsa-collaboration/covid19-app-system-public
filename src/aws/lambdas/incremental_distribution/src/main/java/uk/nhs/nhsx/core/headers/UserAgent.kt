package uk.nhs.nhsx.core.headers

data class UserAgent(private val value: String) {

    fun mobileAppVersion(): MobileAppVersion {
        val versionParts = parseVersion()

        return when (versionParts.size) {
            3 -> MobileAppVersion.Version(major = versionParts[0], minor = versionParts[1], patch = versionParts[2])
            2 -> MobileAppVersion.Version(major = versionParts[0], minor = versionParts[1])
            else -> MobileAppVersion.Unknown
        }
    }

    private fun parseVersion(): List<Int> =
        value.split(",")
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



