package xp.dev.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xp.dev.effect.EffectServer;

@Mixin(ServerWorld.class)
public abstract class SphereProjectileMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void effectsNice$shield(Entity entity, CallbackInfo ci) {
        if (EffectServer.interceptProjectile(entity)) ci.cancel();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void effectsNice$shieldPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (EffectServer.interceptProjectile(passenger)) ci.cancel();
    }
}
