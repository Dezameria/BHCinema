package net.deizyn.bhcinema;

import net.deizyn.bhcinema.client.CinemaClientSetup;
import net.deizyn.bhcinema.client.CinemaConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(BHCinemaMod.MOD_ID)
public final class BHCinemaMod {
    public static final String MOD_ID = "bhcinema";

    public BHCinemaMod() {
        // client config only; the event subscribers in client/ are Dist.CLIENT, so a server loads nothing else
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CinemaConfig.SPEC);
        if (FMLEnvironment.dist.isClient()) {
            CinemaClientSetup.init();
        }
    }
}
