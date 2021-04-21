package me.jraynor.api.enums

/**
 * Simply used to store the type of operation of a port
 */
enum class IO {
    INPUT, OUTPUT;

    /**
     * Simply gets the opposite io port. This is used for checking if ports are connected to the correct opposite port.
     */
    val opposite: IO
        get() {
            if (this == INPUT)
                return OUTPUT
            return INPUT
        }
}

