package xp.dev.client.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface EntityDispatcherAccess {
    @Accessor("renderShadows") boolean effectsNice$renderShadows();
}
