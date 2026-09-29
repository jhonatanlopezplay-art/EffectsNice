package xp.dev.effect;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xp.dev.EffectsNice;

public record EffectPayload(String dimension, float sky, boolean skyActive, float skyYaw, float skyPitch,
                            float rift, boolean riftActive, String image, float width, float height,
                            float yaw, float pitch, long clock) implements CustomPayload {
    public static final Id<EffectPayload> ID = new Id<>(EffectsNice.id("state"));
    public static final PacketCodec<RegistryByteBuf, EffectPayload> CODEC = PacketCodec.ofStatic(EffectPayload::write, EffectPayload::read);
    private static void write(RegistryByteBuf b, EffectPayload p) {
        b.writeString(p.dimension, 256); b.writeFloat(p.sky); b.writeBoolean(p.skyActive);
        b.writeFloat(p.skyYaw); b.writeFloat(p.skyPitch); b.writeFloat(p.rift); b.writeBoolean(p.riftActive);
        b.writeString(p.image, 64); b.writeFloat(p.width); b.writeFloat(p.height);
        b.writeFloat(p.yaw); b.writeFloat(p.pitch); b.writeLong(p.clock);
    }
    private static EffectPayload read(RegistryByteBuf b) {
        return new EffectPayload(b.readString(256), b.readFloat(), b.readBoolean(), b.readFloat(), b.readFloat(),
                b.readFloat(), b.readBoolean(), b.readString(64), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readLong());
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
