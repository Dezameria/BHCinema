package net.deizyn.bhcinema.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only setup called from the mod constructor; kept apart so a server never loads these classes. */
public final class CinemaClientSetup {
    public static void init() {
        // "Config" button for BeeHouse Cinema in the Mods list
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new CinemaSettingsScreen(parent)));
    }

    private CinemaClientSetup() {
    }
}
