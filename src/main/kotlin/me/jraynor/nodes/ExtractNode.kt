package me.jraynor.nodes

import imgui.*
import imgui.type.*
import me.jraynor.api.impl.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.event.*
import net.minecraft.nbt.*
import kotlin.math.*

/**This node will be used to extract items and other things from tile entities**/
class ExtractNode(
    private var type: ExtractType = ExtractType.Item,
    private val speed: ImInt = ImInt(64)
) : ContentNode(
    title = "Extraction Node",
    headerColor = "#2c83a3",
), IEventReceiver {
    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 5f

    /**Used so we can show the selecting type popup**/
    private var selectingType = false

    /**We don't need to store a reference to the pin because it's an input pint**/
    init {
        addPin(EventPin("##extract", IconType.FLOW, baseColor = ImColor.rgbToColor("#99A1A6")))
        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "rate",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#F49FBC")
            )
        )
        addPin(
            Pin(
                inputOutput = InputOutput.Output,
                label = "filter",
                icon = IconType.GRID,
                baseColor = ImColor.rgbToColor("#99A1A6")
            )
        )
        addPin(
            Pin(
                label = "extract",
                inputOutput = InputOutput.Input,
                icon = IconType.SQUARE,
                baseColor = ImColor.rgbToColor("#2A9D8F")
            )
        )
        addPin(
            Pin(
                label = "insert",
                inputOutput = InputOutput.Output,
                icon = IconType.SQUARE,
                baseColor = ImColor.rgbToColor("#F4A261")
            )
        )
    }

    /**Called when the event is fired**/
    override fun onEvent(event: Event<*, *>) {
        if (event is Events.TickEvent) {
            println("Received tick event in extract node")
        }
    }

    /**Returns true if one of the filters was passed. **/
    private fun filter(): Boolean {
        getPin("filter").let { pin ->
            pin.links.forEach {
                if (it is EventPin) {
                    //val event = Events.FilterEvent(pin)
                    //it.fireEvent(graph, Events.TickEvent(pin))
                }
            }
        }
        return true
    }

    /**This should render anything that will be put inside the header**/
    override fun renderHeaderContent() {
        if (ImGui.button("${this.type.name.toLowerCase()}##${this.nodeId}"))
            selectingType = true
    }

    /**This will render our type popup**/
    override fun postProcess() {
        if (this.selectingType) {
            ImGui.openPopup("type_select")
            if (ImGui.beginPopup("type_select")) {
                ExtractType.values().forEach {
                    if (ImGui.menuItem(it.name.toLowerCase())) {
                        this.type = it
                        this.selectingType = false
                        ImGui.closeCurrentPopup()
                        pushUpdate()
                    }
                }
                ImGui.endPopup()
            }
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        if (!graph.hasActiveLinks(getPin("rate").pinId)) {
            ImGui.pushItemWidth(100f)
            if (ImGui.inputInt("rate##$nodeId", speed)) {
                speed.set(max(speed.get(), 0))
                pushUpdate()
            }
            return max(contentWidth, ImGui.getItemRectMaxX())
        }
        return contentWidth
    }

    /**Writes our speed and type**/
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putEnum("extract_type", type)
        tag.putInt("extract_speed", speed.get())
        return tag
    }

    /**Reads our speed and type**/
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        type = tag.getEnum("extract_type")
        speed.set(tag.getInt("extract_speed"))
    }

    /**Stores the type of extraction we wish to evaluate***/
    enum class ExtractType {
        Item, Energy, Liquid, Gas
    }

}