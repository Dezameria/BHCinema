package net.deizyn.bhcinema.mixin;

import net.deizyn.bhcinema.client.HudAutoHide;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps the auto-hide HUD fade intact through the item drawing that GuiGraphics does. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    /**
     * Block items (dirt, stone...) are drawn with the solid/cutout block sheet, which has no blending, so they would
     * stay fully visible while the rest of the hotbar fades and then pop out at the end. While fading, draw them with
     * the translucent block sheet instead: same texture, shader and lighting, but it honours the fade.
     */
    @ModifyArg(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"),
            index = 4)
    private MultiBufferSource bhcinema$fadeBlockItems(MultiBufferSource source) {
        if (HudAutoHide.tint() >= 1.0F) {
            return source;
        }
        return type -> source.getBuffer(
                type == Sheets.solidBlockSheet() || type == Sheets.cutoutBlockSheet()
                        ? Sheets.translucentCullBlockSheet() : type);
    }
}
