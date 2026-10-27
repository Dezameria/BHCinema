package net.deizyn.bhcinema.mixin;

import net.deizyn.bhcinema.client.compat.EpicFightCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Epic Fight (optional): its camera API's first-person check drives first-person view effects, such as turning the
 * player to an animation's point of view. While a BHCinema camera is on there is no first-person view, so it reports
 * false. isTPSMode is left alone, so Epic Fight's shoulder camera stays off.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.api.client.camera.EpicFightCameraAPI", remap = false)
public abstract class EpicFightCameraMixin {
    @Inject(method = "isFirstPerson", at = @At("HEAD"), cancellable = true, require = 0)
    private void bhcinema$noFirstPersonWhileFilming(CallbackInfoReturnable<Boolean> cir) {
        if (EpicFightCompat.filming()) {
            cir.setReturnValue(false);
        }
    }
}
