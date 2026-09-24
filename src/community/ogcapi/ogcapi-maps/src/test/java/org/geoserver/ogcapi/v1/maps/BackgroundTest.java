/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.junit.Test;

/**
 * The "Background" conformance class: {@code /conf/background/bgcolor-definition},
 * {@code /conf/background/transparent-definition}, {@code /conf/background/void-color-definition},
 * {@code /conf/background/void-transparent-definition} and {@code /conf/background/map-success}.
 */
public class BackgroundTest extends MapsTestSupport {

    /** Opaque colours as {@link java.awt.image.BufferedImage#getRGB} returns them, alpha in the high byte. */
    private static final int RED = 0xFFFF0000;

    private static final int GREEN = 0xFF00FF00;

    private static final int BLUE = 0xFF0000FF;

    private static final int WHITE = 0xFFFFFFFF;

    private static final int CORNFLOWER_BLUE = 0xFF6495ED;

    private static final int MID_BLUE = 0xFF3366CC;

    /** Red with a zero alpha channel: the colour a transparent map keeps under the alpha. */
    private static final int TRANSPARENT_RED = 0x00FF0000;

    /** A map wholly outside the Lakes data, so every pixel of it is a no data pixel showing the background. */
    private static final String EMPTY_MAP =
            "ogc/maps/v1/collections/Lakes/map?f=image/png&width=20&height=20&bbox=10,10,11,11";

    /** The same map in a style declaring a green background of its own. */
    private static final String STYLED_EMPTY_MAP =
            "ogc/maps/v1/collections/cite:Lakes/styles/bggreen/map?f=image/png&width=20&height=20&bbox=10,10,11,11";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("bggreen", getClass(), catalog);
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakes.getStyles().add(catalog.getStyleByName("bggreen"));
        catalog.save(lakes);
    }

    /** The colour of the no data area under the given background query. */
    private int background(String query) throws Exception {
        return getAsImage(EMPTY_MAP + query, "image/png").getRGB(10, 10);
    }

    /** The same, in the style that declares a background colour of its own. */
    private int styledBackground(String query) throws Exception {
        return getAsImage(STYLED_EMPTY_MAP + query, "image/png").getRGB(10, 10);
    }

    /** /conf/background/bgcolor-definition A: a six digit hexadecimal red-green-blue value. */
    @Test
    public void testBgColorHexadecimal() throws Exception {
        // the same colour in the three notations the parameter accepts
        assertEquals(MID_BLUE, background("&bgcolor=0x3366CC"));
        assertEquals(MID_BLUE, background("&bgcolor=%233366CC"));
        assertEquals(MID_BLUE, background("&bgcolor=3366CC"));
        // the digit pairs really are red, green and blue in that order
        assertEquals(RED, background("&bgcolor=0xFF0000"));
        assertEquals(GREEN, background("&bgcolor=0x00FF00"));
        assertEquals(BLUE, background("&bgcolor=0x0000FF"));
    }

    /** /conf/background/bgcolor-definition B: a case insensitive W3C web colour name. */
    @Test
    public void testBgColorWebColorName() throws Exception {
        assertEquals(CORNFLOWER_BLUE, background("&bgcolor=cornflowerblue"));
        assertEquals(CORNFLOWER_BLUE, background("&bgcolor=CornflowerBlue"));
        assertEquals(CORNFLOWER_BLUE, background("&bgcolor=CORNFLOWERBLUE"));
        assertEquals(RED, background("&bgcolor=red"));
    }

    /** /conf/background/bgcolor-definition D: an opaque map with no background colour asked for is white. */
    @Test
    public void testBgColorDefaultsToWhite() throws Exception {
        assertEquals(WHITE, background("&transparent=false"));
    }

    /**
     * /conf/background/bgcolor-definition C: with no bgcolor asked for, a style declaring a background colour supplies
     * it. The renderer paints that background as map content, so the map comes out solid in that colour whatever the
     * transparency settings say, and the style wins over an explicit bgcolor too.
     */
    @Test
    public void testStyleBackgroundColor() throws Exception {
        assertEquals(GREEN, styledBackground(""));
        assertEquals(GREEN, styledBackground("&transparent=false"));
        // the style background is content, not an image background, so it survives transparent=true and a bgcolor
        assertEquals(GREEN, styledBackground("&transparent=true"));
        assertEquals(GREEN, styledBackground("&bgcolor=red"));
        // without the style the very same map is transparent, so the colour really comes from the style
        assertEquals(0, alpha(background("")));
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
        assertEquals(0, alpha(background("")));
        // no transparent but a bgcolor: opaque is assumed, or the colour would never show (requirement D)
        assertEquals(RED, background("&bgcolor=red"));
        // an explicit transparent=false is opaque, with or without a colour
        assertEquals(WHITE, background("&transparent=false"));
        assertEquals(RED, background("&transparent=false&bgcolor=red"));
        // an explicit transparent=true wins over the colour, whose opacity becomes 0 (requirement E)
        assertEquals(0, alpha(background("&transparent=true")));
        // the colour still fills the RGB channels, which is what asking for both is for (requirement E)
        assertEquals(TRANSPARENT_RED, background("&transparent=true&bgcolor=red"));
    }

    /**
     * /conf/background/transparent-definition A: JPEG cannot encode transparency, so a transparent map comes out opaque
     * in the background colour asked for, and white when none was.
     */
    @Test
    public void testJpegCannotBeTransparent() throws Exception {
        String jpeg = EMPTY_MAP.replace("f=image/png", "f=image/jpeg");
        assertJpegColor(RED, jpeg + "&transparent=true&bgcolor=red");
        assertJpegColor(WHITE, jpeg + "&transparent=true");
    }

    /** JPEG is lossy, so each band is compared with a tolerance instead of the pixel as a whole. */
    private void assertJpegColor(int expected, String path) throws Exception {
        int actual = getAsImage(path, "image/jpeg").getRGB(10, 10);
        String hex = Integer.toHexString(actual);
        assertThat("red of " + hex, (double) red(actual), closeTo(red(expected), 4));
        assertThat("green of " + hex, (double) green(actual), closeTo(green(expected), 4));
        assertThat("blue of " + hex, (double) blue(actual), closeTo(blue(expected), 4));
    }

    /**
     * /conf/background/void-color-definition: the parameter takes the same values as bgcolor. GeoServer paints the no
     * data areas and the areas outside the valid area of the projection alike, so a void-color on its own drives the
     * whole background, and a bgcolor takes precedence over it.
     */
    @Test
    public void testVoidColor() throws Exception {
        assertEquals(RED, background("&void-color=0xFF0000"));
        assertEquals(CORNFLOWER_BLUE, background("&void-color=CornflowerBlue"));
        // requirement C, the void defaults to the background colour: both spellings give the same map
        assertEquals(background("&bgcolor=red"), background("&bgcolor=red&void-color=red"));
        // a bgcolor and a differing void-color cannot be honoured apart, the background one is used
        assertEquals(RED, background("&bgcolor=red&void-color=lime"));
    }

    /**
     * /conf/background/void-transparent-definition: a boolean, defaulting to the transparent value. With a single
     * background to paint, it also stands in for transparent when only the void form is given.
     */
    @Test
    public void testVoidTransparent() throws Exception {
        // requirement B, the default follows transparent: stating both alike changes nothing
        assertEquals(0, alpha(background("&transparent=true&void-transparent=true")));
        assertEquals(WHITE, background("&transparent=false&void-transparent=false"));
        // transparent wins when the two disagree, the void being painted with the map background
        assertEquals(WHITE, background("&transparent=false&void-transparent=true"));
        // on its own the void form drives the background, so a void-transparent=false map is opaque
        assertEquals(WHITE, background("&void-transparent=false"));
        assertEquals(0, alpha(background("&void-transparent=true")));
    }

    /** With the background class disabled every one of its parameters is ignored, not rejected. */
    @Test
    public void testBackgroundClassDisabled() throws Exception {
        withConformance(MapsConformance::setBackground, false, () -> {
            assertEquals(0, alpha(background("&bgcolor=red")));
            assertEquals(0, alpha(background("&void-color=red")));
            assertEquals(0, alpha(background("&void-transparent=false")));
            // an invalid colour is not parsed either, so it cannot fail
            assertEquals(0, alpha(background("&bgcolor=notAColour")));
        });
    }
}
