package com.mercans.export.helpers

import java.io.InputStream

object IOHelper {

    fun getFileFromResource(fileName: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(fileName) ?: throw IllegalArgumentException("File not found :: $fileName")
    }
}
