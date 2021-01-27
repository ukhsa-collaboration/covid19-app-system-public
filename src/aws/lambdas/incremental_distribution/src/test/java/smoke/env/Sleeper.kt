package smoke.env

import java.lang.Thread.sleep
import java.time.Duration

interface Sleeper : (Duration) -> Unit {
    companion object {
        object Fake : Sleeper {
            override fun invoke(p1: Duration) {
            }
        }

        object Real : Sleeper {
            override fun invoke(p1: Duration) = sleep(p1.toMillis())
        }
    }

}