package xp.dev.client.mixin;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xp.dev.client.effect.ConvertRenderer;

@Mixin(WorldRenderer.class)
public abstract class ConvertWorldMixin {
    @Shadow private void renderEntity(Entity entity, double x, double y, double z, float delta, MatrixStack matrices, VertexConsumerProvider consumers) { throw new AssertionError(); }
    @Inject(method="render", at=@At("HEAD"))
    private void effectsNice$begin(CallbackInfo ci) { ConvertRenderer.beginWorld(); }
    @Inject(method="renderEntity", at=@At("HEAD"))
    private void effectsNice$keepEntity(Entity entity, double x, double y, double z, float delta, MatrixStack matrices, VertexConsumerProvider consumers, CallbackInfo ci) {
        if (!ConvertRenderer.collecting()) return;
        boolean keep = entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof ItemFrameEntity;
        ConvertRenderer.inExcludedEntity(keep);
        if (keep) {
            MatrixStack copy = ConvertRenderer.snapshot(matrices);
            ConvertRenderer.preserve(() -> renderEntity(entity,x,y,z,delta,copy,ConvertRenderer.consumers()));
        }
    }
    @Inject(method="renderEntity", at=@At("RETURN"))
    private void effectsNice$endEntity(CallbackInfo ci) { ConvertRenderer.inExcludedEntity(false); }
    @Inject(method="render", at=@At("RETURN"))
    private void effectsNice$finish(RenderTickCounter ticks, boolean outline, Camera camera, GameRenderer renderer,
                                    LightmapTextureManager lightmap, Matrix4f view, Matrix4f projection, CallbackInfo ci) {
        ConvertRenderer.finishWorld(view);
    }
    @Inject(method="close", at=@At("HEAD"))
    private void effectsNice$close(CallbackInfo ci) { ConvertRenderer.reset(); }
}
