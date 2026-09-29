package xp.dev.effect;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.Node;

public final class Media {
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final int MAX_SIDE = 2048;
    public static final int MAX_FRAMES = 240;
    public static final long MAX_PIXELS = 32L * 1024 * 1024;
    public record Frame(int[] argb, int millis) {}
    public record Decoded(int width, int height, List<Frame> frames) {}
    public record Asset(String hash, byte[] bytes) {}
    public static boolean allowed(String name) {
        return name.matches("[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.(?i:png|jpg|jpeg|gif)");
    }
    public static Asset load(Path folder, String name) throws Exception {
        if (!allowed(name)) throw new IOException("Nombre inválido: usa letras, números, guiones y extensión PNG/JPG/GIF.");
        Path root = folder.toRealPath();
        Path file = root.resolve(name).toRealPath();
        if (!file.getParent().equals(root) || !Files.isRegularFile(file)) throw new IOException("Imagen fuera de la carpeta permitida.");
        if (Files.size(file) > MAX_BYTES) throw new IOException("Máximo 8 MiB por imagen.");
        byte[] bytes;
        try (InputStream in = Files.newInputStream(file)) { bytes = in.readNBytes(MAX_BYTES + 1); }
        if (bytes.length > MAX_BYTES) throw new IOException("Máximo 8 MiB por imagen.");
        decode(bytes); // Validate before broadcasting; no AWT/client classes from Minecraft involved.
        return new Asset(hash(bytes), bytes);
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static Decoded decode(byte[] bytes) throws IOException {
        checkInterrupted();
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IOException("Tamaño de archivo inválido.");
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw new IOException("Imagen PNG/JPG/GIF ilegible.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpeg", "jpg", "gif").contains(format))
                    throw new IOException("Formato no admitido: usa PNG, JPG o GIF.");
                boolean gif = format.equals("gif");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (gif) {
                    Node screen = child(reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
                    width = integer(screen, "logicalScreenWidth", width); height = integer(screen, "logicalScreenHeight", height);
                }
                checkSize(width, height);
                BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                List<Frame> frames = new ArrayList<>();
                for (int index = 0; ; index++) {
                    checkInterrupted();
                    try { reader.getWidth(index); } catch (IndexOutOfBoundsException end) { break; }
                    if (index >= MAX_FRAMES || (long) width * height * (index + 1) > MAX_PIXELS)
                        throw new IOException("GIF demasiado grande: máximo 240 cuadros y 32 millones de píxeles acumulados.");
                    checkSize(reader.getWidth(index), reader.getHeight(index));
                    int left = 0, top = 0, delay = 100; String disposal = "none";
                    if (gif) {
                        Node meta = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
                        Node desc = child(meta, "ImageDescriptor"), control = child(meta, "GraphicControlExtension");
                        left = integer(desc, "imageLeftPosition", 0); top = integer(desc, "imageTopPosition", 0);
                        delay = Math.max(20, integer(control, "delayTime", 10) * 10);
                        disposal = attr(control, "disposalMethod", "none");
                        if (left < 0 || top < 0 || (long) left + reader.getWidth(index) > width
                                || (long) top + reader.getHeight(index) > height)
                            throw new IOException("Cuadro GIF fuera del lienzo de la imagen.");
                    }
                    int[] previous = disposal.equals("restoreToPrevious") ? canvas.getRGB(0, 0, width, height, null, 0, width) : null;
                    BufferedImage frame = reader.read(index);
                    checkInterrupted();
                    var graphics = canvas.createGraphics(); graphics.drawImage(frame, left, top, null); graphics.dispose();
                    frames.add(new Frame(canvas.getRGB(0, 0, width, height, null, 0, width), delay));
                    if (disposal.equals("restoreToBackgroundColor")) {
                        graphics = canvas.createGraphics(); graphics.setComposite(AlphaComposite.Clear);
                        graphics.fillRect(left, top, frame.getWidth(), frame.getHeight()); graphics.dispose();
                    } else if (previous != null) canvas.setRGB(0, 0, width, height, previous, 0, width);
                    if (!gif) break;
                }
                if (frames.isEmpty()) throw new IOException("La imagen no tiene cuadros.");
                return new Decoded(width, height, List.copyOf(frames));
            } finally { reader.dispose(); }
        }
    }
    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Carga de imagen cancelada.");
    }
    private static void checkSize(int w, int h) throws IOException {
        if (w < 1 || h < 1 || w > MAX_SIDE || h > MAX_SIDE) throw new IOException("Resolución máxima: 2048 × 2048.");
    }
    private static Node child(Node n, String name) {
        if (n == null) return null;
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) if (c.getNodeName().equals(name)) return c;
        return null;
    }
    private static String attr(Node n, String key, String fallback) {
        Node a = n == null ? null : n.getAttributes().getNamedItem(key); return a == null ? fallback : a.getNodeValue();
    }
    private static int integer(Node n, String key, int fallback) { return Integer.parseInt(attr(n, key, "" + fallback)); }
}
