@file:Suppress("EnumEntryName")

package uk.nhs.nhsx.localstats.domain

enum class CoronavirusMetric {
    newCasesByPublishDate,
    newCasesByPublishDateChange,
    newCasesByPublishDateChangePercentage,
    newCasesByPublishDateDirection,
    newCasesByPublishDateRollingSum,
    newCasesBySpecimenDateRollingRate
}
