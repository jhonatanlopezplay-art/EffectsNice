package xp.dev.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import xp.dev.client.mixin.EntityDispatcherAccess;
import xp.dev.effect.Animation;
import xp.dev.effect.ConvertPayload;
import java.util.ArrayList;
import java.util.List;

public final class ConvertRenderer {
    public static ShaderProgram shader;
    private static ConvertPayload state;
    private static long receivedAt;
    private static SimpleFramebuffer scene, mask;
    private static final List<Runnable> excluded = new ArrayList<>();
    private static boolean worldPass, capturing, excludedEntity;
    private static float frameAmount;
    private static int frameColor;
    private ConvertRenderer() {}

    public static void accept(ConvertPayload payload) {
        if (!payload.valid()) return;
        state = payload; receivedAt = System.nanoTime();
    }
    private static float amount() {
        var world = MinecraftClient.getInstance().world;
        if (state == null || world == null || !state.dimension().equals(world.getRegistryKey().getValue().toString())) return 0;
        return Animation.smooth(Animation.sample(state.progress(), state.active(),
                Math.min(30, (System.nanoTime()-receivedAt)/50_000_000.0), Animation.CONVERT_TICKS));
    }
    public static void beginWorld() {
        excluded.clear(); capturing = false; excludedEntity = false;
        frameAmount = shader == null ? 0 : amount();
        frameColor = state == null ? 0 : state.color();
        worldPass = frameAmount > 0;
        if (!worldPass && scene != null) { releaseBuffers(); MinecraftClient.getInstance().getFramebuffer().beginWrite(false); }
    }
    public static boolean collecting() { return worldPass && !capturing; }
    public static boolean collectingItem() { return collecting() && !excludedEntity; }
    public static void inExcludedEntity(boolean value) { if (!capturing) excludedEntity = value; }
    public static void preserve(Runnable draw) { excluded.add(draw); }
    public static Framebuffer redirectedTarget() { return capturing ? mask : null; }
    public static VertexConsumerProvider.Immediate consumers() { return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(); }
    public static MatrixStack snapshot(MatrixStack original) {
        MatrixStack copy = new MatrixStack();
        copy.peek().getPositionMatrix().set(original.peek().getPositionMatrix());
        copy.peek().getNormalMatrix().set(original.peek().getNormalMatrix());
        return copy;
    }
    public static void finishWorld(Matrix4f view) {
        if (!worldPass) return;
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        ShaderProgram oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        int oldTexture0 = RenderSystem.getShaderTexture(0), oldTexture1 = RenderSystem.getShaderTexture(1);
        var dispatcher = client.getEntityRenderDispatcher();
        boolean oldShadows = ((EntityDispatcherAccess)dispatcher).effectsNice$renderShadows();
        boolean oldHitboxes = dispatcher.shouldRenderHitboxes();
        try {
            allocate(main.textureWidth, main.textureHeight);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.fbo);
            GL30.glBlitFramebuffer(0,0,main.textureWidth,main.textureHeight,0,0,scene.textureWidth,scene.textureHeight,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
            mask.setClearColor(0,0,0,0); mask.clear(MinecraftClient.IS_SYSTEM_MAC);
            mask.copyDepthFrom(main); mask.beginWrite(true);
            capturing = true;
            dispatcher.setRenderShadows(false); dispatcher.setRenderHitboxes(false);
            RenderSystem.getModelViewStack().pushMatrix().mul(view); RenderSystem.applyModelViewMatrix();
            try {
                RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(true);
                client.gameRenderer.getLightmapTextureManager().enable();
                for (Runnable draw : excluded) draw.run();
                consumers().draw();
            } finally {
                RenderSystem.getModelViewStack().popMatrix(); RenderSystem.applyModelViewMatrix();
                capturing = false;
            }
            main.beginWrite(true);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableBlend(); RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, scene.getColorAttachment());
            RenderSystem.setShaderTexture(1, mask.getColorAttachment());
            shader.getUniformOrDefault("Amount").set(frameAmount);
            shader.getUniformOrDefault("Tint").set(((frameColor>>16)&255)/255f,((frameColor>>8)&255)/255f,(frameColor&255)/255f);
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            b.vertex(-1,-1,0).texture(0,0); b.vertex(1,-1,0).texture(1,0);
            b.vertex(1,1,0).texture(1,1); b.vertex(-1,1,0).texture(0,1);
            BufferRenderer.drawWithGlobalProgram(b.end());
        } finally {
            capturing = false; worldPass = false; excludedEntity = false; excluded.clear();
            dispatcher.setRenderShadows(oldShadows); dispatcher.setRenderHitboxes(oldHitboxes);
            main.beginWrite(true);
            RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableCull(); RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(oldColor[0],oldColor[1],oldColor[2],oldColor[3]);
            RenderSystem.setShaderTexture(0,oldTexture0); RenderSystem.setShaderTexture(1,oldTexture1);
            RenderSystem.setShader(() -> oldShader);
        }
    }
    private static void allocate(int width, int height) {
        if (scene != null && scene.textureWidth == width && scene.textureHeight == height) return;
        releaseBuffers();
        scene = new SimpleFramebuffer(width,height,false,MinecraftClient.IS_SYSTEM_MAC);
        mask = new SimpleFramebuffer(width,height,true,MinecraftClient.IS_SYSTEM_MAC);
    }
    private static void releaseBuffers() {
        if (scene != null) { scene.delete(); scene = null; }
        if (mask != null) { mask.delete(); mask = null; }
    }
    public static void reset() {
        state = null; worldPass = false; capturing = false; excludedEntity = false; excluded.clear(); releaseBuffers();
    }
}
