/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.junit.Before;
import org.junit.Test;

public class JpegOrPngChooserTest {

    private BufferedImage indexed;
    private BufferedImage gray;
    private BufferedImage rgb;
    private BufferedImage rgba;
    private BufferedImage rgba_opaque;
    private BufferedImage rgba_partial;

    @Before
    public void prepareImages() {
        // paletted image
        indexed = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
        // gray one, no transparency
        gray = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        // opaque rgb
        rgb = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
        // transparent rgba
        rgba = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        // fully opaque rgba
        rgba_opaque = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = rgba_opaque.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 10, 10);
        graphics.dispose();
        // partially transparent rgba
        rgba_partial = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        graphics = rgba_partial.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 10, 5);
        graphics.dispose();
    }

    @Test
    public void testJpegPngImageWriter() {
        assertPng(indexed);
        assertJpeg(gray);
        assertJpeg(rgb);
        assertPng(rgba);
        assertJpeg(rgba_opaque);
        assertPng(rgba_partial);
    }

    private void assertPng(RenderedImage image) {
        JpegOrPngChooser chooser = new JpegOrPngChooser(image);
        assertFalse(chooser.isJpegPreferred());
        assertEquals("image/png", chooser.getMime());
        assertEquals("png", chooser.getExtension());
    }

    private void assertJpeg(RenderedImage image) {
        JpegOrPngChooser chooser = new JpegOrPngChooser(image);
        assertTrue(chooser.isJpegPreferred());
        assertEquals("image/jpeg", chooser.getMime());
        assertEquals("jpeg", chooser.getExtension());
    }
}
