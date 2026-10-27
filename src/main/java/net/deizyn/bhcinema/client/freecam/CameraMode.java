package net.deizyn.bhcinema.client.freecam;

/**
 * FLY, LOCKED and FOLLOW are the F4 freecam: it flies, and the lock key pins it (LOCKED or FOLLOW, the mode key
 * switches between those two) so the player can be controlled while it films. BEHIND is the separate Behind camera.
 */
public enum CameraMode {
    /** WASD and mouse fly the camera; the player stands still. */
    FLY,
    /** Locked where it was flown to (catches up when you walk too far); WASD and mouse control the player. */
    LOCKED,
    /** Locked at its offset from the player and travels with them; WASD and mouse control the player. */
    FOLLOW,
    /** Like vanilla F5: camera behind the player, turning with where they face; WASD and mouse control the player. */
    BEHIND;

    public String translationKey() {
        return "bhcinema.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** LOCKED or FOLLOW: a locked freecam. */
    public boolean isLock() {
        return this == LOCKED || this == FOLLOW;
    }

    /** The other lock mode (LOCKED and FOLLOW swap); anything else starts at LOCKED. */
    public CameraMode nextLockMode() {
        return this == LOCKED ? FOLLOW : LOCKED;
    }
}
