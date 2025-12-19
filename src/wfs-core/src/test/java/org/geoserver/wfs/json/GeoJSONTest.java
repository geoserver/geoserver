/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.json.JSONType;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author carlo cancellieri - GeoSolutions */
public class GeoJSONTest extends WFSTestSupport {

    //    TODO only v2.0 tests here -> move them
    public static QName LINE3D = new QName(SystemTestData.CITE_URI, "Line3D", SystemTestData.CITE_PREFIX);
    public static QName POINT_LATLON = new QName(SystemTestData.CITE_URI, "PointLatLon", SystemTestData.CITE_PREFIX);
    public static QName POINT_LONLAT = new QName(SystemTestData.CITE_URI, "PointLonLat", SystemTestData.CITE_PREFIX);
    public static QName MULTI_GEOMETRIES_WITH_NULL =
            new QName(SystemTestData.CITE_URI, "MultiGeometriesWithNull", SystemTestData.CITE_PREFIX);
    public static QName POINT_REDUCED = new QName(SystemTestData.CITE_URI, "PointReduced", SystemTestData.CITE_PREFIX);
    public static QName NAN_INFINITE = new QName(SystemTestData.CITE_URI, "NanInfinite", SystemTestData.CITE_PREFIX);

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        super.setUpInternal(data);
        File security = new File(getTestData().getDataDirectoryRoot(), "security");
        security.mkdir();
        File layers = new File(security, "layers.properties");
        IOUtils.copy(GeoJSONTest.class.getResourceAsStream("layers_ro.properties"), layers);
        data.addVectorLayer(LINE3D, Collections.emptyMap(), getClass(), getCatalog());

        // A feature type with Lat-Lon/North-East axis ordering.
        data.addVectorLayer(POINT_LATLON, Collections.emptyMap(), getClass(), getCatalog());
        CoordinateReferenceSystem crsLatLon = CRS.decode("urn:ogc:def:crs:EPSG::4326");
        FeatureTypeInfo pointLatLon =
                getCatalog().getFeatureTypeByName(POINT_LATLON.getPrefix(), POINT_LATLON.getLocalPart());
        pointLatLon.setNativeCRS(crsLatLon);
        pointLatLon.setSRS("urn:ogc:def:crs:EPSG::4326");
        pointLatLon.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        getCatalog().save(pointLatLon);

        // A feature type with Lon-Lat/East-North axis ordering.
        data.addVectorLayer(POINT_LONLAT, Collections.emptyMap(), getClass(), getCatalog());
        CoordinateReferenceSystem crsLonLat = CRS.decode("EPSG:4326", true);
        FeatureTypeInfo pointLonLat =
                getCatalog().getFeatureTypeByName(POINT_LONLAT.getPrefix(), POINT_LONLAT.getLocalPart());
        pointLatLon.setNativeCRS(crsLonLat);
        pointLatLon.setSRS("EPSG:4326");
        pointLatLon.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        getCatalog().save(pointLonLat);

        // A feature with a constant test setup for testing geometry/geometry_name consistency with
        // null geometries
        data.addVectorLayer(MULTI_GEOMETRIES_WITH_NULL, Collections.emptyMap(), getClass(), getCatalog());

        // A feature type with reduced precision
        data.addVectorLayer(POINT_REDUCED, Collections.emptyMap(), getClass(), getCatalog());
        FeatureTypeInfo pointReduced =
                getCatalog().getFeatureTypeByName(POINT_REDUCED.getPrefix(), POINT_REDUCED.getLocalPart());
        pointReduced.setNativeCRS(crsLatLon);
        pointReduced.setSRS("EPSG:4326");
        pointReduced.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        pointReduced.setNumDecimals(2);
        getCatalog().save(pointReduced);

        // add a feature with NaN and infinite for both float and double
        data.addVectorLayer(NAN_INFINITE, Collections.emptyMap(), "nanInfinite.properties", getClass(), getCatalog());
    }

    @Test
    public void testGetSkipCounting() throws Exception {
        Catalog catalog = getCatalog();
        try {
            // skip the feature count
            FeatureTypeInfo primitive = catalog.getFeatureTypeByName(getLayerId(MockData.PRIMITIVEGEOFEATURE));
            primitive.setSkipNumberMatched(true);
            catalog.save(primitive);

            MockHttpServletResponse response = getAsServletResponse(
                    "wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&outputformat="
                            + JSONType.json);
            assertEquals(JSONType.json, getBaseMimeType(response.getContentType()));
            String out = response.getContentAsString();

            JSONObject rootObject = JSONObject.fromObject(out);
            assertEquals("FeatureCollection", rootObject.get("type"));
            JSONArray featureCol = rootObject.getJSONArray("features");
            JSONObject aFeature = featureCol.getJSONObject(0);
            assertEquals("surfaceProperty", aFeature.getString("geometry_name"));
        } finally {
            FeatureTypeInfo primitive = catalog.getFeatureTypeByName(getLayerId(MockData.PRIMITIVEGEOFEATURE));
            primitive.setSkipNumberMatched(false);
            catalog.save(primitive);
        }
    }

    @Test
    public void testGetFeatureCountWfs20() throws Exception {
        // request without filter
        String out = getAsString(
                "wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&count=10&outputformat="
                        + JSONType.json);
        JSONObject rootObject = JSONObject.fromObject(out);
        // print(rootObject);
        assertEquals(5, rootObject.get("totalFeatures"));
        assertEquals(5, rootObject.get("numberMatched"));
        assertNull(rootObject.get("links"));

        // request with filter (featureid=PrimitiveGeoFeature.f001)
        String out2 = getAsString(
                "wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&count=10&outputformat="
                        + JSONType.json
                        + "&featureid=PrimitiveGeoFeature.f001");
        JSONObject rootObject2 = JSONObject.fromObject(out2);
        assertEquals(1, rootObject2.get("totalFeatures"));
        assertEquals(1, rootObject2.get("numberMatched"));
        assertNull(rootObject2.get("links"));

        // check if maxFeatures doesn't affect totalFeatureCount; set Filter and maxFeatures
        String out3 =
                getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&count=1&outputformat="
                        + JSONType.json
                        + "&featureid=PrimitiveGeoFeature.f001,PrimitiveGeoFeature.f002");
        JSONObject rootObject3 = JSONObject.fromObject(out3);
        assertEquals(2, rootObject3.get("totalFeatures"));
        assertEquals(2, rootObject3.get("numberMatched"));
        assertNull(rootObject3.get("links"));

        // request with multiple featureTypes and Filter
        String out4 = getAsString(
                "wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature,sf:AggregateGeoFeature&outputformat="
                        + JSONType.json
                        + "&featureid=PrimitiveGeoFeature.f001,PrimitiveGeoFeature.f002,AggregateGeoFeature.f009");
        JSONObject rootObject4 = JSONObject.fromObject(out4);
        assertEquals(3, rootObject4.get("totalFeatures"));
        assertEquals(3, rootObject4.get("numberMatched"));
        assertNull(rootObject4.get("links"));
    }

    @Test
    public void getGetFeatureWithPagingFirstPage() throws Exception {
        // request with paging
        String out = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature"
                + "&startIndex=0&&count=2&outputformat="
                + JSONType.json);
        JSONObject rootObject = JSONObject.fromObject(out);
        // print(rootObject);
        assertEquals(5, rootObject.get("totalFeatures"));
        assertEquals(5, rootObject.get("numberMatched"));
        assertEquals(2, rootObject.get("numberReturned"));
        JSONArray links = rootObject.getJSONArray("links");
        assertNotNull(links);
        assertEquals(1, links.size());
        JSONObject link = links.getJSONObject(0);
        assertLink(
                link,
                "next page",
                "application/json",
                "next",
                "http://localhost:8080/geoserver"
                        + "/wfs?TYPENAME=sf%3APrimitiveGeoFeature&REQUEST=GetFeature"
                        + "&OUTPUTFORMAT=application%2Fjson&VERSION=2.0.0&COUNT=2&STARTINDEX=2");
    }

    @Test
    public void getGetFeatureWithPagingMidPage() throws Exception {
        // request with paging
        String out = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature"
                + "&startIndex=2&&count=2&outputformat="
                + JSONType.json);
        JSONObject rootObject = JSONObject.fromObject(out);
        print(rootObject);
        assertEquals(5, rootObject.get("totalFeatures"));
        assertEquals(5, rootObject.get("numberMatched"));
        assertEquals(2, rootObject.get("numberReturned"));
        JSONArray links = rootObject.getJSONArray("links");
        assertNotNull(links);
        assertEquals(2, links.size());
        JSONObject prev = links.getJSONObject(0);
        assertLink(
                prev,
                "previous page",
                "application/json",
                "previous",
                "http://localhost:8080/geoserver"
                        + "/wfs?TYPENAME=sf%3APrimitiveGeoFeature&REQUEST=GetFeature"
                        + "&OUTPUTFORMAT=application%2Fjson&VERSION=2.0.0&COUNT=2&STARTINDEX=0");
        JSONObject next = links.getJSONObject(1);
        assertLink(
                next,
                "next page",
                "application/json",
                "next",
                "http://localhost:8080/geoserver"
                        + "/wfs?TYPENAME=sf%3APrimitiveGeoFeature&REQUEST=GetFeature"
                        + "&OUTPUTFORMAT=application%2Fjson&VERSION=2.0.0&COUNT=2&STARTINDEX=4");
    }

    @Test
    public void getGetFeatureWithPagingLastPage() throws Exception {
        // request with paging
        String out = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature"
                + "&startIndex=4&&count=2&outputformat="
                + JSONType.json);
        JSONObject rootObject = JSONObject.fromObject(out);
        print(rootObject);
        assertEquals(5, rootObject.get("totalFeatures"));
        assertEquals(5, rootObject.get("numberMatched"));
        assertEquals(1, rootObject.get("numberReturned"));
        JSONArray links = rootObject.getJSONArray("links");
        assertNotNull(links);
        assertEquals(1, links.size());
        JSONObject prev = links.getJSONObject(0);
        assertLink(
                prev,
                "previous page",
                "application/json",
                "previous",
                "http://localhost:8080/geoserver"
                        + "/wfs?TYPENAME=sf%3APrimitiveGeoFeature&REQUEST=GetFeature"
                        + "&OUTPUTFORMAT=application%2Fjson&VERSION=2.0.0&COUNT=2&STARTINDEX=2");
    }

    private void assertLink(JSONObject link, String title, String type, String rel, String href) {
        assertNotNull(link);
        assertEquals(title, link.getString("title"));
        assertEquals(type, link.getString("type"));
        assertEquals(rel, link.getString("rel"));
        // a bit too rigid, the order of kvp params does not really matter, but good enough for now
        assertEquals(href, link.getString("href"));
    }

    // Checks that the result is in EAST_NORTH/LON_LAT order regardless of the source order
    protected void doAxisSwapTest(QName layer, CRS.AxisOrder sourceOrder) throws Exception {
        // Failure here means the setup for the test is broken and would invalidate the test
        assertThat(
                CRS.getAxisOrder(getCatalog()
                        .getFeatureTypeByName(layer.getPrefix(), layer.getLocalPart())
                        .getCRS()),
                is(sourceOrder));

        JSONObject collection = (JSONObject) getAsJSON("wfs?request=GetFeature&version=1.0.0&typename="
                + getLayerId(layer)
                + "&outputformat="
                + JSONType.json);
        // print(collection);
        assertThat(collection.getInt("totalFeatures"), is(3));
        // assertEquals("4327",
        // collection.getJSONObject("crs").getJSONObject("properties").getString("code"));
        JSONArray features = collection.getJSONArray("features");
        assertThat((Collection<?>) features, Matchers.hasSize(3));
        JSONObject feature = features.getJSONObject(0);

        JSONObject geometry = feature.getJSONObject("geometry");
        assertThat(geometry.getString("type"), is("Point"));

        JSONArray coords = geometry.getJSONArray("coordinates");
        assertThat((Iterable<?>) coords, contains(120, 0));

        JSONArray bbox = collection.getJSONArray("bbox");
        assertThat((Iterable<?>) bbox, Matchers.contains(-170, -30, 120, 45));

        CoordinateReferenceSystem expectedCrs = CRS.decode("EPSG:4326");
        JSONObject aCRS = collection.getJSONObject("crs");
        assertThat(aCRS, encodesCRS(expectedCrs));
    }

    @Test
    public void testGetFeatureAxisSwap() throws Exception {
        // Check that a NORTH_EAST source is swapped
        doAxisSwapTest(POINT_LATLON, CRS.AxisOrder.NORTH_EAST);
    }

    @Test
    public void testGetFeatureNoAxisSwap() throws Exception {
        // Check that an EAST_NORTH source is not swapped
        doAxisSwapTest(POINT_LONLAT, CRS.AxisOrder.EAST_NORTH);
    }

    private org.hamcrest.Matcher<JSONObject> encodesCRS(final CoordinateReferenceSystem crs) {
        return new org.hamcrest.BaseMatcher<>() {

            @Override
            public boolean matches(Object item) {
                // Try to decode the CRS with both axis orders and check if either matches against
                // the expected CRS.  Sorry, this is a horrible hack. KS
                CoordinateReferenceSystem decodedDefault = decodeCRS((JSONObject) item, false);
                if (CRS.equalsIgnoreMetadata(crs, decodedDefault)) return true;
                CoordinateReferenceSystem decodedXY = decodeCRS((JSONObject) item, true);
                if (CRS.equalsIgnoreMetadata(crs, decodedXY)) return true;
                String identifier =
                        ((JSONObject) item).getJSONObject("properties").getString("name");
                Pattern p = Pattern.compile("^urn:ogc:def:crs:EPSG:[^:]*:(\\d+)$");
                Matcher m = p.matcher(identifier);
                if (m.matches()) {
                    String code = "EPSG:" + m.group(1);
                    CoordinateReferenceSystem decodedStripped;
                    try {
                        decodedStripped = CRS.decode(code, true);
                    } catch (FactoryException e) {
                        throw new IllegalStateException(e);
                    }
                    if (CRS.equalsIgnoreMetadata(crs, decodedStripped)) return true;
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("JSON representation of CRS ");
                description.appendValue(crs);
            }
        };
    }

    private static CoordinateReferenceSystem decodeCRS(JSONObject json, boolean forceXY) {
        if (!json.getString("type").equals("name")) throw new IllegalArgumentException();
        String identifier = json.getJSONObject("properties").getString("name");
        try {
            return CRS.decode(identifier, forceXY);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testIAULayer() throws Exception {
        JSONObject json = (JSONObject)
                getAsJSON("wfs?request=GetFeature&version=2.0.0&typename=iau:MarsPoi&outputformat=" + JSONType.json);
        // print(json);
        String crs = json.getJSONObject("crs").getJSONObject("properties").getString("name");
        assertEquals("urn:ogc:def:crs:IAU::49900", crs);
    }
}
