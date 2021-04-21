package me.jraynor.api.select

import me.jraynor.api.Node
import me.jraynor.api.extensions.FakePlayerExt
import me.jraynor.api.network.Network
import me.jraynor.api.packets.PacketSelectStart
import me.jraynor.api.packets.PacketUpdateNode
import me.jraynor.api.select.render.RenderData
import me.jraynor.util.rayTraceBlock
import me.jraynor.util.whenClient
import me.jraynor.util.whenServer
import net.minecraft.client.Minecraft
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector.Vector4f
import net.minecraft.util.text.StringTextComponent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import thedarkcolour.kotlinforforge.eventbus.KotlinEventBus
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * This is a global object that is kept on the client for what you're selecting
 */
object PlayerHooks {
    /**Our current selection data. Keeps track of the node data and onSelect callback**/
    private val context: SelectionContext = SelectionContext()
    private val renderData = RenderData()
    val selectingPlayers = HashSet<UUID>()
    val showFaces = HashMap<Pair<BlockPos, Direction>, FloatArray>()
    private var nodeContext: FakePlayerExt? = null

    /**
     * This will register the listeners
     */
    internal fun register(modBus: KotlinEventBus, forgeBus: KotlinEventBus) {
        whenClient(false) {
            forgeBus.addListener(this::onTick)
            forgeBus.addListener(this::onRender)
            forgeBus.addListener(this::onClickAir) //This should be registered on both the client and server
        }
        forgeBus.addListener(this::onClickBlock)//This should be registered on both the client and server.
    }

    /**
     * This will start a new selection
     */
    fun start(owner: BlockPos, graphId: UUID, node: Node, callback: (Selection) -> Unit) {
        Network.sendToServer(PacketSelectStart(Minecraft.getInstance().player?.uniqueID))
        context.new(graphId, node, callback, owner)
    }

    /**
     * This is called upon the client tick
     */
    private fun onTick(event: TickEvent.ClientTickEvent) {
        if (context.valid) {
            if (context.player == null)
                context.invalidate()
            else {
                val result = rayTraceBlock(context.player!!, context.world, 50)
                if (!result.isInside) {
                    context.currentBlock = result.pos
                    context.currentFace = result.face
                }
            }
        }
    }

    /**
     * This is called upon the client tick
     */
    private fun onRender(event: RenderWorldLastEvent) {
        renderData.start(event)
        if (context.ready)
            renderData.drawFaceOverlay(context.currentBlock!!, context.currentFace!!)
        showFaces.forEach {
            val pos = it.key.first
            val face = it.key.second
            val color = it.value
            when (color.size) {
                4 -> renderData.drawFaceOverlay(pos, face, Vector4f(color[0], color[1], color[2], color[3]))
                3 -> renderData.drawFaceOverlay(pos, face, Vector4f(color[0], color[1], color[2], 1.0f))
                else -> renderData.drawFaceOverlay(pos, face, Vector4f(0.8f, 0.432f, 0.4f, 0.75f))
            }
        }
        renderData.stop()

    }

    /**
     * This is called upon the client tick
     */
    private fun onClickAir(event: PlayerInteractEvent.LeftClickEmpty) {
        if (context.ready) {
            //TODO invalidate the selectionContext and call the callback with the given block.
            val selection = Selection(context.currentBlock!!, context.currentFace!!, context.node!!)
            finish(selection)
        }
        if (nodeContext != null) {
            val item = event.itemStack
            nodeContext!!.inventory.setStackInSlot(0, item)
            event.player.sendStatusMessage(
                StringTextComponent("Set ${item.item.name.string} as the held item for current node!"),
                true
            )
            (if (nodeContext!! is Node) nodeContext as Node else null)?.pushClientUpdate()
            nodeContext = null
        }
    }

    /**
     * This is called upon the client tick
     */
    private fun onClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        whenClient {
            if (context.ready) {
                //TODO invalidate the selectionContext and call the callback with the given block.
                val selection = Selection(context.currentBlock!!, context.currentFace!!, context.node!!)
                finish(selection)
                event.isCanceled = true
            }
            if (nodeContext != null) {
                val item = event.itemStack
                nodeContext!!.inventory.setStackInSlot(0, item)
                event.player.sendStatusMessage(
                    StringTextComponent("Set ${item.item.name.string} as the held item for current node!"),
                    true
                )
                (if (nodeContext!! is Node) nodeContext as Node else null)?.pushClientUpdate()
                nodeContext = null
            }
        }
        whenServer {
            if (selectingPlayers.contains(event.player.uniqueID)) {
                event.isCanceled = true
                selectingPlayers.remove(event.player.uniqueID)
            }
        }
    }

    /**
     * This will start the selection process for the given node
     */
    fun selectItem(node: FakePlayerExt) {
        nodeContext = node
        Minecraft.getInstance().displayGuiScreen(null)
        Minecraft.getInstance().player?.sendStatusMessage(
            StringTextComponent("Left click with the item you wish to set in your hand!"),
            true
        )
    }

    /**
     * This will invalidate the selection
     */
    private fun finish(selection: Selection) {
        if (context.valid) {
            context.callback!!(selection) //Calls our callback if it's not null with the passed selection data.

        }
        context.invalidate()
    }


}