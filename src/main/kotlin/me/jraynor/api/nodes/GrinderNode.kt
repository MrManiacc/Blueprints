package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import imgui.type.ImBoolean
import imgui.type.ImInt
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.data.Buffers
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.api.logic.IFilter
import me.jraynor.api.select.PlayerHooks
import me.jraynor.util.getFloatArray
import me.jraynor.util.putFloatArray
import me.jraynor.util.toBlockPos
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.DamageSource
import net.minecraft.util.Direction
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.World

/**
 * This node can pick up items within a given radius
 */
class GrinderNode(
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
        Math.random().toFloat()
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
    ),
    /***This is how much damage to apply every tick**/
    val damage: ImInt = ImInt(10)
) : Node() {
    /**This is a buffer for the pins output, Its for performance**/
    private val outputs = ArrayList<Pin>()

    /**This is the current state of the block. This is only present on the client.**/
    private val blockState: BlockState?
        get() {
            return clientWorld.getBlockState(center.toBlockPos())
        }

    /**This is the buffer for the block pos**/
    private var blockPos: BlockPos = center.toBlockPos()

    /**
     * This will create the item filter
     */
    private fun getEntityFilter(graph: Graph): IFilter<ITextComponent, *>? {
        val filterNode = findPinWithLabel("Filter") ?: return null
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
     * This will add our tick pin
     */
    override fun addPins() {
        super.addPins()
        add(
            Pin(
                io = IO.INPUT,
                label = "DoTick",
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
    }

    /**
     * This will write our radius and center position
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putInt("damage", damage.get())
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
        this.damage.set(tag.getInt("damage"))
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
        this.blockPos = center.toBlockPos()
    }

    /***
     * This will allow us to tick the given node
     */
    override fun doTick(world: World, graph: Graph) {
        super.doTick(world, graph)
        val entities = world.getEntitiesWithinAABB(LivingEntity::class.java, box) { it !is PlayerEntity }
        val filter = getEntityFilter(graph)
        entities.forEach lit@{ entity ->
            if (filter == null) {
                if (grindEntity(entity))
                    return@lit
            } else {
                if (filter.filter(entity.name, null, null).value)
                    if (grindEntity(entity))
                        return@lit
            }
        }
    }

    /**
     * This will kill the given entity dropping its loops.
     */
    private fun grindEntity(entity: LivingEntity): Boolean {
        return entity.attackEntityFrom(DamageSource.GENERIC, this.damage.get().toFloat())
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
        ImGui.textColored(ImColor.rgbToColor("#9e9a8e"), "Grinder Node")
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
        if (ImGui.collapsingHeader("Grinder Node")) {
            super.renderEx()
            ImGui.setNextItemWidth(80f)
            if (ImGui.inputInt("damage per tick##$id", damage, 1))
                pushClientUpdate() //When input is update, push update
            ImGui.setNextItemWidth(80f)
            if (ImGui.inputInt("radius##$id", radius, 1, 5))
                pushClientUpdate() //When input is update, push update
            ImGui.setNextItemWidth(150f)
            if (ImGui.inputInt3("grinder center##$id", center))
                pushClientUpdate()
            if (ImGui.checkbox("show center##$id", shown)) {
                val key = Pair(this.center.toBlockPos(), Direction.UP)
                if (shown.get())
                    PlayerHooks.showFaces[key] = centerColor
                else
                    PlayerHooks.showFaces.remove(key)
            }
            if (shown.get())
                if (ImGui.colorEdit3("face color", centerColor))
                    pushClientUpdate()
            buffers.render()
            ImGui.spacing()
        }
    }
}