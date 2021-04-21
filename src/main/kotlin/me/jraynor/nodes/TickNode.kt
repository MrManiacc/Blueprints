package me.jraynor.nodes

import imgui.*
import imgui.type.*
import me.jraynor.api.impl.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.event.*
import net.minecraft.nbt.*
import kotlin.math.*

/**This node is the base node for all tickable nodes.**/
open class TickNode : ContentNode(
    title = "Ticking Node",
    headerColor = "#2c9160"
) {
    /**how many times we should output the tick per second**/
    private val tps: ImInt = ImInt(20)

    /**Used to locally keep track if we're enabled or not**/
    private val enabled: ImBoolean = ImBoolean(false)

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 0f

    /**We store a hard reference to the do tick pin**/
    init {
        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "enabled",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#F49FBC")
            )
        )
        addPin(
            Pin(
                inputOutput = InputOutput.Output,
                label = "##tick",
                icon = IconType.FLOW,
                baseColor = ImColor.rgbToColor("#99A1A6")
            )
        )

        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "rate",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#F49FBC")
            )
        )

    }

    /**Sends a tick to the given pin**/
    fun tick() {
        getPin("##tick").let { pin ->
            pin.links.forEach {
                if (it is EventPin) it.fireEvent(graph, Events.TickEvent(pin))
            }
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        if (!graph.hasActiveLinks(getPin("enabled").pinId)) {
            if (ImGui.checkbox("enabled##$nodeId", enabled))
                pushUpdate()
        }
        if (!graph.hasActiveLinks(getPin("rate").pinId)) {
            ImGui.pushItemWidth(80f)
            if (ImGui.inputInt("rate##$nodeId", tps)) {
                tps.set(max(tps.get(), 0))
                pushUpdate()
            }
        }
        return max(ImGui.getItemRectMaxX(), contentWidth + 20)
    }

    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putInt("tick_rate", this.tps.get())
        tag.putBoolean("tick_enable", this.enabled.get())
        return tag
    }

    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        tps.set(tag.getInt("tick_rate"))
        enabled.set(tag.getBoolean("tick_enable"))
    }

}