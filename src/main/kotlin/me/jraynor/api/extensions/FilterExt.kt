package me.jraynor.api.extensions

import com.mojang.authlib.GameProfile
import imgui.ImColor
import imgui.ImGui
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.api.logic.IFilter
import me.jraynor.api.nodes.FilterNode
import me.jraynor.api.serverdata.WorldData
import me.jraynor.util.clientWorld
import me.jraynor.util.logicalClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.IPacket
import net.minecraft.network.NetworkManager
import net.minecraft.network.PacketDirection
import net.minecraft.network.play.ServerPlayNetHandler
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.items.IItemHandler
import java.util.*
import kotlin.collections.HashMap

/**
 * This type of node is used for automation tasks that require a fake player.
 */
interface FilterExt : INodeExtension {
    /**If the filter should be on the same line or not**/
    val filterOnSameLine: Boolean
        get() = false

    /**If the filter should be on the same line or not**/
    val filterIndent: Float
        get() = 100f

    /**This is a buffer for the pins output, Its for performance**/
    val outputs: MutableList<Pin>

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
        pinAdds.add(this::addFilter)
    }

    /**
     * This will create the item filter
     */
    fun getItemFilter(nodeIn: Node, graph: Graph): IFilter<ItemStack, IItemHandler>? {
        val filterNode = nodeIn.findPinWithLabel("Filter") ?: return null
        val outputs = filterNode.outputs(graph, this.outputs) ?: return null
        for (output in outputs) {
            output.id ?: continue
            val node = graph.findNodeByPinId(output.id!!)
            if (node is FilterNode)
                return node.itemFilter
        }
        return null
    }

    /**
     * This will create the item filter
     */
    fun getTextFilter(nodeIn: Node, graph: Graph): IFilter<ITextComponent, Any>? {
        val filterNode = nodeIn.findPinWithLabel("Filter") ?: return null
        val outputs = filterNode.outputs(graph, this.outputs) ?: return null
        for (output in outputs) {
            output.id ?: continue
            val node = graph.findNodeByPinId(output.id!!)
            if (node is FilterNode)
                return node.genericFilter
        }
        return null
    }

    /**
     * This will add our tick pin the node.
     */
    fun addFilter(node: Node) {
        node.add(
            Pin(
                io = IO.OUTPUT,
                label = "Filter",
                textAfter = false,
                computeText = true,
                sameLine = filterOnSameLine,
                indent = filterIndent,
                icon = IconType.ROUND_SQUARE,
                color = ImColor.rgbToColor("#653cfa"),
                innerColor = ImColor.rgbToColor("#18f01f")
            )
        )
    }

}