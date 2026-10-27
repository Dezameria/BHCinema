package net.deizyn.bhcinema.client.compat;

import java.util.Locale;

/** Where Plasmo Voice hears voice chat from while a BHCinema camera is on. */
public enum VoiceListener {
    /** Your player only, as if there were no camera. */
    PLAYER,
    /** The camera only: fly up to someone to hear them clearer. */
    CAMERA,
    /** Both: each voice is as loud as from whichever of the two is closer; directions are heard from the camera. */
    BOTH;

    public String translationKey() {
        return "bhcinema.voice." + name().toLowerCase(Locale.ROOT);
    }
}
