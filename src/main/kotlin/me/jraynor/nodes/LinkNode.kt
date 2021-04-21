package me.jraynor.nodes

import imgui.*
import imgui.flag.*
import imgui.type.*
import me.jraynor.api.impl.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.client.*
import net.minecraft.nbt.*
import net.minecraft.util.*
import net.minecraft.util.math.*
import java.lang.Float.*

/**This will render a node that has the ability to select a real world block**/
class LinkNode : ContentNode("Link Node") {
    /**The currently selected block**/
    private var posBuffer: IntArray = IntArray(3)

    /**This will be updated via the [posBuffer]*0*/
    var selectedBlock: BlockPos = BlockPos.ZERO

    /**The currently selected face**/
    var selectedFace: Direction = Direction.NORTH

    /**This is used a flag of sorts to determine when to show the popup**/
    private var selectingFace = false

    /**This should be updated frequently**/
    private val blockNameBuffer = ImString()

    /**Gets the current block name**/
    private fun blockName(): ImString {
        blockNameBuffer.set(Minecraft.getInstance().world?.getBlockState(selectedBlock)?.block?.translatedName?.string ?: "Undefined")
        return blockNameBuffer
    }

    /**When true we want to show the face**/
    private var shown = ImBoolean(false)

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 5f

    init {
        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "show",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#F49FBC")
            )
        )
        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "face",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#F49FBC")
            )
        )
        addPin(
            Pin(
                inputOutput = InputOutput.Input,
                label = "pos",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#F49FBC")
            )
        )
        addPin(
            Pin(
                label = "insert",
                inputOutput = InputOutput.Input,
                icon = IconType.SQUARE,
                baseColor = ImColor.rgbToColor("#2A9D8F")
            )
        )
        addPin(
            Pin(
                label = "extract",
                inputOutput = InputOutput.Output,
                icon = IconType.SQUARE,
                baseColor = ImColor.rgbToColor("#F4A261")
            )
        )
    }

    /**Renders stuff inside the node's header**/
    override fun renderHeaderContent() {
        if (ImGui.button("select##${this.nodeId}")) {
            println("DO BLOCK SELECTION")
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        var hasFace = false
        if (!graph.hasActiveLinks(getPin("face").pinId)) {
            if (ImGui.button("${this.selectedFace.name.toLowerCase()}##${this.nodeId}"))
                selectingFace = true
            ImGui.sameLine()
            ImGui.text("face")
            hasFace = true
        }
        if (!graph.hasActiveLinks(getPin("show").pinId)) {
            if (hasFace) ImGui.sameLine()
            if (ImGui.checkbox("show##${this.nodeId}", this.shown)) {
                println("updated show state")
                pushUpdate()
            }
        }
        if (!graph.hasActiveLinks(getPin("pos").pinId)) {
            ImGui.setNextItemWidth(120f)
            this.posBuffer.let { it[0] = selectedBlock.x; it[1] = selectedBlock.y; it[2] = selectedBlock.z }
            if (ImGui.inputInt3("pos##${this.nodeId}", this.posBuffer)) pushUpdate()
            this.selectedBlock = BlockPos(posBuffer[0], posBuffer[1], posBuffer[2])
        }
        ImGui.setNextItemWidth(120f)
        if (ImGui.inputText("name##${this.nodeId}", blockName(), ImGuiInputTextFlags.ReadOnly)) pushUpdate()
        return max(ImGui.getItemRectMaxX(), contentWidth)
    }

    /**This is used as a way to add code to the suspend block in a node**/
    override fun postProcess() {
        if (this.selectingFace) {
            ImGui.openPopup("face_select")
            if (ImGui.beginPopup("face_select")) {
                Direction.values().forEach {
                    if (ImGui.menuItem(it.name.toLowerCase())) {
                        this.selectedFace = it
                        this.selectingFace = false
                        ImGui.closeCurrentPopup()
                        pushUpdate()
                    }
                }
                ImGui.endPopup()
            }
        }
    }

    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putEnum("face", this.selectedFace)
        tag.putBoolean("shown", this.shown.get())
        tag.putBlockPos("block", this.selectedBlock)
        return tag
    }

    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.selectedFace = tag.getEnum("face")
        this.shown.set(tag.getBoolean("shown"))
        tag.getBlockPos("block").let { this.selectedBlock = it }
    }

}