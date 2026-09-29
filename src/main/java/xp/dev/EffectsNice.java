package xp.dev;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xp.dev.effect.*;

public class EffectsNice implements ModInitializer {
    public static final String MOD_ID = "effects-nice";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(EffectPayload.ID, EffectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ImagePayload.ID, ImagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConvertPayload.ID, ConvertPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpherePayload.ID, SpherePayload.CODEC);
        EffectServer.register();
    }
    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }
}
