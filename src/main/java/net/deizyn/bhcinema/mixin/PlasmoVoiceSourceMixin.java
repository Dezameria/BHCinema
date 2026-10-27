package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.deizyn.bhcinema.client.compat.VoiceCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Plasmo Voice (optional): how loud each voice is. Every voice source measures its distance from getListenerPosition(),
 * the eyes of getListener(), which is the camera entity when the "camera sound listener" option is on and the server
 * allows it ({@code pv.allow_freecam}), else the player. A BHCinema camera never changes the camera entity, so without
 * this the distance stayed at the player and flying up to someone did not make them louder.
 * <p>
 * getListener(): the option check (its only {@code booleanValue()}) reads as voiceFromCamera while a camera is on, and
 * reading {@code Minecraft.cameraEntity} marks that the camera was picked; the permission check in between is
 * untouched. getListenerPosition() then swaps those eyes for the BHCinema camera's position. require = 0 everywhere.
 */
@Pseudo
@Mixin(targets = "su.plo.voice.client.audio.source.BaseClientAudioSource", remap = false)
public abstract class PlasmoVoiceSourceMixin {
    @Inject(method = "getListener", at = @At("HEAD"), require = 0)
    private void bhcinema$resetPick(CallbackInfoReturnable<Entity> cir) {
        VoiceCompat.setCameraPicked(false);
    }

    @ModifyExpressionValue(method = "getListener", require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z", ordinal = 0))
    private boolean bhcinema$listenerWhileFilming(boolean cameraListener) {
        return VoiceCompat.cameraListenerOption(cameraListener);
    }

    @ModifyExpressionValue(method = "getListener", require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;cameraEntity:Lnet/minecraft/world/entity/Entity;", remap = true))
    private Entity bhcinema$markCameraPicked(Entity cameraEntity) {
        VoiceCompat.setCameraPicked(true);
        return cameraEntity;
    }

    @WrapOperation(method = "getListenerPosition", require = 0,
            at = @At(value = "INVOKE", target = "Lsu/plo/lib/mod/extensions/EntityKt;eyePosition(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 bhcinema$cameraPosition(Entity listener, Operation<Vec3> original) {
        return VoiceCompat.listenerPosition(original.call(listener));
    }

    /** updateSource(): {@code sourcePosition.distanceTo(listenerPosition)}, its only distanceTo, sets the loudness. */
    @WrapOperation(method = "updateSource", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D", remap = true))
    private double bhcinema$nearestEar(Vec3 source, Vec3 listener, Operation<Double> original) {
        return VoiceCompat.distance(source, original.call(source, listener));
    }
}
