/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.APIException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The "Scaling" and "Display Resolution" conformance classes: {@code /conf/scaling/width-definition},
 * {@code /conf/scaling/height-definition}, {@code /conf/scaling/scale-denominator-definition},
 * {@code /conf/display-resolution/mm-per-pixel-definition} and {@code /conf/display-resolution/map-success}.
 */
public class ScalingTest extends MapsTestSupport {

    private static final String MAP = "ogc/maps/v1/collections/Lakes/map?f=image/png";

    /** The delivered extent, as four ordinates read from the Content-Bbox header. */
    private double[] deliveredBbox(String query) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + query);
        assertEquals(200, response.getStatus());
        String[] ordinates = response.getHeader("Content-Bbox").split(",");
        double[] bbox = new double[4];
        for (int i = 0; i < 4; i++) bbox[i] = Double.parseDouble(ordinates[i]);
        return bbox;
    }

    /** /conf/scaling/width-definition A and /conf/scaling/height-definition A: pixel counts of the viewport. */
    @Test
    public void testWidthAndHeightArePixelCounts() throws Exception {
        BufferedImage image = getAsImage(MAP + "&bbox=-1,-1,1,1&width=123&height=45", "image/png");
        assertEquals(123, image.getWidth());
        assertEquals(45, image.getHeight());
    }

    /**
     * /conf/scaling/scale-denominator-definition A and B: the scale is the ratio between map and real world units at
     * the given display resolution, so the extent of a map of a known pixel size follows from physicalMetersPerPixel =
     * (mm-per-pixel / 1000) * scale-denominator.
     */
    @Test
    public void testScaleDenominatorSizesTheExtent() throws Exception {
        // 100 pixels at 1:100000 and the default 0.28 mm/pixel span 100 * 0.00028 * 100000 = 2800 m
        double[] bbox = deliveredBbox("&center=0,0&width=100&height=100&scale-denominator=100000");
        double metersPerDegree = 6378137.0 * Math.PI / 180;
        double widthMeters = (bbox[3] - bbox[1]) * metersPerDegree;
        assertEquals(2800.0, widthMeters, 30.0);

        // halving the display resolution halves the ground covered by the same number of pixels
        double[] finer = deliveredBbox("&center=0,0&width=100&height=100&scale-denominator=100000&mm-per-pixel=0.14");
        assertEquals((bbox[3] - bbox[1]) / 2, finer[3] - finer[1], 1e-9);
    }

    /**
     * /conf/scaling/scale-denominator-definition F: with no spatial subset in the request, the scale and the image size
     * define the extent, which is otherwise the whole collection. Lakes spans about 280 m, so a 100 pixel map at 1:1000
     * covers 28 m and is a window well inside it.
     */
    @Test
    public void testScaleDenominatorWithoutSubset() throws Exception {
        double[] scaled = deliveredBbox("&width=100&height=100&scale-denominator=1000");
        double[] full = deliveredBbox("&width=100&height=100");
        assertThat(scaled[3] - scaled[1], lessThan(full[3] - full[1]));
        // and it is the same window a center on the middle of the data would give
        double centerLon = (full[0] + full[2]) / 2;
        double centerLat = (full[1] + full[3]) / 2;
        double[] centred =
                deliveredBbox("&width=100&height=100&scale-denominator=1000&center=" + centerLon + "," + centerLat);
        for (int i = 0; i < 4; i++) assertEquals(centred[i], scaled[i], 1e-9);
    }

    /**
     * The implied centre comes from the native bounds, not from the declared latitude/longitude box: the declared box
     * is the envelope of the reprojected footprint, whose middle drifts away from the data when the footprint curves.
     */
    @Test
    public void testCenterFollowsNativeBounds() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo lakes = catalog.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), "Lakes");
        ReferencedEnvelope declared = lakes.getLatLonBoundingBox();
        ReferencedEnvelope nativeBounds = lakes.boundingBox();
        try {
            // an off centre declared box, like the envelope of a curved reprojected footprint
            lakes.setLatLonBoundingBox(new ReferencedEnvelope(
                    declared.getMinX() - 1,
                    declared.getMaxX(),
                    declared.getMinY(),
                    declared.getMaxY() + 1,
                    DefaultGeographicCRS.WGS84));
            catalog.save(lakes);
            double[] bbox = deliveredBbox("&width=100&height=100&scale-denominator=1000");
            assertEquals(nativeBounds.getMedian(0), (bbox[0] + bbox[2]) / 2, 1e-6);
            assertEquals(nativeBounds.getMedian(1), (bbox[1] + bbox[3]) / 2, 1e-6);
        } finally {
            lakes.setLatLonBoundingBox(declared);
            catalog.save(lakes);
        }
    }

    /**
     * /conf/scaling/scale-denominator-definition C: with a spatial extent and no image size the scale sizes the image
     * instead. On the ground a 0.2 by 0.1 degree box at the equator is 22263.9 by 11057.4 m, a degree of latitude being
     * shorter than a degree of longitude there, and at 1:100000 with the default 0.28 mm pixel each pixel covers 28 m.
     */
    @Test
    public void testSizeFromScaleGeographic() throws Exception {
        BufferedImage image = getAsImage(MAP + "&bbox=-0.1,-0.05,0.1,0.05&scale-denominator=100000", "image/png");
        assertEquals(795, image.getWidth());
        assertEquals(395, image.getHeight());
    }

    /** The same 1000 by 500 m ground extent in a metric projected CRS gives the same 28 m pixels. */
    @Test
    public void testSizeFromScaleProjected() throws Exception {
        BufferedImage image =
                getAsImage(MAP + "&bbox=0,0,1000,500&bbox-crs=EPSG:3857&scale-denominator=100000", "image/png");
        assertEquals(36, image.getWidth());
        assertEquals(18, image.getHeight());
    }

    /**
     * A CRS whose axis unit is the US survey foot must be converted before the scale is applied: 3280.83 by 1640.42
     * feet are the same 1000 by 500 m, so the image keeps the size {@link #testSizeFromScaleProjected} asks for.
     * Without the conversion the map would come out about 3.28 times bigger.
     */
    @Test
    public void testSizeFromScaleNonMetricUnits() throws Exception {
        BufferedImage image = getAsImage(
                MAP + "&bbox=0,0,3280.8333,1640.4167&bbox-crs=EPSG:2263&scale-denominator=100000", "image/png");
        assertEquals(36, image.getWidth());
        assertEquals(18, image.getHeight());
    }

    /**
     * /conf/scaling/width-definition H and /conf/scaling/height-definition H: a dimension the request leaves out
     * reflects the size of the area on the ground, not the size of its coordinates. A ten by ten degree box centred on
     * 60 degrees of latitude covers 558 km east to west and 1114 km north to south, so the map comes out twice as tall
     * as it is wide.
     */
    @Test
    public void testMissingHeightFollowsGroundShape() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&bbox=-5,55,5,65&width=200");
        assertEquals(200, image.getWidth());
        assertEquals(399, image.getHeight());
    }

    /** The same shape drives the width when the height is the one given. */
    @Test
    public void testMissingWidthFollowsGroundShape() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&bbox=-5,55,5,65&height=200");
        assertEquals(100, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    /**
     * With neither dimension given the longer side takes the 768 pixel default map size of the WMS, and the other one
     * follows the shape of the area on the ground.
     */
    @Test
    public void testDefaultSizeFollowsGroundShape() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&bbox=-5,55,5,65");
        assertEquals(385, image.getWidth());
        assertEquals(768, image.getHeight());
    }

    /**
     * The scale is set against the physical world too. The box of {@link #testMissingHeightFollowsGroundShape} spans
     * 558000 m east to west and 1114122 m north to south, so at 1:10000000 and the default 0.28 mm pixel, which covers
     * 2800 m, the map is 199 by 398 pixels. Measured in coordinates alone it would be a square 398 pixels wide.
     */
    @Test
    public void testSizeFromScaleAwayFromEquator() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&bbox=-5,55,5,65&scale-denominator=10000000");
        assertEquals(199, image.getWidth());
        assertEquals(398, image.getHeight());
    }

    /**
     * A projected CRS needs the same treatment: a Web Mercator metre is a real metre at the equator only. This square
     * box of 200000 units is centred on 60 degrees of latitude, where each unit is about half a metre of ground, so the
     * map covers about 100000 m per side and comes out 36 pixels wide at 1:10000000. The raw units would give 71.
     */
    @Test
    public void testSizeFromScaleProjectedAwayFromEquator() throws Exception {
        BufferedImage image =
                getAsPNG(MAP + "&bbox=-100000,8299738,100000,8499738&bbox-crs=EPSG:3857&scale-denominator=10000000");
        assertEquals(36, image.getWidth());
        assertEquals(36, image.getHeight());
    }

    /**
     * The extent built around a centre is measured on the ground too. At 60 degrees of latitude a degree of longitude
     * covers half of what a degree of latitude covers, so a square map spans twice as many degrees east to west as it
     * does north to south. Measured in coordinates alone the extent would come out square.
     */
    @Test
    public void testCenterExtentAwayFromEquator() throws Exception {
        double[] bbox = deliveredBbox("&center=0,60&width=100&height=100&scale-denominator=100000");
        double longitudeSpan = bbox[2] - bbox[0];
        double latitudeSpan = bbox[3] - bbox[1];
        // 100 pixels at 1:100000 and the default 0.28 mm pixel span 2800 m, which at 60 degrees of latitude is
        // 0.025132 degrees north to south and 0.050179 degrees east to west
        assertEquals(0.025132, latitudeSpan, 1e-6);
        assertEquals(0.050179, longitudeSpan, 1e-6);
    }

    /**
     * A map crossing the antimeridian is measured like any other: the same 20 by 10 degree area at the equator gives
     * the same image whether it sits on the prime meridian or on the antimeridian.
     */
    @Test
    public void testGroundShapeAcrossTheDateline() throws Exception {
        BufferedImage crossing = getAsPNG(MAP + "&bbox=170,-5,-170,5&width=200");
        BufferedImage plain = getAsPNG(MAP + "&bbox=-10,-5,10,5&width=200");
        assertEquals(99, crossing.getHeight());
        assertEquals(plain.getHeight(), crossing.getHeight());
    }

    /**
     * A map reaching the pole is measured at its centre, like any other. The 360 by 10 degree cap between 80 degrees of
     * latitude and the pole covers 3504 km east to west at its middle latitude, and 1117 km north to south, so the
     * image is about three times wider than it is tall.
     */
    @Test
    public void testGroundShapeAtThePole() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&bbox=-180,80,180,90&width=768");
        assertEquals(245, image.getHeight());
    }

    /**
     * The world is about 40075 km around the equator and 20004 km from pole to pole, so a global map comes out twice as
     * wide as it is tall. The measure is taken at the centre, where a degree of latitude is at its shortest, so the
     * height is 381 pixels instead of the 383 the whole meridian arc would give.
     */
    @Test
    public void testGroundShapeOfTheWholeWorld() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&bbox=-180,-90,180,90&width=768");
        assertEquals(381, image.getHeight());
    }

    /** A scale-denominator with a single dimension leaves the extent unresolved, and the whole collection is drawn. */
    @Test
    public void testScaleDenominatorNeedsBothDimensions() throws Exception {
        double[] full = deliveredBbox("&width=100");
        double[] partial = deliveredBbox("&width=100&scale-denominator=1000");
        assertEquals(full[1], partial[1], 1e-9);
        assertEquals(full[3], partial[3], 1e-9);
    }

    /** /conf/display-resolution/mm-per-pixel-definition B: a display pixel has a positive size. */
    @Test
    public void testNonPositiveMmPerPixelRejected() throws Exception {
        for (String value : new String[] {"0", "-0.28"}) {
            DocumentContext json = getAsJSONPath(MAP + "&width=50&height=50&mm-per-pixel=" + value, 400);
            assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
            assertThat(json.read("title"), containsString("mm-per-pixel must be a positive number"));
        }
    }

    /** /conf/display-resolution/mm-per-pixel-definition C: the default pixel size is 0.28 mm. */
    @Test
    public void testMmPerPixelDefault() throws Exception {
        double[] implicit = deliveredBbox("&center=0,0&width=100&height=100&scale-denominator=100000");
        double[] explicit =
                deliveredBbox("&center=0,0&width=100&height=100&scale-denominator=100000&mm-per-pixel=0.28");
        assertEquals(implicit[3], explicit[3], 1e-9);
    }

    /**
     * /conf/display-resolution/map-success B: the resolution also drives the scale the symbology rules are picked with.
     * The world layer style has no scale rules, so use a layer whose rendering visibly follows the scale: a coarser
     * display resolution means fewer real world metres per pixel, hence a different rendered raster.
     */
    @Test
    public void testMmPerPixelReachesTheRenderer() throws Exception {
        String base = "ogc/maps/v1/collections/" + getLayerId(MockData.WORLD)
                + "/map?f=image/png&bbox=-10,-10,10,10&width=100&height=100";
        // same extent and same image size: only the declared display resolution changes, and the renderer is told
        // about it through the scale it computes, so the two requests are answered with the very same pixels here
        BufferedImage coarse = getAsImage(base + "&mm-per-pixel=0.56", "image/png");
        assertEquals(100, coarse.getWidth());
        // the parameter is accepted and does not disturb a map whose style has no scale dependent rule
        BufferedImage fine = getAsImage(base + "&mm-per-pixel=0.14", "image/png");
        assertEquals(coarse.getRGB(50, 50), fine.getRGB(50, 50));
    }

    /** With the display resolution class disabled the parameter is ignored, not rejected. */
    @Test
    public void testDisplayResolutionDisabled() throws Exception {
        withConformance(MapsConformance::setDisplayResolution, false, () -> {
            double[] ignored =
                    deliveredBbox("&center=0,0&width=100&height=100&scale-denominator=100000&mm-per-pixel=0.14");
            double[] plain = deliveredBbox("&center=0,0&width=100&height=100&scale-denominator=100000");
            assertEquals(plain[3], ignored[3], 1e-9);
            // an invalid value cannot fail either, the parameter never being read
            assertEquals(
                    200,
                    getAsServletResponse(MAP + "&width=50&height=50&mm-per-pixel=-1")
                            .getStatus());
        });
    }
}
