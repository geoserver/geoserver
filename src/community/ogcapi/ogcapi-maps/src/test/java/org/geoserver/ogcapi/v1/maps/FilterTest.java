/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.CQL2Conformance;
import org.geoserver.ogcapi.ECQLConformance;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;

/**
 * Covers the attribute filtering of maps: the {@code filter}, {@code filter-lang} and {@code filter-crs} parameters on
 * the map and feature info resources. A filter selecting one {@code startElevation} value must leave the quadrants of
 * the other features empty, see the quadrant fixture in {@link MapsTestSupport}.
 */
public class FilterTest extends MapsTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addWaterTemp(testData);
    }

    @Test
    public void testCQL2TextFilter() throws Exception {
        BufferedImage ne = filteredMap("startElevation = 2.0", null, null);
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
        assertTransparent(ne, SW);

        BufferedImage west = filteredMap("startElevation = 1.0", null, null);
        assertOpaque(west, NW);
        assertOpaque(west, SW);
        assertTransparent(west, NE);
    }

    /** cql2-text is the default language, an explicit filter-lang must give the same map. */
    @Test
    public void testDefaultFilterLanguage() throws Exception {
        BufferedImage implicit = filteredMap("startElevation = 2.0", null, null);
        BufferedImage explicit = filteredMap("startElevation = 2.0", "cql2-text", null);
        assertEquals(implicit.getRGB(NE[0], NE[1]), explicit.getRGB(NE[0], NE[1]));
        assertTransparent(explicit, NW);
    }

    @Test
    public void testECQLTextFilter() throws Exception {
        // ECQL accepts the same comparison, plus GeoServer own extensions
        BufferedImage ne = filteredMap("startElevation = 2.0", "ecql-text", null);
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
        assertTransparent(ne, SW);
    }

    @Test
    public void testCQL2JsonFilter() throws Exception {
        String json = "{\"op\":\"=\",\"args\":[{\"property\":\"startElevation\"},2.0]}";
        BufferedImage ne = filteredMap(json, "cql2-json", null);
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
        assertTransparent(ne, SW);
    }

    /** Spatial filter literals default to CRS84, per the OGC API - Features - Part 3 filter class. */
    @Test
    public void testSpatialFilterDefaultsToCRS84() throws Exception {
        // a point in the eastern hemisphere, north of the equator: only the NE feature contains it
        BufferedImage ne = filteredMap("S_INTERSECTS(geom, POINT(90 45))", null, null);
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
        assertTransparent(ne, SW);
    }

    @Test
    public void testSpatialFilterWithFilterCRS() throws Exception {
        BufferedImage ne = filteredMap("S_INTERSECTS(geom, POINT(90 45))", null, "EPSG:4326");
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
    }

    /** The filter is combined with the other selection parameters by AND (/req/filter/mixing-expressions). */
    @Test
    public void testFilterAndBBoxCombineWithAnd() throws Exception {
        // the northern half alone would show NW and NE, startElevation=1.0 alone NW and SW: together only NW
        String url = "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50"
                + "&bbox=-180,0,180,90&filter=" + ResponseUtils.urlEncode("startElevation = 1.0");
        BufferedImage image = getAsPNG(url);
        // in a map of the northern half the quadrants are the left and right halves of the image
        assertOpaque(image, new int[] {12, 25});
        assertTransparent(image, new int[] {37, 25});
    }

    /** The filter reaches GetFeatureInfo too, the queried pixel holds no feature once it is excluded. */
    @Test
    public void testFilterAppliedToFeatureInfo() throws Exception {
        DocumentContext excluded = featureInfo("startElevation = 1.0");
        assertEquals(Integer.valueOf(0), excluded.read("features.length()", Integer.class));

        DocumentContext included = featureInfo("startElevation = 2.0");
        assertEquals(Integer.valueOf(1), included.read("features.length()", Integer.class));
        assertEquals(Double.valueOf(2.0), included.read("features[0].properties.startElevation", Double.class));
    }

    @Test
    public void testInvalidFilter() throws Exception {
        // the parser message points at the offending token
        assertInvalidParameter(filteredMapUrl("this is not a filter", null, null), "line 1, column 6");
    }

    /** An attribute no collection exposes as a queryable is a client error, not a silently ignored filter. */
    @Test
    public void testUnknownAttributeRejected() throws Exception {
        assertInvalidParameter(filteredMapUrl("notThere = 2.0", null, null), "notThere");
    }

    @Test
    public void testUnknownAttributeRejectedOnFeatureInfo() throws Exception {
        assertInvalidParameter(featureInfoUrl("notThere = 2.0"), "notThere");
    }

    /** A raster collection has no queryables at all, so any attribute filter on it is unusable. */
    @Test
    public void testUnknownAttributeOnRasterCollection() throws Exception {
        String url = "ogc/maps/v1/collections/wcs:World/map?f=image/png&width=20&height=20&filter="
                + ResponseUtils.urlEncode("startElevation = 2.0");
        assertInvalidParameter(url, "startElevation");
    }

    /** On a dataset map the queryables of all the collections are usable, each layer filtering by what it knows. */
    @Test
    public void testDatasetMapAcceptsAttributeOfOneCollection() throws Exception {
        String base = "ogc/maps/v1/map?f=image/png&width=50&height=50&transparent=true"
                + "&bbox=-180,-90,180,90&collections=sf:TimeWithStartEnd,cite:Lakes&filter=";
        BufferedImage image = getAsPNG(base + ResponseUtils.urlEncode("startElevation = 2.0"));
        assertOpaque(image, NE);

        assertInvalidParameter(base + ResponseUtils.urlEncode("notThere = 2.0"), "notThere");
    }

    @Test
    public void testInvalidFilterLanguage() throws Exception {
        assertInvalidParameter(filteredMapUrl("startElevation = 2.0", "sql", null), "filter-lang");
    }

    @Test
    public void testInvalidFilterCRS() throws Exception {
        // the code is reported upper cased, the way the EPSG factory looked it up
        assertInvalidParameter(
                filteredMapUrl("S_INTERSECTS(geom, POINT(90 45))", null, "EPSG:notACode"), "EPSG:NOTACODE");
    }

    /** With the GeoServer map binding class disabled the parameter is ignored, not rejected. */
    @Test
    public void testFilterIgnoredWhenDisabled() throws Exception {
        withConformance(MapsConformance::setMapFilter, false, () -> {
            BufferedImage image = filteredMap("startElevation = 2.0", null, null);
            assertOpaque(image, NE);
            assertOpaque(image, NW);
            assertOpaque(image, SW);
        });
    }

    /** Same with the standard filter class off, and the filter language is not validated either. */
    @Test
    public void testFilterIgnoredWhenStandardClassDisabled() throws Exception {
        withConformance(MapsConformance::setFilter, false, () -> {
            BufferedImage image = filteredMap("startElevation = 2.0", "sql", null);
            assertOpaque(image, NE);
            assertOpaque(image, NW);
            assertOpaque(image, SW);
        });
    }

    /** With every filter language disabled there is nothing to parse the filter with, so it is ignored. */
    @Test
    public void testFilterIgnoredWhenNoLanguageEnabled() throws Exception {
        withFilterLanguagesDisabled(() -> {
            BufferedImage image = filteredMap("startElevation = 2.0", null, null);
            assertOpaque(image, NE);
            assertOpaque(image, NW);
            assertOpaque(image, SW);
        });
    }

    /** A language whose conformance class is disabled is not in the API document enum, so it is rejected. */
    @Test
    public void testDisabledLanguageRejected() throws Exception {
        withWms(
                wms -> ECQLConformance.configuration(wms).setText(false),
                () -> assertInvalidParameter(filteredMapUrl("startElevation = 2.0", "ecql-text", null), "ecql-text"));
    }

    /** With cql2-text disabled the default language is the first one left, the same the API document declares. */
    @Test
    public void testDefaultLanguageFollowsEnabledOnes() throws Exception {
        withWms(wms -> CQL2Conformance.configuration(wms).setText(false), () -> {
            String json = "{\"op\":\"=\",\"args\":[{\"property\":\"startElevation\"},2.0]}";
            BufferedImage ne = filteredMap(json, null, null);
            assertOpaque(ne, NE);
            assertTransparent(ne, NW);
        });
    }

    /**
     * A structured reader takes the filter on its granule index, so a mosaic filters by granule attribute: the two
     * elevations of sf:watertemp hold different temperatures, and no granule matches an unknown file name.
     */
    @Test
    public void testStructuredCoverageFilter() throws Exception {
        int[] surface = pixels(mosaicMap("elevation = 0"));
        int[] deep = pixels(mosaicMap("elevation = 100"));
        assertThat("the two elevations must not render the same data", surface, not(equalTo(deep)));

        assertEquals("no granule matches, nothing to render", 0, opaquePixels(mosaicMap("location = 'nope'")));
    }

    /** The queryables of a mosaic are the granule index attributes, anything else is rejected. */
    @Test
    public void testUnknownAttributeOnMosaic() throws Exception {
        String url = "ogc/maps/v1/collections/" + getLayerId(WATER_TEMP) + "/map?f=image/png&width=20&height=20&filter="
                + ResponseUtils.urlEncode("notThere = 1");
        assertInvalidParameter(url, "notThere");
    }

    private BufferedImage mosaicMap(String filter) throws Exception {
        // no bbox, the whole layer extent is rendered
        return getAsPNG("ogc/maps/v1/collections/" + getLayerId(WATER_TEMP)
                + "/map?f=image/png&width=20&height=20&transparent=true&filter="
                + ResponseUtils.urlEncode(filter));
    }

    private static long opaquePixels(BufferedImage image) {
        return Arrays.stream(pixels(image)).filter(argb -> alpha(argb) != 0).count();
    }

    private BufferedImage filteredMap(String filter, String filterLang, String filterCrs) throws Exception {
        return getAsPNG(filteredMapUrl(filter, filterLang, filterCrs));
    }

    private String filteredMapUrl(String filter, String filterLang, String filterCrs) {
        StringBuilder query = new StringBuilder("filter=").append(ResponseUtils.urlEncode(filter));
        if (filterLang != null) query.append("&filter-lang=").append(filterLang);
        if (filterCrs != null) query.append("&filter-crs=").append(ResponseUtils.urlEncode(filterCrs));
        return quadrantMapUrl(query.toString());
    }

    private DocumentContext featureInfo(String filter) throws Exception {
        return getAsJSONPath(featureInfoUrl(filter), 200);
    }

    private String featureInfoUrl(String filter) {
        return quadrantInfoUrl("filter=" + ResponseUtils.urlEncode(filter));
    }
}
