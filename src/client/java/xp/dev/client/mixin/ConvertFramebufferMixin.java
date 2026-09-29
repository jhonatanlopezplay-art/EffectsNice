package xp.dev.client.mixin;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xp.dev.client.effect.ConvertRenderer;

@Mixin(Framebuffer.class)
public abstract class ConvertFramebufferMixin {
    @Inject(method="beginWrite", at=@At("HEAD"), cancellable=true)
    private void effectsNice$target(boolean viewport, CallbackInfo ci) {
        Framebuffer mask = ConvertRenderer.redirectedTarget();
        if (mask != null && (Object)this != mask) { mask.beginWrite(viewport); ci.cancel(); }
    }
    @Inject(method="endWrite", at=@At("HEAD"), cancellable=true)
    private void effectsNice$stayInMask(CallbackInfo ci) {
        Framebuffer mask = ConvertRenderer.redirectedTarget();
        if (mask != null) { mask.beginWrite(false); ci.cancel(); }
    }
}
