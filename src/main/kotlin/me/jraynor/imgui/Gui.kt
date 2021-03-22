package me.jraynor.imgui

import imgui.ImGui
import imgui.flag.*
import imgui.type.ImInt
import net.minecraft.client.Minecraft
import imgui.internal.ImGui as ImGuiInternal
import imgui.internal.flag.ImGuiDockNodeFlags as ImGuiDockNodeInternalFlags
import imgui.extension.nodeditor.NodeEditor
import imgui.extension.nodeditor.NodeEditorContext
import me.jraynor.api.Graph
import imgui.type.ImLong

import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.util.*
import org.lwjgl.glfw.GLFW
import kotlin.reflect.KMutableProperty

/**
 * This is kind of a wrapper around imgui for ease of use with kotlin.
 */
object Gui {
    /**The ImGuiNodeEditor's context.**/
    private var nodeEditorContext = NodeEditorContext()

    /**The constant dockspace id for the main dockspace.**/
    private const val DOCKSPACE_ID = "main_dockspace"

    /**Dynamically gets the window long reference.**/
    private val handle: Long get() = Minecraft.getInstance().mainWindow.handle

    /**We only need to initialize once**/
    private var initialized = false

    /**This stores the glfw backend implementation for imgui**/
    private val imGuiGlfw = ImGuiImplGlfw()

    /**This stores the opengl backend implementation for imgui**/
    private val imGuiGl3 = ImGuiImplGl3()

    /**This is the links ending pin**/
    private val linkId: ImLong = ImLong()

    /**This is used as the link starting in**/
    private val outputBuffer: ImLong = ImLong()

    /**This is the links ending pin**/
    private val inputBuffer: ImLong = ImLong()

    /**
     * This will initialize the gui
     */

    fun init() {
        if (!initialized) {
            assert(logicalRender)
            initImGui()
            imGuiGlfw.init(handle, true);
            imGuiGl3.init();//Use default version of #130
            initialized = true
            println("Created the render context!")
        }
    }

    /**
     * This will initialize the imgui stuff
     */
    private fun initImGui() {
        ImGui.createContext();
        val io = ImGui.getIO();
        io.iniFilename = null
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable)
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable)
        io.configViewportsNoTaskBarIcon = true
    }

    /**
     * This will begin the imigui frame
     */
    private fun startFrame() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
    }

    /**
     * This is a magic kotlin wrapper for a frame start with an automatic end.
     */
    fun frame(frame: () -> Unit) {
        startFrame()
        frame()
        endFrame()
    }

    /**
     * This will end the imgui frame
     */
    private fun endFrame() {
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            val backupPtr = GLFW.glfwGetCurrentContext()
            ImGui.updatePlatformWindows()
            ImGui.renderPlatformWindowsDefault()
            GLFW.glfwMakeContextCurrent(backupPtr)
        }
    }

    /**
     * This will create the fullscreen dock-space window.
     */
    fun dockspace(name: String, nodes: () -> Unit, editor: () -> Unit) {
        val flags = ImGuiWindowFlags.NoNavFocus.orEquals(
            ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoCollapse,
            ImGuiWindowFlags.NoResize,
            ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.NoBringToFrontOnFocus
        )
        val window = Minecraft.getInstance().mainWindow
        val viewport = ImGui.getMainViewport()
        ImGui.setNextWindowPos(window.windowX.toFloat(), window.windowY.toFloat())
        ImGui.setNextWindowSize(window.framebufferWidth.toFloat(), window.framebufferHeight.toFloat())
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.begin("Window##$name", flags)
        ImGui.setNextWindowViewport(viewport.id)
        ImGui.popStyleVar()

        var dockspaceID = ImGui.getID(DOCKSPACE_ID)
        val node = ImGuiInternal.dockBuilderGetNode(dockspaceID)
        if (node == null || node.ptr == 0L || node.id == 0) //Null ptr? it we should now create?
            createDock(name)
        dockspaceID = ImGui.getID(DOCKSPACE_ID)
        ImGui.dockSpace(dockspaceID, 0f, 0f, ImGuiDockNodeFlags.None)
        ImGui.end()
        ImGui.begin("Editor##$name", ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoBringToFrontOnFocus)
        editor()
        ImGui.end()
        ImGui.begin("Nodes##$name", ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoBringToFrontOnFocus)
        nodes()
        ImGui.end()
    }

    /**
     * This internally creates the dock when it's not present.
     */
    private fun createDock(name: String) {
        val viewport = ImGui.getWindowViewport()
        val dockspaceID = ImGui.getID(DOCKSPACE_ID)
        ImGuiInternal.dockBuilderRemoveNode(dockspaceID)
        ImGuiInternal.dockBuilderAddNode(dockspaceID, ImGuiDockNodeInternalFlags.DockSpace)
        ImGuiInternal.dockBuilderSetNodeSize(dockspaceID, viewport.sizeX, viewport.sizeY)
        val dockMainId = ImInt(dockspaceID)
        val dockLeft: Int =
            ImGuiInternal.dockBuilderSplitNode(dockMainId.get(), ImGuiDir.Right, 0.3f, null, dockMainId)
        ImGuiInternal.dockBuilderDockWindow("Editor##$name", dockMainId.get())
        ImGuiInternal.dockBuilderDockWindow("Nodes##$name", dockLeft)
        ImGuiInternal.dockBuilderFinish(dockspaceID)
    }

    /**
     * This will render a help tooltip
     */
    fun helpMarker(description: String) {
        ImGui.textDisabled("(?)");
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip()
            ImGui.pushTextWrapPos(ImGui.getFontSize() * 35f)
            ImGui.textUnformatted(description)
            ImGui.popTextWrapPos()
            ImGui.endTooltip()
        }
    }

    /**
     * This will do the node editor equivalent fo the imnode
     */
    private fun nodeEditor(editor: () -> Unit) {
        NodeEditor.setCurrentEditor(nodeEditorContext);
        NodeEditor.begin("Node Editor")
        editor()
        NodeEditor.end()
    }

    /**
     * This will create the node editor and give us a callback
     */
    private fun renderLinkContext(graph: Graph, onLink: OnNodeLink) {
        if (NodeEditor.beginCreate()) {
            val a = ImLong()
            val b = ImLong()
            if (NodeEditor.queryNewLink(a, b)) {
                val source: Pin? = graph.findByOutput(a.get().toInt())
                val target: Pin? = graph.findByInput(b.get().toInt())
                if (source != null && target != null && source.id !== target.id && NodeEditor.acceptNewItem()) {
                    source.addLink(target)
                    onLink(source, target)
                }
            }
        }
        NodeEditor.endCreate()
    }

    /**
     * This will render each of the buttons.
     */
    private fun renderAddContext(graph: Graph, onAddNode: OnAddNode, vararg namedUnits: AddNode) {
        if (ImGui.beginPopup("node_editor_context")) {
            for (unit in namedUnits) {
                val name = unit.first
                if (ImGui.button(name)) {
                    val node = unit.second()
                    graph.add(node)
                    val canvasX = NodeEditor.toCanvasX(ImGui.getMousePosX())
                    val canvasY = NodeEditor.toCanvasY(ImGui.getMousePosY())
                    node.pos = Pair(canvasX, canvasY)
                    NodeEditor.setNodePosition(node.id!!.toLong(), canvasX, canvasY)
                    onAddNode(node)
                    ImGui.closeCurrentPopup()
                }
            }
            ImGui.endPopup()
        }
    }

    /**
     * This will start the node editor rendering
     */
    private fun renderStartNodeEditor() {
        /**This creates the context menu and registers the delete node id**/
        val nodeWithContextMenu = NodeEditor.getNodeWithContextMenu()
        if (nodeWithContextMenu != -1L) {
            ImGui.openPopup("node_context")
            ImGui.getStateStorage().setInt(ImGui.getID("delete_node_id"), nodeWithContextMenu.toInt())
        }
    }

    /**
     * Renders the background context
     */
    private fun renderBackgroundContext(firstRun: KMutableProperty<Boolean>) {
        /**This displays our context editor in imgui when requested from the background context**/
        if (!firstRun.getter.call()) {
            if (NodeEditor.showBackgroundContextMenu()) {
                ImGui.openPopup("node_editor_context")
            }
        }
    }

    /**
     * This will render the delete context
     */
    private fun processRemovals(graph: Graph, onDelete: OnDeleteNode) {
        /**This will open a popup for deleting the currently selected node. there is a callback in the node graph**/
        if (ImGui.isPopupOpen("node_context")) {
            val targetNode = ImGui.getStateStorage().getInt(ImGui.getID("delete_node_id"))
            if (ImGui.beginPopup("node_context")) {
                if (ImGui.button("Delete node")) {
                    val toRemove = graph.findById(targetNode)
                    if (toRemove != null) {
                        NodeEditor.deleteNode(toRemove.id!!.toLong())
                        graph.remove(toRemove)
                        onDelete(toRemove)
                    }
                    ImGui.closeCurrentPopup()
                }
                ImGui.endPopup()
            }
        }
    }

    /**
     * This will render the links of the graph
     */
    private fun processLinks(graph: Graph) {
        var uniqueLinkId = 500_000L
        graph.nodes.forEach { node ->
            node.pins.forEach { port ->
                if (port.id != null)
                    port.links.forEachIndexed { index, otherPinId ->
                        if (otherPinId != -1) {
                            port.linkIds[otherPinId] = (++uniqueLinkId).toInt()
                            NodeEditor.link(uniqueLinkId, port.id!!.toLong(), otherPinId.toLong())
                        }
                    }
            }
        }
    }

    /**
     * This is the logic for when a link is removed
     */
    private fun processLinkRemoval(
        graph: Graph,
        deleteLink: OnDeleteLink
    ) {
        if (NodeEditor.beginDelete()) {
            if (NodeEditor.queryDeletedLink(linkId, outputBuffer, inputBuffer)) {
                if (NodeEditor.acceptDeletedItem()) {
                    val output = graph.findPinById(outputBuffer.get().toInt()) ?: return
                    val input = graph.findPinById(inputBuffer.get().toInt()) ?: return
                    if (output.linkIds.containsKey(input.id)) {
                        val nodeOutput = graph.findNodeByPinId(output.id!!) ?: return
                        val link = output.linkIds[input.id]
                        if (link == linkId.get().toInt()) {
                            output.links.remove(input.id)
                            deleteLink(nodeOutput)
                        }
                    }
                }
            }
            NodeEditor.endDelete()
        }
    }

    /**
     * This will render a node graph
     */
    fun renderNodeGraph(
        updateCallback: (Node) -> Unit,
        blockCallback: (Node) -> Unit,
        firstRun: KMutableProperty<Boolean>,
        onDeleteLink: OnDeleteLink,
        onLink: OnNodeLink,
        onDelete: OnDeleteNode,
        onAdd: OnAddNode,
        graph: Graph,
        vararg addNodes: AddNode
    ) {
        nodeEditor {
            /**First we want to render all of the nodes**/
            for (node in graph.nodes) {
                if (node.updateCallback == null)
                    node.updateCallback = updateCallback
                if (node.blockCallback == null)
                    node.blockCallback = blockCallback
                if (!node.initialized && node.id != null && node.pos != null) {
                    NodeEditor.setNodePosition(node.id!!.toLong(), node.pos!!.first, node.pos!!.second)
                    node.initialized = true
                }
                if (node.initialized)
                    node.render()
            }
            /**Then we do our callbacks for the editor creation**/
            renderLinkContext(graph, onLink)
            /**Then we render our links**/
            processLinks(graph)
            /**This will do the logical code for removing a link**/
            processLinkRemoval(graph, onDeleteLink)
            /**This suspends our**/
            NodeEditor.suspend()
            /**This start the node editor context**/
            renderStartNodeEditor()

            /**This will render the node editor context and the add nodes**/
            renderBackgroundContext(firstRun)
            /**This renders the add context menu**/
            renderAddContext(graph, onAdd, *addNodes)
            /**This renders the delete context**/
            processRemovals(graph, onDelete)
            /**Resumes the current context**/
            NodeEditor.resume()
            if (firstRun.getter.call()) {
                firstRun.setter.call(false)
                NodeEditor.navigateToContent(0.0f)
            }
        }
    }

    /**
     * This will delete all the stuff
     */
    fun destroy() {
        if (initialized) {
            assert(logicalRender)
            nodeEditorContext.destroy()
            this.nodeEditorContext = NodeEditorContext()
            println("Destroyed node editor context!")
        }
    }


}

