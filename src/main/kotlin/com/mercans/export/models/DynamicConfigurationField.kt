package com.mercans.export.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.mercans.export.constants.DataType
import com.mercans.export.constants.FieldType
import lombok.EqualsAndHashCode

@Suppress("MemberVisibilityCanBePrivate", "unused")
@EqualsAndHashCode
class DynamicConfigurationField {

    @JsonFormat(shape = JsonFormat.Shape.BOOLEAN)
    var isMandatory: Boolean = false
    var fieldType: FieldType = FieldType.Regular
    var sourceField: String? = null
    var targetEntity: String? = null
    var targetField: String? = null
    var dataType: DataType = DataType.Text
    var mappingKey: String? = null
    var dateFormat: String? = null
    var validationPattern: String? = null
    var regexCaptureGroupNr: Int? = null
    var payComponentsRefType: String? = null
    var payComponentsField: String? = null
    override fun toString(): String = "$fieldType\t$dataType\t$sourceField" + when (fieldType) {
        FieldType.Regular -> "\t->\t${entityKey()}"
        else -> ""
    }

    fun entityKey(): String = when (targetEntity) {
        null -> ""
        else -> "$targetEntity.$targetField"
    }
}
