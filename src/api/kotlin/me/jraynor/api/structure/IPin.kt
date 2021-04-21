package me.jraynor.api.structure

import me.jraynor.api.utilities.enums.*
import me.jraynor.api.utilities.*
import net.minecraft.nbt.*
import net.minecraftforge.common.util.*

/**
 * This represents a connection that is stored within a node. It can be serialized allowing the ability to be sent
 * over the network.
 */
interface IPin : INBTSerializable<CompoundNBT> {
    /**We can allow our self to keep a reference to the parent node**/
    var nodeId: Int

    /**This should be unique. Technically the id should be set from the client, after the client had synchronized the
     * server's last id's. Then sent to server**/
    var pinId: Int

    /**This is rendered on the client side. It is also used as key for the pin to be indexed upon inside the [INode]**/
    var label: String

    /**The type of icon this pin will render**/
    var icon: IconType

    /**The color of the pin label**/
    var labelColor: Int

    /**The base color of the pin**/
    var baseColor: Int

    /**The inner color of the pin**/
    var innerColor: Int

    /**The type of input/output this pin is**/
    var inputOutput: InputOutput

    /**This keeps track of the pins we're currently linked to.
     * TODO: cache**/
    val links: MutableList<IPin>

    /**This will add a link to the [other] IPin.
     * Returns the index of the link **/
    fun addLink(other: IPin): Int = if (this.links.add(other)) this.links.size - 1 else -1

    /**This should get the link id for the given pin**/
    fun getLinkId(other: IPin): Int

    /**True if the other pin is removed from this**/
    fun removeLink(other: IPin): Boolean = this.links.remove(other)

    /**True if we're linked to the given link**/
    fun hasLink(other: IPin): Boolean = this.links.contains(other)

    /**True if the IPin is empty**/
    fun isEmpty(): Boolean = false

    /**This will write out all  of our pin data**/
    override fun serializeNBT(): CompoundNBT = with(CompoundNBT()) {
        putInt("nodeId", nodeId)
        putInt("pinId", pinId)
        putString("label", label)
        putEnum("icon", icon)
        putInt("labelColor", labelColor)
        putInt("innerColor", innerColor)
        putInt("baseColor", baseColor)
        putDeepList("links", links)
        putEnum("io", inputOutput)
        return this
    }

    /**reads our link data and other stuff**/
    override fun deserializeNBT(tag: CompoundNBT) {
        nodeId = tag.getInt("nodeId")
        pinId = tag.getInt("pinId")
        label = tag.getString("label")
        icon = tag.getEnum("icon")
        inputOutput = tag.getEnum("io")
        labelColor = tag.getInt("labelColor")
        innerColor = tag.getInt("innerColor")
        baseColor = tag.getInt("baseColor")
        with(this.links) {
            clear()
            tag.getDeepList<IPin>("links").forEach {
                addLink(it)
            }
        }
    }

    companion object {
        /**Allows for null safety. We can always check if the [IPin] is empty using this**/
        val Empty: IPin = object : IPin {
            override var nodeId = -1
            override var pinId: Int = -1
            override var label: String = ""
            override var icon: IconType = IconType.NONE
            override var labelColor: Int = 0
            override var baseColor: Int = 0
            override var innerColor: Int = 0
            override val links: MutableList<IPin> = mutableListOf()
            override fun getLinkId(other: IPin): Int = 0
            override fun isEmpty(): Boolean = true
            override var inputOutput: InputOutput = InputOutput.None
        }
    }
}