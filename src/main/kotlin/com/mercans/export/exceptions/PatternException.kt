@file:Suppress("unused")

package com.mercans.export.exceptions

/**
 * The type Pattern exception.
 *
 * @author Fahad Sarwar
 */
class PatternException : RuntimeException {

    /**
     * Instantiates a new Pattern exception.
     * @param message the message
     */
    constructor(message: String?) : super(message)

    /**
     * Instantiates a new Pattern exception.
     * @param message the message
     * @param cause   the cause
     */
    constructor(message: String?, cause: Throwable) : super(message, cause)

    /**
     * Instantiates a new Pattern exception.
     * @param cause   the cause
     */
    constructor(cause: Throwable) : super(cause)
}
