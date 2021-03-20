package me.jraynor

import me.jraynor.internal.Listeners
import me.jraynor.internal.Registry
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * Main mod class. Should be an `object` declaration annotated with `@Mod`.
 * The modid should be declared in this object and should match the modId entry
 * in mods.toml.
 */
@Mod(BlueprintMod.ID)
object BlueprintMod {
    const val ID: String = "bpm"

    /**
     * This will setup everything first initializing out event callbacks,
     * then initializing our blocks and other objects.
     */
    init {
        Listeners.register(MOD_BUS, FORGE_BUS)
        Registry.register(MOD_BUS)
    }


}