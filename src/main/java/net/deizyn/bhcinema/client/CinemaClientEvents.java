package net.deizyn.bhcinema.client;

import net.deizyn.bhcinema.BHCinemaMod;
import net.deizyn.bhcinema.client.freecam.CameraMode;
import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BHCinemaMod.MOD_ID, value = Dist.CLIENT)
public final class CinemaClientEvents {
    private static boolean hideViewModel() {
        return FreeCamera.get().isActive() && CinemaConfig.HIDE_CROSSHAIR.get();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            FreeCamera.get().frame(event.renderTickTime);
            Letterbox.frame();
        }
    }

    /** Highest priority, so the movement keys are released before any other mod reads them this tick. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            FreeCamera.get().releaseMovementKeys();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        HudAutoHide.tick();
        FreeCamera camera = FreeCamera.get();
        while (CinemaKeys.FREECAM.consumeClick()) {
            camera.toggleFreecam();
        }
        while (CinemaKeys.LOCK.consumeClick()) {
            camera.toggleLock();
        }
        while (CinemaKeys.BEHIND.consumeClick()) {
            camera.toggleBehind();
        }
        while (CinemaKeys.MODE.consumeClick()) {
            camera.cycleMode();
        }
        while (CinemaKeys.SETTINGS.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new CinemaSettingsScreen(mc.screen));
        }
        while (CinemaKeys.LETTERBOX.consumeClick()) {
            Letterbox.toggle();
        }
        while (CinemaKeys.RESET_LENS.consumeClick()) {
            if (camera.isActive()) {
                camera.resetLens();
            }
        }
    }

    /**
     * While flying, the keys drive the camera, so the player itself stands still. Lowest priority, so this wins over
     * mods that set the input again.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!FreeCamera.get().isFlying()) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = input.down = input.left = input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /**
     * Plain scroll always stays with the hotbar. Held modifier keys (rebindable) turn it into a camera control: zoom
     * modifier + scroll = FOV in every mode, speed modifier + scroll = flight speed in freecam, and middle mouse +
     * scroll = distance in FOLLOW / BEHIND.
     */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        FreeCamera camera = FreeCamera.get();
        if (!camera.isActive() || Minecraft.getInstance().screen != null) {
            return;
        }
        if (CinemaKeys.ZOOM_MODIFIER.isDown()) {
            camera.scrollFov(event.getScrollDelta());
        } else if (camera.isFlying() && CinemaKeys.SPEED_MODIFIER.isDown()) {
            camera.scrollSpeed(event.getScrollDelta());
        } else if (camera.isOrbiting()) {
            camera.scrollOrbitDistance(event.getScrollDelta());
        } else if (camera.isAdjustingChaseDistance()) {
            camera.scrollChaseDistance(event.getScrollDelta());
        } else {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * Left / right click work as usual in every mode (in freecam they act from the player's own eyes and facing, not
     * from the camera). Only in FOLLOW and BEHIND is the pick-block button taken, as the orbit / distance button.
     */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        FreeCamera camera = FreeCamera.get();
        CameraMode mode = camera.mode();
        if (camera.isActive() && event.isPickBlock() && (mode == CameraMode.FOLLOW || mode == CameraMode.BEHIND)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        FreeCamera camera = FreeCamera.get();
        if (camera.isActive()) {
            event.setRoll(event.getRoll() + camera.roll());
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        FreeCamera camera = FreeCamera.get();
        // usedConfiguredFov is false for the held-item pass, which keeps its own FOV
        if (camera.isActive() && event.usedConfiguredFov()) {
            event.setFOV(camera.fov());
        }
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        if (hideViewModel()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // the HUD auto-hide works on the whole GUI pass (GameRendererMixin); this only drops the crosshair
        if (hideViewModel() && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FreeCamera.get().disable();
    }

    private CinemaClientEvents() {
    }
}
