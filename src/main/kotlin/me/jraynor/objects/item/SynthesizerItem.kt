package me.jraynor.objects.item

import net.minecraft.item.Item
import net.minecraft.item.ItemGroup

/**
 * This is the go to item for the mod. It does all kinds of configurations.
 * It is also required in order to link things with the utility block
 */
class SynthesizerItem : Item(Properties().group(ItemGroup.MISC).maxStackSize(1)) {
    
}