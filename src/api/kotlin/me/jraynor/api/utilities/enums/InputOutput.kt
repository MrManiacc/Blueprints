package me.jraynor.api.utilities.enums

/**
 * Simply used to store the type of operation of a port
 */
enum class InputOutput {
    Input, Output, None;

    /**
     * Simply gets the opposite io port. This is used for checking if ports are connected to the correct opposite port.
     */
    val opposite: InputOutput
        get() = when {
            this == Input -> Output
            this == Output -> Input
            else -> None
        }

}

