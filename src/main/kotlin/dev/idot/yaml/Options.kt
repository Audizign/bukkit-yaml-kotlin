package dev.idot.yaml

open class Options {
    /**
     * Character used to separate sections in paths. Defaults to '.'.
     * Must be a single character.
     */
    var pathSeparator: Char = '.'
        set(value) {
            require(value.toString().length == 1) { "Path separator must be a single character." }
            field = value
        }

    /**
     * Determines whether defaults from the parent configuration should be copied.
     * Defaults to false. When true, it's impossible to distinguish between values
     * set in the section and those provided by default.
     */
    var copyDefaults: Boolean = false

    /**
     * Optional header for the configuration. Can be used to store comments or metadata.
     */
    var header: String? = null

    /**
     * Indicates whether the header, if present, should be copied along with the configuration.
     * Defaults to true.
     */
    var copyHeader: Boolean = true

    /**
     * Specifies the number of spaces used for indentation in the configuration's textual representation.
     * Must be between 2 and 9, inclusive.
     */
    var indent: Int = 2
        set(value) {
            require(value in 2..9) { "Indent must be between 2 and 9 characters." }
            field = value
        }
}
