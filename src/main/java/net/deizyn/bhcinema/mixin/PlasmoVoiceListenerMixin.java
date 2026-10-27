package net.deizyn.bhcinema.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.deizyn.bhcinema.client.compat.VoiceCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Plasmo Voice (optional): the OpenAL listener, which sets which way voices come from. While a BHCinema camera is on,
 * Plasmo Voice's "camera sound listener" client option reads as voiceFromCamera, so voices come from around the
 * camera even with that option off. PlasmoVoiceSourceMixin does the same for loudness.
 * <p>
 * update() checks that option first: {@code getCameraSoundListener().value()} unboxed with
 * {@code Boolean.booleanValue()}, the first such call in the method; only that check is replaced. The server's
 * {@code pv.allow_freecam} permission is checked right after and still applies. @Pseudo and require = 0 make this a
 * no-op when Plasmo Voice is missing or has changed; nothing here is a Minecraft name, so nothing is remapped.
 */
@Pseudo
@Mixin(targets = "su.plo.voice.client.audio.device.AlOutputDevice$AlListener", remap = false)
public abstract class PlasmoVoiceListenerMixin {
    @ModifyExpressionValue(method = "update", require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z", ordinal = 0))
    private boolean bhcinema$listenerWhileFilming(boolean cameraListener) {
        return VoiceCompat.cameraListenerOption(cameraListener);
    }
}
