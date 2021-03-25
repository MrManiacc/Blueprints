package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import imgui.type.ImBoolean
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.data.Buffers
import me.jraynor.api.extensions.*
import me.jraynor.util.extractNext
import me.jraynor.util.simulateNext
import me.jraynor.util.yawFromFacing
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import net.minecraft.util.ActionResultType

import net.minecraft.util.math.BlockRayTraceResult

import net.minecraft.util.Hand
import kotlin.collections.ArrayList

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
    override var inventory: Buffers.ItemHandlerBuffer = Buffers.ItemHandlerBuffer(1),
    override val useInputForPlacement: Boolean = true,
    override val showInventory: ImBoolean = ImBoolean(false),
    override val outputs: MutableList<Pin> = ArrayList()
) : Node(), FakePlayerExt, FilterExt, SelectableBlockExt, TickableExt {

    /**We want our input to be on a new line**/
    override val tickableOnSameLine: Boolean
        get() = true

    /***Push the filter to a new line**/
    override val filterOnSameLine: Boolean
        get() = false

    /**
     * This allows us to read the position data when reading the player
     */
    override fun readFakePlayer(tag: CompoundNBT, node: Node) {
        super.readFakePlayer(tag, node)

    }

    val held = inventory.getStackInSlot(0)

    /**
     * This will try to break the block at the selected position
     */
    override fun doTick(world: World, graph: Graph) {
        super.doTick(world, graph)
        val filter = getTextFilter(this, graph)
        val blockPos = selectedBlock ?: return
        val face = selectedFace ?: return
        val itemStack = inventory.simulateNext()
        val fakePlayer = player ?: return

        if (itemStack != ItemStack.EMPTY) {
            fakePlayer.setPosition(
                this.selectedBlock!!.x.toDouble(),
                this.selectedBlock!!.y.toDouble(),
                this.selectedBlock!!.z.toDouble()
            )

            fakePlayer.rotationYaw = selectedFace?.opposite?.yawFromFacing!!
            fakePlayer.setHeldItem(Hand.MAIN_HAND, itemStack)
            if (filter != null)
                if (!filter.filter(
                        ItemStack(world.getBlockState(blockPos.offset(face)).block).displayName,
                        null,
                        null
                    ).value
                )
                    return
            val result = rightClickBlock(world, itemStack)
            if (result == ActionResultType.CONSUME || result == ActionResultType.SUCCESS) {
                inventory.extractNext()
                pushServerUpdates(world, blockPos, graph)
            }
        }
    }

    @Throws(Exception::class) fun rightClickBlock(world: World, itemStack: ItemStack): ActionResultType {
        if (player == null)
            return ActionResultType.FAIL
        if (selectedBlock == null)
            return ActionResultType.FAIL
        if (selectedFace == null)
            return ActionResultType.FAIL
        val placementOn = selectedFace ?: player!!.adjustedHorizontalFacing
        val result = BlockRayTraceResult(
            player!!.lookVec, placementOn,
            selectedBlock!!, false
        )
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
        super<FilterExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
        super<SelectableBlockExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
        super<FakePlayerExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
    }
}