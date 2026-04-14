/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class FileTypesTest {

    /** Tests that a SVG image will correctly handled (svg more difficult). */
    @Test
    public void testAssertSvgImage() throws Exception {
        try (InputStream is = new FileInputStream("src/test/resources/org/geoserver/catalog/square16.svg"); ) {
            FileTypes.assertSimpleImage(is, true);
        }
    }

    /** Tests that a PNG image will correctly handled (easy case). */
    @Test
    public void testAssertPngImage() throws Exception {
        try (InputStream is = new FileInputStream("src/test/resources/org/geoserver/catalog/rockFillSymbol.png")) {
            FileTypes.assertSimpleImage(is, true);
        }
    }

    /** this tests a file that isn't an image (by magic). Should throw. */
    @Test
    public void testNonImage() throws Exception {
        byte[] data = {65, 66, 67, 68}; // not image magic
        InputStream is = new ByteArrayInputStream(data);

        assertThrows(Exception.class, () -> {
            FileTypes.assertSimpleImage(is, true);
        });
    }

    /**
     * This is an invalid PNG file - it has the correct starting magic bytes, but is not a valid image. This tests that
     * we are actually validating the image content and not just the magic bytes. Should throw.
     */
    @Test
    public void testInvalidImage() {
        // png image magic.  This will typically be detected as a PNG.
        byte[] data = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        InputStream is = new ByteArrayInputStream(data);

        assertThrows(Exception.class, () -> {
            FileTypes.assertSimpleImage(is, true);
        });
    }
}
