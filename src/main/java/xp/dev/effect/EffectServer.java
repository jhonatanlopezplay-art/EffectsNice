package xp.dev.effect;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import xp.dev.EffectsNice;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static net.minecraft.server.command.CommandManager.*;

public final class EffectServer {
    private static EffectServer current;
    private final MinecraftServer server;
    private final Path folder = FabricLoader.getInstance().getConfigDir().resolve("effects-nice/images");
    private final Map<String, Scene> scenes = new HashMap<>();
    private final Map<UUID, Viewer> viewers = new HashMap<>();
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Effects Nice images"); t.setDaemon(true); return t; });
    private volatile List<String> files = List.of();
    private long tick;
    private boolean closed;
    private static class Scene {
        final Animation sky = new Animation(), rift = new Animation(), convert = new Animation();
        final Animation sphere = new Animation();
        Vec3d sphereCenter = Vec3d.ZERO;
        float sphereRadius = 32;
        int convertColor;
        float skyYaw, skyPitch = -70, yaw, pitch = -40, width = 90, height = 28;
        Media.Asset asset;
        Future<?> loading;
        int revision;
    }
    private static class Viewer {
        String dimension = "", hash = "";
        int offset;
    }
    private EffectServer(MinecraftServer server) {
        this.server = server;
        try { Files.createDirectories(folder); refreshFiles(); }
        catch (IOException e) { EffectsNice.LOGGER.error("No se pudo crear {}", folder, e); }
    }
    private void refreshFiles() throws IOException {
        try (var stream = Files.list(folder)) {
            files = stream.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).filter(Media::allowed).sorted().toList();
        }
    }
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> current = new EffectServer(s));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            if (current != null) { current.closed = true; current.loader.shutdownNow(); current = null; }
        });
        ServerTickEvents.END_SERVER_TICK.register(s -> { if (current != null) current.tick(); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> { if (current != null) current.viewers.remove(handler.player.getUuid()); });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
            literal("efnice").requires(source -> source.hasPermissionLevel(2))
                .then(literal("esfera")
                    .then(literal("stop").executes(c -> sphere(c.getSource(), 32, false)))
                    .then(argument("radio", FloatArgumentType.floatArg(SphereGeometry.MIN_RADIUS, SphereGeometry.MAX_RADIUS))
                        .then(literal("start").executes(c -> sphere(c.getSource(), FloatArgumentType.getFloat(c, "radio"), true)))
                        .then(literal("stop").executes(c -> sphere(c.getSource(), FloatArgumentType.getFloat(c, "radio"), false)))))
                .then(literal("convert")
                    .then(literal("stop").executes(c -> convert(c.getSource(), "black", false)))
                    .then(argument("color", StringArgumentType.word())
                        .suggests((c, b) -> {
                            ConvertColors.names().stream().filter(n -> n.startsWith(b.getRemainingLowerCase())).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(literal("start").executes(c -> convert(c.getSource(), StringArgumentType.getString(c, "color"), true)))
                        .then(literal("stop").executes(c -> convert(c.getSource(), StringArgumentType.getString(c, "color"), false)))))
                .then(literal("skys")
                    .then(literal("start").executes(c -> sky(c.getSource(), true)))
                    .then(literal("stop").executes(c -> sky(c.getSource(), false))))
                .then(literal("grieta")
                    .then(literal("stop").executes(c -> stop(c.getSource())))
                    .then(literal("list").executes(c -> list(c.getSource())))
                    .then(argument("imagen", StringArgumentType.string())
                        .suggests((c, b) -> {
                            if (current != null) current.files.stream().filter(n -> n.startsWith(b.getRemaining())).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(argument("largo", FloatArgumentType.floatArg(10, 150))
                            .then(argument("alto", FloatArgumentType.floatArg(2, 80))
                                .then(literal("start").executes(EffectServer::start))
                                .then(literal("stop").executes(c -> stop(c.getSource())))))))));
    }
    private Scene scene(ServerCommandSource source) { return scenes.computeIfAbsent(source.getWorld().getRegistryKey().getValue().toString(), k -> new Scene()); }
    private static int sphere(ServerCommandSource source, float radius, boolean active) {
        if (current == null || !Float.isFinite(radius)) return 0;
        Scene scene = current.scene(source);
        if (active) {
            Vec3d position = source.getPosition();
            if (Math.abs(position.x) > 30_000_000 || Math.abs(position.y) > 30_000_000 || Math.abs(position.z) > 30_000_000) {
                source.sendError(Text.literal("El centro de la esfera está fuera de los límites del mundo."));
                return 0;
            }
            scene.sphereCenter = position;
            scene.sphereRadius = radius;
        }
        scene.sphere.target(active, current.tick, SphereGeometry.FADE_TICKS);
        current.sync();
        source.sendFeedback(() -> Text.literal(active
                ? "Esfera protectora activada: radio " + radius + " bloques. Absorbe proyectiles; los jugadores pueden atravesarla."
                : "Esfera desactivada: retirando la capa de protección."), true);
        return 1;
    }

    public static boolean interceptProjectile(Entity entity) {
        return interceptProjectile(entity, entity.getPos(), entity.getVelocity());
    }

    public static boolean interceptProjectile(Entity entity, Vec3d position, Vec3d velocity) {
        if (current == null || entity.getWorld().isClient() || !(entity instanceof ProjectileEntity) || entity.isRemoved()) return false;
        Scene scene = current.scenes.get(entity.getWorld().getRegistryKey().getValue().toString());
        if (scene == null || !scene.sphere.active()) return false;
        Vec3d relative = position.add(0, entity.getHeight()*.5, 0).subtract(scene.sphereCenter);
        double hit = SphereGeometry.contact(relative.x, relative.y, relative.z,
                velocity.x, velocity.y, velocity.z, scene.sphereRadius, Math.max(entity.getWidth(), entity.getHeight()) * .5 + .15);
        if (!Double.isFinite(hit)) return false;
        Vec3d impact = position.add(velocity.multiply(hit));
        entity.discard();
        if (entity.getWorld() instanceof ServerWorld world)
            world.spawnParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 6, .16, .16, .16, .025);
        return true;
    }
    private static int convert(ServerCommandSource source, String name, boolean active) {
        if (current == null) return 0;
        OptionalInt color = ConvertColors.find(name);
        if (color.isEmpty()) {
            source.sendError(Text.literal("Color desconocido. Usa black/negro, verde, rojo, azul, blanco, amarillo, naranja, morado, rosa, cian, magenta, gris o marron."));
            return 0;
        }
        Scene scene = current.scene(source);
        if (active) scene.convertColor = color.getAsInt();
        scene.convert.target(active, current.tick, Animation.CONVERT_TICKS);
        current.sync();
        source.sendFeedback(() -> Text.literal(active ? "Convert: mundo a " + name + ". Jugadores e ítems conservan sus colores." : "Convert: restaurando los colores del mundo."), true);
        return 1;
    }
    private static int sky(ServerCommandSource source, boolean active) {
        if (current == null) return 0;
        Scene s = current.scene(source);
        if (active && s.sky.at(current.tick, Animation.SKY_TICKS) == 0) {
            s.skyYaw = source.getRotation().y;
            s.skyPitch = Math.clamp(source.getRotation().x, -85, -20);
        }
        s.sky.target(active, current.tick, Animation.SKY_TICKS);
        current.sync();
        source.sendFeedback(() -> Text.literal(active ? "Cielo carmesí: expansión iniciada (8 s)." : "Cielo carmesí: retirada iniciada."), true);
        return 1;
    }
    private static int stop(ServerCommandSource source) {
        if (current == null) return 0;
        Scene s = current.scene(source); s.revision++;
        if (s.loading != null) { s.loading.cancel(true); s.loading = null; }
        s.rift.target(false, current.tick, Animation.RIFT_TICKS); current.sync();
        source.sendFeedback(() -> Text.literal("Grieta: cerrando la abertura y retirando la fractura."), true);
        return 1;
    }
    private static int list(ServerCommandSource source) {
        if (current == null) return 0;
        try { current.refreshFiles(); }
        catch (IOException e) { source.sendError(Text.literal("No se pudo leer la carpeta de imágenes.")); return 0; }
        String message = current.files.isEmpty() ? "Añade PNG/JPG/GIF a config/effects-nice/images/." : "Imágenes: " + String.join(", ", current.files);
        source.sendFeedback(() -> Text.literal(message), false); return current.files.size();
    }
    private static int start(CommandContext<ServerCommandSource> context) {
        if (current == null) return 0;
        EffectServer owner = current;
        ServerCommandSource source = context.getSource(); Scene s = owner.scene(source);
        if (s.rift.at(owner.tick, Animation.RIFT_TICKS) > 0) {
            source.sendError(Text.literal("Ya hay una grieta. Usa /efnice grieta stop y espera a que desaparezca.")); return 0;
        }
        int revision = ++s.revision;
        if (s.loading != null) s.loading.cancel(true);
        String name = StringArgumentType.getString(context, "imagen");
        float width = FloatArgumentType.getFloat(context, "largo"), height = FloatArgumentType.getFloat(context, "alto");
        float yaw = source.getRotation().y, pitch = Math.clamp(source.getRotation().x, -80, -20);
        source.sendFeedback(() -> Text.literal("Cargando " + name + "…"), false);
        s.loading = owner.loader.submit(() -> {
            try {
                Media.Asset asset = Media.load(owner.folder, name);
                owner.server.execute(() -> {
                    if (owner.closed || s.revision != revision) return;
                    s.loading = null;
                    s.asset = asset; s.width = width; s.height = height; s.yaw = yaw; s.pitch = pitch;
                    s.rift.target(true, owner.tick, Animation.RIFT_TICKS); owner.sync();
                    source.sendFeedback(() -> Text.literal("Grieta iniciada: fractura 3 s → apertura 2,5 s. " + width + "° × " + height + "°."), true);
                });
            } catch (Exception e) {
                owner.server.execute(() -> {
                    if (!owner.closed && s.revision == revision) {
                        s.loading = null;
                        source.sendError(Text.literal("No se pudo cargar " + name + ": " + e.getMessage()));
                    }
                });
            }
        });
        return 1;
    }
    private void tick() {
        tick++;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) update(player, tick % 20 == 0);
        for (Scene scene : scenes.values()) if (!scene.rift.active() && scene.rift.at(tick, Animation.RIFT_TICKS) == 0) scene.asset = null;
    }
    private void sync() { for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) update(p, true); }
    private void update(ServerPlayerEntity player, boolean send) {
        if (!ServerPlayNetworking.canSend(player, EffectPayload.ID) || !ServerPlayNetworking.canSend(player, ImagePayload.ID)) return;
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        Viewer viewer = viewers.computeIfAbsent(player.getUuid(), k -> new Viewer());
        if (!viewer.dimension.equals(dimension)) { viewer.dimension = dimension; viewer.hash = ""; viewer.offset = 0; send = true; }
        Scene s = scenes.computeIfAbsent(dimension, k -> new Scene());
        String hash = s.asset == null ? "" : s.asset.hash();
        if (!viewer.hash.equals(hash)) { viewer.hash = hash; viewer.offset = 0; send = true; }
        if (send && ServerPlayNetworking.canSend(player, SpherePayload.ID)) {
            ServerPlayNetworking.send(player, new SpherePayload(dimension,
                    s.sphereCenter.x, s.sphereCenter.y, s.sphereCenter.z, s.sphereRadius,
                    s.sphere.at(tick, SphereGeometry.FADE_TICKS), s.sphere.active()));
        }
        if (send && ServerPlayNetworking.canSend(player, ConvertPayload.ID)) {
            ServerPlayNetworking.send(player, new ConvertPayload(dimension, s.convertColor,
                    s.convert.at(tick, Animation.CONVERT_TICKS), s.convert.active()));
        }
        if (send) ServerPlayNetworking.send(player, new EffectPayload(dimension,
                s.sky.at(tick, Animation.SKY_TICKS), s.sky.active(), s.skyYaw, s.skyPitch,
                s.rift.at(tick, Animation.RIFT_TICKS), s.rift.active(), hash, s.width, s.height, s.yaw, s.pitch, tick));
        if (s.asset != null && viewer.offset < s.asset.bytes().length) {
            for (int i = 0; i < 8 && viewer.offset < s.asset.bytes().length; i++) {
                int end = Math.min(s.asset.bytes().length, viewer.offset + ImagePayload.CHUNK);
                ServerPlayNetworking.send(player, new ImagePayload(hash, s.asset.bytes().length, viewer.offset, Arrays.copyOfRange(s.asset.bytes(), viewer.offset, end)));
                viewer.offset = end;
            }
        }

    }
}
