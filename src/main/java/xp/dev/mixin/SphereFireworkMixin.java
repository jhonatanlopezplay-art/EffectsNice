package xp.dev.mixin;

import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xp.dev.effect.EffectServer;

@Mixin(FireworkRocketEntity.class)
public abstract class SphereFireworkMixin {
    @Unique private Vec3d effectsNice$start;

    @Inject(method = "tick", at = @At("HEAD"))
    private void effectsNice$remember(CallbackInfo ci) {
        effectsNice$start = ((FireworkRocketEntity)(Object)this).getPos();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;"), cancellable = true)
    private void effectsNice$intercept(CallbackInfo ci) {
        var rocket = (FireworkRocketEntity)(Object)this;
        if (EffectServer.interceptProjectile(rocket, effectsNice$start, rocket.getPos().subtract(effectsNice$start))) ci.cancel();
    }
}
