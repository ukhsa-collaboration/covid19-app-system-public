package smoke.actors

import dev.forkhandles.values.StringValue

class MobileDeviceModel(value: String) : StringValue(value)

sealed class ApiVersion {
    object V1 : ApiVersion()
    object V2 : ApiVersion()
}
