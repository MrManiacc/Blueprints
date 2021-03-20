package me.jraynor.api

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.extension.nodeditor.flag.NodeEditorPinKind
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.imgui.Widgets
import me.jraynor.util.getEnum
import me.jraynor.util.putEnum
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.util.INBTSerializable
import java.util.stream.Collectors

/**
 * This represents a connection that is stored within a node. It can be serialized allowing the ability to be sent
 * over the network.
 */
class Pin(
    /**This is import for connection seeking**/
    var nodeId: Int? = null,
    /**This should be unique. Technically the id should be set from the client, after the client had synchronized the
     * server's last id's. Then sent to server**/
    var id: Int? = null,
    /**This specifies the type of connection this is**/
    var io: IO = IO.INPUT,
    /**This is rendered on the client side**/
    var label: String = "Input",
    /**This keeps track of the other connections we're linked to**/
    var links: MutableList<Int> = ArrayList(),
    /**This keeps track of the other connections we're linked to**/
    var linkIds: MutableMap<Int, Int> = HashMap(),
    /**This is the primary color/background color for the pin. The default color is blueish**/
    var color: Int = ImColor.rgbToColor("#43617d"),
    /**This is the secondary color/inner color for the pin. The default color is redish**/
    var innerColor: Int = ImColor.rgbToColor("#db2e56"),
    /**If this is true we are going to push the icon to the right side of the node**/
    var textAfter: Boolean = false,
    /**If true [ImGui.sameLine()] wiil be called**/
    var sameLine: Boolean = false,
    /**If true [ImGui.sameLine()] wiil be called**/
    var indent: Float = -1f,
    /**This is the type of icon we're going to be rendering**/
    var icon: IconType = IconType.SQUARE,
    /**If this is true the pin with will be on a new line at the start.**/
    var newLine: Boolean = false,
    /**If this is true we compute the */
    var computeText: Boolean = false
) : INBTSerializable<CompoundNBT> {

    /**If there are links we want to be filled**/
    private val filled: Boolean get() = links.isEmpty()

    /**
     * I know that this calls [Graph#findbyInput] but the inputs are techinally the output for this pin hence
     * the method name outputs
     */
    fun outputs(graph: Graph): MutableList<Pin> {
        return links.stream().map { graph.findByInput(it) }.collect(Collectors.toList())!!
    }

    /**
     * This will render the pin
     */
    @OnlyIn(Dist.CLIENT) fun render() {
        if (id != null) {
            if (indent > 0f)
                ImGui.indent(indent)
            Widgets.icon(this, 24f, 24f, icon, filled, color, innerColor, label, textAfter, computeText)

            if (indent > 0f)
                ImGui.unindent(indent)
            if (sameLine)
                ImGui.sameLine()

        }
    }

    /**
     * This will write our connection to nbt
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = CompoundNBT()
        if (id != null)
            tag.putInt("id", id!!)
        tag.putEnum("io", io)
        tag.putString("label", label)
        tag.put("links", writeLinks())
        tag.putInt("color", color)
        tag.putInt("innerColor", innerColor)
        tag.putBoolean("pushIcon", textAfter)
        tag.putBoolean("sameLine", sameLine)
        tag.putFloat("indent", indent)
        tag.putEnum("icon", icon)
        tag.putBoolean("newLine", newLine)
        tag.putBoolean("computeText", computeText)
        return tag
    }

    /**
     * This will write the links to the given tag
     */
    private fun writeLinks(): CompoundNBT {
        val tag = CompoundNBT()
        tag.putInt("size", links.size)
        links.forEachIndexed { i, link ->
            tag.putInt("link_$i", link)
        }
        return tag
    }

    /**
     * This will deserialize the pin
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        if (tag.contains("id"))
            this.id = tag.getInt("id")
        this.io = tag.getEnum("io")
        this.label = tag.getString("label")
        readLinks(tag.getCompound("links"))
        this.color = tag.getInt("color")
        this.innerColor = tag.getInt("innerColor")
        this.textAfter = tag.getBoolean("pushIcon")
        this.sameLine = tag.getBoolean("sameLine")
        this.indent = tag.getFloat("indent")
        this.icon = tag.getEnum("icon")
        this.newLine = tag.getBoolean("newLine")
        this.computeText = tag.getBoolean("computeText")
    }

    /**
     * This will read the links from the tag
     */
    private fun readLinks(tag: CompoundNBT) {
        this.links.clear()
        val size = tag.getInt("size")
        for (i in 0 until size)
            this.links.add(tag.getInt("link_$i"))
    }

    /**
     * This will start the pinl
     */
    fun beginPin() {
        NodeEditor.beginPin(id!!.toLong(), if (io!! == IO.OUTPUT) NodeEditorPinKind.Output else NodeEditorPinKind.Input)
    }

    /**
     * This will end the drawing of the pin
     */
    fun endPin() {
        NodeEditor.endPin()
    }

    /**
     * This will add the other pin as a link to this pin but only if not already present.
     */
    fun addLink(other: Pin): Boolean {
        if (!links.contains(other.id) &&
            id != null &&
            this.io.opposite == other.io
        ) {
            return links.add(other.id!!)
        }
        return false
    }

    /**
     * This will get the port at the given index
     */
    operator fun get(index: Int): Int {
        return links[index]
    }

    /**
     * This checks to see if the other pin is one of this Pin's link's. If [checkBoth] is true it will check to see not
     * only if the other is one this pins links but also if the other has this pin as a link.
     */
    fun isLinked(other: Pin, checkBoth: Boolean = false): Boolean {
        val oId = other.id ?: return false
        if (links.contains(oId)) return true
        if (checkBoth) {
            id ?: return false
            return other.links.contains(id)
        }
        return false
    }

    /**
     * This will add the other pin as a link to this pin but only if not already present.
     */
    fun removeLink(other: Int): Boolean {
        return if (links.contains(other))
            links.remove(other)
        else false
    }

    /**
     * This will add the other pin as a link to this pin but only if not already present.
     */
    fun removeLink(other: Pin): Boolean {
        return if (links.contains(other.id))
            links.remove(other.id)
        else false
    }

}