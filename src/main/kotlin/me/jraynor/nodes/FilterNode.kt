package me.jraynor.nodes

import imgui.*
import me.jraynor.api.impl.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.event.*
import net.minecraft.nbt.*

/**This node is the base node for all tickable nodes.**/
class FilterNode : ContentNode(
    title = "Filter Node",
    headerColor = "#8c34eb"
), IEventReceiver {

    /**Used to display the selecting type popup**/
    private var selectingType = false

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 0f

    /**We store a hard reference to the do tick pin**/
    init {
        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "##filter",
                icon = IconType.GRID,
                baseColor = ImColor.rgbToColor("#99A1A6")
            )
        )
    }

    /**Called when the event is fired**/
    override fun onEvent(event: Event<*, *>) {
        if (event is Events.FilterEvent) {
            println("Received filer event in $this from ${event.sender}")
            event.result = true
        }
    }

    /**This is used as a way to add code to the suspend block in a node**/
    override fun postProcess() {
        if (selectingType) ImGui.openPopup("filter_type_select##${nodeId}")
        if (ImGui.beginPopup("filter_type_select##${nodeId}")) {

            ImGui.endPopup()
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        return (contentWidth)
    }

    /**Writes the variable to file**/
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        return tag
    }

    /**Reads our data like the nodeId, and graphX/graphY and pins.**/
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
    }


}

