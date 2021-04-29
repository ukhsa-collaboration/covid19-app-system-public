package uk.nhs.nhsx.analyticsexporter

interface ExportDestinationUploader {
    fun uploadFile(filename:String, content:ByteArray, contentType:String)
}
