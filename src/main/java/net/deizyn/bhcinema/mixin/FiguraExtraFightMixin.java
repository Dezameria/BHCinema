package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.deizyn.bhcinema.client.compat.EpicFightCompat;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * figura_extra_fight (optional, Figura + Epic Fight): in first person with a Figura avatar loaded it vetoes Epic
 * Fight's model for the local player, since in real first person that body is not seen. Under a BHCinema camera the
 * body is seen from outside but the game is still in first person, so the player showed as the plain vanilla model
 * instead of the animated Epic Fight one. While a camera is on it sees third person, as with F5.
 */
@Pseudo
@Mixin(targets = "com.mikihina.figuraextrafight.compat.FiguraCompat", remap = false)
public abstract class FiguraExtraFightMixin {
    @ModifyExpressionValue(method = "onRenderEpicFightPlayer", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;", remap = true))
    private static CameraType bhcinema$thirdPersonWhileFilming(CameraType type) {
        return EpicFightCompat.cameraType(type);
    }
}
