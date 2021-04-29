package uk.nhs.nhsx.testhelper.junit

import java.time.Duration

interface Sleeper : (Duration) -> Unit {
    companion object {
        object Fake : Sleeper {
            override fun invoke(p1: Duration) {
            }
        }

        object Real : Sleeper {
            override fun invoke(p1: Duration) = Thread.sleep(p1.toMillis())
        }
    }

}
