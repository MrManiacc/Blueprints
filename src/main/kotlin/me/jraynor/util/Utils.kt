@file:Suppress("DEPRECATION")

package me.jraynor.util

import com.mojang.blaze3d.systems.RenderSystem
import imgui.ImDrawList
import imgui.ImVec2
import imgui.flag.ImDrawCornerFlags
import me.jraynor.api.Node
import me.jraynor.api.Pin
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.concurrent.ThreadTaskExecutor
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.LogicalSidedProvider
import net.minecraftforge.fml.common.thread.SidedThreadGroups
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.network.NetworkEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import com.sun.javafx.geom.Vec3d
import net.minecraft.block.Blocks
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.item.TieredItem
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math.RayTraceContext
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler

/**
 * This is used for when a link is created in the node graph
 */
typealias OnNodeLink = (source: Pin, target: Pin) -> Unit

/**
 * This is used for adding of node buttons to the context menu
 * the return value determines if the node can be deleted or not
 */
typealias OnDeleteNode = (nodeToDelete: Node) -> Boolean

/**
 * This is used for adding of node buttons to the context menu
 * the return value determines if the node can be deleted or not
 */
typealias OnDeleteLink = (updatedNode: Node) -> Unit

/**
 * This is used for adding of node buttons to the context menu
 * the return value determines if the node can be deleted or not
 */
typealias OnAddNode = (Node) -> Unit
/**
 * This is used for adding of node buttons to the context menu
 */
typealias AddNode = Pair<String, () -> Node>

/**
 * This is used for adding of node buttons to the context menu
 */
typealias Listener = (packet: Any, context: NetworkEvent.Context) -> Unit

/***
 * This is used for rendering an icon
 */
typealias IconRenderers = (
    drawList: ImDrawList,
    rectX: Float,
    rectY: Float,
    rectW: Float,
    rectH: Float,
    rectCenterX: Float,
    rectCenterY: Float,
    outlineScale: Float,
    extractSegments: Int,
    filled: Boolean,
    color: Int,
    innerColor: Int
) -> Unit

/**
 * This will draw a rect filled with the given imvecs
 */
fun ImDrawList.addRectFilled(
    topLeft: ImVec2,
    bottomLeft: ImVec2,
    color: Int,
    rounding: Float? = null,
    flags: Int = ImDrawCornerFlags.All
) {
    if (rounding == null)
        this.addRectFilled(topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y, color)
    else
        this.addRectFilled(topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y, color, rounding, flags)
}

/**
 * This will draw a rect filled with the given imvecs
 */
fun ImDrawList.addRect(
    topLeft: ImVec2,
    bottomLeft: ImVec2,
    color: Int,
    rounding: Float? = null,
    thickness: Float? = null,
    flags: Int = ImDrawCornerFlags.All
) {
    if (rounding == null && thickness == null)
        this.addRect(topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y, color)
    else if (rounding != null && thickness == null)
        this.addRect(topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y, color, rounding, flags)
    else if (rounding != null && thickness != null) {
        this.addRect(
            topLeft.x,
            topLeft.y,
            bottomLeft.x,
            bottomLeft.y,
            color,
            rounding,
            flags,
            thickness
        )
    }
}

/**
 * Returns the coords for the blockpost
 */
val BlockPos.coords: String get() = "$x, $y, $z"

/**
 * This will write a uuid
 */
fun CompoundNBT.putClass(name: String, clazz: Class<*>) {

    this.putString(name, clazz.name)
}

/**
 * This will get a class with the given super type of T
 */
fun CompoundNBT.getClass(name: String): Class<*> {
    val clsName = this.getString(name)
    return Class.forName(clsName)
}

/**
 * This will write a uuid
 */
fun CompoundNBT.putUUID(name: String, uuid: UUID): CompoundNBT {
    this.putLongArray(name, longArrayOf(uuid.mostSignificantBits, uuid.leastSignificantBits))
    return this
}

/**
 * This will read a uuid
 */
fun CompoundNBT.getUUID(name: String): UUID? {
    val array = this.getLongArray(name)
    if (array.size != 2) return null
    return UUID(array[0], array[1])
}

/**
 * This will write a uuid
 */
fun CompoundNBT.putBlockPos(name: String, pos: BlockPos): CompoundNBT {
    this.putIntArray(name, intArrayOf(pos.x, pos.y, pos.z))
    return this
}

/**
 * This will read a uuid
 */
fun CompoundNBT.getBlockPos(name: String): BlockPos? {
    val array = this.getIntArray(name)
    if (array.size != 3) return null
    return BlockPos(array[0], array[1], array[2])
}

/**
 * This will write a uuid
 */
inline fun <reified T : Enum<*>> CompoundNBT.putEnum(name: String, enum: T): CompoundNBT {
    this.putInt(name, enum.ordinal)
    return this
}

/**
 * This will read a uuid
 */
inline fun <reified T : Enum<*>> CompoundNBT.getEnum(name: String): T {
    return T::class.java.enumConstants[this.getInt(name)]
}

/**
 * This will do ors of the given values.
 */
fun Int.orEquals(vararg ints: Int): Int {
    var out = this
    for (element in ints)
        out = out or element
    return out
}

/**This boolean checks to see if the current program is on the physical client or not**/
val physicalClient: Boolean
    get() = FMLEnvironment.dist == Dist.CLIENT

/**This boolean checks to see if the current program is on the physical server or not**/
val physicalServer: Boolean
    get() = FMLEnvironment.dist == Dist.DEDICATED_SERVER

/**This boolean checks to see if the current thread group is thew logical client**/
val logicalClient: Boolean
    get() {
        if (physicalServer) return false //This is so we don't end up calling [Minecraft] calls from the client
        if (Thread.currentThread().threadGroup == SidedThreadGroups.CLIENT) return true
        try {
            if (RenderSystem.isOnRenderThread()) return true
        } catch (notFound: ClassNotFoundException) {
            return false //We're not on the client if there's a class not found execetion
        }
        return false
    }

/**This boolean checks to see if the current thread group is thew logical client**/
val logicalServer: Boolean
    get() = Thread.currentThread().threadGroup == SidedThreadGroups.SERVER

/**This boolean checks to see if the current thread group is thew logical client**/
val logicalRender: Boolean
    get() = RenderSystem.isOnRenderThread()

/**
 * This block of code will execute only if we're on the physical client
 */
fun whenClient(logical: Boolean = true, block: () -> Unit) {
    if (logical && logicalClient) block()
    else if (!logical && physicalClient) block()
}

/**
 * This block of code will execute only if we're on the physical client
 */
fun whenServer(logical: Boolean = true, block: () -> Unit) {
    if (logical && logicalServer) block()
    else if (!logical && physicalServer) block()
}

/**
 * This will run the given block on the logical side
 */
fun runOn(side: LogicalSide, block: () -> Unit): CompletableFuture<Void> {
    val executor = LogicalSidedProvider.WORKQUEUE.get<ThreadTaskExecutor<*>>(side)
    return if (!executor.isOnExecutionThread)
        executor.deferTask(block) // Use the internal method so thread check isn't done twice
    else {
        block()
        CompletableFuture.completedFuture(null)
    }
}

/**
 * This run the given chunk of code on the client
 */
fun runOnClient(block: () -> Unit): CompletableFuture<Void> {
    return runOn(LogicalSide.CLIENT, block)
}

/**
 * This will run the render method
 */
fun runOnRender(block: () -> Unit) {
    if (logicalRender)
        block()
    else
        RenderSystem.recordRenderCall(block)
}

/**
 * This run the given chunk of code on the server
 */
fun runOnServer(block: () -> Unit): CompletableFuture<Void> {
    return runOn(LogicalSide.SERVER, block)
}

/**
 * This will raytrace the given distance for the given player
 */
fun rayTraceBlock(player: PlayerEntity, world: World, distance: Int): BlockRayTraceResult {
    val vec: Vector3d = player.positionVec
    val vec3 = Vector3d(vec.x, vec.y + player.eyeHeight, vec.z)
    val vec3a: Vector3d = player.getLook(1.0f)
    val vec3b: Vector3d = vec3.add(vec3a.getX() * distance, vec3a.getY() * distance, vec3a.getZ() * distance)

    val rayTraceResult = world.rayTraceBlocks(
        RayTraceContext(
            vec3,
            vec3b,
            RayTraceContext.BlockMode.COLLIDER,
            RayTraceContext.FluidMode.NONE,
            player
        )
    )

    var xm = rayTraceResult.hitVec.getX()
    var ym = rayTraceResult.hitVec.getY()
    var zm = rayTraceResult.hitVec.getZ()
    var pos = BlockPos(xm, ym, zm)
    val block = world.getBlockState(pos)
    if (block.isAir(world, pos)) {
        if (rayTraceResult.face == Direction.SOUTH)
            zm--
        if (rayTraceResult.face == Direction.EAST)
            xm--
        if (rayTraceResult.face == Direction.UP)
            ym--
    }
    pos = BlockPos(xm, ym, zm)
    return BlockRayTraceResult(rayTraceResult.hitVec, rayTraceResult.face, pos, false)
}

/**
 * This will pop and consume the next available if there is one
 */
fun <E : Any> Stack<E>.consumeNext(consumer: (element: E) -> Unit) {
    if (isNotEmpty()) {
        val next = this.pop() ?: return
        consumer(next)
    }
}

/**
 * This creates a new block pos from this int array
 */
fun IntArray.toBlockPos(): BlockPos {
    return BlockPos(this[0], this[1], this[2])
}

/**
 * This creates a new block pos from this int array
 */
fun BlockPos.toArray(): IntArray {
    return intArrayOf(x, y, z)
}

/**
 * This will put a float array iin the compound tag
 */
fun CompoundNBT.putFloatArray(name: String, floatArray: FloatArray) {
    val tag = CompoundNBT()
    tag.putInt("size", floatArray.size)
    for (i in floatArray.indices)
        tag.putFloat("f_$i", floatArray[i])
    this.put(name, tag)
}

/**
 * This will read the float array
 */
fun CompoundNBT.getFloatArray(name: String): FloatArray {
    val tag = this.getCompound(name)
    val size = tag.getInt("size")
    val array = FloatArray(size)
    for (i in array.indices)
        array[i] = tag.getFloat("f_$i")
    return array
}

/**
 * This will allow us to access the current client's world from anywhere
 */
val clientWorld: World?
    get() {
        if (logicalServer) return null
        return Minecraft.getInstance().world
    }

/**
 * This is a helper function to clear an itemstack
 */
fun ItemStackHandler.clear() {
    for (i in 0 until slots)
        this.setStackInSlot(0, ItemStack.EMPTY)
}

/**
 * This will return the next non empty item stack (if possible)
 */
fun ItemStackHandler.simulateNext(): ItemStack {
    for (i in 0 until slots) {
        val stack = this.getStackInSlot(i)
        if (!stack.isEmpty)
            return this.extractItem(i, 1, true)
    }
    return ItemStack.EMPTY
}

/**
 * This will return the next non empty item stack (if possible)
 */
fun ItemStackHandler.extractNext(): ItemStack {
    for (i in 0 until slots) {
        val stack = this.getStackInSlot(i)
        if (!stack.isEmpty)
            this.extractItem(i, 1, false)
    }
    return ItemStack.EMPTY
}

/**
 * This will get the yaw for the placement.
 */
val Direction.yawFromFacing: Float
    get() = when (this) {
        Direction.DOWN, Direction.UP, Direction.SOUTH -> 0f
        Direction.EAST -> 270f
        Direction.NORTH -> 180f
        Direction.WEST -> 90f
        else -> 0f
    }
