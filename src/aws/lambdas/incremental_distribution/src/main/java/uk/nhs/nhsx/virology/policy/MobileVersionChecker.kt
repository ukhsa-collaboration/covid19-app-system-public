package uk.nhs.nhsx.virology.policy

import uk.nhs.nhsx.core.headers.MobileAppVersion

typealias MobileVersionChecker = (MobileAppVersion) -> Boolean

object AllVersions : MobileVersionChecker {
    override fun invoke(p1: MobileAppVersion): Boolean = true
}

class FromMinimumInclusive(private val version: MobileAppVersion.Version) : MobileVersionChecker {
    override fun invoke(mobileAppVersion: MobileAppVersion): Boolean =
        when (mobileAppVersion) {
            is MobileAppVersion.Version -> mobileAppVersion >= version
            MobileAppVersion.Unknown -> false
        }
}
