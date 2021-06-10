package smoke.actors

import org.http4k.core.Filter

fun SetAuthHeader(authHeader: String) = Filter { next ->
    {
        next(it.header("Authorization", authHeader))
    }
}
