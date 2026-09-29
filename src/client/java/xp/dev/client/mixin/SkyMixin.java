package xp.dev.client.mixin;

import net.minecraft.client.render.Camera;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Unique;
import xp.dev.client.effect.ClientEffects;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xp.dev.client.effect.SkyRenderer;

@Mixin(WorldRenderer.class)
public class SkyMixin {
    @Inject(method = "renderSky", at = @At("TAIL"))
    private void effectsNice$sky(Matrix4f view, Matrix4f projection, float tickDelta, Camera camera,
                                 boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (!thickFog) SkyRenderer.render(view, projection, camera);
    }
    @Unique private float[] effectsNice$cloudColor;
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void effectsNice$fadeClouds(CallbackInfo ci) {
        float progress = ClientEffects.phase(true);
        effectsNice$cloudColor = RenderSystem.getShaderColor().clone();
        if (progress >= 0.999f) { ci.cancel(); return; }
        RenderSystem.setShaderColor(effectsNice$cloudColor[0], effectsNice$cloudColor[1], effectsNice$cloudColor[2], effectsNice$cloudColor[3] * (1 - progress));
    }
    @Inject(method = "renderClouds", at = @At("RETURN"))
    private void effectsNice$restoreCloudColor(CallbackInfo ci) {
        if (effectsNice$cloudColor != null) RenderSystem.setShaderColor(effectsNice$cloudColor[0], effectsNice$cloudColor[1], effectsNice$cloudColor[2], effectsNice$cloudColor[3]);
    }
}
