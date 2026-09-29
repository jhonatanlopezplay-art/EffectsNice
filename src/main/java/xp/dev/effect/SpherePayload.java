package xp.dev.effect;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xp.dev.EffectsNice;

public record SpherePayload(String dimension, double x, double y, double z,
                            float radius, float progress, boolean active) implements CustomPayload {
    public static final Id<SpherePayload> ID = new Id<>(EffectsNice.id("sphere"));
    public static final PacketCodec<RegistryByteBuf, SpherePayload> CODEC = PacketCodec.ofStatic(SpherePayload::write, SpherePayload::read);
    private static void write(RegistryByteBuf b, SpherePayload p) {
        b.writeString(p.dimension, 256);
        b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
        b.writeFloat(p.radius); b.writeFloat(p.progress); b.writeBoolean(p.active);
    }
    private static SpherePayload read(RegistryByteBuf b) {
        return new SpherePayload(b.readString(256), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readFloat(), b.readFloat(), b.readBoolean());
    }
    public boolean valid() {
        return dimension != null && !dimension.isBlank() && dimension.length() <= 256
                && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) <= 30_000_000 && Math.abs(y) <= 30_000_000 && Math.abs(z) <= 30_000_000
                && Float.isFinite(radius) && radius >= SphereGeometry.MIN_RADIUS && radius <= SphereGeometry.MAX_RADIUS
                && Float.isFinite(progress) && progress >= 0 && progress <= 1;
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
