/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.APIException;
import org.junit.Test;

/**
 * The "Background" conformance class: {@code /conf/background/bgcolor-definition},
 * {@code /conf/background/transparent-definition}, {@code /conf/background/void-color-definition},
 * {@code /conf/background/void-transparent-definition} and {@code /conf/background/map-success}.
 */
public class BackgroundTest extends MapsTestSupport {

    /** A map wholly outside the Lakes data, so every pixel of it is a no data pixel showing the background. */
    private static final String EMPTY_MAP =
            "ogc/maps/v1/collections/Lakes/map?f=image/png&width=20&height=20&bbox=10,10,11,11";

    /** The colour of the no data area under the given background query. */
    private int background(String query) throws Exception {
        return getAsImage(EMPTY_MAP + query, "image/png").getRGB(10, 10);
    }

    /** /conf/background/bgcolor-definition A: a six digit hexadecimal red-green-blue value. */
    @Test
    public void testBgColorHexadecimal() throws Exception {
        // the same colour in the three notations the parameter accepts
        assertEquals(0xFF3366CC, background("&bgcolor=0x3366CC"));
        assertEquals(0xFF3366CC, background("&bgcolor=%233366CC"));
        assertEquals(0xFF3366CC, background("&bgcolor=3366CC"));
        // the digit pairs really are red, green and blue in that order
        assertEquals(0xFFFF0000, background("&bgcolor=0xFF0000"));
        assertEquals(0xFF00FF00, background("&bgcolor=0x00FF00"));
        assertEquals(0xFF0000FF, background("&bgcolor=0x0000FF"));
    }

    /** /conf/background/bgcolor-definition B: a case insensitive W3C web colour name. */
    @Test
    public void testBgColorWebColorName() throws Exception {
        assertEquals(0xFF6495ED, background("&bgcolor=cornflowerblue"));
        assertEquals(0xFF6495ED, background("&bgcolor=CornflowerBlue"));
        assertEquals(0xFF6495ED, background("&bgcolor=CORNFLOWERBLUE"));
        assertEquals(0xFFFF0000, background("&bgcolor=red"));
    }

    /** /conf/background/bgcolor-definition D: an opaque map with no background colour asked for is white. */
    @Test
    public void testBgColorDefaultsToWhite() throws Exception {
        assertEquals(0xFFFFFFFF, background("&transparent=false"));
    }

    /** A colour that is neither hexadecimal nor a known name is a client error, not a server one. */
    @Test
    public void testInvalidColorRejected() throws Exception {
        for (String parameter : new String[] {"bgcolor", "void-color"}) {
            DocumentContext json = getAsJSONPath(EMPTY_MAP + "&" + parameter + "=notAColour", 400);
            assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
            assertThat(json.read("title"), containsString(parameter));
        }
    }

    /**
     * /conf/background/transparent-definition and /conf/background/map-success: the whole matrix of the transparent and
     * bgcolor combinations, read on a no data pixel.
     */
    @Test
    public void testTransparentAndBgColorMatrix() throws Exception {
        // no transparent and no bgcolor: transparent is assumed (requirement C)
        assertEquals(0, background("") >>> 24);
        // no transparent but a bgcolor: opaque is assumed, or the colour would never show (requirement D)
        assertEquals(0xFFFF0000, background("&bgcolor=red"));
        // an explicit transparent=false is opaque, with or without a colour
        assertEquals(0xFFFFFFFF, background("&transparent=false"));
        assertEquals(0xFFFF0000, background("&transparent=false&bgcolor=red"));
        // an explicit transparent=true wins over the colour, whose opacity becomes 0 (requirement E)
        assertEquals(0, background("&transparent=true") >>> 24);
        assertEquals(0, background("&transparent=true&bgcolor=red") >>> 24);
    }

    /**
     * /conf/background/void-color-definition: the parameter takes the same values as bgcolor. GeoServer paints the no
     * data areas and the areas outside the valid area of the projection alike, so a void-color on its own drives the
     * whole background, and a bgcolor takes precedence over it.
     */
    @Test
    public void testVoidColor() throws Exception {
        assertEquals(0xFFFF0000, background("&void-color=0xFF0000"));
        assertEquals(0xFF6495ED, background("&void-color=CornflowerBlue"));
        // requirement C, the void defaults to the background colour: both spellings give the same map
        assertEquals(background("&bgcolor=red"), background("&bgcolor=red&void-color=red"));
        // a bgcolor and a differing void-color cannot be honoured apart, the background one is used
        assertEquals(0xFFFF0000, background("&bgcolor=red&void-color=lime"));
    }

    /**
     * /conf/background/void-transparent-definition: a boolean, defaulting to the transparent value. With a single
     * background to paint, it also stands in for transparent when only the void form is given.
     */
    @Test
    public void testVoidTransparent() throws Exception {
        // requirement B, the default follows transparent: stating both alike changes nothing
        assertEquals(0, background("&transparent=true&void-transparent=true") >>> 24);
        assertEquals(0xFFFFFFFF, background("&transparent=false&void-transparent=false"));
        // transparent wins when the two disagree, the void being painted with the map background
        assertEquals(0xFFFFFFFF, background("&transparent=false&void-transparent=true"));
        // on its own the void form drives the background, so a void-transparent=false map is opaque
        assertEquals(0xFFFFFFFF, background("&void-transparent=false"));
        assertEquals(0, background("&void-transparent=true") >>> 24);
    }

    /** With the background class disabled every one of its parameters is ignored, not rejected. */
    @Test
    public void testBackgroundClassDisabled() throws Exception {
        withConformance(MapsConformance::setBackground, false, () -> {
            assertEquals(0, background("&bgcolor=red") >>> 24);
            assertEquals(0, background("&void-color=red") >>> 24);
            assertEquals(0, background("&void-transparent=false") >>> 24);
            // an invalid colour is not parsed either, so it cannot fail
            assertEquals(0, background("&bgcolor=notAColour") >>> 24);
        });
    }
}
