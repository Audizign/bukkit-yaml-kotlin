package dev.idot.yaml

class InvalidConfigurationException : Exception {
    /**
     * Creates a new instance of InvalidConfigurationException without a
     * message or cause.
     */
    constructor()

    /**
     * Constructs an instance of InvalidConfigurationException with the
     * specified message.
     *
     * @param msg The details of the exception.
     */
    constructor(msg: String?) : super(msg)

    /**
     * Constructs an instance of InvalidConfigurationException with the
     * specified cause.
     *
     * @param cause The cause of the exception.
     */
    constructor(cause: Throwable?) : super(cause)

    /**
     * Constructs an instance of InvalidConfigurationException with the
     * specified message and cause.
     *
     * @param msg The details of the exception.
     * @param cause The cause of the exception.
     */
    constructor(msg: String?, cause: Throwable?) : super(msg, cause)
}