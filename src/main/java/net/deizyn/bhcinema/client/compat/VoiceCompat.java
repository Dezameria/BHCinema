package net.deizyn.bhcinema.client.compat;

import net.deizyn.bhcinema.client.CinemaConfig;
import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.world.phys.Vec3;

/** State and answers for the Plasmo Voice mixins. */
public final class VoiceCompat {
    // set by the audio source's getListener() when it picked "the camera" (client option on and server allows it),
    // and the camera entity's eyes (the player, under a BHCinema camera) seen by getListenerPosition(). Per thread,
    // because Plasmo Voice updates its sources on several worker threads; both are read straight after, in updateSource
    private static final ThreadLocal<Boolean> CAMERA_PICKED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Vec3> PLAYER_EYES = new ThreadLocal<>();

    private static VoiceListener listener() {
        return FreeCamera.get().isActive() ? CinemaConfig.VOICE_LISTENER.get() : VoiceListener.PLAYER;
    }

    /** Plasmo Voice's "camera sound listener" client option, as BHCinema wants it right now. */
    public static boolean cameraListenerOption(boolean option) {
        return FreeCamera.get().isActive() ? listener() != VoiceListener.PLAYER : option;
    }

    public static void setCameraPicked(boolean picked) {
        CAMERA_PICKED.set(picked);
    }

    /**
     * Where a voice source's listener is. Plasmo Voice uses the camera entity's eyes, but a BHCinema camera only moves
     * the view (the camera entity stays the player), so when it picked the camera this is the camera itself.
     */
    public static Vec3 listenerPosition(Vec3 entityEyes) {
        PLAYER_EYES.set(entityEyes);
        if (!CAMERA_PICKED.get() || listener() == VoiceListener.PLAYER) {
            return entityEyes;
        }
        FreeCamera camera = FreeCamera.get();
        return new Vec3(camera.x(), camera.y(), camera.z());
    }

    /**
     * Distance from a voice to the listener, which sets its loudness. In BOTH it is the shorter of the distances to
     * the camera and to the player's eyes, so voices near either are heard as near.
     */
    public static double distance(Vec3 source, double toListener) {
        Vec3 eyes = PLAYER_EYES.get();
        if (!CAMERA_PICKED.get() || listener() != VoiceListener.BOTH || eyes == null) {
            return toListener;
        }
        return Math.min(toListener, source.distanceTo(eyes));
    }

    private VoiceCompat() {
    }
}
