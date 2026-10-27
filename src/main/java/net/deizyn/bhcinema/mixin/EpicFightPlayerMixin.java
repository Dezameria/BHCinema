package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.deizyn.bhcinema.client.compat.EpicFightCompat;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Epic Fight (optional). A BHCinema camera leaves the game in first person, so Epic Fight kept treating the player
 * as seen from their own eyes: the body seen from the camera got the stiff first-person pose (arms held up for the
 * first-person model, head hidden) and the turning limits of first-person animations. While a camera is on, the
 * local player's patch sees third person instead.
 * <p>
 * Only these methods are touched, so Epic Fight's own third-person shoulder camera (which checks the camera type
 * elsewhere) does not switch on. @Pseudo and require = 0 make this a no-op without Epic Fight or after it changes.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch", remap = false)
public abstract class EpicFightPlayerMixin {
    @ModifyExpressionValue(method = {"isFirstPerson", "overrideRender", "checkXTurn", "checkYTurn"}, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;", remap = true))
    private CameraType bhcinema$thirdPersonWhileFilming(CameraType type) {
        return EpicFightCompat.cameraType(type);
    }
}
