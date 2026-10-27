package net.deizyn.bhcinema.client;

import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Keeps the whole HUD hidden: everything drawn in the GUI pass, which includes HUDs from other mods (health bars,
 * stamina, skill icons...), the crosshair and chat. It only shows for a moment after the selected hotbar slot changes,
 * so the shot stays clean but you still see what you switched to, then fades out. The first-person hand is drawn
 * outside the GUI pass, so it stays. GameRendererMixin skips or tints the GUI pass using {@link #alpha()}.
 */
public final class HudAutoHide {
    // fade applied to the GUI pass being drawn right now (1 = none); read by the render mixins
    private static float tint = 1.0F;

    public static float tint() {
        return tint;
    }

    public static void setTint(float value) {
        tint = value;
    }

    private static int lastSlot = -1;
    private static long lastChangeMillis;

    /** Called every client tick to notice hotbar slot changes (scroll wheel or number keys). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastSlot = -1;
            return;
        }
        int slot = mc.player.getInventory().selected;
        if (slot != lastSlot) {
            // the first slot seen after joining does not count as a change
            if (lastSlot != -1) {
                lastChangeMillis = System.currentTimeMillis();
            }
            lastSlot = slot;
        }
    }

    /**
     * Opacity for the HUD right now: 1 while it is shown after a slot change, then fading to 0 over hudFadeSeconds.
     * Always 1 with auto-hide off, while typing in chat (the messages are part of the HUD) and with F3 open.
     */
    public static float alpha() {
        if (!CinemaConfig.AUTO_HIDE_HUD.get()) {
            return 1.0F;
        }
        if (CinemaConfig.AUTO_HIDE_ONLY_IN_FREECAM.get() && !FreeCamera.get().isActive()) {
            return 1.0F;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen || mc.options.renderDebug) {
            return 1.0F;
        }
        double sinceChange = (System.currentTimeMillis() - lastChangeMillis) / 1000.0;
        double fading = sinceChange - CinemaConfig.HUD_SHOW_SECONDS.get();
        if (fading <= 0.0) {
            return 1.0F;
        }
        double fade = CinemaConfig.HUD_FADE_SECONDS.get();
        if (fade <= 0.0 || fading >= fade) {
            return 0.0F;
        }
        // ease out: quick at first, settling gently into invisible
        double t = fading / fade;
        return (float) ((1.0 - t) * (1.0 - t));
    }

    private HudAutoHide() {
    }
}
