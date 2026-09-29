package xp.dev.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xp.dev.effect.Animation;

public final class SkyRenderer {
    public static ShaderProgram storm, rift;
    private static final float[][] CORNERS = {{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
    public static void render(Matrix4f view, Matrix4f projection, Camera camera) {
        var state = ClientEffects.state();
        if (state == null || camera.getSubmersionType() != CameraSubmersionType.NONE) return;
        if (camera.getFocusedEntity() instanceof net.minecraft.entity.LivingEntity living &&
                (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS))) return;
        float skyPhase = ClientEffects.phase(true), riftPhase = ClientEffects.phase(false);
        if (skyPhase <= 0 && riftPhase <= 0) return;
        ShaderProgram previous = RenderSystem.getShader();
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        RenderSystem.depthMask(false); RenderSystem.disableDepthTest();
        Matrix4f rotation = new Matrix4f(view).m30(0).m31(0).m32(0);
        try {
            if (skyPhase > 0 && storm != null) {
                setup(storm, rotation, projection);
                storm.getUniformOrDefault("Progress").set(skyPhase);
                Vector3f center = direction(state.skyYaw(), state.skyPitch());
                storm.getUniformOrDefault("Origin").set(center.x, center.y, center.z);
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                for (int[] face : FACES) for (int corner : face) { float[] v = CORNERS[corner]; b.vertex(v[0] * 100, v[1] * 100, v[2] * 100); }
                BufferRenderer.drawWithGlobalProgram(b.end());
            }
            if (riftPhase > 0 && rift != null) {
                setup(rift, rotation, projection);
                Identifier image = ClientEffects.image();
                rift.getUniformOrDefault("Fracture").set(Animation.fracture(riftPhase));
                rift.getUniformOrDefault("Opening").set(ClientEffects.opening());
                rift.getUniformOrDefault("ImageAspect").set(ClientEffects.aspect());
                float halfWidth = (float)Math.tan(Math.toRadians(state.width() * .5f)) * 100;
                float halfHeight = (float)Math.tan(Math.toRadians(state.height() * .5f)) * 100;
                rift.getUniformOrDefault("RiftAspect").set(halfWidth / halfHeight);
                if (image != null) RenderSystem.setShaderTexture(0, image);
                Vector3f center = direction(state.yaw(), state.pitch()).mul(100);
                Vector3f right = new Vector3f((float)Math.cos(Math.toRadians(state.yaw())), 0, (float)Math.sin(Math.toRadians(state.yaw())));
                Vector3f up = new Vector3f(center).normalize().cross(right).normalize();
                right.mul(halfWidth * 1.18f); up.mul(halfHeight * 1.3f);
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                vertex(b, center, right, up, -1, -1, 0, 1);
                vertex(b, center, right, up, 1, -1, 1, 1);
                vertex(b, center, right, up, 1, 1, 1, 0);
                vertex(b, center, right, up, -1, 1, 0, 0);
                BufferRenderer.drawWithGlobalProgram(b.end());
            }
        } finally {
            RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.setShader(() -> previous);
        }
    }
    private static void setup(ShaderProgram shader, Matrix4f view, Matrix4f projection) {
        RenderSystem.setShader(() -> shader);
        shader.getUniformOrDefault("ViewRotation").set(view);
        shader.getUniformOrDefault("ClipProjection").set(projection);
        shader.getUniformOrDefault("Time").set(ClientEffects.time());
    }
    private static Vector3f direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f((float)(-Math.sin(y)*Math.cos(p)), (float)-Math.sin(p), (float)(Math.cos(y)*Math.cos(p)));
    }
    private static void vertex(BufferBuilder b, Vector3f center, Vector3f right, Vector3f up, float x, float y, float u, float v) {
        Vector3f point = new Vector3f(center).fma(x, right).fma(y, up);
        b.vertex(point.x, point.y, point.z).texture(u, v);
    }
}
