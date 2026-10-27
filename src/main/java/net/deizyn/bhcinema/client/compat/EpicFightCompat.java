package net.deizyn.bhcinema.client.compat;

import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.CameraType;

/** What the Epic Fight mixins tell Epic Fight while a BHCinema camera films the player. */
public final class EpicFightCompat {
    /** True while a BHCinema camera is on: the player is seen from outside, never from their own eyes. */
    public static boolean filming() {
        return FreeCamera.get().isActive();
    }

    /**
     * The camera type Epic Fight should act on: third person while filming. THIRD_PERSON_BACK only replaces
     * FIRST_PERSON, and the patched methods only ask "is it first person", so nothing else changes.
     */
    public static CameraType cameraType(CameraType real) {
        return filming() && real == CameraType.FIRST_PERSON ? CameraType.THIRD_PERSON_BACK : real;
    }

    private EpicFightCompat() {
    }
}
