package xp.dev.effect;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import xp.dev.EffectsNice;

public record ConvertPayload(String dimension, int color, float progress, boolean active) implements CustomPayload {
    public static final Id<ConvertPayload> ID = new Id<>(EffectsNice.id("convert"));
    public static final PacketCodec<RegistryByteBuf, ConvertPayload> CODEC = PacketCodec.ofStatic(ConvertPayload::write, ConvertPayload::read);
    private static void write(RegistryByteBuf b, ConvertPayload p) {
        b.writeString(p.dimension, 256); b.writeInt(p.color); b.writeFloat(p.progress); b.writeBoolean(p.active);
    }
    private static ConvertPayload read(RegistryByteBuf b) {
        return new ConvertPayload(b.readString(256), b.readInt(), b.readFloat(), b.readBoolean());
    }
    public boolean valid() { return color >= 0 && color <= 0xffffff && Float.isFinite(progress) && progress >= 0 && progress <= 1; }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
