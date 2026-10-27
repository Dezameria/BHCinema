package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.deizyn.bhcinema.client.compat.EpicFightCompat;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Epic Fight (optional): in first person its camera-angles listener bends the view to the playing animation's point
 * of view, which would fight the BHCinema camera's own angles. The check sits in a lambda of cameraSetupEvent, named
 * here directly; require = 0, so a renamed lambda only skips this.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.client.events.engine.RenderEngine$Events", remap = false)
public abstract class EpicFightRenderEventsMixin {
    @ModifyExpressionValue(method = "lambda$cameraSetupEvent$2", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;", remap = true))
    private static CameraType bhcinema$thirdPersonWhileFilming(CameraType type) {
        return EpicFightCompat.cameraType(type);
    }
}
