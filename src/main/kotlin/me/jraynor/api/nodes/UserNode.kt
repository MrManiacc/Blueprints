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
import me.jraynor.util.extractNext
import me.jraynor.util.simulateNext
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import net.minecraft.util.ActionResultType

import net.minecraft.util.math.BlockRayTraceResult

import net.minecraft.util.Hand

/**
 * This node is magical. It can place blocks into the world, it can right click with certain items in hand. It can drop
 * items in the world, it can sneak, and literally anything else a real player could do. This is the bread and butter
 * of automation. It also stores an internal inventory that is the size of a normal player's inventory. You can set items
 * to either main hand, off hand, hot bar slot, armor, or the inventory. It is a true "fake player" in the world and allows
 * for extreme amounts of customization.
 */
class UserNode(
    /**This is required for every node that extends [FakePlayerExt]. It keeps track of the node's player uuid**/
    override var playerId: UUID = UUID.randomUUID(),
    override var selectedBlock: BlockPos? = null,
    override var selectedFace: Direction? = null,
    override var shown: ImBoolean = ImBoolean(false),
    override var showColor: FloatArray = floatArrayOf(1f, 0f, 0f),
    override var inventory: Buffers.ItemHandlerBuffer = Buffers.ItemHandlerBuffer(64),
    override val useInputForPlacement: Boolean = true,
    override val showInventory: ImBoolean = ImBoolean(false)
) : Node(), FakePlayerExt, SelectableBlockExt, TickableExt {

    /**We want our input to be on a new line**/
    override val tickableOnSameLine: Boolean
        get() = false

    /**
     * This will try to break the block at the selected position
     */
    override fun doTick(world: World, graph: Graph) {
        super.doTick(world, graph)
        val blockPos = selectedBlock ?: return
        val itemStack = inventory.simulateNext()
        if (rightClickBlock(world, itemStack) == ActionResultType.SUCCESS) {
            inventory.extractNext()
            pushServerUpdates(world, blockPos, graph)
        }
    }

    @Throws(Exception::class) fun rightClickBlock(world: World, itemStack: ItemStack): ActionResultType {
        if (player == null)
            return ActionResultType.FAIL
        if (selectedBlock == null)
            return ActionResultType.FAIL
        if (selectedFace == null)
            return ActionResultType.FAIL
//
//        player?.setPosition(
//            this.selectedBlock!!.x.toDouble(),
//            this.selectedBlock!!.y.toDouble(),
//            this.selectedBlock!!.z.toDouble()
//        )
//        player?.rotationYaw = selectedFace?.opposite?.yawFromFacing
        val placementOn = selectedFace ?: player!!.adjustedHorizontalFacing
        val result = BlockRayTraceResult(
            player!!.lookVec, placementOn,
            selectedBlock!!, true
        )
        //processRightClick
        //it becomes CONSUME result 1 bucket. then later i guess it doesnt save, and then its water_bucket again
        return player!!.interactionManager.func_219441_a(
            player!!, world,
            itemStack, Hand.MAIN_HAND, result
        )
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
        ImGui.textColored(ImColor.rgbToColor("#D65076"), "User Node")
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
        if (ImGui.collapsingHeader("User Node")) {
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
        super<SelectableBlockExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
        super<FakePlayerExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
    }
}