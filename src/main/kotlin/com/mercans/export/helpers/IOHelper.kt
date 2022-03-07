package com.mercans.export.helpers

import java.io.InputStream

/**
 * IO helper
 *
 * @constructor Create empty I o helper
 */
object IOHelper {

    /**
     * Get file from resource
     *
     * @param fileName
     */
    fun getFileFromResource(fileName: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(fileName) ?: throw IllegalArgumentException("File not found :: $fileName")
    }
}
