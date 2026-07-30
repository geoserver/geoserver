/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.awt.image.BufferedImage;
import org.geoserver.data.test.MockData;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The "Orientation" conformance class: {@code /conf/orientation/orientation} and
 * {@code /conf/orientation/response-headers}.
 */
public class OrientationTest extends MapsTestSupport {

    /** A square extent on the world coverage, so a rotation about its centre stays inside the data. */
    private String map(String query) {
        return "ogc/maps/v1/collections/" + getLayerId(MockData.WORLD)
                + "/map?f=image/png&bbox=-20,-20,20,20&width=80&height=80" + query;
    }

    /** /conf/orientation/orientation B: no orientation parameter means a north up map. */
    @Test
    public void testDefaultOrientation() throws Exception {
        MockHttpServletResponse plain = getAsServletResponse(map(""));
        assertEquals(200, plain.getStatus());
        assertEquals("0.0", plain.getHeader("Content-Orientation"));
        // and an explicit zero is the very same map
        ImageAssert.assertEquals(getAsImage(map(""), "image/png"), getAsImage(map("&orientation=0"), "image/png"), 0);
    }

    /**
     * /conf/orientation/orientation A and C: the map is rotated counterclockwise about the centre of the selected
     * spatial subset. A half turn about the centre of a square extent maps every pixel onto its point reflection.
     */
    @Test
    public void testHalfTurnAboutTheSubsetCentre() throws Exception {
        BufferedImage north = getAsImage(map(""), "image/png");
        BufferedImage turned = getAsImage(map("&orientation=180"), "image/png");
        assertNotEquals(north.getRGB(20, 20), north.getRGB(59, 59));
        for (int[] p : new int[][] {{20, 20}, {10, 60}, {40, 25}}) {
            assertEquals("pixel " + p[0] + "," + p[1], north.getRGB(p[0], p[1]), turned.getRGB(79 - p[0], 79 - p[1]));
        }
        // a full turn is the identity, which confirms the direction is applied consistently
        ImageAssert.assertEquals(north, getAsImage(map("&orientation=360"), "image/png"), 0);
    }

    /**
     * /conf/orientation/orientation D: with a spatial subset the rotation is applied to the corners of the clipping
     * box, as if the equivalent center, width and height had been used, so the rotated map has no empty corners.
     */
    @Test
    public void testRotatedMapHasNoEmptyCorners() throws Exception {
        BufferedImage turned = getAsImage(map("&orientation=45"), "image/png");
        assertEquals(80, turned.getWidth());
        assertEquals(80, turned.getHeight());
        // the world coverage covers the whole extent, so every corner must carry data, not the background
        for (int[] corner : new int[][] {{1, 1}, {78, 1}, {1, 78}, {78, 78}}) {
            assertEquals("corner " + corner[0] + "," + corner[1], 255, turned.getRGB(corner[0], corner[1]) >>> 24);
        }
    }

    /**
     * /conf/orientation/response-headers: the applied rotation is reported in decimal degrees, and the Content-Bbox is
     * the extent before the rotation was applied.
     */
    @Test
    public void testOrientationResponseHeaders() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(map("&orientation=30.5"));
        assertEquals(200, response.getStatus());
        assertEquals("30.5", response.getHeader("Content-Orientation"));
        // EPSG:4326 is latitude first, and the extent is the requested one, unrotated
        assertEquals("-20.0,-20.0,20.0,20.0", response.getHeader("Content-Bbox"));
    }
}
