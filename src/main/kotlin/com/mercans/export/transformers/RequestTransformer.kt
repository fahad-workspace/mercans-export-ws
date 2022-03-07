package com.mercans.export.transformers

import com.mercans.export.constants.ActionCode
import com.mercans.export.constants.CommonConstant
import com.mercans.export.constants.DataType
import com.mercans.export.exceptions.ParseException
import com.mercans.export.exceptions.PatternException
import com.mercans.export.models.DynamicConfiguration
import com.mercans.export.models.DynamicConfigurationField
import com.mercans.export.models.Request
import org.apache.commons.lang3.StringUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Request transformer
 *
 * @author Fahad Sarwar
 *
 * @constructor Create empty Request transformer
 */
object RequestTransformer {

    private var failedFields = mutableSetOf<String>()
    private var parsingFailedFields = mutableSetOf<String>()
    private var patternFailedFields = mutableSetOf<String>()

    /**
     * Transform
     * @param request
     * @param csvValues
     * @param dynamicConfiguration
     */
    fun transform(
        request: Request,
        csvValues: List<Map<String, String>>,
        dynamicConfiguration: DynamicConfiguration
    ): Request {
        val payloads = mutableListOf<Request.Payload>()
        failedFields = mutableSetOf()
        csvValues.withIndex().forEach { (index, csvValue) ->
            parsingFailedFields = mutableSetOf()
            patternFailedFields = mutableSetOf()
            // Check for mandatory fields as configured in config file
            if (checkMandatorySourceFields(dynamicConfiguration, csvValue, index)) {
                // Skip record in case of any mismatch
                return@forEach
            }
            val payload = Request.Payload()
            // Extracting mandatory payload root components
            if (extractPayloadRootComponent(payload, csvValue, dynamicConfiguration, index)) {
                // Skip record in case of any mismatch
                return@forEach
            }
            // Extracting data components
            if (extractDataComponent(dynamicConfiguration, csvValue, payload, index)) {
                // Skip record in case of any mismatch
                return@forEach
            }
            // Extracting pay components
            extractPayComponent(csvValue, dynamicConfiguration, payload)
            payloads.add(payload)
        }
        request.payload = payloads
        request.errors = failedFields
        return request
    }

    /**
     * Extract payload root component
     * @param payload
     * @param csvValue
     * @param dynamicConfiguration
     * @param index
     */
    private fun extractPayloadRootComponent(
        payload: Request.Payload,
        csvValue: Map<String, String>,
        dynamicConfiguration: DynamicConfiguration,
        index: Int
    ): Boolean {
        extractPayloadRoot(payload, csvValue, dynamicConfiguration)
        if (parsingFailedFields.isNotEmpty() || patternFailedFields.isNotEmpty()) {
            reportErrors(index)
            return true
        }
        return false
    }

    /**
     * Extract payload root
     * @param payload
     * @param csvValue
     * @param dynamicConfiguration
     */
    private fun extractPayloadRoot(
        payload: Request.Payload,
        csvValue: Map<String, String>,
        dynamicConfiguration: DynamicConfiguration
    ) {
        try {
            payload.action = getCsvValue(csvValue, dynamicConfiguration.fields.first { field ->
                StringUtils.equalsIgnoreCase(field.sourceField, CommonConstant.ACTION)
            }, dynamicConfiguration.mappings).toString()
            payload.employeeCode = getCsvValue(csvValue, dynamicConfiguration.fields.first { field ->
                StringUtils.equalsIgnoreCase(field.sourceField, CommonConstant.CONTRACT_WORKER_ID)
            }, dynamicConfiguration.mappings).toString()
            if (StringUtils.isBlank(payload.employeeCode) && StringUtils.equalsIgnoreCase(payload.action, ActionCode.Hire.name)) {
                // Generate employee code for new hire if missing
                payload.employeeCode = generateEmployeeCode(csvValue, dynamicConfiguration)
            }
        } catch (ex: Exception) {
            if (ex is ParseException) {
                parsingFailedFields.add(ex.message!!)
            } else if (ex is PatternException) {
                patternFailedFields.add(ex.message!!)
            }
        }
    }

    /**
     * Generate employee code
     * @param csvValue
     * @param dynamicConfiguration
     */
    private fun generateEmployeeCode(
        csvValue: Map<String, String>,
        dynamicConfiguration: DynamicConfiguration
    ): String {
        val contractWorkStartDate = getCsvValue(
            csvValue,
            dynamicConfiguration.fields.first { field ->
                StringUtils.equalsIgnoreCase(field.sourceField, CommonConstant.CONTRACT_WORK_START_DATE)
            },
            dynamicConfiguration.mappings
        ).toString()
        return StringUtils.upperCase(
            SimpleDateFormat(CommonConstant.YY_M_MDD).format(SimpleDateFormat(DynamicConfiguration.GLOBAL_DATE_FORMAT).parse(contractWorkStartDate))
                +
                StringUtils.leftPad(Integer.toHexString(ThreadLocalRandom.current().nextInt(0, 255)), 2, '0')
        )
    }

    /**
     * Extract data component
     * @param dynamicConfiguration
     * @param csvValue
     * @param payload
     * @param index
     */
    private fun extractDataComponent(
        dynamicConfiguration: DynamicConfiguration,
        csvValue: Map<String, String>,
        payload: Request.Payload,
        index: Int
    ): Boolean {
        val data = extractData(dynamicConfiguration, csvValue, payload)
        if (parsingFailedFields.isNotEmpty() || patternFailedFields.isNotEmpty()) {
            reportErrors(index)
            return true
        } else {
            payload.data = data
        }
        return false
    }

    /**
     * Extract data
     * @param dynamicConfiguration
     * @param csvValue
     * @param payload
     */
    private fun extractData(
        dynamicConfiguration: DynamicConfiguration,
        csvValue: Map<String, String>,
        payload: Request.Payload
    ): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>()
        dynamicConfiguration.fields.stream().filter { field ->
            StringUtils.isNoneBlank(field.targetField, field.targetEntity)
        }.collect(Collectors.toSet()).forEach { dataSourceField ->
            try {
                val value = getCsvValue(csvValue, dataSourceField, dynamicConfiguration.mappings)
                if (value !is String || StringUtils.isNotBlank(value)) {
                    data[dataSourceField.entityKey()] = value
                }
            } catch (ex: Exception) {
                if (dataSourceField.isMandatory) {
                    if (ex is ParseException) {
                        parsingFailedFields.add(ex.message!!)
                    } else if (ex is PatternException) {
                        patternFailedFields.add(ex.message!!)
                    }
                }
            }
        }
        if (StringUtils.equalsIgnoreCase(payload.action, ActionCode.Terminate.name) && !data.containsKey(CommonConstant.PERSON_TERMINATION_DATE)) {
            // Generate termination date if missing
            data[CommonConstant.PERSON_TERMINATION_DATE] = SimpleDateFormat(DynamicConfiguration.GLOBAL_DATE_FORMAT).format(Date())
        }
        return data
    }

    /**
     * Extract pay component
     * @param csvValue
     * @param dynamicConfiguration
     * @param payload
     */
    private fun extractPayComponent(
        csvValue: Map<String, String>,
        dynamicConfiguration: DynamicConfiguration, payload: Request.Payload
    ) {
        val payComponents = extractPay(dynamicConfiguration, csvValue)
        if (payComponents.isNotEmpty()) {
            payload.payComponents = payComponents
        }
    }

    /**
     * Extract pay
     * @param dynamicConfiguration
     * @param csvValue
     */
    private fun extractPay(dynamicConfiguration: DynamicConfiguration,
        csvValue: Map<String, String>): MutableList<Map<String, Any>> {
        val payComponents = mutableListOf<Map<String, Any>>()
        dynamicConfiguration.fields.stream().filter { field ->
            StringUtils.isNoneBlank(field.payComponentsRefType, field.payComponentsField)
        }.collect(Collectors.groupingBy(DynamicConfigurationField::payComponentsRefType)).entries.forEach { groupByPayComponentsRefTypeEntry ->
            val payComponent = mutableMapOf<String, Any>()
            groupByPayComponentsRefTypeEntry.value.forEach { dynamicConfigurationField ->
                try {
                    payComponent[dynamicConfigurationField.payComponentsField!!] = getCsvValue(csvValue, dynamicConfigurationField, dynamicConfiguration.mappings)
                } catch (ex: Exception) {
                    // pass
                }
            }
            // Pay component will only be added if all 4 mandatory attributes are present
            if (payComponent.size == 4) {
                payComponents.add(payComponent)
            }
        }
        return payComponents
    }

    /**
     * Check mandatory source fields
     * @param dynamicConfiguration
     * @param csvValue
     * @param index
     */
    private fun checkMandatorySourceFields(
        dynamicConfiguration: DynamicConfiguration,
        csvValue: Map<String, String>,
        index: Int
    ): Boolean {
        val missingMandatorySourceFields = mutableSetOf<String>()
        dynamicConfiguration.fields.stream()
            .filter { field -> field.isMandatory }
            .map { field -> field.sourceField }
            .collect(Collectors.toSet()).all {
                val containsKey = csvValue.containsKey(it)
                if (!containsKey || StringUtils.isBlank(csvValue[it])) {
                    missingMandatorySourceFields.add(it!!)
                }
                containsKey
            }
        if (missingMandatorySourceFields.isNotEmpty()) {
            failedFields.add("Missing Mandatory Source Fields (${StringUtils.join(missingMandatorySourceFields, ", ")}) at Row Number ${index + 2}")
            return true
        }
        return false
    }

    /**
     * Report errors
     * @param index
     */
    private fun reportErrors(index: Int) {
        if (parsingFailedFields.isNotEmpty()) {
            failedFields.add("Unable to Parse Fields (${StringUtils.join(parsingFailedFields, ", ")}) at Row Number ${index + 2}")
        } else {
            failedFields.add("Pattern match failed for fields (${StringUtils.join(patternFailedFields, ", ")}) at Row Number ${index + 2}")
        }
    }

    /**
     * Get csv value
     * @param csvValue
     * @param dataSourceField
     * @param mappings
     */
    private fun getCsvValue(csvValue: Map<String, String>, dataSourceField: DynamicConfigurationField, mappings: Map<String, Map<String, String>>): Any {
        val value = validatePattern(csvValue, dataSourceField)
        try {
            // Read and parse csv value to correct data type
            return when (dataSourceField.dataType) {
                DataType.Integer -> value.toInt()
                DataType.Decimal -> value.toDouble()
                DataType.Bool -> value.toBoolean()
                DataType.Date -> SimpleDateFormat(DynamicConfiguration.GLOBAL_DATE_FORMAT)
                    .format(SimpleDateFormat(dataSourceField.dateFormat!!).parse(value))
                else -> {
                    getMappedValue(dataSourceField, mappings, value)
                }
            }
        } catch (ex: Exception) {
            throw ParseException("{$value as ${dataSourceField.dataType}}")
        }
    }

    /**
     * Validate pattern
     * @param csvValue
     * @param dataSourceField
     */
    private fun validatePattern(csvValue: Map<String, String>, dataSourceField: DynamicConfigurationField): String {
        var value = csvValue[dataSourceField.sourceField]!!
        if (StringUtils.isNotBlank(dataSourceField.validationPattern)) {
            val matcher = Pattern.compile(dataSourceField.validationPattern!!).matcher(value)
            var regexCaptureGroupNr = 0
            if (dataSourceField.regexCaptureGroupNr != null) {
                regexCaptureGroupNr = dataSourceField.regexCaptureGroupNr!!
            }
            if (matcher.find()) {
                value = matcher.group(regexCaptureGroupNr)
                if (StringUtils.isBlank(value)) {
                    throw PatternException("{$value as ${dataSourceField.dataType}}")
                }
            } else {
                throw PatternException("{$value as ${dataSourceField.dataType}}")
            }
        }
        return value
    }

    /**
     * Get mapped value
     * @param dataSourceField
     * @param mappings
     * @param value
     */
    private fun getMappedValue(dataSourceField: DynamicConfigurationField, mappings: Map<String, Map<String, String>>, value: String): String {
        return if (StringUtils.isNotBlank(dataSourceField.mappingKey)) {
            mappings[dataSourceField.mappingKey]!![StringUtils.lowerCase(value)]!!
        } else {
            value
        }
    }
}
