package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import imgui.type.ImBoolean
import imgui.type.ImInt
import me.jraynor.api.Graph
import me.jraynor.api.data.Buffers
import me.jraynor.api.enums.Mode
import me.jraynor.api.logic.IExtraction
import me.jraynor.api.select.BlockSelect
import me.jraynor.util.coords
import me.jraynor.util.getFloatArray
import me.jraynor.util.putFloatArray
import me.jraynor.util.toBlockPos
import net.minecraft.block.BlockState
import net.minecraft.entity.item.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.wrapper.InvWrapper
import java.util.*

/**
 * This node can pick up items within a given radius
 */
class HopperNode(
    /**This is how far we should extend out **/
    val radius: ImInt = ImInt(),
    /**The center of the pick up sphere**/
    var center: IntArray = intArrayOf(0, 0, 0),
    /**This shows the center of the hopper area**/
    val shown: ImBoolean = ImBoolean(false),
    /**We store the center color, and it's random when generated**/
    var centerColor: FloatArray = floatArrayOf(
        Math.random().toFloat(),
        Math.random().toFloat(),
        Math.random().toFloat()
    ),
    /**This keeps track of the internal storage data.**/
    val buffers: Buffers = Buffers()
) : FilterableIONode(hasInput = false) {
    /**
     * This will generate an aabb for the current radius and center.
     */
    private val box: AxisAlignedBB
        get() = AxisAlignedBB(
            (center[0] - radius.get()).toDouble(),
            (center[1] - radius.get()).toDouble(),
            (center[2] - radius.get()).toDouble(),
            (center[0] + radius.get()).toDouble(),
            (center[1] + radius.get()).toDouble(),
            (center[2] + radius.get()).toDouble()
        )

    /**This is the current state of the block. This is only present on the client.**/
    private val blockState: BlockState?
        get() {
            return world.getBlockState(center.toBlockPos())
        }

    /**
     * This will write our radius and center position
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putInt("radius", radius.get())
        tag.putIntArray("center", center)
        tag.putFloatArray("centerColor", centerColor)
        tag.put("buffers", buffers.serializeNBT())
        return tag
    }

    /**
     * This will read our hopper data
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.radius.set(tag.getInt("radius"))
        this.center = tag.getIntArray("center")
        this.centerColor = tag.getFloatArray("centerColor")
        this.buffers.deserializeNBT(tag.getCompound("buffers"))
    }

    /***
     * This will allow us to tick the given node
     */
    override fun onTick(world: World, graph: Graph) {
        doItemSuckUp(world, graph)
        doInputOutput(graph, world as ServerWorld)
    }

    /**
     * This will suck the items into this
     */
    private fun doItemSuckUp(serverWorld: World, graph: Graph) {
        val filter = getItemFilter(graph)
        val entities = serverWorld.getEntitiesWithinAABB(ItemEntity::class.java, box) { true }
        entities.forEach { entity ->
            val stack = entity.item
            val source = LazyOptional.of {
                ItemStackHandler(1).apply {
                    this.setStackInSlot(0, stack)
                }
            }
            if (filter == null) {
                if (Mode.ITEM.extract(Mode.ITEM.type, stack.count, source, buffers[IItemHandler::class.java]).value) {
                    println("Successfully extracted the item entity into the node")
                    entity.remove()
                    pushUpdate()
                }
            } else {
                if (Mode.ITEM.extractFiltered(
                        Mode.ITEM.type, IItemHandler::class.java, stack.count, source, buffers[IItemHandler::class.java], filter
                    ).value
                ) {
                    println("Successfully extracted the item entity into the node")
                    entity.remove()
                    pushUpdate()
                }
            }
        }

    }

    /**
     * This should only be called from the client.
     * This code should not exist on the server.
     * (or at least should be called)
     */
    override fun render() {
        id ?: return
        NodeEditor.beginNode(id!!.toLong())
        ImGui.textColored(ImColor.rgbToColor("#9e9a8e"), "Hopper Node")
        ImGui.sameLine()
        if (ImGui.button("Select##$id"))
            pushFaceSelect()
        ImGui.textDisabled("${blockState?.block?.translatedName?.string}, ${center.contentToString()}")
        ImGui.dummy(0f, 6f)
        renderPorts()
        NodeEditor.endNode()
    }

    /**
     * This is used as the code that will render on the side view
     */
    override fun renderEx() {
        ImGui.setNextItemOpen(true, ImGuiCond.FirstUseEver)
        if (ImGui.collapsingHeader("Hopper Node")) {
            ImGui.setNextItemWidth(80f)
            if (ImGui.inputInt("radius##$id", radius, 1, 5))
                pushUpdate() //When input is update, push update
            ImGui.setNextItemWidth(150f)
            if (ImGui.inputInt3("hopper center##$id", center))
                pushUpdate()
            if (ImGui.checkbox("show center##$id", shown)) {
                val key = Pair(this.center.toBlockPos(), Direction.UP)
                if (shown.get())
                    BlockSelect.showFaces[key] = centerColor
                else
                    BlockSelect.showFaces.remove(key)
            }
            if (shown.get())
                if (ImGui.colorEdit3("face color", centerColor))
                    pushUpdate()
            buffers.render()
            ImGui.spacing()
            modes.render(this::pushUpdate)
        }
    }
}