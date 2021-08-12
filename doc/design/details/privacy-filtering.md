# Privacy filtering

The data the mobile app submits as analytics data to the COVID-19 App backend include the post district and local authority information entered by the user.

The combination of the two fields can lead under certain circumstances to the identification of the mobile app user (when the intersection between post district and local authority encompasses a small population the risk of identification rises - there are intersections with an estimated population in the single digits).

To preserve user privacy the COVID-19 App backend system filters analytics submissions _before_ persisting and forwarding the analytics data.

This filtering is done using a list provided by ONS. The list used at any point in time is [versioned with the system](../../../src/aws/lambdas/incremental_distribution/cta/src/main/resources/analyticssubmission/PD_LA_to_MergedPD_LA.csv).

The list contains a key (Postcode_District_LAD_ID) - this is the concatenation of the post district with the local authority code - that maps to the local authority and post district values to enter when matched.

Privacy is preserved by aggregating several post districts together and using the ID provided for the aggregated district (usually a concatenation of all the post district IDs) as the value that is persisted by the system.

The list contains keys that will match incomplete mobile app entries (missing local authority information etc.), known invalid entries (such as obsolete or otherwise invalid post districts) and also handles the case where both values are missing.

# Filtering algorithm

* Calculate the key by concatenating the post district(PD) and local authority (LA) values provided by the app as "PD_LA".
* Look up in the filtering table and assign the value of _LAD20CD_ as the local authority and the value of *Merged_Postcode_District* as the post district.
* If there is no match persist UNKNOWN for both values

# Notes 

Most of the edge cases (empty value for local authority, invalid post district values etc.) are handled by taking advantage of the way the lookup key is calculated.
The filtering table contains entries that will amtch when these edge cases happen.

