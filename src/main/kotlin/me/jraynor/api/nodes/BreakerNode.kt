package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import imgui.type.ImBoolean
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.data.Buffers
import me.jraynor.api.extensions.*
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import java.util.*

/**
 * This node is magical. It can place blocks into the world, it can right click with certain items in hand. It can drop
 * items in the world, it can sneak, and literally anything else a real player could do. This is the bread and butter
 * of automation. It also stores an internal inventory that is the size of a normal player's inventory. You can set items
 * to either main hand, off hand, hot bar slot, armor, or the inventory. It is a true "fake player" in the world and allows
 * for extreme amounts of customization.
 */
class BreakerNode(
    /**This is required for every node that extends [FakePlayerExt]. It keeps track of the node's player uuid**/
    override var playerId: UUID = UUID.randomUUID(),
    override var selectedBlock: BlockPos? = null,
    override var selectedFace: Direction? = null,
    override var shown: ImBoolean = ImBoolean(false),
    override var showColor: FloatArray = floatArrayOf(1f, 0f, 0f),
    override var inventory: Buffers.ItemHandlerBuffer = Buffers.ItemHandlerBuffer(1),
    override val useInputForPlacement: Boolean = false,
    override val showInventory: ImBoolean = ImBoolean(false)
) : Node(), FakePlayerExt, SelectableBlockExt, TickableExt, FilterExt {

    /**
     * This will try to break the block at the selected position
     */
    override fun doTick(world: World, graph: Graph) {
        val filter = getTextFilter(this, graph)
        super.doTick(world, graph)
        val blockPos = selectedBlock ?: return
        val block = world.getBlockState(blockPos)
        if (filter != null)
            if (!filter.filter(block.block.translatedName, null, null).value)
                return
        player?.interactionManager?.tryHarvestBlock(blockPos)
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
        ImGui.textColored(ImColor.rgbToColor("#D65076"), "Breaker Node")
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
        if (ImGui.collapsingHeader("Breaker Node")) {
            super.renderEx()
        }
    }

    /**
     * This will hook our player extensions then our selected block extensions
     */
    override fun hook(
        nodeRenders: MutableList<Callback>,
        propertyRenders: MutableList<Callback>,
        pinAdds: MutableList<Callback>,
        tickCalls: MutableList<TickCallback>,
        readCalls: MutableList<NBTCallback>,
        writeCalls: MutableList<NBTCallback>
    ) {
        super<TickableExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
        super<FilterExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
        super<SelectableBlockExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
        super<FakePlayerExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
    }
}