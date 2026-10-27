package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While flying, or orbiting in FOLLOW mode, the mouse moves the camera instead of the player (sensitivity, invert-Y
 * and F8 smoothing still apply). Otherwise it turns the player as usual.
 *
 * <p>A wrap, not a {@code @Redirect}: Epic Fight redirects the same call, and two redirects on one call crash the
 * game at launch. Wraps chain with each other and with one redirect.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void bhcinema$turnFreeCamera(LocalPlayer player, double yRot, double xRot, Operation<Void> original) {
        if (!FreeCamera.get().handleMouse(yRot, xRot)) {
            original.call(player, yRot, xRot);
        }
    }
}
