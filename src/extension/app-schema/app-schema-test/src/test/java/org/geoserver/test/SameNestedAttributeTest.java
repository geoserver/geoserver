/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Checks having two nested attributes with the same xpath and different FEATURE_LINK */
public class SameNestedAttributeTest extends AbstractAppSchemaTestSupport {

    private static final String STATIONS_PREFIX = "st";
    private static final String STATIONS_URI = "http://www.stations.org/1.0";

    @Override
    protected StationsMockData createTestData() {
        return new MockData();
    }

    private static final class MockData extends StationsMockData {
        @Override
        public void addContent() {
            // add stations namespaces
            putNamespace(STATIONS_PREFIX, STATIONS_URI);
            // add stations feature types
            addAppSchemaFeatureType(
                    STATIONS_PREFIX,
                    null,
                    "Station",
                    "/test-data/stations/sameNameAttribute/stations.xml",
                    Collections.emptyMap(),
                    "/test-data/stations/sameNameAttribute/stations.xsd",
                    "/test-data/stations/sameNameAttribute/institutes.xml",
                    "/test-data/stations/sameNameAttribute/persons.xml",
                    "/test-data/stations/sameNameAttribute/stations.properties",
                    "/test-data/stations/sameNameAttribute/institutes.properties",
                    "/test-data/stations/sameNameAttribute/persons.properties");
        }
    }

    @Test
    public void testWfsGetFeatureWithAdvancedNestedFilter() throws Exception {
        // execute the WFS 2.0 request
        MockHttpServletResponse response =
                postAsServletResponse(
                        "wfs",
                        readResource(
                                "/test-data/stations/sameNameAttribute/requests/wfs_get_feature_1.xml"));
        // check that station 1 was returned
        String content = response.getContentAsString();
        System.out.println(content);
        assertThat(content, containsString("gml:id=\"st.1\""));
        assertThat(content, containsString("gml:id=\"st.2\""));
        assertThat(StringUtils.countMatches(content, "<wfs:member>"), is(2));
    }
}
