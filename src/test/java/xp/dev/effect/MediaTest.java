package xp.dev.effect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MediaTest {
    @TempDir Path folder;
    private byte[] image(String type, int w, int h) throws Exception {
        BufferedImage image = new BufferedImage(w, h, type.equals("jpg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffff3400);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); assertTrue(ImageIO.write(image, type, out)); return out.toByteArray();
    }
    @Test void pngJpgAndGifDecode() throws Exception {
        for (String type : new String[]{"png","jpg","gif"}) {
            Media.Decoded decoded = Media.decode(image(type, 16, 8));
            assertEquals(16, decoded.width()); assertEquals(8, decoded.height()); assertEquals(1, decoded.frames().size());
            if (!type.equals("jpg")) assertEquals(0xffff3400, decoded.frames().getFirst().argb()[0]);
            else assertEquals(255, decoded.frames().getFirst().argb()[0] >>> 24);
        }
    }
    @Test void limitsAndCorruptionAreRejected() throws Exception {
        assertThrows(IOException.class, () -> Media.decode(new byte[0]));
        assertThrows(IOException.class, () -> Media.decode(new byte[Media.MAX_BYTES+1]));
        assertThrows(IOException.class, () -> Media.decode(new byte[]{1,2,3,4}));
        byte[] large = image("png", 2049, 1);
        assertThrows(IOException.class, () -> Media.decode(large));
    }
    @Test void renamedUnsupportedImageIsRejected() throws Exception {
        var bitmap = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(bitmap, "bmp", out));
        Files.write(folder.resolve("disguised.png"), out.toByteArray());
        assertThrows(IOException.class, () -> Media.load(folder, "disguised.png"));
    }
    @Test void gifFrameMustFitLogicalScreen() throws Exception {
        byte[] gif = image("gif", 2, 2);
        // The GIF logical screen descriptor starts after the six-byte signature.
        gif[6] = 1; gif[7] = 0;
        gif[8] = 1; gif[9] = 0;
        assertThrows(IOException.class, () -> Media.decode(gif));
    }
    @Test void cancelledDecodeStopsAndPreservesInterrupt() throws Exception {
        byte[] png = image("png", 2, 2);
        try {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedIOException.class, () -> Media.decode(png));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertEquals(2, Media.decode(png).width());
    }
    @Test void namesAndSymlinksStayInsideFolder() throws Exception {
        assertTrue(Media.allowed("portal-01.PNG")); assertTrue(Media.allowed("imagen.gif"));
        assertFalse(Media.allowed("../secret.png")); assertFalse(Media.allowed("/tmp/p.png")); assertFalse(Media.allowed("payload.jar"));
        Path directory = Files.createDirectory(folder.resolve("images"));
        Path outside = Files.write(folder.resolve("outside.png"), image("png", 2, 2));
        Files.createSymbolicLink(directory.resolve("escape.png"), outside);
        assertThrows(IOException.class, () -> Media.load(directory, "escape.png"));
        Files.write(directory.resolve("good.png"), image("png", 2, 2));
        assertEquals(64, Media.load(directory,"good.png").hash().length());
    }
    @Test void gifPreservesOffsetsDelayAndDisposal() throws Exception {
        var writer = ImageIO.getImageWritersByFormatName("gif").next();
        var output = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream); writer.prepareWriteSequence(null);
            for (int i=0;i<3;i++) {
                BufferedImage frame = new BufferedImage(i==0?4:1,i==0?4:1,BufferedImage.TYPE_INT_ARGB);
                if(i==0) for(int y=0;y<4;y++) for(int x=0;x<4;x++) frame.setRGB(x,y,0xffff0000);
                else frame.setRGB(0,0,i==1?0xff00ff00:0xff0000ff);
                var metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frame),writer.getDefaultWriteParam());
                var root=(IIOMetadataNode)metadata.getAsTree("javax_imageio_gif_image_1.0");
                var control=(IIOMetadataNode)root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod",i==1?"restoreToPrevious":"doNotDispose");
                control.setAttribute("delayTime", ""+(i+1)*7);
                var desc=(IIOMetadataNode)root.getElementsByTagName("ImageDescriptor").item(0);
                desc.setAttribute("imageLeftPosition",i==0?"0":"1"); desc.setAttribute("imageTopPosition",i==2?"2":"1");
                if(i==0) desc.setAttribute("imageTopPosition","0");
                metadata.setFromTree("javax_imageio_gif_image_1.0",root);
                writer.writeToSequence(new IIOImage(frame,null,metadata),writer.getDefaultWriteParam());
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
        var decoded=Media.decode(output.toByteArray());
        assertEquals(3,decoded.frames().size()); assertEquals(140,decoded.frames().get(1).millis());
        assertEquals(0xff00ff00,decoded.frames().get(1).argb()[5]);
        assertEquals(0xffff0000,decoded.frames().get(2).argb()[5]);
        assertEquals(0xff0000ff,decoded.frames().get(2).argb()[9]);
    }
}
