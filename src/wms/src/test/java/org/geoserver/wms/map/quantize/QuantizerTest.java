/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;

import javax.media.jai.PlanarImage;

import org.geotools.image.test.ImageAssert;
import org.junit.Test;

public class QuantizerTest {

    static {
        ColorIndexerDescriptor.register();
    }

    @Test
    public void testThreeColors() {
        BufferedImage bi = new BufferedImage(4, 4, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(Color.RED);
        gr.fillRect(0, 0, 2, 2);
        gr.setColor(Color.GREEN);
        gr.fillRect(2, 0, 2, 2);
        gr.setColor(Color.BLUE);
        gr.fillRect(0, 2, 4, 2);
        gr.dispose();

        // simple palette checks
        ColorIndexer indexer = new Quantizer(256).buildColorIndexer(bi);
        IndexColorModel icm = indexer.toIndexColorModel();
        assertEquals(Transparency.OPAQUE, icm.getTransparency());
        assertEquals(3, icm.getNumComponents());

        // quantize and check
        RenderedImage indexed = ColorIndexerDescriptor.create(bi, indexer, null);
        IndexColorModel icm2 = (IndexColorModel) indexed.getColorModel();
        assertEquals(icm, icm2);

        assertImagesSimilar(bi, indexed, 0);
    }

    @Test
    public void testRedGradient() {
        BufferedImage bi = new BufferedImage(10, 256, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gr = bi.createGraphics();
        for (int i = 0; i < 256; i++) {
            gr.setColor(new Color(i, 0, 0));
            gr.drawLine(0, i, 10, i);
        }
        gr.dispose();

        // simple palette checks
        ColorIndexer indexer = new Quantizer(256).buildColorIndexer(bi);
        IndexColorModel icm = indexer.toIndexColorModel();
        assertEquals(Transparency.OPAQUE, icm.getTransparency());
        assertEquals(3, icm.getNumComponents());

        // quantize and check
        RenderedImage indexed = ColorIndexerDescriptor.create(bi, indexer, null);
        IndexColorModel icm2 = (IndexColorModel) indexed.getColorModel();
        assertEquals(icm, icm2);
        assertImagesSimilar(bi, indexed, 0);
    }

    @Test
    public void testRedGradientSubsample() {
        BufferedImage bi = new BufferedImage(10, 256, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gr = bi.createGraphics();
        for (int i = 0; i < 256; i++) {
            gr.setColor(new Color(i, 0, 0));
            gr.drawLine(0, i, 10, i);
        }
        gr.dispose();

        // simple palette checks
        ColorIndexer indexer = new Quantizer(256).subsample().buildColorIndexer(bi);
        IndexColorModel icm = indexer.toIndexColorModel();
        assertEquals(Transparency.OPAQUE, icm.getTransparency());
        assertEquals(3, icm.getNumComponents());

        // quantize and check
        RenderedImage indexed = ColorIndexerDescriptor.create(bi, indexer, null);
        IndexColorModel icm2 = (IndexColorModel) indexed.getColorModel();
        assertEquals(icm, icm2);
        assertImagesSimilar(bi, indexed, 2); // allow a very small color difference
    }

    @Test
    public void testColorWheelBitmask() {
        final int SIZE = 100;
        BufferedImage bi = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gr = bi.createGraphics();

        for (int s = 0; s < SIZE; s++) {
            int arcw = SIZE * s / SIZE;
            int arch = SIZE * s / SIZE;
            for (int h = 0; h < 360; h++) {
                float hue = h / 360f;
                float sat = s / (float) SIZE;
                Color c = Color.getHSBColor(hue, sat, 1F);
                gr.setColor(c);
                gr.fillArc(SIZE / 2 - arcw / 2, SIZE / 2 - arch / 2, arcw, arch, h, 1);
            }
        }
        gr.dispose();

        // simple palette checks
        ColorIndexer indexer = new Quantizer(256).buildColorIndexer(bi);
        IndexColorModel icm = indexer.toIndexColorModel();
        assertEquals(Transparency.BITMASK, icm.getTransparency());
        assertEquals(4, icm.getNumComponents());

        // quantize and check
        RenderedImage indexed = ColorIndexerDescriptor.create(bi, indexer, null);
        IndexColorModel icm2 = (IndexColorModel) indexed.getColorModel();
        assertEquals(icm, icm2);

        // the source image has up to 36000 colors (the distance has been checked visually, and
        // yes, in this particular case pngquant and pngnq do much better, the Quantizer algorithm
        // has been setup to preserve translucent borders better than the inside)
        assertImagesSimilar(bi, indexed, 250);
    }

    @Test
    public void testColorWheelTranslucent() throws Exception {
        final int SIZE = 100;
        BufferedImage bi = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gr = bi.createGraphics();
        gr.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON));
        for (int s = 0; s < SIZE; s++) {
            int arcw = SIZE * s / SIZE;
            int arch = SIZE * s / SIZE;
            for (int h = 0; h < 360; h++) {
                float hue = h / 360f;
                float sat = s / (float) SIZE;
                Color c = Color.getHSBColor(hue, sat, 1F);
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), s * 255 / SIZE);
                gr.setColor(c);
                gr.fillArc(SIZE / 2 - arcw / 2, SIZE / 2 - arch / 2, arcw, arch, h, 1);
            }
        }
        gr.dispose();

        // simple palette checks
        ColorIndexer indexer = new Quantizer(256).buildColorIndexer(bi);
        IndexColorModel icm = indexer.toIndexColorModel();
        assertEquals(Transparency.TRANSLUCENT, icm.getTransparency());
        assertEquals(4, icm.getNumComponents());

        // quantize and check
        RenderedImage indexed = ColorIndexerDescriptor.create(bi, indexer, null);
        IndexColorModel icm2 = (IndexColorModel) indexed.getColorModel();
        assertEquals(icm, icm2);

        // the source image has up to 36000 colors + alpha (the distance has been checked visually)
        assertImagesSimilar(bi, indexed, 250);
    }

    /**
     * Checks two images are visually equal given a certain maximum color distance. For a
     * better tool you might want to check out {@link ImageAssert}, but that works only with RGB
     * images, this one is color model independent
     * 
     * @param image1
     * @param image2
     * @param maxColorDistance
     */
    private void assertImagesSimilar(RenderedImage image1, RenderedImage image2,
            int maxColorDistance) {
        assertEquals(image1.getWidth(), image2.getWidth());
        assertEquals(image1.getHeight(), image2.getHeight());

        BufferedImage bi1 = toBufferedImage(image1);
        BufferedImage bi2 = toBufferedImage(image2);
        for (int i = 0; i < bi1.getWidth(); i++) {
            for (int j = 0; j < bi1.getHeight(); j++) {
                int c1 = bi1.getRGB(i, j);
                int c2 = bi2.getRGB(i, j);

                int a1 = ColorUtils.alpha(c1);
                int a2 = ColorUtils.alpha(c2);

                int r1 = ColorUtils.red(c1);
                int r2 = ColorUtils.red(c2);

                int g1 = ColorUtils.green(c1);
                int g2 = ColorUtils.green(c2);

                int b2 = ColorUtils.blue(c2);
                int b1 = ColorUtils.blue(c1);

                int dr = r1 - r2;
                int dg = g1 - g2;
                int db = b1 - b2;
                int da = a1 - a2;
                double d = Math.sqrt((1.5 * dr * dr + 2 * dg * dg + db * db + 2 * da * da) / (1.5 + 2 + 1 + 2));
                assertTrue("Color distance " + d + " excessive for pixels " + i + "," + j,
                        d <= maxColorDistance);
            }
        }

    }

    private BufferedImage toBufferedImage(RenderedImage ri) {
        if (ri instanceof BufferedImage) {
            return (BufferedImage) ri;
        } else {
            return PlanarImage.wrapRenderedImage(ri).getAsBufferedImage();
        }
    }

}
