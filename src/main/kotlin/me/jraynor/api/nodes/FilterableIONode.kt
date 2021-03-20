package me.jraynor.api.nodes

import imgui.ImColor
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.data.Modes
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.api.enums.Mode
import me.jraynor.api.logic.IFilter
import me.jraynor.api.logic.IResolver
import me.jraynor.api.logic.Return
import me.jraynor.api.network.Network
import me.jraynor.api.packets.PacketUpdateNode
import me.jraynor.api.serverdata.WorldData
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.items.IItemHandler

/**
 * This is a base for nodes that have input and output of some kind
 */
abstract class FilterableIONode(val modes: Modes = Modes(), val hasInput: Boolean = true) : Node() {

    /**
     * This is called when the node is added to the graph. At this point the node's id should be set so we can
     * safely set the node id of the mode
     */
    override

    fun onAdd(graph: Graph) {
        modes.node = this
        this.parent = graph
    }

    /**
     * This will create the item filter
     */
    protected fun getItemFilter(graph: Graph): IFilter<ItemStack, IItemHandler>? {
        val filterNode = findPinWithLabel("Filter") ?: return null
        val outputs = filterNode.outputs(graph)
        for (output in outputs) {
            output.id ?: continue
            val node = graph.findNodeByPinId(output.id!!)
            if (node is FilterNode) {
                return node.itemFilter
            }
        }
        return null
    }

    /**
     * This should add the pins
     */
    override fun addPins() {
        modes.node = this
        add(
            Pin(
                io = IO.INPUT,
                label = "DoExtract",
                textAfter = true,
                computeText = false,
                sameLine = true,
                icon = IconType.ROUND_SQUARE,
                innerColor = ImColor.rgbToColor("#fafafa")
            )
        )
        add(
            Pin(
                io = IO.OUTPUT,
                label = "Filter",
                textAfter = false,
                computeText = true,
                indent = 125f,
                icon = IconType.ROUND_SQUARE,
                color = ImColor.rgbToColor("#653cfa"),
                innerColor = ImColor.rgbToColor("#18f01f")
            )
        )
        if (hasInput)
            add(
                Pin(
                    io = IO.INPUT,
                    label = "Insert",
                    textAfter = true,
                    computeText = false,
                    sameLine = true,
                    newLine = true,
                    icon = IconType.GRID,
                    innerColor = ImColor.rgbToColor("#c910cc")
                )
            )
        add(
            Pin(
                io = IO.OUTPUT,
                label = "Extract",
                textAfter = false,
                computeText = true,
                indent = 120f,
                icon = IconType.GRID,
                innerColor = ImColor.rgbToColor("#18f01f")
            )
        )
    }

    /**
     * This will write the link and the face. It has null safety so if they are null they will not be written. There
     * won't be an exception the data just isn't present currently.
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.put("mode", modes.serializeNBT())
        return tag
    }

    /**
     * This will first deserialize the node node data from the base [Node] class. Then it will deserialize the position
     * of the block and the face.
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.modes.deserializeNBT(tag.getCompound("mode"))
        this.modes.node = this
    }

    /**
     * This is the code that is
     */
    protected open fun doInputOutput(graph: Graph, world: ServerWorld) {
        val filter = getItemFilter(graph)
        val serverWorld = WorldData.world ?: return
        parent ?: return
        val blockPos = parent!!.blockPos ?: return
        val extract = findPinWithLabel("Extract") ?: return
        val outputs = extract.outputs(graph)
        for (output in outputs) {
            output.nodeId ?: continue
            val node = graph.findById(output.nodeId!!) ?: continue
            modes.forEach { mode, speed ->
                val source = IResolver.resolve(mode.type, this, serverWorld)
                val target = IResolver.resolve(mode.type, node, serverWorld)
                if (filter == null)
                    mode.extract(mode.type, speed, source, target).value
                else
                    mode.extractFiltered(mode.type, ItemStack::class.java, speed, source, target, filter)
                node.onInput(mode, graph)
                Network.sendToClientsWithBlockLoaded(
                    PacketUpdateNode(
                        uuid = parent!!.uuid,
                        pos = blockPos,
                        node = node
                    ),
                    blockPos,
                    world
                )
            }
        }
        Network.sendToClientsWithBlockLoaded(
            PacketUpdateNode(
                uuid = parent!!.uuid,
                pos = blockPos,
                node = this
            ),
            blockPos, world
        )
    }
}