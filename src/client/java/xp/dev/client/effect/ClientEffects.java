package xp.dev.client.effect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xp.dev.EffectsNice;
import xp.dev.effect.*;
import java.util.concurrent.*;

public final class ClientEffects {
    private static final Identifier TEXTURE = EffectsNice.id("dynamic/rift");
    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Effects Nice GIF decoder"); t.setDaemon(true); return t; });
    private static EffectPayload state;
    private static long receivedAt;
    private static String receivingHash = "", readyHash = "";
    private static byte[] receiving;
    private static int receivedBytes, generation, frameIndex = -1;
    private static Future<?> decoding;
    private static Media.Decoded media;
    private static NativeImageBackedTexture texture;
    private static boolean reported;
    private static float shownOpening;
    private static long openingFrameTime;
    private ClientEffects() {}
    public static void accept(EffectPayload payload) {
        if (!valid(payload)) return;
        if (state == null || !state.dimension().equals(payload.dimension()) || !state.image().equals(payload.image())) clearMedia();
        state = payload; receivedAt = System.nanoTime();
    }
    private static boolean valid(EffectPayload p) {
        return Float.isFinite(p.sky()) && p.sky() >= 0 && p.sky() <= 1 && Float.isFinite(p.rift()) && p.rift() >= 0 && p.rift() <= 1
            && Float.isFinite(p.yaw()) && Float.isFinite(p.pitch()) && Float.isFinite(p.skyYaw()) && Float.isFinite(p.skyPitch())
            && Float.isFinite(p.width()) && p.width() >= 10 && p.width() <= 150 && Float.isFinite(p.height()) && p.height() >= 2 && p.height() <= 80
            && (p.image().isEmpty() || p.image().matches("[a-f0-9]{64}"));
    }
    public static void accept(ImagePayload p) {
        // Only the image announced by the current scene may allocate or replace media.
        if (state == null || state.image().isEmpty() || !state.image().equals(p.hash()) || readyHash.equals(p.hash())) return;
        if (!p.hash().matches("[a-f0-9]{64}") || p.total() < 1 || p.total() > Media.MAX_BYTES || p.offset() < 0
                || p.bytes().length == 0 || p.bytes().length > ImagePayload.CHUNK || (long)p.offset() + p.bytes().length > p.total()) return;
        if (p.offset() == 0) {
            if (receivingHash.equals(p.hash())) return;
            clearMedia(); receivingHash = p.hash(); receiving = new byte[p.total()]; receivedBytes = 0;
        }
        if (receiving == null || !receivingHash.equals(p.hash()) || receiving.length != p.total() || p.offset() != receivedBytes) return;
        System.arraycopy(p.bytes(), 0, receiving, receivedBytes, p.bytes().length); receivedBytes += p.bytes().length;
        if (receivedBytes != receiving.length) return;
        byte[] bytes = receiving; receiving = null;
        String hash = receivingHash; int token = generation;
        decoding = DECODER.submit(() -> {
            try {
                if (!Media.hash(bytes).equals(hash)) throw new IllegalArgumentException("La transferencia no coincide con la imagen.");
                Media.Decoded decoded = Media.decode(bytes);
                MinecraftClient.getInstance().execute(() -> {
                    if (token != generation) return;
                    media = decoded; readyHash = hash; frameIndex = -1;
                    texture = new NativeImageBackedTexture(new NativeImage(decoded.width(), decoded.height(), false));
                    MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, texture);
                    upload(0);
                });
            } catch (java.io.InterruptedIOException e) {
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> { if (token == generation) error("No se pudo abrir la imagen de la grieta", e); });
            }
        });
    }
    public static EffectPayload state() {
        var world = MinecraftClient.getInstance().world;
        return state != null && world != null && world.getRegistryKey().getValue().toString().equals(state.dimension()) ? state : null;
    }
    public static float phase(boolean sky) {
        var s = state(); if (s == null) return 0;
        double elapsed = Math.min(30, (System.nanoTime() - receivedAt) / 50_000_000.0);
        return Animation.sample(sky ? s.sky() : s.rift(), sky ? s.skyActive() : s.riftActive(), elapsed, sky ? Animation.SKY_TICKS : Animation.RIFT_TICKS);
    }
    public static float time() { return state == null ? 0 : (state.clock() + (System.nanoTime() - receivedAt) / 50_000_000.0f) / 20f; }
    public static Identifier image() {
        var s = state();
        if (s == null || media == null || !readyHash.equals(s.image())) return null;
        if (media.frames().size() > 1) {
            long total = media.frames().stream().mapToLong(Media.Frame::millis).sum();
            long cursor = (long)(time() * 1000) % total;
            int index = 0;
            while (index < media.frames().size() - 1 && cursor >= media.frames().get(index).millis()) cursor -= media.frames().get(index++).millis();
            if (index != frameIndex) upload(index);
        }
        return TEXTURE;
    }
    public static float opening() {
        var s = state();
        long now = System.nanoTime();
        float delta = openingFrameTime == 0 ? 0 : Math.min(0.1f, (now - openingFrameTime) / 1_000_000_000f);
        openingFrameTime = now;
        if (s == null || media == null || !readyHash.equals(s.image())) { shownOpening = 0; return 0; }
        float desired = Math.clamp((phase(false) * Animation.RIFT_TICKS - Animation.FRACTURE_TICKS) / Animation.OPEN_TICKS, 0, 1);
        float step = delta * 20 / Animation.OPEN_TICKS;
        if (s.riftActive()) shownOpening = Math.min(desired, shownOpening + step);
        else shownOpening = Math.max(0, Math.min(desired, shownOpening - step));
        return Animation.smooth(shownOpening);
    }
    public static float aspect() { return media == null ? 1 : (float)media.width() / media.height(); }
    private static void upload(int index) {
        int[] pixels = media.frames().get(index).argb(); NativeImage image = texture.getImage();
        for (int y = 0; y < media.height(); y++) for (int x = 0; x < media.width(); x++) {
            int c = pixels[y * media.width() + x];
            image.setColor(x, y, (c & 0xff00ff00) | ((c >> 16) & 255) | ((c & 255) << 16));
        }
        texture.upload(); texture.setFilter(true, false); frameIndex = index;
    }
    private static void clearMedia() {
        generation++; shownOpening = 0; openingFrameTime = 0;
        if (decoding != null) { decoding.cancel(true); decoding = null; }
        if (texture != null) { MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE); texture = null; }
        receiving = null; media = null; receivingHash = ""; readyHash = ""; receivedBytes = 0;
    }
    public static void reset() { clearMedia(); state = null; reported = false; }
    public static void error(String message, Exception e) {
        EffectsNice.LOGGER.error(message, e);
        var player = MinecraftClient.getInstance().player;
        if (!reported && player != null) { player.sendMessage(Text.literal("[Effects Nice] " + message + ". Revisa el registro."), false); reported = true; }
    }
}
