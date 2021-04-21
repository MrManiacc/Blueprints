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
import me.jraynor.api.select.PlayerHooks
import me.jraynor.util.getFloatArray
import me.jraynor.util.putFloatArray
import me.jraynor.util.toBlockPos
import net.minecraft.block.BlockState
import net.minecraft.entity.item.ItemEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler

/**
 * This node can pick up items within a given radius
 */
class HopperNode(
    /**This is how far we should extend out **/
    val radius: ImInt = ImInt(2),
    /**The center of the pick up sphere**/
    var center: IntArray = intArrayOf(0, 0, 0),
    /**This shows the center of the hopper area**/
    val shown: ImBoolean = ImBoolean(false),
    /**We store the center color, and it's random when generated**/
    var centerColor: FloatArray = floatArrayOf(
        Math.random().toFloat(),
        Math.random().toFloat(),
        Math.random().toFloat(),
        0.8f
    ),
    /**This keeps track of the internal storage data.**/
    val buffers: Buffers = Buffers(),
    /***This is used for getting the entities**/
    private var box: AxisAlignedBB = AxisAlignedBB(
        (center[0] - radius.get()).toDouble(),
        (center[1] - radius.get()).toDouble(),
        (center[2] - radius.get()).toDouble(),
        (center[0] + radius.get()).toDouble(),
        (center[1] + radius.get()).toDouble(),
        (center[2] + radius.get()).toDouble()
    )
) : ExtractableNode(hasInput = false, hasTick = true) {

    /**This is the current state of the block. This is only present on the client.**/
    private val blockState: BlockState?
        get() {
            return clientWorld.getBlockState(center.toBlockPos())
        }

    /**This is the buffer for the block pos**/
    private var blockPos: BlockPos = center.toBlockPos()

    /**This is the source buffer for th**/
    private val sourceBuffer = ItemStackHandler(1)

    /**
     * This will write our radius and center position
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putInt("radius", radius.get())
        tag.putIntArray("center", center)
        tag.putFloatArray("centerColor", centerColor)
        tag.put("buffers", buffers.serializeNBT())
        tag.putBoolean("shown", shown.get())
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
        this.box = AxisAlignedBB(
            (center[0] - radius.get()).toDouble(),
            (center[1] - radius.get()).toDouble(),
            (center[2] - radius.get()).toDouble(),
            (center[0] + radius.get()).toDouble(),
            (center[1] + radius.get()).toDouble(),
            (center[2] + radius.get()).toDouble()
        )
        this.shown.set(tag.getBoolean("shown"))
        this.blockPos = center.toBlockPos()
    }

    /***
     * This will allow us to tick the given node
     */
    override fun doTick(world: World, graph: Graph) {
        super.doTick(world, graph)
        doItemSuckUp(world, graph)
    }

    /**
     * This will suck the items into this
     */
    private fun doItemSuckUp(serverWorld: World, graph: Graph) {
        val filter = getItemFilter(graph)
        val entities = serverWorld.getLoadedEntitiesWithinAABB(ItemEntity::class.java, box) { true }
        entities.forEach { entity ->
            val stack = entity.item
            sourceBuffer.setStackInSlot(0, stack)
            val source = LazyOptional.of { sourceBuffer }
            if (filter == null) {
                if (Mode.ITEM.extract(
                        Mode.ITEM.type,
                        stack.count,
                        source,
                        buffers[IItemHandler::class.java]
                    ).value
                ) {
                    entity.remove()
                    pushServerUpdates(serverWorld, blockPos, graph)
                }
            } else {
                if (Mode.ITEM.extractFiltered(
                        Mode.ITEM.type,
                        IItemHandler::class.java,
                        stack.count,
                        source,
                        buffers[IItemHandler::class.java],
                        filter
                    ).value
                ) {
                    entity.remove()
                    pushServerUpdates(serverWorld, blockPos, graph)
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
        super.render()
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
            ImGui.text("Hopper info: ")
            ImGui.separator()
            ImGui.indent()
            super.renderEx()
            ImGui.setNextItemWidth(80f)
            if (ImGui.inputInt("radius##$id", radius, 1, 5))
                pushClientUpdate() //When input is update, push update
            ImGui.setNextItemWidth(150f)
            if (ImGui.inputInt3("hopper center##$id", center))
                pushClientUpdate()
            val key = Pair(this.center.toBlockPos(), Direction.UP)
            if (ImGui.checkbox("show center##$id", shown)) {
                pushClientUpdate()
                if (shown.get())
                    PlayerHooks.showFaces[key] = centerColor
                else
                    PlayerHooks.showFaces.remove(key)
            }
            if (shown.get())
                if (ImGui.colorEdit4("face color", centerColor)) {
                    PlayerHooks.showFaces[key] = centerColor
                    pushClientUpdate()
                }
            buffers.render()
            ImGui.unindent()
            ImGui.spacing()
            modes.render(this::pushClientUpdate)
        }
    }
}