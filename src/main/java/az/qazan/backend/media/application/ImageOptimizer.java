package az.qazan.backend.media.application;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Shrinks photos before they are stored: longest side at most
 * {@value #MAX_SIDE}px, JPEGs re-encoded at {@value #JPEG_QUALITY} quality.
 * A phone photo of several MB becomes a few hundred KB, which is what makes
 * business pages open quickly. Anything that can't be improved (GIF, WebP,
 * odd files, or a result that isn't smaller) is kept as uploaded.
 */
public final class ImageOptimizer {

    static final int MAX_SIDE = 1600;
    static final float JPEG_QUALITY = 0.82f;
    /** Small images are left alone; re-encoding them gains nothing. */
    static final int SKIP_BELOW_BYTES = 300 * 1024;

    private ImageOptimizer() {}

    public record Result(byte[] bytes, String contentType) {}

    public static Result optimize(byte[] data, String contentType) {
        if (data.length < SKIP_BELOW_BYTES
                || !("image/jpeg".equals(contentType) || "image/png".equals(contentType))) {
            return new Result(data, contentType);
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
            if (src == null) return new Result(data, contentType);

            int w = src.getWidth(), h = src.getHeight();
            double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));

            boolean alpha = src.getColorModel().hasAlpha();
            // Photos (no transparency) become JPEG; transparent PNGs stay PNG.
            String outType = alpha ? "image/png" : "image/jpeg";
            BufferedImage dst = new BufferedImage(nw, nh,
                    alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!alpha) {
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, nw, nh);
                }
                g.drawImage(src, 0, 0, nw, nh, null);
            } finally {
                g.dispose();
            }

            byte[] out = alpha ? writePng(dst) : writeJpeg(dst);
            return out.length < data.length ? new Result(out, outType) : new Result(data, contentType);
        } catch (IOException | RuntimeException e) {
            return new Result(data, contentType);
        }
    }

    private static byte[] writeJpeg(BufferedImage img) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = writer.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(JPEG_QUALITY);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), p);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }

    private static byte[] writePng(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }
}
