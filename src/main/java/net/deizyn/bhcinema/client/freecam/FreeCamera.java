package net.deizyn.bhcinema.client.freecam;

import net.deizyn.bhcinema.client.CinemaConfig;
import net.deizyn.bhcinema.client.CinemaKeys;
import com.mojang.blaze3d.platform.InputConstants;
import net.deizyn.bhcinema.client.freecam.FlightMath.Motion;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Client-side cinema camera. Simulated once per rendered frame with real frame time (not per 20 Hz tick), so motion is
 * smooth at any FPS. CameraMixin places the vanilla Camera here. Two cameras share it: the F4 freecam, which flies
 * (FLY, the player stands still and gets no input) and can be locked where it was flown to (LOCKED / FOLLOW, the
 * player is controlled as usual while the camera films them), and the Behind camera (F5-style).
 */
public final class FreeCamera {
    private static final FreeCamera INSTANCE = new FreeCamera();

    public static FreeCamera get() {
        return INSTANCE;
    }

    private boolean active;
    private CameraMode mode = CameraMode.FLY;
    private double x, y, z;
    private double vx, vy, vz;
    // FOLLOW mode: camera position relative to the player's feet
    private double offsetX, offsetY, offsetZ;
    // the offset FOLLOW started with, restored by reset (R) after orbiting / scrolling
    private double startOffsetX, startOffsetY, startOffsetZ;
    // FOLLOW mode: where the camera would be with no walls, and how far from the player's eyes walls let it go
    private double followX, followY, followZ;
    private double followClip = Double.POSITIVE_INFINITY;
    // half-size of the camera's collision box; big enough that the near plane never shows the inside of a wall
    private static final double CAMERA_RADIUS = 0.25;
    // FOLLOW collision: seconds to pull in front of a wall (quick, so walls are barely seen through) and to ease back
    private static final double CLIP_IN_SMOOTHING = 0.05;
    private static final double CLIP_OUT_SMOOTHING = 0.4;
    // ordinary jumps rise ~1.25 blocks; with ignoreJumps only height above this pulls the camera up mid-air
    private static final double JUMP_ALLOWANCE = 1.5;
    // LOCKED/FOLLOW starting closer than START_MIN_DISTANCE to the eyes back out to START_DISTANCE (vanilla F5 distance)
    private static final double START_MIN_DISTANCE = 1.5;
    private static final double START_DISTANCE = 4.0;
    // look-at-player needs some distance to aim along; closer than this it keeps the current angle
    private static final double MIN_AIM_DISTANCE = 0.5;

    // the feet height the camera tracks (see updateTrackedHeight), and that height through an extra low-pass, used
    // for following and aiming so jumps do not jerk the shot
    private double trackedFeetY;
    private double smoothFeetY;
    // framing shift as currently eased in, and the final drawn position (simulated position + shift)
    private double shiftRight, shiftUp, shiftForward;
    private double renderX, renderY, renderZ;
    // targets are what the mouse/keys ask for; the plain fields chase them with smoothing.
    // Yaw is never wrapped, so smoothing never takes the long way round.
    private float yaw, pitch, targetYaw, targetPitch;
    private float roll, targetRoll;
    private double fov, targetFov;
    // BEHIND distance for this session: starts at the config value, middle mouse + scroll changes it until closed
    private double chaseDistance;
    private long lastFrameNanos;

    private FreeCamera() {
    }

    public boolean isActive() {
        return active;
    }

    public CameraMode mode() {
        return mode;
    }

    /** True when the keys and mouse fly the camera instead of controlling the player. */
    public boolean isFlying() {
        return active && mode == CameraMode.FLY;
    }
    /**
     * FOLLOW mode with the pick-block button (middle mouse by default) held: the mouse swings the camera around the
     * player instead of turning the player.
     */
    public boolean isOrbiting() {
        return mode == CameraMode.FOLLOW && isPickHeld();
    }

    /** BEHIND mode with the pick-block button held: scroll changes how far behind the camera sits. */
    public boolean isAdjustingChaseDistance() {
        return mode == CameraMode.BEHIND && isPickHeld();
    }

    private boolean isPickHeld() {
        Minecraft mc = Minecraft.getInstance();
        return active && mc.screen == null && mc.options.keyPickItem.isDown();
    }

    /** Changes the BEHIND distance for this session only; it goes back to the config value when the camera closes. */
    public void scrollChaseDistance(double notches) {
        chaseDistance = Mth.clamp(chaseDistance * Math.pow(0.9, notches), 1.0, 32.0);
        overlay(Component.translatable("bhcinema.freecam.distance", format(chaseDistance)));
    }

    /** Mouse movement, in the same units vanilla passes to {@code Entity.turn}, routed by the current mode. */
    public boolean handleMouse(double dx, double dy) {
        if (isFlying()) {
            turn(dx, dy);
            return true;
        }
        if (isOrbiting()) {
            orbit(dx, dy);
            return true;
        }
        return false;
    }

    /** F4: freecam on, or off if it is already on (flying or locked). From the Behind camera it switches over. */
    public void toggleFreecam() {
        if (active && mode != CameraMode.BEHIND) {
            disable();
        } else if (enable(CameraMode.FLY)) {
            overlay(Component.translatable("bhcinema.freecam.on", CinemaKeys.LOCK.getTranslatedKeyMessage()));
        }
    }

    /** Behind key: the Behind (F5-style) camera on, or off if it is already on. From freecam it switches over. */
    public void toggleBehind() {
        if (active && mode == CameraMode.BEHIND) {
            disable();
        } else if (enable(CameraMode.BEHIND)) {
            overlay(Component.translatable("bhcinema.behind.on"));
        }
    }

    /**
     * Lock key, freecam only: locks the camera where it was flown to (LOCKED or FOLLOW, per the lock mode) and gives
     * control back to the player; pressed again it unlocks, so the camera can be flown somewhere else.
     */
    public void toggleLock() {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mode == CameraMode.BEHIND || mc.player == null) {
            overlay(Component.translatable("bhcinema.lock.needs_freecam", CinemaKeys.FREECAM.getTranslatedKeyMessage()));
            return;
        }
        if (mode == CameraMode.FLY) {
            startMode(CinemaConfig.LOCK_MODE.get(), mc.player, mc.player.getPosition(mc.getFrameTime()));
            overlay(Component.translatable("bhcinema.lock.on", Component.translatable(mode.translationKey())));
        } else {
            // keep exactly the shot on screen: fold the framing shift into the position instead of easing it out
            x = renderX;
            y = renderY;
            z = renderZ;
            shiftRight = shiftUp = shiftForward = 0.0;
            startMode(CameraMode.FLY, mc.player, mc.player.getPosition(mc.getFrameTime()));
            overlay(Component.translatable("bhcinema.lock.off", CinemaKeys.LOCK.getTranslatedKeyMessage()));
        }
    }

    /**
     * Mode key: switches a locked camera between LOCKED and FOLLOW. While flying it picks which of the two the next
     * lock uses.
     */
    public void cycleMode() {
        if (!active || mode == CameraMode.BEHIND) {
            overlay(Component.translatable("bhcinema.lock.needs_freecam", CinemaKeys.FREECAM.getTranslatedKeyMessage()));
            return;
        }
        CameraMode next = CinemaConfig.LOCK_MODE.get().nextLockMode();
        CinemaConfig.LOCK_MODE.set(next);
        CinemaConfig.SPEC.save();
        // a locked camera picks the new mode up in frame(), the same way it does for a change in the settings screen
        overlay(Component.translatable(mode == CameraMode.FLY ? "bhcinema.lock.next" : "bhcinema.lock.mode",
                Component.translatable(next.translationKey()), CinemaKeys.LOCK.getTranslatedKeyMessage()));
    }

    /** Starts a fresh session in {@code newMode}; false if there is no world to film. */
    private boolean enable(CameraMode newMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        // always start from the first-person view (the player's eyes and head angle), even when F5 is on
        float partialTick = mc.getFrameTime();
        Vec3 pos = mc.player.getEyePosition(partialTick);
        x = pos.x;
        y = pos.y;
        z = pos.z;
        yaw = targetYaw = mc.player.getViewYRot(partialTick);
        pitch = targetPitch = mc.player.getViewXRot(partialTick);
        // roll, zoom and distance tweaks made with keys / scroll only last for one session
        roll = targetRoll = 0.0F;
        fov = targetFov = mc.options.fov().get();
        chaseDistance = CinemaConfig.CHASE_DISTANCE.get();
        // the framing shift eases in from the real view instead of jumping
        shiftRight = shiftUp = shiftForward = 0.0;
        renderX = x;
        renderY = y;
        renderZ = z;
        lastFrameNanos = System.nanoTime();
        active = true;
        if (newMode == CameraMode.FLY) {
            // a little behind the head, so the player is in the shot instead of the camera sitting inside it
            backOutOfPlayer(mc.player, CinemaConfig.START_DISTANCE.get());
        }
        startMode(newMode, mc.player, mc.player.position());
        return true;
    }

    public void disable() {
        if (!active) {
            return;
        }
        if (mode == CameraMode.FLY) {
            restoreMovementKeys();
        }
        active = false;
        CinemaConfig.SPEC.save(); // keeps the scroll-wheel fly speed for next time
        overlay(Component.translatable(mode == CameraMode.BEHIND ? "bhcinema.behind.off" : "bhcinema.freecam.off"));
    }

    private void startMode(CameraMode newMode, LocalPlayer player, Vec3 playerPos) {
        if (active && mode == CameraMode.FLY && newMode != CameraMode.FLY) {
            restoreMovementKeys();
        }
        mode = newMode;
        vx = vy = vz = 0.0;
        if (newMode == CameraMode.LOCKED || newMode == CameraMode.FOLLOW) {
            backOutOfPlayer(player, START_DISTANCE);
        }
        offsetX = startOffsetX = x - playerPos.x;
        offsetY = startOffsetY = y - playerPos.y;
        offsetZ = startOffsetZ = z - playerPos.z;
        trackedFeetY = smoothFeetY = playerPos.y;
        followX = x;
        followY = y;
        followZ = z;
        followClip = Double.POSITIVE_INFINITY;
        // the mouse may have turned the player meanwhile; keep the camera looking where it looks now
        targetYaw = yaw;
        targetPitch = pitch;
    }

    /**
     * A camera at the player's eyes films the inside of their head, and LOCKED / FOLLOW would keep it there (with
     * look-at-player having no direction to aim along). So a camera closer than {@link #START_MIN_DISTANCE} backs out
     * {@code wanted} blocks behind along the view. It always stops in front of walls, even with collision off, so a
     * camera never starts inside a block.
     */
    private void backOutOfPlayer(LocalPlayer player, double wanted) {
        Vec3 eye = player.getEyePosition();
        if (wanted <= 0.0 || new Vec3(x, y, z).distanceTo(eye) >= START_MIN_DISTANCE) {
            return;
        }
        Motion back = FlightMath.behind(yaw, pitch, 1.0);
        double distance = clearDistance(player, eye, new Vec3(back.x(), back.y(), back.z()), wanted);
        x = renderX = eye.x + back.x() * distance;
        y = renderY = eye.y + back.y() * distance;
        z = renderZ = eye.z + back.z() * distance;
    }

    private void turn(double dx, double dy) {
        targetYaw += (float) dx * 0.15F;
        targetPitch = Mth.clamp(targetPitch + (float) dy * 0.15F, -90.0F, 90.0F);
    }

    /** Swings the FOLLOW offset around the player's eyes and turns the view by the same amount, so it stays framed. */
    private void orbit(double dx, double dy) {
        float dYaw = (float) dx * 0.15F;
        float dPitch = (float) dy * 0.15F;
        double eyeHeight = eyeHeight();
        Motion around = FlightMath.orbit(new Motion(offsetX, offsetY - eyeHeight, offsetZ), dYaw, dPitch);
        offsetX = around.x();
        offsetY = around.y() + eyeHeight;
        offsetZ = around.z();
        turn(dx, dy);
    }

    /** While orbiting, scroll moves the camera closer to / further from the player. */
    public void scrollOrbitDistance(double notches) {
        double eyeHeight = eyeHeight();
        double ox = offsetX, oy = offsetY - eyeHeight, oz = offsetZ;
        double distance = Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (distance < 1.0e-6) {
            return;
        }
        double newDistance = Mth.clamp(distance * Math.pow(0.9, notches), 1.0, 64.0);
        double k = newDistance / distance;
        offsetX = ox * k;
        offsetY = oy * k + eyeHeight;
        offsetZ = oz * k;
        overlay(Component.translatable("bhcinema.freecam.distance", format(newDistance)));
    }

    private static double eyeHeight() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 1.62 : player.getEyeHeight();
    }

    public void scrollSpeed(double notches) {
        double speed = Mth.clamp(CinemaConfig.FLY_SPEED.get() * Math.pow(1.2, notches), 0.05, 500.0);
        CinemaConfig.FLY_SPEED.set(speed);
        overlay(Component.translatable("bhcinema.freecam.speed", format(speed)));
    }

    public void scrollFov(double notches) {
        targetFov = Mth.clamp(targetFov - notches * CinemaConfig.FOV_STEP.get(), 1.0, 150.0);
        overlay(Component.translatable("bhcinema.freecam.fov", format(targetFov)));
    }

    /**
     * R (Reset Camera): roll, zoom and the distance tweaks (BEHIND scroll distance, FOLLOW orbit) go back to how the session started.
     * It is a straight cut, not an eased swing back, which is less dizzying.
     */
    public void resetLens() {
        roll = targetRoll = 0.0F;
        fov = targetFov = Minecraft.getInstance().options.fov().get();
        chaseDistance = CinemaConfig.CHASE_DISTANCE.get();
        if (mode == CameraMode.FOLLOW) {
            offsetX = startOffsetX;
            offsetY = startOffsetY;
            offsetZ = startOffsetZ;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3 feet = player.getPosition(Minecraft.getInstance().getFrameTime());
                followX = x = feet.x + offsetX;
                followY = y = smoothFeetY + offsetY;
                followZ = z = feet.z + offsetZ;
            }
        }
        snapFraming();
        overlay(Component.translatable("bhcinema.freecam.reset"));
    }

    /** Jumps the framing shift straight to its setting (used by the reset buttons, so they cut instead of easing). */
    public void snapFraming() {
        boolean framed = mode != CameraMode.FLY;
        shiftRight = framed ? CinemaConfig.SHIFT_RIGHT.get() : 0.0;
        shiftUp = framed ? CinemaConfig.SHIFT_UP.get() : 0.0;
        shiftForward = framed ? CinemaConfig.SHIFT_FORWARD.get() : 0.0;
    }

    /** Called at the start of every rendered frame. */
    public void frame(float partialTick) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrameNanos) / 1.0e9, 0.1);
        lastFrameNanos = now;
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            active = false;
            return;
        }
        Vec3 feet = player.getPosition(partialTick);
        // the lock mode changed in the settings screen or with the mode key
        if (mode.isLock() && CinemaConfig.LOCK_MODE.get() != mode) {
            startMode(CinemaConfig.LOCK_MODE.get(), player, feet);
        }
        if (mc.isPaused()) {
            return;
        }

        Options options = mc.options;
        boolean input = mc.screen == null;
        // roll belongs to freecam (flying or locked); the Behind camera always stays level
        boolean behind = mode == CameraMode.BEHIND;
        double rollInput = input && !behind ? axis(CinemaKeys.ROLL_RIGHT, CinemaKeys.ROLL_LEFT) : 0.0;
        if (behind) {
            targetRoll = 0.0F;
        }
        double lookTau = CinemaConfig.LOOK_SMOOTHING.get();
        double lensTau = CinemaConfig.LENS_SMOOTHING.get();
        double followTau = CinemaConfig.FOLLOW_SMOOTHING.get();

        updateTrackedHeight(player, feet.y, behind ? CinemaConfig.BEHIND_IGNORE_JUMPS.get() : CinemaConfig.IGNORE_JUMPS.get());
        double heightTau = behind ? CinemaConfig.BEHIND_HEIGHT_SMOOTHING.get() : CinemaConfig.VERTICAL_SMOOTHING.get();
        smoothFeetY = FlightMath.approach(smoothFeetY, trackedFeetY, heightTau, dt);
        // the player's eyes as the camera tracks them: exact sideways, softened (and jump-free) in height
        Motion eye = new Motion(feet.x, smoothFeetY + player.getEyeHeight(), feet.z);
        boolean collide = CinemaConfig.COLLISION.get();

        switch (mode) {
            case FLY -> {
                releaseMovementKeys();
                fly(options, input, dt, collide);
            }
            case LOCKED -> {
                Motion target = FlightMath.leash(new Motion(x, y, z), eye, CinemaConfig.CATCH_UP_DISTANCE.get());
                moveTowards(target, followTau, dt, collide);
            }
            case FOLLOW -> follow(player, feet, eye, followTau, dt, collide);
            case BEHIND -> {
                // the view turns with the player's face; the position is placed after smoothing, below
                targetYaw += Mth.wrapDegrees(player.getViewYRot(partialTick) - targetYaw);
                targetPitch = Mth.clamp(player.getViewXRot(partialTick), -90.0F, 90.0F);
            }
        }

        double aimX = eye.x() - x, aimY = eye.y() - y, aimZ = eye.z() - z;
        boolean lookAtPlayer = mode == CameraMode.LOCKED ? CinemaConfig.LOCKED_LOOK_AT_PLAYER.get()
                : mode == CameraMode.FOLLOW && CinemaConfig.FOLLOW_LOOK_AT_PLAYER.get();
        if (lookAtPlayer && aimX * aimX + aimY * aimY + aimZ * aimZ > MIN_AIM_DISTANCE * MIN_AIM_DISTANCE) {
            FlightMath.Angles aim = FlightMath.lookAt(aimX, aimY, aimZ);
            targetYaw += Mth.wrapDegrees(aim.yaw() - targetYaw);
            targetPitch = Mth.clamp(aim.pitch(), -90.0F, 90.0F);
        }

        targetRoll += (float) (rollInput * CinemaConfig.ROLL_SPEED.get() * dt);
        yaw = (float) FlightMath.approach(yaw, targetYaw, lookTau, dt);
        pitch = (float) FlightMath.approach(pitch, targetPitch, lookTau, dt);
        roll = (float) FlightMath.approach(roll, targetRoll, lensTau, dt);
        fov = FlightMath.approach(fov, targetFov, lensTau, dt);
        if (mode == CameraMode.BEHIND) {
            chase(player, eye, dt, collide);
        }
        updateRenderPosition(lensTau, dt);
    }

    /**
     * With ignoreJumps, the height the camera tracks stays at the ground the player jumped from while they are in the
     * air. It still drops at once when they fall below it, and only rises for the part of a jump above
     * {@link #JUMP_ALLOWANCE} (jump boost, slime blocks). Flying, swimming, climbing and riding track the real height.
     */
    private void updateTrackedHeight(LocalPlayer player, double feetY, boolean ignoreJumps) {
        if (!ignoreJumps) {
            trackedFeetY = feetY;
            return;
        }
        if (player.onGround()) {
            // the tick's real height, not the interpolated one: on the landing tick onGround is already true while the
            // drawn position is still coming down, which would lift the camera for a moment and drop it again
            trackedFeetY = player.getY();
            return;
        }
        boolean followRealHeight = player.getAbilities().flying || player.isFallFlying() || player.isPassenger()
                || player.onClimbable() || player.isInWater() || player.isInLava();
        if (followRealHeight || feetY < trackedFeetY) {
            trackedFeetY = feetY;
        } else {
            trackedFeetY = Math.max(trackedFeetY, feetY - JUMP_ALLOWANCE);
        }
    }

    /** Moves the camera by (dx, dy, dz), stopping and sliding along block collision boxes when {@code collide}. */
    private Vec3 move(double dx, double dy, double dz, boolean collide) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 moved = new Vec3(dx, dy, dz);
        if (collide && mc.level != null) {
            AABB box = new AABB(x - CAMERA_RADIUS, y - CAMERA_RADIUS, z - CAMERA_RADIUS,
                    x + CAMERA_RADIUS, y + CAMERA_RADIUS, z + CAMERA_RADIUS);
            moved = Entity.collideBoundingBox(null, moved, box, mc.level, List.of());
        }
        x += moved.x;
        y += moved.y;
        z += moved.z;
        return moved;
    }

    /**
     * FOLLOW: the camera trails a wall-free follow position. With collision it never slides along walls (it could get
     * stuck behind one); like vanilla third person it moves in along the line from the player's eyes instead. Only that
     * distance reacts to walls, pulling in fast and easing back out, so blocks passing behind the player do not make
     * the camera (and the player on screen) jitter.
     */
    private void follow(LocalPlayer player, Vec3 feet, Motion eye, double tau, double dt, boolean collide) {
        followX = FlightMath.approach(followX, feet.x + offsetX, tau, dt);
        followY = FlightMath.approach(followY, smoothFeetY + offsetY, tau, dt);
        followZ = FlightMath.approach(followZ, feet.z + offsetZ, tau, dt);
        placeInFrontOfWalls(player, new Vec3(eye.x(), eye.y(), eye.z()), new Vec3(followX, followY, followZ), dt, collide);
    }

    /**
     * BEHIND (F5-style): the camera sits {@code chaseDistance} behind the player's eyes, along the direction the
     * camera itself looks. The view chases the player's head with look smoothing, so the camera swings round smoothly
     * as the player turns while the player stays centred.
     */
    private void chase(LocalPlayer player, Motion eye, double dt, boolean collide) {
        Motion back = FlightMath.behind(yaw, pitch, chaseDistance);
        Vec3 pivot = new Vec3(eye.x(), eye.y(), eye.z());
        placeInFrontOfWalls(player, pivot, pivot.add(back.x(), back.y(), back.z()), dt, collide);
    }

    /**
     * Puts the camera at {@code desired}, or with collision moves it in along the line from the player's eyes to just
     * in front of whatever is in the way.
     */
    private void placeInFrontOfWalls(LocalPlayer player, Vec3 pivot, Vec3 desired, double dt, boolean collide) {
        if (!collide) {
            x = desired.x;
            y = desired.y;
            z = desired.z;
            followClip = Double.POSITIVE_INFINITY;
            return;
        }

        Vec3 toCam = desired.subtract(pivot);
        double distance = toCam.length();
        if (distance < 1.0e-6) {
            x = pivot.x;
            y = pivot.y;
            z = pivot.z;
            return;
        }
        double allowed = clearDistance(player, pivot, toCam.scale(1.0 / distance), distance);
        followClip = Math.min(followClip, distance);
        followClip = FlightMath.approach(followClip, allowed,
                allowed < followClip ? CLIP_IN_SMOOTHING : CLIP_OUT_SMOOTHING, dt);
        Vec3 pos = pivot.add(toCam.scale(Math.min(followClip, distance) / distance));
        x = pos.x;
        y = pos.y;
        z = pos.z;
    }

    /**
     * How far the camera can go from {@code pivot} along {@code dir} before its box would touch a block. Like vanilla's
     * third-person check it casts a ray from each corner of the camera box, so the answer does not flicker at block edges.
     */
    private static double clearDistance(LocalPlayer player, Vec3 pivot, Vec3 dir, double maxDistance) {
        double best = maxDistance;
        Vec3 end = pivot.add(dir.scale(maxDistance));
        for (int corner = 0; corner < 8; corner++) {
            double ox = ((corner & 1) * 2 - 1) * CAMERA_RADIUS * 0.5;
            double oy = (((corner >> 1) & 1) * 2 - 1) * CAMERA_RADIUS * 0.5;
            double oz = (((corner >> 2) & 1) * 2 - 1) * CAMERA_RADIUS * 0.5;
            Vec3 from = pivot.add(ox, oy, oz);
            BlockHitResult hit = player.level().clip(new ClipContext(from, end.add(ox, oy, oz),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                best = Math.min(best, hit.getLocation().distanceTo(from) - CAMERA_RADIUS);
            }
        }
        return Math.max(0.0, best);
    }

    private void fly(Options options, boolean input, double dt, boolean collide) {
        // read from the keyboard itself: the key mappings are held released so the player does not move
        double forward = input ? heldAxis(options.keyUp, options.keyDown) : 0.0;
        double strafe = input ? heldAxis(options.keyLeft, options.keyRight) : 0.0;
        double vertical = input ? heldAxis(options.keyJump, options.keyShift) : 0.0;

        // direction from the yaw/pitch actually on screen, so W always goes where the view points
        Motion dir = FlightMath.direction(yaw, pitch, CinemaConfig.FOLLOW_PITCH.get(), forward, strafe, vertical);
        double s = CinemaConfig.FLY_SPEED.get()
                * (input && isHeld(options.keySprint) ? CinemaConfig.SPRINT_MULTIPLIER.get() : 1.0);
        double moveTau = CinemaConfig.MOVE_SMOOTHING.get();
        vx = FlightMath.approach(vx, dir.x() * s, moveTau, dt);
        vy = FlightMath.approach(vy, dir.y() * s, moveTau, dt);
        vz = FlightMath.approach(vz, dir.z() * s, moveTau, dt);
        double dx = vx * dt, dy = vy * dt, dz = vz * dt;
        Vec3 moved = move(dx, dy, dz, collide);
        // a wall stops that axis dead, so speed does not build up against it
        if (Math.abs(moved.x - dx) > 1.0e-7) {
            vx = 0.0;
        }
        if (Math.abs(moved.y - dy) > 1.0e-7) {
            vy = 0.0;
        }
        if (Math.abs(moved.z - dz) > 1.0e-7) {
            vz = 0.0;
        }
    }

    private void moveTowards(Motion target, double tau, double dt, boolean collide) {
        move(FlightMath.approach(x, target.x(), tau, dt) - x,
                FlightMath.approach(y, target.y(), tau, dt) - y,
                FlightMath.approach(z, target.z(), tau, dt) - z, collide);
    }

    /**
     * Applies the framing shift (right/up/forward in the camera's own frame) on top of the simulated position, except
     * in FLY. With collision the shift stops short of blocks, so it never pushes the view into a wall.
     */
    private void updateRenderPosition(double lensTau, double dt) {
        // framing is for shots around the player (LOCKED/FOLLOW/BEHIND); a flying camera goes exactly where you fly
        // it. The shift eases out and back in when switching modes
        boolean framed = mode != CameraMode.FLY;
        shiftRight = FlightMath.approach(shiftRight, framed ? CinemaConfig.SHIFT_RIGHT.get() : 0.0, lensTau, dt);
        shiftUp = FlightMath.approach(shiftUp, framed ? CinemaConfig.SHIFT_UP.get() : 0.0, lensTau, dt);
        shiftForward = FlightMath.approach(shiftForward, framed ? CinemaConfig.SHIFT_FORWARD.get() : 0.0, lensTau, dt);
        Motion shift = FlightMath.viewOffset(yaw, pitch, shiftRight, shiftUp, shiftForward);
        double length = shift.length();
        Vec3 base = new Vec3(x, y, z);
        Vec3 shifted = base.add(shift.x(), shift.y(), shift.z());
        LocalPlayer player = Minecraft.getInstance().player;
        if (length > 1.0e-4 && CinemaConfig.COLLISION.get() && player != null) {
            BlockHitResult hit = player.level().clip(new ClipContext(base, shifted, ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                double safe = Math.max(0.0, hit.getLocation().distanceTo(base) - CAMERA_RADIUS);
                shifted = base.add(shift.x() * safe / length, shift.y() * safe / length, shift.z() * safe / length);
            }
        }
        renderX = shifted.x;
        renderY = shifted.y;
        renderZ = shifted.z;
    }

    /** Where the view is drawn from: the simulated position plus the framing shift. */
    public double x() {
        return renderX;
    }

    public double y() {
        return renderY;
    }

    public double z() {
        return renderZ;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float roll() {
        return roll;
    }

    public double fov() {
        return fov;
    }

    /**
     * While flying, the movement keys (WASD, jump, sneak, sprint) are kept released, so nothing that reads them moves
     * the player. Clearing the movement input is not enough: Epic Fight and many skill mods read the jump key directly,
     * or set the input again after it was cleared, and the player would jump along with the camera. Called at the start
     * of every tick and frame; key presses arrive in between.
     */
    public void releaseMovementKeys() {
        if (!isFlying()) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        for (KeyMapping key : movementKeys(options)) {
            if (!isToggleMode(options, key)) {
                key.setDown(false);
                while (key.consumeClick()) {
                    // a press also counts as a click, which some mods act on (dodges, double jumps)
                }
            }
        }
    }

    /** Leaving FLY: keys still held go back to pressed, so walking on does not need a second press. */
    private static void restoreMovementKeys() {
        Options options = Minecraft.getInstance().options;
        for (KeyMapping key : movementKeys(options)) {
            if (!isToggleMode(options, key)) {
                key.setDown(isHeld(key));
            }
        }
    }

    private static KeyMapping[] movementKeys(Options options) {
        return new KeyMapping[]{options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyShift, options.keySprint};
    }

    /** Sneak / sprint set to Toggle flip on every press, so they are left alone. */
    private static boolean isToggleMode(Options options, KeyMapping key) {
        return key == options.keyShift ? options.toggleCrouch().get()
                : key == options.keySprint && options.toggleSprint().get();
    }

    /** Whether the key bound to {@code key} is physically down, regardless of its pressed flag. */
    private static boolean isHeld(KeyMapping key) {
        if (key.isUnbound()) {
            return false;
        }
        InputConstants.Key bound = key.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        return switch (bound.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> key.isDown();
        };
    }

    private static double heldAxis(KeyMapping positive, KeyMapping negative) {
        return (isHeld(positive) ? 1.0 : 0.0) - (isHeld(negative) ? 1.0 : 0.0);
    }

    private static double axis(KeyMapping positive, KeyMapping negative) {
        return (positive.isDown() ? 1.0 : 0.0) - (negative.isDown() ? 1.0 : 0.0);
    }

    private static String format(double value) {
        return String.format("%.1f", value);
    }

    private static void overlay(Component message) {
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }
}
