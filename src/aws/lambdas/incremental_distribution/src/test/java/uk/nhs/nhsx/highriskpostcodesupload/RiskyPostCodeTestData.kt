package uk.nhs.nhsx.highriskpostcodesupload

object RiskyPostCodeTestData {

    val tierMetadata = mapOf(
        "EN.Tier1" to mapOf(
            "colorScheme" to "yellow",
            "name" to mapOf("en" to "yellow name"),
            "heading" to mapOf("en" to "yellow heading"),
            "content" to mapOf("en" to "yellow content"),
            "linkTitle" to mapOf("en" to "yellow link title"),
            "linkUrl" to mapOf("en" to "yellow link url"),
            "policyData" to mapOf(
                "localAuthorityRiskTitle" to mapOf("en" to "yellow title"),
                "heading" to mapOf("en" to "yellow heading"),
                "content" to mapOf("en" to "yellow content"),
                "footer" to mapOf("en" to "yellow footer"),
                "policies" to emptyList<Map<String, Any>>(),
            )
        ),
        "EN.Tier2" to mapOf(
            "colorScheme" to "amber",
            "name" to mapOf("en" to "amber name"),
            "heading" to mapOf("en" to "amber heading"),
            "content" to mapOf("en" to "amber content"),
            "linkTitle" to mapOf("en" to "amber link title"),
            "linkUrl" to mapOf("en" to "amber link url"),
            "policyData" to mapOf(
                "localAuthorityRiskTitle" to mapOf("en" to "amber title"),
                "heading" to mapOf("en" to "amber heading"),
                "content" to mapOf("en" to "amber content"),
                "footer" to mapOf("en" to "amber footer"),
                "policies" to emptyList<Map<String, Any>>(),
            )
        ),
        "EN.Tier3" to mapOf(
            "colorScheme" to "red",
            "name" to mapOf("en" to "red name"),
            "heading" to mapOf("en" to "red heading"),
            "content" to mapOf("en" to "red content"),
            "linkTitle" to mapOf("en" to "red link title"),
            "linkUrl" to mapOf("en" to "red link url")
        ),
        "WA.Tier2" to mapOf(
            "colorScheme" to "yellow",
            "name" to mapOf("en" to "yellow name"),
            "heading" to mapOf("en" to "yellow heading"),
            "content" to mapOf("en" to "yellow content"),
            "linkTitle" to mapOf("en" to "yellow link title"),
            "linkUrl" to mapOf("en" to "yellow link url")
        )
    )
}
