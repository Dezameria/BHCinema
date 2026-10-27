package net.deizyn.bhcinema.mixin;

import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the vanilla camera to the free camera. The camera entity stays the player, and marking the camera detached
 * makes LevelRenderer draw the player's body like in third person.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void bhcinema$applyFreeCamera(BlockGetter level, Entity entity, boolean detachedIn, boolean mirrored,
                                          float partialTick, CallbackInfo ci) {
        FreeCamera camera = FreeCamera.get();
        if (!camera.isActive()) {
            return;
        }
        this.detached = true;
        this.setRotation(camera.yaw(), camera.pitch());
        this.setPosition(camera.x(), camera.y(), camera.z());
    }
}
