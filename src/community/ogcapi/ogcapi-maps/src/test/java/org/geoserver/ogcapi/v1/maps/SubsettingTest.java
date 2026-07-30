/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The subsetting axis vocabulary and the parameters that qualify it:
 * {@code /conf/spatial-subsetting/subset-definition}, {@code /conf/spatial-subsetting/bbox-crs},
 * {@code /conf/spatial-subsetting/subset-crs}, {@code /conf/spatial-subsetting/center-crs} and
 * {@code /conf/datetime/subset-definition}.
 */
public class SubsettingTest extends MapsTestSupport {

    private static final String MAP = "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // an elevation dimension, so the vertical subset axes have something to act on
        setupStartEndTimeDimension(TIME_WITH_START_END, "elevation", "startElevation", "endElevation");
    }

    /** The delivered extent of a map, as the Content-Bbox header reports it. */
    private String bbox(String query) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + query);
        assertEquals(200, response.getStatus());
        return response.getHeader("Content-Bbox");
    }

    /**
     * /conf/spatial-subsetting/subset-definition A: Lat and Lon for a geographic CRS, E and N for a projected one, plus
     * the aliases the standard recommends. All of them must name the same two axes.
     */
    @Test
    public void testSpatialAxisAliases() throws Exception {
        String expected = bbox("&subset=Lon(0:2),Lat(0:2)");
        for (String x :
                new String[] {"Lon", "lon", "long", "Long", "Longitude", "longitude", "E", "e", "easting", "x"}) {
            for (String y : new String[] {"Lat", "latitude", "N", "northing", "y"}) {
                assertEquals(
                        "subset=" + x + "(0:2)," + y + "(0:2)",
                        expected,
                        bbox("&subset=" + x + "(0:2)," + y + "(0:2)"));
            }
        }
    }

    /** An axis that is neither a known alias nor a dimension of the collection is a client error. */
    @Test
    public void testUnknownSpatialAxisRejected() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + "&subset=Breadth(0:2),Lat(0:2)");
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("Breadth"));
    }

    /**
     * /conf/spatial-subsetting/subset-definition B: the vertical axis, h for a geographic CRS and z for a projected
     * one. A map is flat, so it selects on the elevation dimension of the collection.
     */
    @Test
    public void testVerticalAxisAliases() throws Exception {
        String base = "ogc/maps/v1/collections/sf:TimeWithStartEnd/map/info?f=application%2Fjson"
                + "&width=40&height=40&bbox=-180,-90,180,90&i=30&j=10&subset=";
        // the elevation range of the north east quadrant feature, named through each accepted spelling
        for (String axis : new String[] {"elevation", "h", "H", "z", "Z"}) {
            assertEquals(
                    "subset=" + axis,
                    Integer.valueOf(1),
                    getAsJSONPath(base + axis + "(1:3)", 200).read("$.numberReturned", Integer.class));
        }
    }

    /** /conf/datetime/subset-definition B and C: the time axis, and the aliases the standard recommends for it. */
    @Test
    public void testTimeAxisAliases() throws Exception {
        String base = "ogc/maps/v1/collections/sf:TimeWithStartEnd/map/info?f=application%2Fjson"
                + "&width=40&height=40&bbox=-180,-90,180,90&i=30&j=10&subset=";
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        for (String axis : new String[] {"time", "Time", "TIME", "t", "T"}) {
            // the single first instant leaves the north east quadrant feature out
            assertEquals(
                    "subset=" + axis,
                    Integer.valueOf(0),
                    getAsJSONPath(base + axis + "(%222012-02-11T00:00:00Z%22)", 200)
                            .read("$.numberReturned", Integer.class));
        }
    }

    /** A time range has two bounds at most, a third one is a client error. */
    @Test
    public void testTimeRangeWithThreeBoundsRejected() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                MAP + "&subset=time(%222012-02-11T00:00:00Z%22:%222012-02-12T00:00:00Z%22:%222012-02-13T00:00:00Z%22)");
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("2012-02-13T00:00:00Z"));
    }

    /**
     * /conf/spatial-subsetting/subset-definition H, /conf/datetime/subset-definition H and
     * /conf/general-subsetting/subset-definition F: repeated subset parameters read as a single comma separated one.
     */
    @Test
    public void testRepeatedSubsetParameters() throws Exception {
        assertEquals(bbox("&subset=Lon(0:2),Lat(0:2)"), bbox("&subset=Lon(0:2)&subset=Lat(0:2)"));
        // and mixing classes across the parameters works too: a spatial axis in one, the time axis in another
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        MockHttpServletResponse response =
                getAsServletResponse("ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50"
                        + "&subset=Lon(-180:0)&subset=Lat(0:90)&subset=time(%222012-02-11T00:00:00Z%22)");
        assertEquals(200, response.getStatus());
        assertEquals("0.0,-180.0,90.0,0.0", response.getHeader("Content-Bbox"));
    }

    /** /conf/spatial-subsetting/bbox-crs F: with no bbox in the request the bbox-crs is ignored. */
    @Test
    public void testBboxCrsIgnoredWithoutBbox() throws Exception {
        assertEquals(bbox("&subset=Lon(0:2),Lat(0:2)"), bbox("&subset=Lon(0:2),Lat(0:2)&bbox-crs=EPSG:3857"));
    }

    /** /conf/spatial-subsetting/subset-crs F: with no spatial subset in the request the subset-crs is ignored. */
    @Test
    public void testSubsetCrsIgnoredWithoutSubset() throws Exception {
        assertEquals(bbox("&bbox=0,0,2,2"), bbox("&bbox=0,0,2,2&subset-crs=EPSG:3857"));
    }

    /** /conf/spatial-subsetting/center-crs F: with no center in the request the center-crs is ignored. */
    @Test
    public void testCenterCrsIgnoredWithoutCenter() throws Exception {
        assertEquals(bbox("&bbox=0,0,2,2"), bbox("&bbox=0,0,2,2&center-crs=EPSG:3857"));
    }

    /**
     * /conf/spatial-subsetting/bbox-crs C and /conf/spatial-subsetting/subset-crs C: with no CRS parameter the
     * coordinates are read as CRS84, which is longitude first.
     */
    @Test
    public void testSubsettingCrsDefaultsToCRS84() throws Exception {
        String crs84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        assertEquals(bbox("&bbox=0,0,2,1"), bbox("&bbox=0,0,2,1&bbox-crs=" + crs84));
        assertEquals(bbox("&subset=Lon(0:2),Lat(0:1)"), bbox("&subset=Lon(0:2),Lat(0:1)&subset-crs=" + crs84));
    }

    /**
     * /conf/spatial-subsetting/bbox-crs D and /conf/spatial-subsetting/subset-crs D: the storage CRS of the collection
     * is an accepted value, both as a URI and as a safe CURIE.
     */
    @Test
    public void testStorageCrsAccepted() throws Exception {
        String projected = "ogc/maps/v1/collections/" + getLayerId(MockData.TASMANIA_DEM)
                + "/map?f=image/png&width=50&height=50&bbox=145,-43,146,-42&bbox-crs=";
        for (String crs : new String[] {"[EPSG:4326]", "http://www.opengis.net/def/crs/EPSG/0/4326", "EPSG:4326"}) {
            MockHttpServletResponse response = getAsServletResponse(projected + crs.replace("[", "%5B"));
            assertEquals(crs, 200, response.getStatus());
        }
    }
    /**
     * /conf/spatial-subsetting/subset-definition D: an interval falling entirely outside the valid range of its axis
     * has nothing to return, so a CITE compliant server answers 404 rather than drawing an empty map. Only latitude can
     * ever trip this: longitude wraps around, and projected and vertical axes declare no bounds.
     */
    @Test
    public void testSubsetOutsideAxisRangeNotFoundWhenCiteCompliant() throws Exception {
        withCiteCompliance(true, () -> {
            for (String subset : new String[] {"Lat(200:300)", "Lat(-300:-200)", "Lat(95)"}) {
                MockHttpServletResponse response = getAsServletResponse(MAP + "&subset=" + subset);
                assertEquals(subset, 404, response.getStatus());
            }
            // an interval merely overlapping the valid range is clipped and rendered, not refused
            assertEquals(
                    200,
                    getAsServletResponse(MAP + "&subset=Lat(80:100),Lon(0:2)").getStatus());
            // a projected subset CRS declares no axis bounds, so nothing is refused there
            assertEquals(
                    200,
                    getAsServletResponse(MAP + "&subset=N(9000000:9500000),E(0:100000)&subset-crs=EPSG:3857")
                            .getStatus());
        });
    }

    /**
     * Outside CITE compliance the same request is served rather than refused: clients ask for round latitude bounds all
     * the time, and an empty map is more useful to them than an error.
     */
    @Test
    public void testSubsetOutsideAxisRangeToleratedByDefault() throws Exception {
        for (String subset : new String[] {"Lat(200:300)", "Lat(-300:-200)", "Lat(95)"}) {
            MockHttpServletResponse response = getAsServletResponse(MAP + "&subset=" + subset);
            assertEquals(subset, 200, response.getStatus());
        }
    }

    /** Runs the body with the CITE compliance flag set, restoring the previous value afterwards. */
    private void withCiteCompliance(boolean citeCompliant, ThrowingRunnable body) throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        boolean previous = wms.isCiteCompliant();
        wms.setCiteCompliant(citeCompliant);
        gs.save(wms);
        try {
            body.run();
        } finally {
            wms.setCiteCompliant(previous);
            gs.save(wms);
        }
    }

    /**
     * Longitude wraps around, so no interval is ever entirely outside it: a low value greater than the high one is the
     * antimeridian case, and values past 180 are equivalent positions.
     */
    @Test
    public void testWrappingAxisNeverOutOfRange() throws Exception {
        for (String subset : new String[] {"Lon(170:-170),Lat(-5:5)", "Lon(200:300),Lat(-5:5)"}) {
            MockHttpServletResponse response = getAsServletResponse(MAP + "&subset=" + subset);
            assertEquals(subset, 200, response.getStatus());
        }
    }

    /**
     * /conf/crs/crs-definition B and C: the CRSs the collection advertises are accepted by the crs parameter and, when
     * the same value is used there, by bbox-crs and subset-crs too.
     */
    @Test
    public void testAdvertisedCrsAcceptedEverywhere() throws Exception {
        List<String> advertised =
                getAsJSONPath("ogc/maps/v1/collections/cite:Lakes", 200).read("crs");
        // the list is large when no SRS list is configured, so check the ones the standard names plus a projected one
        for (String crs : new String[] {
            "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
            "http://www.opengis.net/def/crs/EPSG/0/4326",
            "http://www.opengis.net/def/crs/EPSG/0/3857"
        }) {
            assertThat(advertised, hasItem(crs));
            MockHttpServletResponse response = getAsServletResponse(MAP + "&crs=" + crs);
            assertEquals(crs, 200, response.getStatus());
            // and the same value drives the subsetting parameters, as requirement C asks
            assertEquals(
                    crs,
                    200,
                    getAsServletResponse(MAP + "&crs=" + crs + "&bbox=0,0,2,1&bbox-crs=" + crs)
                            .getStatus());
            assertEquals(
                    crs,
                    200,
                    getAsServletResponse(MAP + "&crs=" + crs + "&subset=Lon(0:2),Lat(0:1)&subset-crs=" + crs)
                            .getStatus());
        }
    }
}
