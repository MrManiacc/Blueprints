@file:Suppress("DEPRECATION")

package me.jraynor.api.utilities

import com.mojang.blaze3d.systems.RenderSystem
import imgui.ImDrawList
import imgui.ImVec2
import net.minecraft.entity.player.PlayerEntity
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
import imgui.ImGui
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.inventory.*
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math.RayTraceContext
import net.minecraftforge.common.util.*
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import java.io.File
import java.nio.file.Path

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
    flags: Int = 0xF
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
    flags: Int = 0xF
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
 * Returns the coords for the blockpos
 */
val BlockPos.coords: String get() = "$x, $y, $z"

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
    println("here")
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

/**This simply returns the lower case of the name**/
internal val Enum<*>.lower: String
    get() = this.name.toLowerCase()

/**
 * This will render the interface for for an inventory
 */
fun IItemHandler.renderGui(text: String, intRange: IntRange? = null) {
    if (ImGui.beginListBox(text)) {
        InventoryUtils.forEachItem(this) lit@{ i, _, item, count ->
            if (intRange == null) {
                if (item == Items.AIR) return@lit
                ImGui.text("${item.name.string}: $count")
                ImGui.separator()
            } else {
                if (intRange.contains(i)) {
                    if (item == Items.AIR) return@lit
                    ImGui.text("${item.name.string}: $count")
                    ImGui.separator()
                }
            }
        }
        ImGui.endListBox()
    }
}

/**
 * this will generate a new directory of the path if it doesnn't exist and return the file
 */
fun Path.toDirectoryOrNew(createParents: Boolean = false): File {
    val file = this.toFile()
    if (!file.exists())
        if (createParents) file.mkdirs() else file.mkdir()
    return file
}

inline fun <reified T : IPin> INode.hasPin(): Boolean = this.hasPin(T::class)

inline fun <reified T : IPin> INode.getPin(): T = this.getPin(T::class)