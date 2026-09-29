package xp.dev.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexFormats;
import xp.dev.EffectsNice;
import xp.dev.client.effect.*;
import xp.dev.effect.*;

public class EffectsNiceClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(EffectsNice.id("sphere"), VertexFormats.POSITION_TEXTURE, shader -> SphereRenderer.shader = shader);
            context.register(EffectsNice.id("convert"), VertexFormats.POSITION_TEXTURE, shader -> ConvertRenderer.shader = shader);
            context.register(EffectsNice.id("storm"), VertexFormats.POSITION, shader -> SkyRenderer.storm = shader);
            context.register(EffectsNice.id("rift"), VertexFormats.POSITION_TEXTURE, shader -> SkyRenderer.rift = shader);
        });
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.ID, (payload, context) -> context.client().execute(() -> ClientEffects.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ImagePayload.ID, (payload, context) -> context.client().execute(() -> ClientEffects.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ConvertPayload.ID, (payload, context) -> context.client().execute(() -> ConvertRenderer.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SpherePayload.ID, (payload, context) -> context.client().execute(() -> SphereRenderer.accept(payload)));
        WorldRenderEvents.LAST.register(SphereRenderer::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { ClientEffects.reset(); ConvertRenderer.reset(); SphereRenderer.reset(); });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> { ClientEffects.reset(); ConvertRenderer.reset(); SphereRenderer.reset(); });
    }
}
