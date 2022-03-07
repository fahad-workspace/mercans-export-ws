@file:Suppress("unused")

package com.mercans.export.exceptions

/**
 * The type Parse exception.
 *
 * @author Fahad Sarwar
 */
class ParseException : RuntimeException {

    /**
     * Instantiates a new Parse exception.
     * @param message the message
     */
    constructor(message: String?) : super(message)

    /**
     * Instantiates a new Parse exception.
     * @param message the message
     * @param cause   the cause
     */
    constructor(message: String?, cause: Throwable) : super(message, cause)

    /**
     * Instantiates a new Parse exception.
     * @param cause   the cause
     */
    constructor(cause: Throwable) : super(cause)
}
