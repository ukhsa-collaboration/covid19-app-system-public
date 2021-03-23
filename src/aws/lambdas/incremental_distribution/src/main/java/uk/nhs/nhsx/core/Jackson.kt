package uk.nhs.nhsx.core

import com.fasterxml.jackson.core.JsonProcessingException
import uk.nhs.nhsx.core.SystemObjectMapper.MAPPER
import uk.nhs.nhsx.core.SystemObjectMapper.STRICT_MAPPER
import java.io.InputStream
import java.util.function.Consumer

object Jackson {

    fun <T> readJsonOrThrow(inputStream: InputStream?, clazz: Class<T>): T = MAPPER.readValue(inputStream, clazz)

    inline fun <reified T> readJsonOrThrow(value: String?): T = MAPPER.readValue(value, T::class.java)

    inline fun <reified T> readOrNull(value: String?, handleError: Consumer<Exception> = Consumer { }): T? = try {
        readJsonOrThrow<T>(value)
    } catch (e: Exception) {
        handleError.accept(e)
        null
    }

    inline fun <reified T> readStrictOrNull(value: String?, handleError: Consumer<Exception> = Consumer { }): T? = try {
        STRICT_MAPPER.readValue(value, T::class.java)
    } catch (e: Exception) {
        handleError.accept(e)
        null
    }

    fun toJson(value: Any?): String = try {
        MAPPER.writeValueAsString(value)
    } catch (e: JsonProcessingException) {
        throw RuntimeException(e)
    }
}
