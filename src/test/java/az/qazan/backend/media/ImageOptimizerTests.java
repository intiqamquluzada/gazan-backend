package az.qazan.backend.media;

import az.qazan.backend.media.application.ImageOptimizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageOptimizerTests {

    private static byte[] photo(int w, int h, String format, boolean alpha) throws Exception {
        BufferedImage img = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, Color.ORANGE, w, h, Color.BLUE));
        g.fillRect(0, 0, w, h);
        Random r = new Random(1);
        for (int i = 0; i < 4000; i++) {  // noise, so it compresses like a photo
            g.setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
            g.fillRect(r.nextInt(w), r.nextInt(h), 6, 6);
        }
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, format, bos);
        return bos.toByteArray();
    }

    @Test
    void bigPhotoIsScaledDownAndShrunk() throws Exception {
        byte[] big = photo(4000, 3000, "png", false);
        ImageOptimizer.Result r = ImageOptimizer.optimize(big, "image/png");
        assertThat(r.contentType()).isEqualTo("image/jpeg");
        assertThat(r.bytes().length).isLessThan(big.length / 4);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(r.bytes()));
        assertThat(Math.max(out.getWidth(), out.getHeight())).isEqualTo(1600);
        assertThat(out.getWidth()).isEqualTo(1600);
        assertThat(out.getHeight()).isEqualTo(1200);
    }

    @Test
    void transparentPngStaysPng() throws Exception {
        byte[] logo = photo(2400, 2400, "png", true);
        ImageOptimizer.Result r = ImageOptimizer.optimize(logo, "image/png");
        assertThat(r.contentType()).isEqualTo("image/png");
        assertThat(ImageIO.read(new ByteArrayInputStream(r.bytes())).getColorModel().hasAlpha()).isTrue();
    }

    @Test
    void smallOrUnsupportedFilesAreKept() throws Exception {
        byte[] small = photo(200, 200, "jpeg", false);
        assertThat(ImageOptimizer.optimize(small, "image/jpeg").bytes()).isSameAs(small);
        byte[] junk = new byte[400 * 1024];
        assertThat(ImageOptimizer.optimize(junk, "image/jpeg").bytes()).isSameAs(junk);
        assertThat(ImageOptimizer.optimize(junk, "image/webp").bytes()).isSameAs(junk);
    }
}
