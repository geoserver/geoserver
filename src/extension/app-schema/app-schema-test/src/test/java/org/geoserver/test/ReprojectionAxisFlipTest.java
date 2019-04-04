/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import org.geoserver.catalog.FeatureTypeInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Validates that reprojection and axis flipping are correctly handled. */
public final class ReprojectionAxisFlipTest extends AbstractAppSchemaTestSupport {

    private static final String STATIONS_PREFIX = "st";
    private static final String STATIONS_URI = "http://www.stations.org/1.0";

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    /** Helper class that will setup custom complex feature types using the stations data set. */
    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add stations namespaces
            putNamespace(STATIONS_PREFIX, STATIONS_URI);
            // add stations feature type
            addAppSchemaFeatureType(
                    STATIONS_PREFIX,
                    null,
                    "Station",
                    "/test-data/stations/noDefaultGeometry/stations.xml",
                    Collections.emptyMap(),
                    "/test-data/stations/noDefaultGeometry/stations.xsd",
                    "/test-data/stations/noDefaultGeometry/stations.properties");
        }
    }

    @Before
    public void beforeTest() {
        // set the declared SRS on the feature type info
        setDeclaredCrs("st:Station", "EPSG:4052");
    }

    @Test
    public void testWfsGetFeatureWithBbox() throws Exception {
        genericWfsGetFeatureWithBboxTest(
                () ->
                        getAsServletResponse(
                                "wfs?service=WFS"
                                        + "&version=2.0&request=GetFeature&typeName=st:Station&maxFeatures=1"
                                        + "&outputFormat=gml32&srsName=urn:ogc:def:crs:EPSG::4052&bbox=3,-3,6,0"));
    }

    @Test
    public void testWfsGetFeatureWithBboxPost() throws Exception {
        // execute the WFS 2.0 request
        genericWfsGetFeatureWithBboxTest(
                () ->
                        postAsServletResponse(
                                "wfs",
                                readResource(
                                        "/test-data/stations/noDefaultGeometry/requests/wfs20_get_feature_1.xml")));
    }

    /**
     * Helper method holding the common code used to test a WFS GetFeature operation with a BBOX
     * requiring axis flipping,
     */
    private void genericWfsGetFeatureWithBboxTest(Request request) throws Exception {
        // execute the WFS 2.0 request
        MockHttpServletResponse response = request.execute();
        // check that both stations were returned
        String content = response.getContentAsString();
        assertThat(content, containsString("gml:id=\"st.1\""));
        assertThat(content, containsString("gml:id=\"st.2\""));
        assertThat(countMatches(content, "<wfs:member>"), is(2));
        // check that with no declared SRS no features are returned
        setDeclaredCrs("st:Station", null);
        response = request.execute();
        content = response.getContentAsString();
        assertThat(countMatches(content, "Exception"), is(0));
        assertThat(countMatches(content, "<wfs:member>"), is(0));
    }

    /**
     * Helper method that sets the provided SRS as the declared SRS on feature type info
     * corresponding to provided feature type name.
     */
    private void setDeclaredCrs(String featureTypeName, String srs) {
        // get the feature type info
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        assertThat(info, notNullValue());
        // set the declared SRS
        info.setSRS(srs);
        getCatalog().save(info);
    }

    @FunctionalInterface
    private interface Request {

        // executes a request allowing an exception to be throw
        MockHttpServletResponse execute() throws Exception;
    }
}
