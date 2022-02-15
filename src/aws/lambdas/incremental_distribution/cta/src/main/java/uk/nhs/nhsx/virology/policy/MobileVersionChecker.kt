package uk.nhs.nhsx.virology.policy

import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileAppVersion.*

typealias MobileVersionChecker = (MobileAppVersion) -> Boolean

object AllVersions : MobileVersionChecker {
    override fun invoke(version: MobileAppVersion) = true
}

class FromMinimumInclusive(private val version: Version) : MobileVersionChecker {
    override fun invoke(mobileAppVersion: MobileAppVersion) = when (mobileAppVersion) {
        is Version -> mobileAppVersion >= version
        Unknown -> false
    }
}
