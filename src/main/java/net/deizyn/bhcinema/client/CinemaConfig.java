package net.deizyn.bhcinema.client;

import net.deizyn.bhcinema.client.compat.VoiceListener;
import net.deizyn.bhcinema.client.freecam.CameraMode;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * config/bhcinema-client.toml; also edited in game by CinemaSettingsScreen. One section per camera (F4 freecam, its
 * lock, Behind) plus the shared ones, so it is obvious which camera a setting belongs to. Every comment is short and
 * bilingual, since people edit this file by hand.
 */
public final class CinemaConfig {
    public static final ForgeConfigSpec SPEC;

    // [general] - every camera
    public static final ForgeConfigSpec.BooleanValue COLLISION;
    public static final ForgeConfigSpec.BooleanValue HIDE_CROSSHAIR;
    public static final ForgeConfigSpec.EnumValue<VoiceListener> VOICE_LISTENER;
    public static final ForgeConfigSpec.DoubleValue LOOK_SMOOTHING;
    public static final ForgeConfigSpec.DoubleValue FOV_STEP;
    public static final ForgeConfigSpec.DoubleValue LENS_SMOOTHING;

    // [freecam] - F4
    public static final ForgeConfigSpec.DoubleValue START_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FLY_SPEED;
    public static final ForgeConfigSpec.DoubleValue SPRINT_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue FOLLOW_PITCH;
    public static final ForgeConfigSpec.DoubleValue MOVE_SMOOTHING;
    public static final ForgeConfigSpec.DoubleValue ROLL_SPEED;

    // [lock] - freecam locked with the lock key (LOCKED / FOLLOW)
    public static final ForgeConfigSpec.EnumValue<CameraMode> LOCK_MODE;
    public static final ForgeConfigSpec.BooleanValue IGNORE_JUMPS;
    public static final ForgeConfigSpec.DoubleValue VERTICAL_SMOOTHING;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_SMOOTHING;
    public static final ForgeConfigSpec.BooleanValue LOCKED_LOOK_AT_PLAYER;
    public static final ForgeConfigSpec.DoubleValue CATCH_UP_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue FOLLOW_LOOK_AT_PLAYER;

    // [behind] - Behind key (unbound by default)
    public static final ForgeConfigSpec.DoubleValue CHASE_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue BEHIND_IGNORE_JUMPS;
    public static final ForgeConfigSpec.DoubleValue BEHIND_HEIGHT_SMOOTHING;

    // [framing] - locked freecam and Behind
    public static final ForgeConfigSpec.DoubleValue SHIFT_RIGHT;
    public static final ForgeConfigSpec.DoubleValue SHIFT_UP;
    public static final ForgeConfigSpec.DoubleValue SHIFT_FORWARD;

    // [letterbox]
    public static final ForgeConfigSpec.DoubleValue LETTERBOX_RATIO;
    public static final ForgeConfigSpec.DoubleValue LETTERBOX_SMOOTHING;

    // [hud]
    public static final ForgeConfigSpec.BooleanValue AUTO_HIDE_HUD;
    public static final ForgeConfigSpec.BooleanValue AUTO_HIDE_ONLY_IN_FREECAM;
    public static final ForgeConfigSpec.DoubleValue HUD_SHOW_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HUD_FADE_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment(
                "BHCinema has two cameras / มีกล้อง 2 แบบ:",
                "  F4 = Freecam: fly the camera anywhere, the player stands still / กล้องอิสระ บินได้ ตัวละครยืนนิ่ง",
                "       B = lock it there and play normally, B again = unlock and fly / B = ล็อคกล้องไว้แล้วเล่นตัวละคร, B อีกครั้ง = ปลดล็อคไปบินต่อ",
                "       G = Locked <-> Follow / G = สลับ ล็อค <-> ตามตัว",
                "  Behind (no key by default, set one in Controls): like F5, the camera behind you turns with your face",
                "       / กล้องข้างหลัง (ยังไม่ตั้งปุ่ม ตั้งเองได้ใน Controls): แบบ F5 กล้องอยู่ข้างหลังหันตามหน้า",
                "Everything here can also be changed in game: F7 / ปรับในเกมได้ที่ F7",
                "",
                "Settings for every camera / ตั้งค่าที่ใช้กับกล้องทุกแบบ"
        ).push("general");
        COLLISION = b.comment(
                "Camera bumps into blocks instead of passing through / กล้องชนบล็อกแทนการทะลุ",
                "true = collide / ชน, false = noclip / ทะลุ"
        ).define("cameraCollision", false);
        HIDE_CROSSHAIR = b.comment("Hide crosshair and block outline while a camera is on / ซ่อนเป้าเล็งและกรอบบล็อกตอนเปิดกล้อง")
                .define("hideCrosshair", true);
        VOICE_LISTENER = b.comment(
                "Plasmo Voice: where voice chat is heard from while a camera is on / ตอนเปิดกล้อง ได้ยินเสียงคุยจากตรงไหน",
                "  PLAYER = your player / ตัวละคร",
                "  CAMERA = the camera: fly closer to hear someone clearer / กล้อง: บินเข้าไปใกล้ก็ได้ยินชัดขึ้น",
                "  BOTH = both, each voice as loud as from the closer one / ทั้งคู่ เสียงดังตามฝั่งที่ใกล้กว่า",
                "The server must allow freecam listening (pv.allow_freecam, on by default), else it is your player",
                "เซิร์ฟเวอร์ต้องอนุญาต (pv.allow_freecam ค่าเริ่มต้นอนุญาต) ไม่งั้นจะได้ยินจากตัวละคร"
        ).defineEnum("voiceListener", VoiceListener.BOTH);
        LOOK_SMOOTHING = b.comment("Seconds of camera turning smoothness. 0 = instant / ความนุ่มตอนหันกล้อง (วินาที) 0 = ทันที")
                .defineInRange("turnSmoothing", 0.08, 0.0, 5.0);
        FOV_STEP = b.comment("FOV change per scroll notch while holding Zoom Modifier (Left Alt), in degrees / FOV ที่เปลี่ยนต่อการหมุนลูกกลิ้งตอนกดปุ่มซูม (Alt ซ้าย) ค้าง (องศา)")
                .defineInRange("zoomStep", 5.0, 0.5, 30.0);
        LENS_SMOOTHING = b.comment("Seconds of zoom / roll / framing smoothness / ความนุ่มของซูม roll และการเลื่อนมุมกล้อง (วินาที)")
                .defineInRange("zoomSmoothing", 0.15, 0.0, 5.0);
        b.pop();

        b.comment("F4 Freecam only / ใช้เฉพาะกล้องอิสระ (F4)").push("freecam");
        START_DISTANCE = b.comment(
                "Blocks behind your head the freecam starts, so you see yourself. 0 = from your eyes",
                "กล้องอิสระเริ่มห่างจากหัวกี่บล็อก (ถอยไปข้างหลัง จะได้เห็นตัวเอง) 0 = เริ่มที่ตา"
        ).defineInRange("startDistance", 2.0, 0.0, 16.0);
        FLY_SPEED = b.comment("Flight speed, blocks per second (hold Speed Modifier, Z, + scroll in game) / ความเร็วบิน บล็อกต่อวินาที (กดปุ่มปรับความเร็ว Z ค้าง + ลูกกลิ้ง ในเกม)")
                .defineInRange("flySpeed", 10.0, 0.05, 500.0);
        SPRINT_MULTIPLIER = b.comment("Speed multiplier while holding sprint / ตัวคูณความเร็วตอนกดวิ่ง")
                .defineInRange("sprintMultiplier", 3.0, 1.0, 20.0);
        FOLLOW_PITCH = b.comment(
                "true = W/S also climb/dive where you look (spectator style). false = stay level like creative flight",
                "true = W/S บินขึ้นลงตามทิศที่มอง (แบบ spectator) false = บินแนวราบแบบ creative"
        ).define("flyWhereYouLook", false);
        MOVE_SMOOTHING = b.comment("Seconds to speed up / slow down. 0 = instant / ความนุ่มตอนเร่งและชะลอ (วินาที) 0 = ทันที")
                .defineInRange("moveSmoothing", 0.2, 0.0, 5.0);
        ROLL_SPEED = b.comment("Roll speed with Roll Left / Roll Right (arrow keys), degrees per second / ความเร็ว roll ด้วยลูกศรซ้าย/ขวา องศาต่อวินาที")
                .defineInRange("rollSpeed", 30.0, 1.0, 360.0);
        b.pop();

        b.comment(
                "Freecam lock (press B in freecam): the camera stays where you flew it and you play normally",
                "ล็อคกล้องอิสระ (กด B ตอนอยู่ในกล้องอิสระ): กล้องอยู่ตรงที่บินไว้ แล้วเราเล่นตัวละครได้ปกติ"
        ).push("lock");
        LOCK_MODE = b.comment(
                "What a lock does (G switches in game) / ล็อคแล้วเป็นแบบไหน (กด G สลับในเกม)",
                "  LOCKED = camera stays put / กล้องอยู่กับที่",
                "  FOLLOW = camera travels with you at the same distance / กล้องตามตัวไปที่ระยะเดิม"
        ).defineEnum("mode", CameraMode.LOCKED, CameraMode.LOCKED, CameraMode.FOLLOW);
        IGNORE_JUMPS = b.comment(
                "Camera stays level when you jump (still follows falls, steps, flying, swimming)",
                "กล้องไม่ขยับขึ้นลงตอนกระโดด (ยังตามตอนตก ขึ้นบล็อก บิน ว่ายน้ำ)"
        ).define("cameraIgnoresJumps", true);
        VERTICAL_SMOOTHING = b.comment("Seconds of smoothness following your height (steps, drops) / ความนุ่มตอนกล้องตามความสูง (วินาที)")
                .defineInRange("heightSmoothing", 0.6, 0.0, 5.0);
        FOLLOW_SMOOTHING = b.comment("Seconds of smoothness when the camera moves after you / ความนุ่มตอนกล้องเคลื่อนตามตัว (วินาที)")
                .defineInRange("followSmoothing", 0.3, 0.0, 5.0);
        LOCKED_LOOK_AT_PLAYER = b.comment("LOCKED: camera keeps turning to look at you / ล็อค: กล้องหันมองตัวเราตลอด")
                .define("lockedLookAtPlayer", false);
        CATCH_UP_DISTANCE = b.comment(
                "LOCKED: walk further than this many blocks from the camera and it moves to catch up. 0 = never moves",
                "ล็อค: ถ้าเดินห่างกล้องเกินกี่บล็อก กล้องจะเลื่อนตามมา 0 = กล้องไม่ขยับเลย"
        ).defineInRange("lockedCatchUpDistance", 24.0, 0.0, 256.0);
        FOLLOW_LOOK_AT_PLAYER = b.comment(
                "FOLLOW: camera keeps turning to look at you (hold middle mouse to swing it round)",
                "ตามตัว: กล้องหันมองตัวเราตลอด (กดเมาส์กลางค้างเพื่อหมุนกล้องรอบตัว)"
        ).define("followLookAtPlayer", false);
        b.pop();

        b.comment("Behind camera, like F5 (no key by default) / กล้องข้างหลังแบบ F5 (ยังไม่ตั้งปุ่ม)").push("behind");
        CHASE_DISTANCE = b.comment(
                "Blocks between you and the camera. Middle mouse + scroll changes it until the camera is closed",
                "ระยะกล้องจากตัว (บล็อก) เมาส์กลาง + ลูกกลิ้งปรับชั่วคราวได้ จนกว่าจะปิดกล้อง"
        ).defineInRange("distance", 4.0, 1.0, 32.0);
        BEHIND_IGNORE_JUMPS = b.comment(
                "Camera stays level when you jump (still follows falls, steps, flying, swimming)",
                "กล้องไม่ขยับขึ้นลงตอนกระโดด (ยังตามตอนตก ขึ้นบล็อก บิน ว่ายน้ำ)"
        ).define("cameraIgnoresJumps", true);
        BEHIND_HEIGHT_SMOOTHING = b.comment("Seconds of smoothness following your height (steps, drops) / ความนุ่มตอนกล้องตามความสูง (วินาที)")
                .defineInRange("heightSmoothing", 0.6, 0.0, 5.0);
        b.pop();

        b.comment(
                "Locked freecam and Behind: shift the shot relative to where it looks, in blocks (e.g. over-the-shoulder). 0 = none",
                "ใช้กับกล้องที่ล็อคไว้และกล้องข้างหลัง: เลื่อนช็อตตามทิศที่กล้องมอง (บล็อก) เช่น ช็อตข้ามไหล่ 0 = ไม่เลื่อน"
        ).push("framing");
        SHIFT_RIGHT = b.comment("Right (+) / left (-) / ขวา (+) ซ้าย (-)")
                .defineInRange("right", 0.0, -8.0, 8.0);
        SHIFT_UP = b.comment("Up (+) / down (-) / บน (+) ล่าง (-)")
                .defineInRange("up", 0.0, -8.0, 8.0);
        SHIFT_FORWARD = b.comment("Forward (+) / back (-) / หน้า (+) หลัง (-)")
                .defineInRange("forward", 0.0, -8.0, 8.0);
        b.pop();

        b.comment("Black cinema bars, key K / ขอบดำบนล่าง ปุ่ม K").push("letterbox");
        LETTERBOX_RATIO = b.comment("Picture shape between the bars. 2.39 = cinema, 1.85 = flat / สัดส่วนภาพ 2.39 = โรงหนัง, 1.85 = แบบ flat")
                .defineInRange("ratio", 2.39, 1.0, 4.0);
        LETTERBOX_SMOOTHING = b.comment("Seconds for the bars to slide in/out. 0 = instant / เวลาขอบดำเลื่อนเข้าออก (วินาที) 0 = ทันที")
                .defineInRange("slideTime", 0.25, 0.0, 5.0);
        b.pop();

        b.comment(
                "Auto-hide the whole HUD (hotbar, health, chat, crosshair, HUDs from other mods...) and show it for a moment",
                "when you change hotbar slot. The first-person hand stays",
                "ซ่อน HUD ทั้งจออัตโนมัติ (hotbar เลือด แชท เป้าเล็ง HUD ของม็อดอื่น...) แล้วโชว์แป๊บเดียวตอนเลื่อนช่อง item",
                "มือตอนมุมมองบุคคลที่หนึ่งยังเห็นอยู่"
        ).push("hud");
        AUTO_HIDE_HUD = b.comment("true = on / เปิด").define("autoHide", false);
        AUTO_HIDE_ONLY_IN_FREECAM = b.comment(
                "true = only while freecam (F4) or Behind is on. false = always / true = เฉพาะตอนเปิดกล้องอิสระ (F4) หรือกล้องข้างหลัง, false = ตลอด"
        ).define("onlyWhileCameraOn", true);
        HUD_SHOW_SECONDS = b.comment("Seconds the HUD stays after changing slot / HUD โชว์ค้างกี่วินาทีหลังเลื่อนช่อง")
                .defineInRange("showTime", 2.0, 0.5, 10.0);
        HUD_FADE_SECONDS = b.comment("Seconds to fade out after that. 0 = disappear at once / เวลาจางหาย (วินาที) 0 = หายทันที")
                .defineInRange("fadeTime", 0.5, 0.0, 5.0);
        b.pop();

        SPEC = b.build();
    }

    private CinemaConfig() {
    }
}
