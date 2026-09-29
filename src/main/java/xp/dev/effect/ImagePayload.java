package xp.dev.effect;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xp.dev.EffectsNice;

public record ImagePayload(String hash, int total, int offset, byte[] bytes) implements CustomPayload {
    public static final int CHUNK = 24 * 1024;
    public static final Id<ImagePayload> ID = new Id<>(EffectsNice.id("image"));
    public static final PacketCodec<RegistryByteBuf, ImagePayload> CODEC = PacketCodec.ofStatic(ImagePayload::write, ImagePayload::read);
    private static void write(RegistryByteBuf b, ImagePayload p) {
        b.writeString(p.hash, 64); b.writeVarInt(p.total); b.writeVarInt(p.offset); b.writeByteArray(p.bytes);
    }
    private static ImagePayload read(RegistryByteBuf b) {
        return new ImagePayload(b.readString(64), b.readVarInt(), b.readVarInt(), b.readByteArray(CHUNK));
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
