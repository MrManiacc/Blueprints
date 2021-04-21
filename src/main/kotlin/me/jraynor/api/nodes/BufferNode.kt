package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.data.Buffers

import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

/**
 * This is a block node, it will be
 */
class BufferNode(
    /**This keeps track of the internal storage data.**/
    val buffers: Buffers = Buffers()
) : ExtractableNode() {

    /**
     * This will write the link and the face. It has null safety so if they are null they will not be written. There
     * won't be an exception the data just isn't present currently.
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.put("buffers", buffers.serializeNBT())
        return tag
    }

    /**
     * This will first deserialize the node node data from the base [Node] class. Then it will deserialize the position
     * of the block and the face.
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.buffers.deserializeNBT(tag.getCompound("buffers"))
    }

    /**
     * This should only be called from the client.
     * This code should not exist on the server.
     * (or at least should be called)
     */
    override fun render() {
        id ?: return
        NodeEditor.beginNode(id!!.toLong())
        super.render()
        ImGui.textColored(ImColor.rgbToColor("#34568B"), "Buffer node")
        ImGui.dummy(0f, 10f)
        renderPorts()
        NodeEditor.endNode()
    }

    /**
     * This is used as the code that will render on the side view
     */
    override fun renderEx() {
        ImGui.setNextItemOpen(true, ImGuiCond.FirstUseEver)
        if (ImGui.collapsingHeader("Buffer Node")) {
            ImGui.spacing()
            buffers.render()
            ImGui.spacing()
            modes.render(this::pushClientUpdate)
            ImGui.spacing()
        }
    }
}
