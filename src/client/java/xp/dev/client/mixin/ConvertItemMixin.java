package xp.dev.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xp.dev.client.effect.ConvertRenderer;

@Mixin(ItemRenderer.class)
public abstract class ConvertItemMixin {
    @Shadow public abstract void renderItem(ItemStack stack, ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay, BakedModel model);
    @Inject(method="renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", at=@At("HEAD"))
    private void effectsNice$keepItem(ItemStack stack, ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (!ConvertRenderer.collectingItem() || stack.isEmpty()) return;
        MatrixStack copy = ConvertRenderer.snapshot(matrices);
        ConvertRenderer.preserve(() -> renderItem(stack,mode,leftHanded,copy,ConvertRenderer.consumers(),light,overlay,model));
    }
}
