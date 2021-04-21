package me.jraynor.api.impl

import imgui.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*

/**The base pin. All other pins should be of this type**/
open class Pin(
    override var pinId: Int = 0,
    override var label: String = "",
    override var icon: IconType = IconType.SQUARE,
    override var inputOutput: InputOutput = InputOutput.None,
    override var labelColor: Int = ImColor.rgbToColor("#ffffff"),
    override var baseColor: Int = ImColor.rgbToColor("#cccccc"),
    override var innerColor: Int = ImColor.rgbToColor("#212121"),
    override val links: MutableList<IPin> = ArrayList(),
) : IPin {
    /**We can allow our self to keep a reference to the parent node**/
    override var nodeId: Int = -1

    /**Stores the links with **/
    private val linkCache = HashMap<Int, Int>()

    /**This will add a link to the [other] IPin.
     * Returns the index of the link **/
    override fun addLink(other: IPin): Int {
        linkCache[other.pinId] = currentLink++
        return super.addLink(other)
    }

    /**This should get the link id for the given pin**/
    override fun getLinkId(other: IPin): Int = linkCache[other.pinId] ?: -1

    override fun toString(): String =
        "${javaClass.simpleName}(pinId=$pinId, label='$label', icon=$icon, inputOutput=$inputOutput, links=$links)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IPin) return false
        if (pinId != other.pinId) return false
        if (inputOutput != other.inputOutput) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pinId
        result = 31 * result + inputOutput.hashCode()
        return result
    }

    companion object {
        /**Used to keep track of the current link**/
        private var currentLink = 0
    }


}