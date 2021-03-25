package me.jraynor.api.nodes

import imgui.ImColor
import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.api.logic.IFilter
import me.jraynor.api.logic.Return
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.items.IItemHandler
import java.awt.TextComponent

/**
 * This is a block node, it will be
 */
class FilterNode(
    private var whitelist: ImBoolean = ImBoolean(true),
    private val regex: ImString = ImString(),
    private val regexList: MutableList<String> = ArrayList(),
    private val selectedList: MutableMap<String, ImBoolean> = HashMap()
) : Node() {
    private var selectedRegex = String()//This keeps track of our selected regex

    /**
     * This is the handler for the item filter
     */
    val itemFilter: IFilter<ItemStack, IItemHandler> = IFilter { test, _, _ ->
        val name = test.textComponent.string.toLowerCase()
        if (test.isEmpty)
            return@IFilter Return.of(false)
        else {
            var hasMatch = false
            for (regex in regexList) {
                if (regex.toLowerCase().toRegex().containsMatchIn(name)) {
                    hasMatch = true
                    break
                }
            }
            if (whitelist.get() && hasMatch)
                return@IFilter Return.of(true)
            else if (!whitelist.get() && !hasMatch)
                return@IFilter Return.of(true)
        }
        Return.of(false)
    }

    /**
     * This is the handler for the item filter
     */
    val genericFilter: IFilter<ITextComponent, Any> = IFilter { test, _, _ ->
        var hasMatch = false
        for (regex in regexList) {
            if (regex.toRegex().containsMatchIn(test.string)) {
                hasMatch = true
                break
            }
        }
        if (whitelist.get() && hasMatch)
            return@IFilter Return.of(true)
        else if (!whitelist.get() && !hasMatch)
            return@IFilter Return.of(true)
        Return.of(false)
    }

    /**
     * This will add all of the pins for the filter node.
     */
    override fun addPins() {
        super.addPins()
        add(
            Pin(
                io = IO.INPUT,
                label = "DoFilter",
                textAfter = true,
                computeText = false,
                icon = IconType.GRID,
                innerColor = ImColor.rgbToColor("#fafafa")
            )
        )
    }

    /**
     * This will write the regex filter
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putInt("regex_size", regexList.size)
        tag.putBoolean("whitelist", whitelist.get())
        for (i in 0 until regexList.size) tag.putString("regex_$i", regexList[i])
        return tag
    }

    /**
     * This will deserialize the regex list
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        regexList.clear()
        super.deserializeNBT(tag)
        this.whitelist.set(tag.getBoolean("whitelist"))
        val size = tag.getInt("regex_size")
        for (i in 0 until size) regexList.add(tag.getString("regex_$i"))
    }

    /**
     * This should only be called from the client.
     * This code should not exist on the server.
     * (or at least should be called)
     */
    override fun render() {
        ImGui.dummy(1f, 1f)
        id ?: return
        NodeEditor.beginNode(id!!.toLong())
        super.render()
        ImGui.textColored(ImColor.rgbToColor("#FF6F61"), "Filter node")
        ImGui.dummy(100f, 10f)
        ImGui.spacing()
        renderPorts()
        NodeEditor.endNode()
    }

    /**
     * This is used as the code that will render on the side view
     */
    override fun renderEx() {
        ImGui.setNextItemOpen(true, ImGuiCond.FirstUseEver)
        if (ImGui.collapsingHeader("Filter Node")) {
            super.renderEx()
            ImGui.setNextItemWidth(120f)
            if (ImGui.inputTextWithHint("##input_regex_$id", "regex", regex, ImGuiInputTextFlags.EnterReturnsTrue)) {
                val newRegex = this.regex.get()!!
                this.regexList.add(newRegex)
                regex.set("")
                pushClientUpdate()
                ImGui.setKeyboardFocusHere(-1)
            }
            ImGui.sameLine()
            ImGui.text("enter to add")
            if (ImGui.beginListBox("##regex_listbox")) {
                regexList.forEach { regex ->
                    if (!selectedList.containsKey(regex))
                        selectedList[regex] = ImBoolean(false)
                    if (ImGui.selectable(regex, selectedList[regex])) //This checks to regex to see if it's selected
                        selectedRegex = regex
                }
                ImGui.endListBox()
            }
            if (ImGui.button("clear all##$id")) {
                regexList.clear()
                pushClientUpdate()
            }
            ImGui.sameLine()
            if (ImGui.button("remove selected##$id"))
                if (selectedRegex.isNotBlank())
                    if (this.regexList.remove(selectedRegex))
                        pushClientUpdate()

            if (ImGui.button(if (this.whitelist.get()) "whitelist" else "blacklist")) {
                whitelist.set(!whitelist.get())
                pushClientUpdate()
            }
        }


    }


}