package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import imgui.type.ImBoolean
import me.jraynor.api.Node
import me.jraynor.api.extensions.InputOutputExt
import me.jraynor.api.extensions.SelectableBlockExt
import me.jraynor.api.select.PlayerHooks
import me.jraynor.util.*
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos

/**
 * This is a block node, it will be
 */
class LinkNode(
    /**This keeps track of the linked block**/
    override var selectedBlock: BlockPos? = null,
    /***This is the block we're currently linked to**/
    override var selectedFace: Direction? = null,
    /**This is used to display the current location for a linked block**/
    override var shown: ImBoolean = ImBoolean(false),
    /**This keeps track of the show color**/
    override var showColor: FloatArray = floatArrayOf(1f, 0f, 0f)
) : ExtractableNode(), SelectableBlockExt {

    /**This is the current state of the block. This is only present on the client.**/
    private val blockState: BlockState?
        get() {
            selectedBlock ?: return null
            return clientWorld.getBlockState(selectedBlock!!)
        }

    /**
     * This will first deserialize the node node data from the base [Node] class. Then it will deserialize the position
     * of the block and the face.
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.modes.node = this
    }

    /**
     * This should only be called from the client.
     * This code should not exist on the server.
     * (or at least should be called)
     */
    override fun render() {
        id ?: return
        NodeEditor.beginNode(id!!.toLong())
        ImGui.textColored(ImColor.rgbToColor("#D65076"), "Block Node")
        ImGui.sameLine()
        super.render()
        renderPorts()
        NodeEditor.endNode()
    }

    /**
     * This is used as the code that will render on the side view
     */
    override fun renderEx() {
        ImGui.setNextItemOpen(true, ImGuiCond.FirstUseEver)
        if (ImGui.collapsingHeader("Block Node")) {
            super.renderEx()
            modes.render(this::pushClientUpdate)
        }
    }

    /**
     *  Keeps track of our gui data
     */
    companion object {
        private val INFO_COLOR: Int = ImColor.rgbToColor("#a75ced")
    }
}
