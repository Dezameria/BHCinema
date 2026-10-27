package net.deizyn.bhcinema.client;

import com.mojang.blaze3d.platform.Window;
import net.deizyn.bhcinema.client.freecam.FlightMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Black cinema bars above and below the picture. Drawn by GameRendererMixin between the world and the HUD, so they
 * also show with F1 (hidden GUI) and end up in F2 screenshots.
 */
public final class Letterbox {
    private static boolean enabled;
    private static double progress;
    private static long lastFrameNanos = System.nanoTime();

    public static void toggle() {
        setEnabled(!enabled);
        Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable(enabled ? "bhcinema.letterbox.on" : "bhcinema.letterbox.off"), false);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void frame() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrameNanos) / 1.0e9, 0.1);
        lastFrameNanos = now;
        progress = FlightMath.approach(progress, enabled ? 1.0 : 0.0, CinemaConfig.LETTERBOX_SMOOTHING.get(), dt);
        if (!enabled && progress < 0.001) {
            progress = 0.0;
        }
    }

    public static void render() {
        Minecraft mc = Minecraft.getInstance();
        if (progress <= 0.0 || mc.level == null) {
            return;
        }
        // work in real pixels so the bars slide smoothly instead of in GUI-scale steps
        Window window = mc.getWindow();
        int width = window.getWidth();
        int height = window.getHeight();
        double fullBar = Math.max(0.0, (height - width / CinemaConfig.LETTERBOX_RATIO.get()) / 2.0);
        int bar = (int) Math.round(fullBar * progress);
        if (bar <= 0) {
            return;
        }

        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        float scale = (float) (1.0 / window.getGuiScale());
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.fill(0, 0, width, bar, 0xFF000000);
        graphics.fill(0, height - bar, width, height, 0xFF000000);
        graphics.pose().popPose();
        graphics.flush();
    }

    private Letterbox() {
    }
}
