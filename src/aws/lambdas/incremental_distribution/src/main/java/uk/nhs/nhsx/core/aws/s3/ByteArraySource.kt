package uk.nhs.nhsx.core.aws.s3

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

data class ByteArraySource(val bytes: ByteArray) {
    val size: Int = bytes.size

    fun openStream() = BufferedInputStream(ByteArrayInputStream(bytes))

    fun toArray(): ByteArray = bytes.copyOf(bytes.size)

    fun toUtf8String(): String = bytes.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ByteArraySource
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun fromUtf8String(input: String): ByteArraySource = ByteArraySource(input.toByteArray())

        fun fromFile(input: File): ByteArraySource = try {
            ByteArraySource(Files.readAllBytes(input.toPath()))
        } catch (e: IOException) {
            throw RuntimeException("Could not read ${input.absolutePath}")
        }
    }
}
