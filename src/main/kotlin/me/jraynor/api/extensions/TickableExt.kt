package me.jraynor.api.extensions

import imgui.ImColor
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import java.util.*

/**
 * This type of node is used for automation tasks that require a fake player.
 */
interface TickableExt : INodeExtension {
    /**If the filter should be on the same line or not**/
    val tickableOnSameLine: Boolean
        get() = true

    /**If the filter should be on the same line or not**/
    val tickableIndent: Float
        get() = -1f

    /**
     * This hooks our methods needed for a fake player .
     */
    override fun hook(
        nodeRenders: MutableList<Callback>,
        propertyRenders: MutableList<Callback>,
        pinAdds: MutableList<Callback>,
        tickCalls: MutableList<TickCallback>,
        readCalls: MutableList<NBTCallback>,
        writeCalls: MutableList<NBTCallback>
    ) {
        pinAdds.add(this::addTickPin)
    }

    /**
     * This will add our tick pin the node.
     */
    fun addTickPin(node: Node) {
        node.add(
            Pin(
                io = IO.INPUT,
                label = "DoTick",
                textAfter = true,
                computeText = false,
                sameLine = tickableOnSameLine,
                indent = this.tickableIndent,
                icon = IconType.ROUND_SQUARE,
                innerColor = ImColor.rgbToColor("#fafafa")
            )
        )
    }

}