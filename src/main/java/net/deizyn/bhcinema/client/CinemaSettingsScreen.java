package net.deizyn.bhcinema.client;

import com.mojang.serialization.Codec;
import net.deizyn.bhcinema.client.compat.VoiceListener;
import net.deizyn.bhcinema.client.freecam.CameraMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.deizyn.bhcinema.client.freecam.FreeCamera;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-game editor for CinemaConfig, split into tabs like the Create World screen and ordered by how often people reach
 * for each setting. Changes apply immediately (a running freecam picks them up next frame) and are written to
 * bhcinema-client.toml when the screen closes. Opened with the settings key or the Config button in the Mods list.
 */
public final class CinemaSettingsScreen extends Screen {
    private static final int FOOTER_HEIGHT = 36;
    private static final int BUTTON_WIDTH = 150;

    @Nullable
    private final Screen parent;
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabBar;
    private List<CinemaTab> tabs = List.of();
    // the open tab survives resizes (init runs again), resets and reopening the screen
    private static int lastTab;

    public CinemaSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("bhcinema.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rememberTab();
        // one tab per camera (freecam, its lock, Behind) after the shared settings, same order as the toml file
        tabs = List.of(
                new TabBuilder("general")
                        .toggle("collision", CinemaConfig.COLLISION)
                        .toggle("hide_crosshair", CinemaConfig.HIDE_CROSSHAIR)
                        .addWide(voiceOption(), CinemaConfig.VOICE_LISTENER)
                        .slider("look_smoothing", CinemaConfig.LOOK_SMOOTHING, 0.0, 1.0, 0.01, "bhcinema.unit.seconds")
                        .slider("lens_smoothing", CinemaConfig.LENS_SMOOTHING, 0.0, 2.0, 0.05, "bhcinema.unit.seconds")
                        .slider("fov_step", CinemaConfig.FOV_STEP, 1.0, 20.0, 1.0, "bhcinema.unit.degrees")
                        .add(OptionInstance.createBoolean(key("letterbox"), tooltip("letterbox"),
                                Letterbox.isEnabled(), Letterbox::setEnabled), () -> Letterbox.setEnabled(false))
                        .slider("letterbox_ratio", CinemaConfig.LETTERBOX_RATIO, 1.5, 3.0, 0.01, "bhcinema.unit.ratio")
                        .slider("letterbox_smoothing", CinemaConfig.LETTERBOX_SMOOTHING, 0.0, 2.0, 0.05, "bhcinema.unit.seconds")
                        .build(),
                new TabBuilder("freecam")
                        .slider("start_distance", CinemaConfig.START_DISTANCE, 0.0, 8.0, 0.5, "bhcinema.unit.blocks")
                        .slider("fly_speed", CinemaConfig.FLY_SPEED, 0.5, 100.0, 0.5, "bhcinema.unit.speed")
                        .slider("sprint_multiplier", CinemaConfig.SPRINT_MULTIPLIER, 1.0, 10.0, 0.5, "bhcinema.unit.times")
                        .toggle("follow_pitch", CinemaConfig.FOLLOW_PITCH)
                        .slider("move_smoothing", CinemaConfig.MOVE_SMOOTHING, 0.0, 2.0, 0.05, "bhcinema.unit.seconds")
                        .slider("roll_speed", CinemaConfig.ROLL_SPEED, 5.0, 180.0, 5.0, "bhcinema.unit.degrees_per_second")
                        .build(),
                new TabBuilder("lock")
                        .add(modeOption(), CinemaConfig.LOCK_MODE)
                        .toggle("ignore_jumps", CinemaConfig.IGNORE_JUMPS)
                        .slider("vertical_smoothing", CinemaConfig.VERTICAL_SMOOTHING, 0.0, 3.0, 0.05, "bhcinema.unit.seconds")
                        .slider("follow_smoothing", CinemaConfig.FOLLOW_SMOOTHING, 0.0, 2.0, 0.05, "bhcinema.unit.seconds")
                        .toggle("locked_look_at_player", CinemaConfig.LOCKED_LOOK_AT_PLAYER)
                        .slider("catch_up_distance", CinemaConfig.CATCH_UP_DISTANCE, 0.0, 128.0, 1.0, "bhcinema.unit.blocks")
                        .toggle("follow_look_at_player", CinemaConfig.FOLLOW_LOOK_AT_PLAYER)
                        .build(),
                new TabBuilder("behind")
                        .slider("behind_distance", CinemaConfig.CHASE_DISTANCE, 1.0, 32.0, 0.5, "bhcinema.unit.blocks")
                        .toggle("behind_ignore_jumps", CinemaConfig.BEHIND_IGNORE_JUMPS)
                        .slider("behind_vertical_smoothing", CinemaConfig.BEHIND_HEIGHT_SMOOTHING, 0.0, 3.0, 0.05, "bhcinema.unit.seconds")
                        .build(),
                new FramingTab(),
                new TabBuilder("hud")
                        .toggle("auto_hide_hud", CinemaConfig.AUTO_HIDE_HUD)
                        .toggle("auto_hide_only_in_freecam", CinemaConfig.AUTO_HIDE_ONLY_IN_FREECAM)
                        .slider("hud_show_seconds", CinemaConfig.HUD_SHOW_SECONDS, 0.5, 10.0, 0.5, "bhcinema.unit.seconds")
                        .slider("hud_fade_seconds", CinemaConfig.HUD_FADE_SECONDS, 0.0, 3.0, 0.05, "bhcinema.unit.seconds")
                        .build());
        tabBar = TabNavigationBar.builder(tabManager, width).addTabs(tabs.toArray(new CinemaTab[0])).build();
        addRenderableWidget(tabBar);

        int footerY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("bhcinema.settings.reset_tab"), button -> resetTab())
                .tooltip(Tooltip.create(
                        Component.translatable("bhcinema.settings.reset_tab.tooltip")))
                .bounds(width / 2 - 155, footerY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("bhcinema.settings.reset_all"), button -> confirmResetAll())
                .tooltip(Tooltip.create(
                        Component.translatable("bhcinema.settings.reset_all.tooltip")))
                .bounds(width / 2 - 50, footerY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 + 55, footerY, 100, 20)
                .build());

        tabBar.selectTab(Mth.clamp(lastTab, 0, tabs.size() - 1), false);
        tabBar.setWidth(width);
        tabBar.arrangeElements();
        int top = tabBar.getRectangle().bottom();
        tabManager.setTabArea(new ScreenRectangle(0, top, width, height - FOOTER_HEIGHT - top));
    }

    /** Puts the open tab's settings back to their defaults; the widgets are rebuilt to show the new values. */
    private void resetTab() {
        rememberTab();
        tabs.get(Mth.clamp(lastTab, 0, tabs.size() - 1)).resets.forEach(Runnable::run);
        CinemaConfig.SPEC.save();
        // a reset is a cut, not an eased drift back
        FreeCamera.get().snapFraming();
        rebuildWidgets();
    }

    private void confirmResetAll() {
        rememberTab();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                tabs.forEach(tab -> tab.resets.forEach(Runnable::run));
                CinemaConfig.SPEC.save();
                FreeCamera.get().snapFraming();
            }
            minecraft.setScreen(this);
        }, Component.translatable("bhcinema.settings.reset_all.confirm_title"),
                Component.translatable("bhcinema.settings.reset_all.confirm_message")));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ctrl+Tab / Ctrl+1..4 switch tabs, as on the Create World screen
        return tabBar.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The Framing tab over a world: the shot stays visible and keeps running, so shifts can be judged live. */
    private boolean isLivePreview() {
        return minecraft != null && minecraft.level != null && tabManager.getCurrentTab() instanceof FramingTab;
    }

    @Override
    public boolean isPauseScreen() {
        return !isLivePreview();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isLivePreview()) {
            // no full-screen dimming; just dark bands behind the tab bar, the controls and the footer
            int top = tabBar.getRectangle().bottom();
            graphics.fill(0, 0, width, top, 0xA0000000);
            if (tabManager.getCurrentTab() instanceof FramingTab framing) {
                framing.renderPanel(graphics);
            }
            graphics.fill(0, height - FOOTER_HEIGHT, width, height, 0xA0000000);
        } else {
            renderBackground(graphics);
        }
        graphics.blit(CreateWorldScreen.FOOTER_SEPERATOR, 0, Mth.roundToward(height - FOOTER_HEIGHT - 2, 2),
                0.0F, 0.0F, width, 2, 32, 2);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void rememberTab() {
        // no current tab before the first init (List.of rejects indexOf(null))
        if (tabManager.getCurrentTab() == null) {
            return;
        }
        int open = tabs.indexOf(tabManager.getCurrentTab());
        if (open >= 0) {
            lastTab = open;
        }
    }

    @Override
    public void onClose() {
        rememberTab();
        CinemaConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    /** A settings tab that knows how to put each of its settings back to the default. */
    private abstract static class CinemaTab extends GridLayoutTab {
        final List<Runnable> resets = new ArrayList<>();

        CinemaTab(String name) {
            super(Component.translatable("bhcinema.settings.tab." + name));
        }
    }

    /** One tab of options in two columns; wide options take a whole row, for labels that do not fit in one. */
    private static final class OptionsTab extends CinemaTab {
        OptionsTab(String name, List<OptionInstance<?>> options, Set<OptionInstance<?>> wide, List<Runnable> resets) {
            super(name);
            this.resets.addAll(resets);
            layout.columnSpacing(10).rowSpacing(6);
            GridLayout.RowHelper rows = layout.createRowHelper(2);
            Options vanilla = Minecraft.getInstance().options;
            for (OptionInstance<?> option : options) {
                if (wide.contains(option)) {
                    rows.addChild(option.createButton(vanilla, 0, 0, BUTTON_WIDTH * 2 + 10), 2);
                } else {
                    rows.addChild(option.createButton(vanilla, 0, 0, BUTTON_WIDTH));
                }
            }
        }
    }

    /**
     * Framing: one row per axis, [-] slider [+] [0]. The buttons nudge by {@link #NUDGE} (Shift: {@link #NUDGE_BIG}),
     * which is much easier than hitting an exact value by dragging.
     */
    private final class FramingTab extends CinemaTab {
        private static final double NUDGE = 0.1;
        private static final double NUDGE_BIG = 1.0;
        private static final double LIMIT = 8.0;

        FramingTab() {
            super("framing");
            layout.columnSpacing(4).rowSpacing(6);
            GridLayout.RowHelper rows = layout.createRowHelper(4);
            // the shift only shows on a locked freecam or the Behind camera, not while flying
            FreeCamera camera = FreeCamera.get();
            Component hint = Component.translatable(camera.isActive() && camera.mode() != CameraMode.FLY
                    ? "bhcinema.settings.framing.hint_live" : "bhcinema.settings.framing.hint_off");
            rows.addChild(new StringWidget(BUTTON_WIDTH + 2 * 24 + 24 + 50, 12, hint, font), 4);
            axis(rows, "shift_right", CinemaConfig.SHIFT_RIGHT);
            axis(rows, "shift_up", CinemaConfig.SHIFT_UP);
            axis(rows, "shift_forward", CinemaConfig.SHIFT_FORWARD);
        }

        private void axis(GridLayout.RowHelper rows, String name, ForgeConfigSpec.DoubleValue value) {
            Options vanilla = Minecraft.getInstance().options;
            rows.addChild(smallButton("-", "bhcinema.settings.framing.minus", () -> nudge(value, -1)));
            rows.addChild(sliderOption(name, value, -LIMIT, LIMIT, NUDGE, "bhcinema.unit.blocks")
                    .createButton(vanilla, 0, 0, BUTTON_WIDTH + 50));
            rows.addChild(smallButton("+", "bhcinema.settings.framing.plus", () -> nudge(value, 1)));
            rows.addChild(smallButton("0", "bhcinema.settings.framing.zero", () -> {
                value.set(0.0);
                // back to 0 is a reset: cut, don't drift
                FreeCamera.get().snapFraming();
                rebuildWidgets();
            }));
            resets.add(() -> value.set(value.getDefault()));
        }

        private Button smallButton(String label, String tooltipKey, Runnable action) {
            return Button.builder(Component.literal(label), button -> action.run())
                    .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                    .size(20, 20)
                    .build();
        }

        private void nudge(ForgeConfigSpec.DoubleValue value, int direction) {
            double step = Screen.hasShiftDown() ? NUDGE_BIG : NUDGE;
            double next = Mth.clamp(value.get() + direction * step, -LIMIT, LIMIT);
            value.set(Math.round(next * 1000.0) / 1000.0);
            // rebuild so the slider shows the new value
            rebuildWidgets();
        }

        /** Dark panel behind the controls, used instead of dimming the whole screen. */
        void renderPanel(GuiGraphics graphics) {
            int pad = 6;
            graphics.fill(layout.getX() - pad, layout.getY() - pad,
                    layout.getX() + layout.getWidth() + pad, layout.getY() + layout.getHeight() + pad, 0xA0000000);
        }
    }

    private static final class TabBuilder {
        private final String name;
        private final List<OptionInstance<?>> options = new ArrayList<>();
        private final List<Runnable> resets = new ArrayList<>();
        private final Set<OptionInstance<?>> wide = new HashSet<>();

        TabBuilder(String name) {
            this.name = name;
        }

        TabBuilder add(OptionInstance<?> option, Runnable reset) {
            options.add(option);
            resets.add(reset);
            return this;
        }

        <T> TabBuilder add(OptionInstance<?> option, ForgeConfigSpec.ConfigValue<T> value) {
            return add(option, () -> value.set(value.getDefault()));
        }

        /** A whole-row option; put it where a row starts (after an even number of options), or it skips a cell. */
        <T> TabBuilder addWide(OptionInstance<?> option, ForgeConfigSpec.ConfigValue<T> value) {
            wide.add(option);
            return add(option, value);
        }

        TabBuilder toggle(String name, ForgeConfigSpec.BooleanValue value) {
            return add(OptionInstance.createBoolean(key(name), tooltip(name), value.get(), value::set), value);
        }

        TabBuilder slider(String name, ForgeConfigSpec.DoubleValue value, double min, double max, double step,
                          String unitKey) {
            return add(sliderOption(name, value, min, max, step, unitKey), value);
        }

        OptionsTab build() {
            return new OptionsTab(name, options, wide, resets);
        }
    }

    private static String key(String name) {
        return "bhcinema.settings." + name;
    }

    private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
        return OptionInstance.cachedConstantTooltip(Component.translatable(key(name) + ".tooltip"));
    }

    private static OptionInstance<CameraMode> modeOption() {
        return new OptionInstance<>(key("mode"), tooltip("mode"),
                (caption, mode) -> Options.genericValueLabel(caption, Component.translatable(mode.translationKey())),
                new OptionInstance.Enum<>(List.of(CameraMode.LOCKED, CameraMode.FOLLOW),
                        Codec.STRING.xmap(CameraMode::valueOf, CameraMode::name)),
                CinemaConfig.LOCK_MODE.get(), CinemaConfig.LOCK_MODE::set);
    }

    private static OptionInstance<VoiceListener> voiceOption() {
        return new OptionInstance<>(key("voice_listener"), tooltip("voice_listener"),
                (caption, listener) -> Options.genericValueLabel(caption, Component.translatable(listener.translationKey())),
                new OptionInstance.Enum<>(List.of(VoiceListener.values()),
                        Codec.STRING.xmap(VoiceListener::valueOf, VoiceListener::name)),
                CinemaConfig.VOICE_LISTENER.get(), CinemaConfig.VOICE_LISTENER::set);
    }

    /** A slider over min..max in fixed steps; it only writes the config once it is moved. */
    private static OptionInstance<Integer> sliderOption(String name, ForgeConfigSpec.DoubleValue value,
                                                        double min, double max, double step, String unitKey) {
        int steps = (int) Math.round((max - min) / step);
        int initial = Mth.clamp((int) Math.round((value.get() - min) / step), 0, steps);
        return new OptionInstance<>(key(name), tooltip(name),
                (caption, i) -> Options.genericValueLabel(caption, valueLabel(name, min + i * step, step, unitKey)),
                new OptionInstance.IntRange(0, steps), initial,
                // round so the file gets 0.3 rather than 0.30000000000000004
                i -> value.set(Math.round((min + i * step) * 1000.0) / 1000.0));
    }

    private static Component valueLabel(String name, double value, double step, String unitKey) {
        if (name.equals("catch_up_distance") && value <= 0.0) {
            return CommonComponents.OPTION_OFF;
        }
        String number = step < 0.1 ? String.format("%.2f", value) : String.format("%.1f", value);
        return Component.translatable(unitKey, number);
    }
}
