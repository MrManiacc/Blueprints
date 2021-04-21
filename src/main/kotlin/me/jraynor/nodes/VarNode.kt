package me.jraynor.nodes

import imgui.*
import imgui.type.*
import me.jraynor.api.impl.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.nbt.*
import net.minecraft.util.*
import net.minecraft.util.math.*
import net.minecraft.util.math.vector.*
import net.minecraftforge.common.util.*
import java.lang.Float.*

/**This node is the base node for all tickable nodes.**/
class VarNode : ContentNode(
    title = "Variable",
    headerColor = "#eb346e"
) {
    /**Keeps track of all of our variable values**/
    internal val vars: Vars = Vars(type = Vars.Type.Bool)

    /**Used to display the selecting type popup**/
    private var selectingType = false

    /**Used to dispaly the selceting face popup**/
    private var selectingFace = false

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 5f

    /**We store a hard reference to the do tick pin**/
    init {
        addPin(
            Pin(
                inputOutput = InputOutput.Output,
                label = "",
                icon = IconType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#169873")
            )
        )
    }

    /**This should render anything that will be put inside the header**/
    override fun renderHeaderContent() {
        if (ImGui.button("type##${this.nodeId}")) {
            println("Start type select")
            ImGui.closeCurrentPopup()
            selectingType = true
            selectingFace = false
        }
    }

    /**This is used as a way to add code to the suspend block in a node**/
    override fun postProcess() {
        ImGui.closeCurrentPopup()
        if (selectingType) ImGui.openPopup("var_type_select##${nodeId}")
        if (ImGui.beginPopup("var_type_select##${nodeId}")) {
            Vars.Type.values().forEach {
                if (ImGui.menuItem("${it.name}##${nodeId}")) {
                    this.vars.type = it
                    this.selectingType = false
                    ImGui.closeCurrentPopup()
                    pushUpdate()
                }
            }
            ImGui.endPopup()
        }
        if (selectingFace) ImGui.openPopup("var_face_select##${nodeId}")
        if (ImGui.beginPopup("var_face_select##${nodeId}")) {
            Direction.values().forEach {
                if (ImGui.menuItem("${it.name2.capitalize()}##$nodeId")) {
                    this.vars.face = it
                    this.selectingFace = false
                    ImGui.closeCurrentPopup()
                    pushUpdate()
                }
            }
            ImGui.endPopup()
        }

    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        with(vars) {
            val name = "${type.name}##$nodeId"
            when (type) {
                Vars.Type.Bool -> {
                    ImGui.pushItemWidth(60f)
                    if (ImGui.checkbox(name, this.imBoolean))
                        pushUpdate()
                }
                Vars.Type.Int -> {
                    ImGui.pushItemWidth(100f)
                    if (ImGui.inputInt(name, this.imInt))
                        pushUpdate()
                }
                Vars.Type.Float -> {
                    ImGui.pushItemWidth(85f)
                    if (ImGui.inputFloat(name, this.imFloat))
                        pushUpdate()
                }
                Vars.Type.Vec2 -> {
                    ImGui.pushItemWidth(120f)
                    if (ImGui.inputFloat2(name, this.imVec2))
                        pushUpdate()
                }
                Vars.Type.Vec3 -> {
                    ImGui.pushItemWidth(120f)
                    if (ImGui.inputFloat3(name, this.imVec3))
                        pushUpdate()
                }
                Vars.Type.BlockPos -> {
                    ImGui.pushItemWidth(120f)
                    if (ImGui.inputInt3(name, this.imBlockPos))
                        pushUpdate()
                }
                Vars.Type.Face -> {
                    if (ImGui.button("${vars.face.name.toLowerCase()}##$nodeId")) {
                        selectingFace = true
                        selectingType = false
                        ImGui.closeCurrentPopup()
                    }
                    ImGui.sameLine()
                    ImGui.text("face")
                }
            }
            return max(contentWidth, ImGui.getItemRectMaxX())
        }
    }

    /**Writes the variable to file**/
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.put("var_data", vars.serializeNBT())
        return tag
    }

    /**Reads our data like the nodeId, and graphX/graphY and pins.**/
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.vars.deserializeNBT(tag.getCompound("var_data"))
    }

    /**This is used to store the variable types**/
    internal class Vars(
        internal val imBoolean: ImBoolean = ImBoolean(false),
        internal val imInt: ImInt = ImInt(0),
        internal val imFloat: ImFloat = ImFloat(0f),
        internal val imVec2: FloatArray = floatArrayOf(0f, 0f),
        internal val imVec3: FloatArray = floatArrayOf(0f, 0f, 0f),
        internal val imBlockPos: IntArray = intArrayOf(0, 0, 0),
        internal var type: Type
    ) : INBTSerializable<CompoundNBT> {
        /**Gets the value of the boolean**/
        var bool: Boolean = imBoolean.get()
            get() = imBoolean.get()
            set(value) {
                field = value.apply { imBoolean.set(this) }
            }

        /**Gets the value of the int**/
        var int: Int = imInt.get()
            get() = imInt.get()
            set(value) {
                field = value.apply { imInt.set(this) }
            }

        /**Gets the value of the float**/
        var float: Float = imFloat.get()
            get() = imFloat.get()
            set(value) {
                field = value.apply { imFloat.set(this) }
            }

        /**Gets the value of the vec2**/
        var vec2: Vector2f = Vector2f(this.imVec2[0], this.imVec2[1])
            get() = Vector2f(this.imVec2[0], this.imVec2[1])
            set(value) {
                field = value.apply { imVec2[0] = this.x; imVec2[1] = this.y }
            }

        /**Gets the value of the vec2**/
        var vec3: Vector3f = Vector3f(this.imVec3[0], this.imVec3[1], this.imVec3[2])
            get() = Vector3f(this.imVec3[0], this.imVec3[1], this.imVec3[2])
            set(value) {
                value.let { imVec3[0] = it.x; imVec3[1] = it.y; imVec3[2] = it.z }
                field = value
            }

        /**Gets the value of the vec2**/
        var blockPos: BlockPos = BlockPos(this.imBlockPos[0], this.imBlockPos[1], this.imBlockPos[2])
            get() = BlockPos(this.imBlockPos[0], this.imBlockPos[1], this.imBlockPos[2])
            set(value) {
                value.let { imBlockPos[0] = it.x; imBlockPos[1] = it.y; imBlockPos[2] = it.z }
                field = value
            }

        /**Keeps track of the current face**/
        var face: Direction = Direction.NORTH

        /**Used to keep track of the current variable type**/
        internal enum class Type {
            Bool, Int, Float, Vec2, Vec3, BlockPos, Face
        }

        /**Writes all of our variable to a compound**/
        override fun serializeNBT(): CompoundNBT {
            val tag = CompoundNBT()
            tag.putEnum("var_type", type)
            tag.putBoolean("var_bool", this.bool)
            tag.putInt("var_int", this.int)
            tag.putFloat("var_float", this.float)
            tag.putVec2("var_vec2", this.vec2)
            tag.putVec3("var_vec3", this.vec3)
            tag.putBlockPos("var_blockpos", this.blockPos)
            tag.putEnum("var_face", this.face)
            return tag
        }

        /**Read all of our variables from the compound**/
        override fun deserializeNBT(tag: CompoundNBT) {
            this.type = tag.getEnum("var_type")
            this.bool = tag.getBoolean("var_bool")
            this.int = tag.getInt("var_int")
            this.float = tag.getFloat("var_float")
            this.vec2 = tag.getVec2("var_vec2")
            this.vec3 = tag.getVec3("var_vec3")
            this.blockPos = tag.getBlockPos("var_blockpos")
            this.face = tag.getEnum("var_face")
        }
    }


}

