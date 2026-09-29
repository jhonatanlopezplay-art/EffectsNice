package xp.dev.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import xp.dev.effect.*;
import java.util.*;

public final class SphereRenderer {
    public static ShaderProgram shader;
    private static final int MAX_OPENINGS = 32;
    private static SpherePayload state;
    private static long receivedAt;
    private static int detail;
    private record Panel(SphereMesh.Point center, float[] vertices) {}
    private static List<Panel> cells = List.of();
    private SphereRenderer() {}

    public static void accept(SpherePayload payload) {
        if (!payload.valid()) return;
        state = payload;
        receivedAt = System.nanoTime();
    }
    public static void reset() { state = null; cells = List.of(); detail = 0; }

    public static void render(WorldRenderContext context) {
        SpherePayload s = state;
        if (s == null || shader == null || !s.dimension().equals(context.world().getRegistryKey().getValue().toString())) return;
        float progress = Animation.smooth(Animation.sample(s.progress(), s.active(),
                Math.min(30, (System.nanoTime()-receivedAt)/50_000_000.0), SphereGeometry.FADE_TICKS));
        if (progress <= 0) return;
        float radius = s.radius();
        Vec3d center = new Vec3d(s.x(), s.y(), s.z());
        if (context.frustum() != null && !context.frustum().isVisible(new Box(
                s.x()-radius, s.y()-radius, s.z()-radius, s.x()+radius, s.y()+radius, s.z()+radius))) return;
        int wantedDetail = radius <= 8 ? 1 : radius <= 24 ? 2 : radius <= 64 ? 3 : 4;
        if (wantedDetail != detail) { cells = prepareMesh(wantedDetail); detail = wantedDetail; }
        Vec3d camera = context.camera().getPos(), offset = center.subtract(camera);
        SphereMesh.Point eye = new SphereMesh.Point(-offset.x/radius, -offset.y/radius, -offset.z/radius);
        List<Panel> sorted = new ArrayList<>(cells);
        sorted.sort(Comparator.comparingDouble(cell -> cell.center().dot(eye)));
        ShaderProgram previous = RenderSystem.getShader();
        try {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(false);
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            shader.getUniformOrDefault("ViewRotation").set(context.positionMatrix());
            shader.getUniformOrDefault("ClipProjection").set(context.projectionMatrix());
            shader.getUniformOrDefault("Offset").set((float)offset.x, (float)offset.y, (float)offset.z);
            shader.getUniformOrDefault("Eye").set((float)-offset.x, (float)-offset.y, (float)-offset.z);
            shader.getUniformOrDefault("Opacity").set(progress);
            shader.getUniformOrDefault("Time").set((context.world().getTime() % 24000 + context.tickCounter().getTickDelta(false))/20f);
            openings(context, center, radius);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
            for (Panel cell : sorted) {
                float[] v = cell.vertices();
                for (int i = 0; i < v.length; i += 5)
                    buffer.vertex(v[i]*radius, v[i+1]*radius, v[i+2]*radius).texture(v[i+3]*radius,v[i+4]);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.depthMask(true); RenderSystem.enableCull();
            RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> previous);
        }
    }

    private static void openings(WorldRenderContext context, Vec3d center, float radius) {
        float delta = context.tickCounter().getTickDelta(false);
        List<? extends PlayerEntity> players = context.world().getPlayers().stream()
                .filter(p -> !p.isSpectator() && p.isAlive())
                .filter(p -> Math.abs(p.getPos().add(0,p.getHeight()*.5,0).distanceTo(center)-radius) < p.getHeight()+2)
                .sorted(Comparator.comparingDouble(p -> p.squaredDistanceTo(context.camera().getPos())))
                .limit(MAX_OPENINGS).toList();
        for (int i = 0; i < MAX_OPENINGS; i++) {
            if (i < players.size()) {
                PlayerEntity player = players.get(i);
                Vec3d p = player.getLerpedPos(delta).add(0, player.getHeight()*.5, 0).subtract(center);
                shader.getUniformOrDefault("Hole"+i).set((float)p.x, (float)p.y, (float)p.z, Math.max(.1f, player.getHeight()*.5f-.25f));
            } else shader.getUniformOrDefault("Hole"+i).set(0f,0f,0f,-1f);
        }
    }
    private static List<Panel> prepareMesh(int level) {
        List<Panel> result = new ArrayList<>();
        for (SphereMesh.Cell cell : SphereMesh.create(level)) {
            float[] vertices = new float[cell.corners().size()*15];
            float seed = (float)(cell.center().x()*13 + cell.center().y()*31 + cell.center().z()*7);
            int cursor = 0;
            for (int i = 0; i < cell.corners().size(); i++) {
                var a = cell.corners().get(i);
                var b = cell.corners().get((i+1)%cell.corners().size());
                float edge = (float)(cell.center().subtract(a).cross(b.subtract(a)).length()/b.subtract(a).length());
                for (var p : List.of(cell.center(), a, b)) {
                    vertices[cursor++] = (float)p.x(); vertices[cursor++] = (float)p.y(); vertices[cursor++] = (float)p.z();
                    vertices[cursor++] = p == cell.center() ? edge : 0;
                    vertices[cursor++] = seed;
                }
            }
            result.add(new Panel(cell.center(), vertices));
        }
        return List.copyOf(result);
    }
}
