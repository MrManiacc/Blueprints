package me.jraynor.api.utilities.enums

/**Simply stores a given side.**/
enum class Side {
    Client, Server, Neither;

    /**Gets the opposite side**/
    val opposite: Side get() = if (this == Client) Server else if (this == Server) Client else Neither
}