package uk.nhs.nhsx.core.headers

sealed class MobileAppVersion {

    data class Version(val major: Int, val minor: Int, val patch: Int = 0) : Comparable<Version>, MobileAppVersion() {
        val semVer = "$major.$minor.$patch"

        override fun compareTo(other: Version): Int = when {
            major > other.major -> 1
            major < other.major -> -1
            minor > other.minor -> 1
            minor < other.minor -> -1
            patch > other.patch -> 1
            patch < other.patch -> -1
            else -> 0
        }
    }

    object Unknown : MobileAppVersion()
}
