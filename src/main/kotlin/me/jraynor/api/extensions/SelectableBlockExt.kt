package me.jraynor.api.extensions

import imgui.ImGui
import imgui.type.ImBoolean
import imgui.type.ImInt
import me.jraynor.api.Node
import me.jraynor.api.select.PlayerHooks
import me.jraynor.util.*
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos

/**
 * This type of node is used for automation tasks that require a fake player.
 */
interface SelectableBlockExt : INodeExtension {
    /**This keeps track of the current selected block pos**/
    var selectedBlock: BlockPos?

    /**This is the currently selected direction**/
    var selectedFace: Direction?

    /**This is used to display the current location for a linked block**/
    var shown: ImBoolean

    /**Keeps track of the current show color**/
    var showColor: FloatArray

    /**
     * This hooks our methods needed for a fake player .
     */
    override fun hook(
        nodeRenders: MutableList<Callback>,
        propertyRenders: MutableList<Callback>,
        pinAdds: MutableList<Callback>,
        tickCalls: MutableList<TickCallback>,
        readCalls: MutableList<NBTCallback>,
        writeCalls: MutableList<NBTCallback>
    ) {
        readCalls.add(this::readSelectedBlocks)
        writeCalls.add(this::writeSelectedBlocks)
        nodeRenders.add(this::renderSelectedBlockNode)
        propertyRenders.add(this::renderSelectedBlockProperty)
    }

    /**
     * This will read the player info for the given ndoe
     */
    fun readSelectedBlocks(tag: CompoundNBT, node: Node) {
        this.selectedBlock = tag.getBlockPos("selected_block_pos")
        this.selectedFace = tag.getEnum("selected_block_face")
        this.shown.set(tag.getBoolean("shown"))
        this.showColor = tag.getFloatArray("show_color")

    }

    /**
     * This will read the player info for the given ndoe
     */
    fun writeSelectedBlocks(tag: CompoundNBT, node: Node) {
        if (selectedBlock != null)
            tag.putBlockPos("selected_block_pos", selectedBlock!!)
        if (selectedFace != null)
            tag.putEnum("selected_block_face", selectedFace!!)
        tag.putBoolean("shown", shown.get())
        tag.putFloatArray("show_color", this.showColor)

    }

    /**
     * This will render the debug info for the fake player extension
     */
    fun renderSelectedBlockNode(node: Node) {
        node.id ?: return
        if (ImGui.button("select##${node.id}"))
            node.pushFaceSelect()
        if (selectedBlock != null && selectedFace != null) {
            ImGui.text(node.getBlockName(selectedBlock!!))
            ImGui.textDisabled("[${this.selectedBlock!!.coords}], [${selectedFace!!.name.toLowerCase()}]")
        }
    }

    /**
     * This will render the debug info for the fake player extension
     */
    fun renderSelectedBlockProperty(node: Node) {
        ImGui.text("Selection: ")
        ImGui.separator()
        ImGui.indent()
        if (selectedBlock != null)
            ImGui.text("Name: ${node.getBlockName(this.selectedBlock!!)}")
        if (ImGui.button("select##${node.id}"))
            node.pushFaceSelect()
        if (selectedBlock != null && selectedFace != null) {
            ImGui.sameLine()
            val array = this.selectedBlock!!.toArray()
            ImGui.pushItemWidth(120f)
            if (ImGui.inputInt3("Block Pos##${node.id}", array)) {
                val key = Pair(this.selectedBlock!!, this.selectedFace!!)
                this.selectedBlock = array.toBlockPos()
                if (PlayerHooks.showFaces.containsKey(key)) {
                    val color = PlayerHooks.showFaces[key]
                    PlayerHooks.showFaces.remove(key)
                    val newKey = Pair(this.selectedBlock!!, this.selectedFace!!)
                    PlayerHooks.showFaces[newKey] = color!!
                }
                node.pushClientUpdate()
            }
            ImGui.pushItemWidth(120f)
            if (ImGui.beginCombo("Face##${node.id}", this.selectedFace!!.name.toLowerCase())) {
                for (dir in Direction.values()) {
                    if (ImGui.selectable(dir.name.toLowerCase() + "##${node.id}")) {
                        val key = Pair(this.selectedBlock!!, this.selectedFace!!)
                        this.selectedFace = dir
                        this.selectedBlock = array.toBlockPos()
                        if (PlayerHooks.showFaces.containsKey(key)) {
                            val color = PlayerHooks.showFaces[key]
                            PlayerHooks.showFaces.remove(key)
                            val newKey = Pair(this.selectedBlock!!, this.selectedFace!!)
                            PlayerHooks.showFaces[newKey] = color!!
                        }
                        node.pushClientUpdate()
                    }
                }
                ImGui.endCombo()
            }
        }

        if (this.selectedBlock != null && this.selectedFace != null) {
            val key = Pair(this.selectedBlock!!, this.selectedFace!!)
            if (ImGui.checkbox("highlight##${node.id}", shown)) {
                if (shown.get())
                    PlayerHooks.showFaces[key] = this.showColor
                else
                    PlayerHooks.showFaces.remove(key)
                node.pushClientUpdate()
            }
            if (shown.get())
                if (ImGui.colorEdit4("face color", this.showColor)) {
                    PlayerHooks.showFaces[key] = this.showColor
                    node.pushClientUpdate()
                }
        }
        ImGui.unindent()
    }

}