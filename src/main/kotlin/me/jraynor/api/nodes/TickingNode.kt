package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.type.ImBoolean
import imgui.type.ImInt
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.api.enums.Mode
import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.World
import kotlin.math.max

/**
 * This is a block node, it will be
 */
class TickingNode(
    /**When active the signal will be sent to all of the nodes that are connected to this node**/
    var active: ImBoolean = ImBoolean(true),
    /**How often this will trigger it's outputs. A speed of 20 will call once ever second.**/
    var tickSpeed: ImInt = ImInt(20)
) : Node() {
    private var tick = 0

    /**
     * This is called in the constructor of the node. it will
     */
    override fun addPins() {
        add(
            Pin(
                io = IO.OUTPUT,
                label = "DoTick",
                textAfter = false,
                icon = IconType.ROUND_SQUARE,
                indent = 95f,
                computeText = true,
                innerColor = ImColor.rgbToColor("#fafafa")
            )
        )
    }

    /**
     * This will write the regex filter
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putBoolean("active", active.get())
        tag.putInt("tick_speed", tickSpeed.get())
        return tag
    }

    /**
     * This will deserialize the regex list
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.active.set(tag.getBoolean("active"))
        this.tickSpeed.set(tag.getInt("tick_speed"))
    }

    /***
     * This will allow us to tick the given node
     */
    override fun onTick(world: World, graph: Graph) {
        if (active.get()) {
            if (++tick >= tickSpeed.get()) {
                val pin = findPinWithLabel("DoTick") ?: return
                val outputs =
                    pin.outputs(graph) //This says inputs but it's correct, technically it's the "outputs" for this instance
                for (output in outputs) {
                    output.nodeId ?: continue
                    val node = graph.findById(output.nodeId!!) ?: continue
                    node.onTick(world, graph)
                }
                tick = 0
            }
        }
    }

    /**
     * This should only be called from the client.
     * This code should not exist on the server.
     * (or at least should be called)
     */
    override fun render() {
        ImGui.dummy(1f, 1f)
        id ?: return
        NodeEditor.beginNode(id!!.toLong())
        renderEx()
        renderPorts()
        NodeEditor.endNode()
    }

    /**
     * This is used as the code that will render on the side view
     */
    override fun renderEx() {
        ImGui.text("Ticking Node")
        ImGui.spacing()
        if (ImGui.checkbox("Activated##$id", active))
            pushUpdate()
        ImGui.setNextItemWidth(80f)
        if (ImGui.inputInt("Tick Speed##$id", tickSpeed, 1, 5)) {
            tickSpeed.set(max(0, tickSpeed.get()))
            pushUpdate()
        }
        ImGui.spacing()
    }


}