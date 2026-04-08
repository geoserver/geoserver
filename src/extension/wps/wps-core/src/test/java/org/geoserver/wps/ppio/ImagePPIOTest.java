/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import org.geoserver.wps.WPSException;
import org.junit.Test;

public class ImagePPIOTest {

    private static BufferedImage makeTestImage(int w, int h, boolean withAlpha) {
        int type = withAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage img = new BufferedImage(w, h, type);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.RED);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.BLUE);
            g.fillRect(w / 4, h / 4, w / 2, h / 2);
        } finally {
            g.dispose();
        }
        return img;
    }

    @Test
    public void testPNGWriterAndReaderAvailable() {
        ImagePPIO png = new ImagePPIO.PNGPPIO();
        ImageWriter writer = png.getWriter();
        ImageReader reader = png.getReader();
        assertNotNull("PNG writer should be available", writer);
        assertNotNull("PNG reader should be available", reader);
        // Sanity check: file extension
        assertEquals("png", png.getFileExtension());
    }

    @Test
    public void testJPEGWriterAvailable() {
        ImagePPIO jpeg = new ImagePPIO.JPEGPPIO();
        ImageWriter writer = jpeg.getWriter();
        assertNotNull("JPEG writer should be available", writer);
        assertEquals("jpeg", jpeg.getFileExtension());
        // Note: jpeg.getReader() returns a PNG reader by design in current code.
        assertNotNull("Reader (currently PNG) should be available", jpeg.getReader());
    }

    @Test
    public void testPNGEncodeDecodeRoundTrip() throws Exception {
        ImagePPIO png = new ImagePPIO.PNGPPIO();
        BufferedImage original = makeTestImage(16, 12, true);

        // encode
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        png.encode(original, bos);
        byte[] data = bos.toByteArray();
        assertTrue("Encoded PNG should not be empty", data.length > 0);

        // decode
        Object decoded = png.decode(new ByteArrayInputStream(data));
        assertTrue("Decoded object should be a RenderedImage", decoded instanceof RenderedImage);
        RenderedImage ri = (RenderedImage) decoded;

        assertEquals(16, ri.getWidth());
        assertEquals(12, ri.getHeight());

        // spot-check a pixel value (center) — decode returns a RenderedImage, so use getData
        int centerX = 8, centerY = 6;
        int[] pixel = ri.getData().getPixel(centerX, centerY, (int[]) null);
        // Expect the blue rectangle drawn over red background -> blue has non-zero in B channel
        assertTrue("Center pixel should have non-zero blue component", pixel[2] > 0);
    }

    @Test
    public void testJPEGEncodeButDecodeReturnsWPSExceptionGivenPNGReader() throws Exception {
        ImagePPIO jpeg = new ImagePPIO.JPEGPPIO();
        BufferedImage original = makeTestImage(20, 10, false); // RGB (no alpha) to be friendly to JPEG

        // encode to JPEG
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        jpeg.encode(original, bos);
        byte[] data = bos.toByteArray();
        assertTrue("Encoded JPEG should not be empty", data.length > 0);

        // decode via ImagePPIO (which currently uses a PNG reader)
        Object decoded = jpeg.decode(new ByteArrayInputStream(data));

        // With current implementation, decode uses a PNG reader and should fail, returning WPSException.
        assertTrue(
                "Decoding JPEG with a PNG reader should return a WPSException instance",
                decoded instanceof WPSException);
        WPSException ex = (WPSException) decoded;
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue("Exception message should mention mimetype", msg.contains("mimetype"));
    }

    @Test
    public void testDecodeInvalidStreamReturnsWPSException() throws Exception {
        ImagePPIO png = new ImagePPIO.PNGPPIO();
        byte[] garbage = "not an image".getBytes("UTF-8");

        Object decoded = png.decode(new ByteArrayInputStream(garbage));
        assertTrue("Invalid input should return a WPSException", decoded instanceof WPSException);

        WPSException ex = (WPSException) decoded;
        assertTrue(
                "Exception should mention inability to decode",
                ex.getMessage().toLowerCase().contains("unable to decode"));
    }
}
