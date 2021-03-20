package me.jraynor.api.data

import imgui.ImColor
import imgui.ImGui
import me.jraynor.api.enums.Mode
import me.jraynor.util.InventoryUtils
import net.minecraft.item.Items
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.items.ItemStackHandler
import java.util.*

/**
 * This class keeps track of serializable internal buffer data
 */
class Buffers(
    /**This keeps track of our different handlers for the map. We will have each of them even if they're not in use.**/
    private val handlers: MutableMap<Mode, Handler> = EnumMap(Mode::class.java)
) : INBTSerializable<CompoundNBT> {

    init {
        if (!handlers.containsKey(Mode.ITEM))
            handlers[Mode.ITEM] = ItemHandlerBuffer()
        if (!handlers.containsKey(Mode.ENERGY))
            handlers[Mode.ENERGY] = EnergyHandlerBuffer()
    }

    /**
     * This will display the buffer data
     */
    fun render() {
        handlers.values.forEach(Handler::render)
    }

    /***
     * This will write each of our handlers to file
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = CompoundNBT()
        tag.put("item_handler", handlers[Mode.ITEM]!!.serializeNBT())
        tag.put("energy_handler", handlers[Mode.ENERGY]!!.serializeNBT())
        return tag
    }

    /**
     * This will attempt to get the handler for the give type.
     */
    operator fun get(mode: Mode): LazyOptional<*> {
        return LazyOptional.of { handlers[mode]!! }
    }

    /**
     * This will attempt to get the handler for the give type.
     */
    operator fun <T : Any> get(type: Class<T>): LazyOptional<T> {
        for (handler in handlers.values)
            if (type.isAssignableFrom(handler.javaClass))
                return LazyOptional.of { type.cast(handler) }
        return LazyOptional.empty()
    }

    /**
     * This will read our handlers
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        handlers[Mode.ITEM]?.deserializeNBT(tag.getCompound("item_handler"))
        handlers[Mode.ENERGY]?.deserializeNBT(tag.getCompound("energy_handler"))
    }

    /**
     * This represents a renewable handler
     */
    interface Handler : INBTSerializable<CompoundNBT> {
        /**
         * Used to display the data in the ndoe
         */
        fun render()
    }

    /**Max buffer slots = max int**/
    private class ItemHandlerBuffer(
        size: Int = MAX_BUFFER_SIZE
    ) : ItemStackHandler(size), Handler {
        /**
         * This will display our item renderer
         */
        override fun render() {
            if (ImGui.beginListBox("inventory")) {
                InventoryUtils.forEachItem(this) lit@{ _, _, item, count ->
                    if (item == Items.AIR) return@lit
                    ImGui.text("${item.name.string}: $count")
                    ImGui.separator()
                }
                ImGui.endListBox()
            }

        }
    }

    /**Max buffer slots = max int**/
    private class EnergyHandlerBuffer(
        size: Int = MAX_BUFFER_SIZE
    ) : EnergyStorage(size), Handler {

        /**
         * This will write our energy handler to file
         */
        override fun serializeNBT(): CompoundNBT {
            val tag = CompoundNBT()
            tag.putInt("energy", this.energy)
            tag.putInt("capacity", this.capacity)
            return tag
        }

        /**
         * This will read current energy from the tag
         */
        override fun deserializeNBT(tag: CompoundNBT) {
            this.energy = tag.getInt("energy")
            this.capacity = tag.getInt("capacity")
            this.maxExtract = capacity
            this.maxReceive = capacity
        }

        /**
         * Used to display the data in the node
         */
        override fun render() {
            ImGui.textColored(ITEMS_COLOR, "energy: ${"%,d".format(energy)} rf")
            ImGui.textColored(ENERGY_COLOR, "energy capacity: ${"%,d".format(capacity)} rf")
        }
    }

    companion object {
        /**This is the maximum value to set the item slot handler to**/
        const val MAX_BUFFER_SIZE = 1_000_000 //Default value is 1 million

        /**The current color of the energy output**/
        val ENERGY_COLOR: Int = ImColor.rgbToColor("#6293e3")
        val ITEMS_COLOR: Int = ImColor.rgbToColor("#e362d0")
    }

}