package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.deizyn.bhcinema.client.HudAutoHide;
import net.deizyn.bhcinema.client.Letterbox;
import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns off view bobbing for the free camera. Letterbox bars go in right after the GUI ortho projection is set up and before the HUD, which runs only when the GUI
 * is not hidden. Hooking here keeps the bars visible with F1.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * View bobbing follows the player's walk cycle even when the camera is detached, which shakes a free camera
     * whenever the player walks (LOCKED/FOLLOW). A free camera never bobs.
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bhcinema$noBobInFreecam(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (FreeCamera.get().isActive()) {
            ci.cancel();
        }
    }

    /**
     * No first-person hands or held items while a camera is on. Cancelling Forge's RenderHandEvent is not enough: Epic
     * Fight draws its animated first-person arms from its own highest-priority listener and cancels the event itself.
     * This also skips the first-person screen overlays (fire, inside a block), which belong to the player's eyes.
     */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void bhcinema$noHandsInFreecam(PoseStack poseStack, Camera camera, float partialTicks, CallbackInfo ci) {
        if (FreeCamera.get().isActive()) {
            ci.cancel();
        }
    }

    /**
     * Auto-hide HUD: the whole GUI pass, so HUDs other mods add (through overlays, GUI events or their own mixins) go
     * too. Hidden, it is skipped outright; fading, it is drawn with the fade as shader alpha (RenderSystemMixin keeps
     * the fade in every colour set while it draws). The first-person hand is drawn before this and stays.
     */
    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void bhcinema$autoHideHud(Gui gui, GuiGraphics graphics, float partialTicks, Operation<Void> original) {
        float alpha = HudAutoHide.alpha();
        if (alpha <= 0.0F) {
            return;
        }
        if (alpha >= 1.0F) {
            original.call(gui, graphics, partialTicks);
            return;
        }
        HudAutoHide.setTint(alpha);
        try {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            original.call(gui, graphics, partialTicks);
            // batched text and items are drawn on flush, so flush while the fade still applies
            graphics.flush();
        } finally {
            HudAutoHide.setTint(1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V"))
    private void bhcinema$renderLetterbox(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (renderLevel) {
            Letterbox.render();
        }
    }
}
