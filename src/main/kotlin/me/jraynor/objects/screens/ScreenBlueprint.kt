package me.jraynor.objects.screens

import com.mojang.blaze3d.matrix.MatrixStack
import imgui.extension.nodeditor.NodeEditor
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.extensions.SelectableBlockExt
import me.jraynor.api.network.Network
import me.jraynor.api.nodes.*
import me.jraynor.api.packets.*
import me.jraynor.api.select.PlayerHooks
import me.jraynor.imgui.Gui
import me.jraynor.objects.tile.SingularityTile
import me.jraynor.util.AddNode
import me.jraynor.util.consumeNext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.StringTextComponent
import java.util.*

/**
 * This is the base blueprint screen.
 */
class ScreenBlueprint(private val tile: SingularityTile) : Screen(StringTextComponent("")) {
    /**This is used to prevent the right client context menu upon opening the screen**/
    var firstRun = true

    /**This keeps track of the correct window name**/
    val name = "Instance_${currentInstance++}"

    /**Eventually this will be requested/read from the server.**/
    private val graph: Graph?
        get() = tile.clientGraph

    /**This is used as a buffer for the selected links**/
    private val selectedNodes: LongArray = LongArray(100) { -1 } //100  links max

    /**This is the currently selected node**/
    private var selectedNode: Node? = null

    /**This stores the pairs for our add node**/
    private val addNodes: Array<AddNode> = arrayOf(
        Pair("new tick node", {
            graph?.createNode<TickingNode>()!!
        }),
        Pair("new link node", {
            graph?.createNode<LinkNode>().apply {
                this?.selectedBlock = BlockPos(0, 0, 0)
                this?.selectedFace = Direction.UP
            }!!
        }),
        Pair("new filter node", {
            graph?.createNode<FilterNode>()!!
        }),
        Pair("new buffer node", {
            graph?.createNode<BufferNode>()!!
        }),
        Pair("new hopper node", {
            graph?.createNode<HopperNode>()!!
        }),
        Pair("new grinder node", {
            graph?.createNode<GrinderNode>()!!
        }),
        Pair("new breaker node", {
            graph?.createNode<BreakerNode>()!!
        }),
        Pair("new user node", {
            graph?.createNode<UserNode>()!!
        })
    )

    /**
     * This method will push the update to the server
     */
    private fun pushUpdate(updatedNode: Node) {
        Network.sendToServer(PacketUpdateNode(tile.pos, tile.uuid, updatedNode))
    }

    /**
     * This method will push the update to the server
     */
    private fun pushBlockSelect(nodeIn: Node) {
        closeScreen()
        PlayerHooks.start(tile.pos, tile.uuid!!, nodeIn) {
            when (val node = it.forNode) {
                is SelectableBlockExt -> {
                    node.selectedBlock = it.blockPos
                    node.selectedFace = it.face
                    pushUpdate(node)
                }
                is HopperNode -> {
                    node.center = intArrayOf(it.blockPos.x, it.blockPos.y, it.blockPos.z)
                    pushUpdate(it.forNode)
                }
                is GrinderNode -> {
                    node.center = intArrayOf(it.blockPos.x, it.blockPos.y, it.blockPos.z)
                    pushUpdate(node)
                }
            }
        }
    }

    /**
     * Sets the current editor context, for ease of access
     */
    override fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        Gui.frame {
            Gui.dockspace(name, this::renderProperties, this::renderEditorWindow)
        }
    }

    /**
     * This will render the  editor
     */
    private fun renderEditorWindow() {
        if (graph != null)
            Gui.renderNodeGraph(
                this::pushUpdate,
                this::pushBlockSelect,
                this::firstRun,
                this::pushUpdate,
                this::onLink,
                this::onDelete,
                this::onAdd,
                graph!!,
                * addNodes
            )
    }

    /**
     * This will render the nodes window
     */
    private fun renderProperties() {
        pushSelectedNodes()
        popSelectedNodes {
            it.renderEx()
        }
    }

    /**
     * This pushes the selected nodes onto the stack
     */
    private fun pushSelectedNodes() {
        selectedNodes.fill(-1, 0, selectedNodes.size)
        NodeEditor.getSelectedNodes(selectedNodes, selectedNodes.size)
        if (selectedNodes.isNotEmpty()) {
            val nodeId = if (selectedNodes[0] != -1L) selectedNodes[0].toInt() else return
            val node = if (graph?.hasNode(nodeId) == true) graph?.findById(nodeId) else return
            selectedNode = node
        }
    }

    /**
     * This will pop the selected nodes and
     */
    private fun popSelectedNodes(consumer: (node: Node) -> Unit) {
        if (selectedNode != null) {
            consumer(selectedNode!!)
            selectedNode = null
        }

    }

    /**
     * This is called when a new link is created. We should update the server at this point.
     */
    private fun onAdd(node: Node) {
        if (graph != null)
            Network.sendToServer(PacketNodeAdd(tile.pos, tile.uuid, node, graph?.nextNodeId, graph?.nextPinId))
    }

    /**
     * This is called when a new link is created. We should update the server at this point.
     */
    private fun onLink(source: Pin, target: Pin) {
        Network.sendToServer(PacketNodeLink(tile.pos, tile.uuid, source.id, target.id))
    }

    /**
     * This is called before deleting the node. It has a return of a boolean that if true will allow it to be deleted
     */
    private fun onDelete(node: Node): Boolean {
        Network.sendToServer(PacketNodeRemove(tile.pos, tile.uuid, node.id))
        return true
    }

    /**
     * This will send a packet to the server for each node that has moved.
     */
    private fun updateMoved() {
        graph ?: return
        tile.uuid ?: return
        graph!!.nodes.forEach {
            if (it.id != null && it.pos != null) {
                val startX = it.pos!!.first
                val startY = it.pos!!.second
                val stopX = NodeEditor.getNodePositionX(it.id!!.toLong())
                val stopY = NodeEditor.getNodePositionY(it.id!!.toLong())
                if (startX != stopX || startY != stopY)
                    Network.sendToServer(PacketNodeMove(tile.pos, tile.uuid, it.id!!, stopX, stopY))
            }
        }
    }

    /**
     * This will send the server the updated positions for the nodes
     */
    override fun onClose() {
        updateMoved()
    }

    /**
     * We don't want to pause the game on the screen on the client!
     */
    override fun isPauseScreen(): Boolean {
        return false
    }

    companion object {
        private var currentInstance = 0
    }

}