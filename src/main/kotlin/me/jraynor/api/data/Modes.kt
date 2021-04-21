package me.jraynor.api.data

import imgui.ImGui
import imgui.type.ImBoolean
import imgui.type.ImInt
import me.jraynor.api.Node
import me.jraynor.api.enums.Mode
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.common.util.INBTSerializable
import java.util.*

/**
 * This inner class keeps track of the modes
 */
class Modes(
    private val modeStates: MutableMap<Mode, Boolean> = EnumMap(Mode::class.java),
    private val modeSpeeds: MutableMap<Mode, Int> = EnumMap(Mode::class.java),
    internal var node: Node? = null,
    private val activeBuffer: ImBoolean = ImBoolean(false),
    private val speedBuffer: ImInt = ImInt(1),
    private val uuid: UUID = UUID.randomUUID()
) :
    INBTSerializable<CompoundNBT> {

    init {
        Mode.values().forEachIndexed { i, mode ->
            modeStates[mode] = false
            modeSpeeds[mode] = mode.initial
        }
    }

    /**
     * Returns true if active.
     */
    fun forEach(modes: (Mode, Int) -> Unit) {
        Mode.values().forEach { mode ->
            if (isActive(mode))
                modes(mode, getSpeed(mode))
        }

    }

    /**
     * Returns true if active.
     */
    fun isActive(mode: Mode): Boolean {
        return modeStates[mode]!!
    }

    /**
     * This will set the mode to active or inative
     */
    fun setActive(mode: Mode, active: Boolean) {
        modeStates[mode] = active
    }

    /**
     * Returns the currnet speed for the given mode
     */
    fun getSpeed(mode: Mode): Int {
        return modeSpeeds[mode]!!
    }

    /**
     * This will set the speed for the given node
     */
    fun setSpeed(mode: Mode, speedIn: Int) {
        var speed = speedIn
        if (speed < mode.min)
            speed = mode.min
        if (speed > mode.max)
            speed = mode.max
        modeSpeeds[mode] = speed
    }

    /**
     * This will serialize the modes
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = CompoundNBT()
        Mode.values().forEach {
            tag.putBoolean("${it.name}_active", isActive(it))
            tag.putInt("${it.name}_speed", getSpeed(it))
        }
        return tag
    }

    /**
     * This will deserialize the modes
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        Mode.values().forEach {
            setActive(it, tag.getBoolean("${it.name}_active"))
            setSpeed(it, tag.getInt("${it.name}_speed"))
        }
    }

    /**
     * This will render the checkbox
     */
    internal fun render(update: () -> Unit) {
        ImGui.text("Extraction: ")
        ImGui.separator()
        ImGui.indent()
        node ?: return
        ImGui.spacing()
        Mode.values().forEachIndexed { index, mode ->
            val id = "${index}_$uuid"
            this.activeBuffer.set(isActive(mode))

            if (ImGui.checkbox("extract ${mode.name.toLowerCase()}##$id", this.activeBuffer)) {
                setActive(mode, this.activeBuffer.get())
                update()
            }
        }
        Mode.values().forEachIndexed { index, mode ->
            val id = "${index}_$uuid"
            if (isActive(mode)) {
                ImGui.setNextItemWidth(120f)
                speedBuffer.set(getSpeed(mode))
                if (ImGui.inputInt(
                        "${mode.label}##speed_$id",
                        this.speedBuffer,
                        mode.step,
                        mode.speedStep
                    )
                ) {
                    setSpeed(mode, speedBuffer.get())
                    update()
                }
            }
        }
        ImGui.unindent()
    }

}