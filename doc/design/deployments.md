# Deployment structure for AppSystem and supporting subsystems

The complete CTA system combines several subsystems that are loosely interdependent but can be deployed individually.

We refer to an individual subsystem as a "bubble". A bubble is comprised of a set of AWS resources and associated artifacts (i.e. lambda binaries, static resources etc.) that are deployed together and managed in a separate terraform state.

There are no restrictions on the (co-)location of bubbles in respect to AWS accounts, other than whatever restrictions are imposed by the implementations - and such implementation coupling is to be avoided.

The build system provides deployment tasks per bubble, per target environment.

There are currently the following bubbles hosting production functionality:

Bubble Name|Description|
---|---|
AppSystem|The main CTA functionality serving the COVID19 app and third party backend integrations|
Analytics|The analytics subsystem based on QuickSight|
PublicDashboard|The website providing a dashboard view on the CTA system statistics|

## Functionality vs. Content

The bubbles correspond too the implementation of different parts of the system and represent subsystems of functionality supporting features.

In addition to the bubbles, there is a requirement for updates to the content of the system (i.e. configuration files, content served to the mobile clients etc.) individually and independently of the rest of the system.

Similarly to the bubbles, the build system provides tasks to deploy such content individually and independently.

Currently the following content groups can be deployed individually:

Content Group|Description|
---|---|
|Tiers|The tier metadata configuration controlling the display of region risk information|
|Availability|The app availability configuration controlling the active versions of the mobile app|
|Local Messages|The targeted local messaging metadata and message per local authority index|
