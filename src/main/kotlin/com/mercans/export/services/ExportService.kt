package com.mercans.export.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.mercans.export.helpers.IOHelper
import com.mercans.export.models.DynamicConfiguration
import com.mercans.export.models.Request
import com.mercans.export.transformers.RequestTransformer
import java.util.*
import java.util.regex.Pattern
import javax.inject.Singleton

/**
 * Export service
 *
 * @author Fahad Sarwar
 *
 * @constructor Create empty Export service
 */
@Singleton
class ExportService {

    /**
     * Export
     * @param inputFileName
     * @param dynamicConfigurationFileName
     */
    fun export(inputFileName: String, dynamicConfigurationFileName: String): Request {
        var request = Request()
        request.uuid = UUID.randomUUID().toString()
        request.fname = inputFileName
        val dynamicConfiguration = ObjectMapper().readValue(
            IOHelper.getFileFromResource(dynamicConfigurationFileName), object : TypeReference<DynamicConfiguration>() {}
        )
        if (validateFileNamePattern(dynamicConfiguration, inputFileName, request)) {
            val csvValues = csvReader {
                autoRenameDuplicateHeaders = true
            }.readAllWithHeader(IOHelper.getFileFromResource(inputFileName))
            request = RequestTransformer.transform(request, csvValues, dynamicConfiguration)
        }
        return request
    }

    /**
     * Validate file name pattern
     * @param dynamicConfiguration
     * @param inputFileName
     * @param request
     */
    private fun validateFileNamePattern(
        dynamicConfiguration: DynamicConfiguration,
        inputFileName: String,
        request: Request
    ): Boolean {
        val fileNamePatternMatches = Pattern.matches(dynamicConfiguration.fileNamePattern, inputFileName)
        return if (fileNamePatternMatches) {
            true
        } else {
            request.payload = listOf()
            request.errors = setOf("File name pattern match failed")
            false
        }
    }
}
