/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import org.geoserver.wms.WMS;
import org.junit.Test;

/**
 * The void parameters of the "Background" conformance class, {@code /conf/background/void-color-definition} and
 * {@code /conf/background/void-transparent-definition}. They paint the area outside the valid area of the projection.
 * The maps here use the Antarctic polar stereographic projection, whose valid area is the southern hemisphere.
 */
public class VoidBackgroundTest extends MapsTestSupport {

    /**
     * A map straddling the equator, where the renderer cuts the BasicPolygons data at the edge of the valid area. The
     * upper half of the image is the void, the lower half the map background, with data drawn on it.
     */
    private static final String CUT_MAP = "ogc/maps/v1/collections/cite:BasicPolygons/map?f=image/png"
            + "&width=21&height=21&crs=EPSG:3031&bbox=-3,-3,3,3";

    /** A map wholly outside the Lakes data, so every pixel of it is a no data pixel showing the background. */
    private static final String EMPTY_MAP =
            "ogc/maps/v1/collections/Lakes/map?f=image/png&width=20&height=20&bbox=10,10,11,11";

    /**
     * Renders {@link #CUT_MAP} and checks the areas it holds: a pixel on the data, a pixel in the void north of the
     * equator, one where the renderer cut the data at the equator, and a pixel with no data south of it.
     */
    private void assertMap(String query, int data, int voidPaint, int background) throws Exception {
        BufferedImage image = getAsPNG(CUT_MAP + query);
        assertEquals("data", data, image.getRGB(10, 13));
        assertEquals("void", voidPaint, image.getRGB(0, 0));
        assertEquals("cut data", voidPaint, image.getRGB(10, 8));
        assertEquals("background", background, image.getRGB(0, 20));
    }

    /** The two colours apply to their own side of the valid area boundary, and neither covers the data. */
    @Test
    public void testVoidColorDiffersFromBackground() throws Exception {
        assertMap("&bgcolor=blue&void-color=red", BLACK, RED, BLUE);
        assertMap("&bgcolor=red&void-color=blue", BLACK, BLUE, RED);
    }

    /** /conf/background/void-color-definition A and B: the parameter takes the same values as bgcolor. */
    @Test
    public void testVoidColorNotations() throws Exception {
        assertMap("&bgcolor=blue&void-color=0xFF0000", BLACK, RED, BLUE);
        assertMap("&bgcolor=blue&void-color=%23FF0000", BLACK, RED, BLUE);
        assertMap("&bgcolor=blue&void-color=FF0000", BLACK, RED, BLUE);
        assertMap("&bgcolor=blue&void-color=CornflowerBlue", BLACK, CORNFLOWER_BLUE, BLUE);
    }

    /** /conf/background/void-color-definition C: with no void colour of its own the void takes the background one. */
    @Test
    public void testVoidColorDefaultsToBackground() throws Exception {
        assertMap("&bgcolor=blue", BLACK, BLUE, BLUE);
        assertMap("&transparent=false", BLACK, WHITE, WHITE);
        assertMap("", BLACK, TRANSPARENT_WHITE, TRANSPARENT_WHITE);
    }

    /** An opaque map with a transparent void: the background cannot be painted over the whole image. */
    @Test
    public void testTransparentVoidOverOpaqueBackground() throws Exception {
        assertMap("&transparent=false&void-transparent=true", BLACK, TRANSPARENT_WHITE, WHITE);
        // requirement E again: the colour still fills the RGB channels where the alpha is zero
        assertMap("&bgcolor=blue&void-transparent=true", BLACK, BLUE & TRANSPARENT_WHITE, BLUE);
    }

    /** A transparent map with an opaque void, the mirror of {@link #testTransparentVoidOverOpaqueBackground}. */
    @Test
    public void testOpaqueVoidOverTransparentBackground() throws Exception {
        assertMap("&transparent=true&void-color=red", BLACK, RED, TRANSPARENT_WHITE);
        assertMap("&void-color=red", BLACK, RED, TRANSPARENT_WHITE);
    }

    /** /conf/background/void-transparent-definition B: with nothing asked for, the void follows the background. */
    @Test
    public void testVoidTransparentDefaultsToTransparent() throws Exception {
        assertMap("&transparent=true", BLACK, TRANSPARENT_WHITE, TRANSPARENT_WHITE);
        assertMap("&transparent=false&void-transparent=false", BLACK, WHITE, WHITE);
        assertMap("&void-transparent=false", BLACK, WHITE, TRANSPARENT_WHITE);
    }

    /** A projection covering the whole world has no void, so the void parameters change nothing. */
    @Test
    public void testNoVoidInGeographicMap() throws Exception {
        assertEquals(TRANSPARENT_WHITE, getAsPNG(EMPTY_MAP + "&void-color=red").getRGB(10, 10));
        assertEquals(
                TRANSPARENT_WHITE,
                getAsPNG(EMPTY_MAP + "&void-transparent=false").getRGB(10, 10));
        assertEquals(RED, getAsPNG(EMPTY_MAP + "&bgcolor=red&void-color=lime").getRGB(10, 10));
    }

    /**
     * The valid area of the projection is only respected when advanced projection handling is on. With it off the
     * renderer draws data outside the valid area, so the void keeps the map background rather than covering that data.
     */
    @Test
    public void testVoidNeedsAdvancedProjectionHandling() throws Exception {
        WMS.ENABLE_ADVANCED_PROJECTION = false;
        try {
            BufferedImage image = getAsPNG(CUT_MAP + "&bgcolor=blue&void-color=red");
            // the data reaches north of the equator, and the whole map takes the background colour
            assertEquals(BLACK, image.getRGB(10, 8));
            assertEquals(BLUE, image.getRGB(0, 0));
            assertEquals(BLUE, image.getRGB(0, 20));
        } finally {
            WMS.ENABLE_ADVANCED_PROJECTION = true;
        }
    }

    /**
     * The valid area of a transverse mercator map reaches 45 degrees away from its central meridian, where the
     * projection refuses to transform. The map is drawn all the same, with its background over the void.
     */
    @Test
    public void testValidAreaOutsideProjectionDomain() throws Exception {
        String map = "ogc/maps/v1/collections/Lakes/map?f=image/png&width=41&height=21&crs=EPSG:32633"
                + "&bbox=-20000000,0,20000000,2000000&bbox-crs=EPSG:32633&bgcolor=blue&void-color=red";
        assertEquals(BLUE, getAsPNG(map).getRGB(0, 10));
    }
}
