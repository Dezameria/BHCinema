package net.deizyn.bhcinema.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.deizyn.bhcinema.client.HudAutoHide;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the auto-hide fade reach the vanilla HUD. Some overlays start with setupOverlayRenderState(false, ...), which
 * turns blending off, and the XP bar switches blending off before drawing; without blending the fade does nothing.
 * <p>
 * The two method names are Forge's own (not obfuscated), so their selectors are not remapped; RenderSystem is a
 * Minecraft class, so the redirect target is.
 */
@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {
    @Inject(method = "setupOverlayRenderState(ZZ)V", at = @At("TAIL"), remap = false)
    private void bhcinema$keepFade(boolean blend, boolean depthTest, CallbackInfo ci) {
        if (HudAutoHide.tint() < 1.0F) {
            RenderSystem.enableBlend();
        }
    }

    @Redirect(method = "renderExperience", remap = false,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V", remap = true))
    private void bhcinema$blendWhileFading() {
        if (HudAutoHide.tint() >= 1.0F) {
            RenderSystem.disableBlend();
        }
    }
}
