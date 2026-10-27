package net.deizyn.bhcinema.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.deizyn.bhcinema.client.HudAutoHide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While the HUD fades, every shader colour set during the GUI pass has the fade multiplied into its alpha. HUD code
 * (vanilla and other mods) keeps resetting the colour to opaque white, which would otherwise undo the fade.
 * RenderSystem's names are not obfuscated, so nothing here is remapped.
 */
@Mixin(value = RenderSystem.class, remap = false)
public abstract class RenderSystemMixin {
    @ModifyVariable(method = "setShaderColor", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private static float bhcinema$fadeHud(float alpha) {
        float tint = HudAutoHide.tint();
        return tint < 1.0F ? alpha * tint : alpha;
    }
}
