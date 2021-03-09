package uk.nhs.nhsx.core

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import uk.nhs.nhsx.core.SystemObjectMapper.MAPPER
import uk.nhs.nhsx.core.SystemObjectMapper.STRICT_MAPPER
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.function.Consumer

object Jackson {

    @JvmStatic
    @Throws(JsonProcessingException::class)
    fun <T> readJson(value: String?, clazz: Class<T>): T = MAPPER.readValue(value, clazz)

    @JvmStatic
    @Throws(JsonProcessingException::class)
    fun <T> readStrict(value: String?, clazz: Class<T>): T = STRICT_MAPPER.readValue(value, clazz)

    @JvmStatic
    @Throws(IOException::class)
    fun <T> readJson(inputStream: InputStream?, clazz: Class<T>): T = MAPPER.readValue(inputStream, clazz)

    @JvmStatic
    @Throws(IOException::class)
    fun <T> readJson(value: String?, clazz: TypeReference<T>): T = MAPPER.readValue(value, clazz)

    @JvmStatic
    @Throws(IOException::class)
    fun <T> readJson(inputStream: InputStream?, clazz: TypeReference<T>): T = MAPPER.readValue(inputStream, clazz)

    inline fun <reified T> readOrNull(value: String?, handleError: Consumer<Exception> = Consumer { }): T? = try {
        readJson(value, T::class.java)
    } catch (e: Exception) {
        handleError.accept(e)
        null
    }

    inline fun <reified T> readStrictOrNull(value: String?, handleError: Consumer<Exception> = Consumer { }): T? = try {
        readStrict(value, T::class.java)
    } catch (e: Exception) {
        handleError.accept(e)
        null
    }

    @JvmStatic
    fun toJson(value: Any?): String = try {
        MAPPER.writeValueAsString(value)
    } catch (e: JsonProcessingException) {
        throw RuntimeException(e)
    }
}
