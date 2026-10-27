package net.deizyn.bhcinema.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.deizyn.bhcinema.BHCinemaMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BHCinemaMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CinemaKeys {
    private static final String CATEGORY = "key.categories.bhcinema";

    // F4 is the classic Freecam mod key; this mod replaces it, so do not run both
    public static final KeyMapping FREECAM = key("freecam", GLFW.GLFW_KEY_F4);
    // freecam only: lock the camera where it was flown to / unlock it to fly again
    public static final KeyMapping LOCK = key("lock", GLFW.GLFW_KEY_B);
    // freecam only: LOCKED <-> FOLLOW
    public static final KeyMapping MODE = key("mode", GLFW.GLFW_KEY_G);
    // unbound by default (F6 is often taken); set it in Controls
    public static final KeyMapping BEHIND = key("behind", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping SETTINGS = key("settings", GLFW.GLFW_KEY_F7);
    public static final KeyMapping LETTERBOX = key("letterbox", GLFW.GLFW_KEY_K);
    // roll only works in freecam; the arrow keys are free in game
    public static final KeyMapping ROLL_LEFT = key("roll_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping ROLL_RIGHT = key("roll_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping RESET_LENS = key("reset_lens", GLFW.GLFW_KEY_R);
    // held while scrolling, so plain scroll stays with the hotbar
    public static final KeyMapping SPEED_MODIFIER = key("speed_modifier", GLFW.GLFW_KEY_Z);
    public static final KeyMapping ZOOM_MODIFIER = key("zoom_modifier", GLFW.GLFW_KEY_LEFT_ALT);

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping("key.bhcinema." + name, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, glfwKey, CATEGORY);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(FREECAM);
        event.register(LOCK);
        event.register(MODE);
        event.register(BEHIND);
        event.register(SETTINGS);
        event.register(LETTERBOX);
        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
        event.register(RESET_LENS);
        event.register(SPEED_MODIFIER);
        event.register(ZOOM_MODIFIER);
    }

    private CinemaKeys() {
    }
}
