/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Validates the encoding of filters on nested properties with some advanced mappings and xpaths.
 */
public final class NestedElementsFilteringTest extends AbstractAppSchemaTestSupport {

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
            // add stations feature types
            addAppSchemaFeatureType(
                    STATIONS_PREFIX,
                    null,
                    "Station",
                    "/test-data/stations/nestedElements/stations.xml",
                    Collections.emptyMap(),
                    "/test-data/stations/nestedElements/stations.xsd",
                    "/test-data/stations/nestedElements/institutes.xml",
                    "/test-data/stations/nestedElements/persons.xml",
                    "/test-data/stations/nestedElements/stations.properties",
                    "/test-data/stations/nestedElements/institutes.properties",
                    "/test-data/stations/nestedElements/persons.properties");
        }
    }

    @Test
    public void testWfsGetFeatureWithAdvancedNestedFilter() throws Exception {
        // execute the WFS 2.0 request
        MockHttpServletResponse response =
                postAsServletResponse(
                        "wfs",
                        readResource(
                                "/test-data/stations/nestedElements/requests/wfs_get_feature_1.xml"));
        // check that station 1 was returned
        String content = response.getContentAsString();
        assertThat(content, containsString("gml:id=\"ins.1\""));
        assertThat(StringUtils.countMatches(content, "<wfs:member>"), is(1));
    }

    /** Checks for included types namespaces on output GML. */
    @Test
    public void testWfsIncludedNamespacesDeclaration() throws Exception {
        // execute the WFS 2.0 request
        Document doc =
                postAsDOM(
                        "wfs",
                        readResource(
                                "/test-data/stations/nestedElements/requests/wfs_get_feature_1.xml"));
        Node perAttributeNode = doc.getFirstChild().getAttributes().getNamedItem("xmlns:per");
        assertTrue(perAttributeNode instanceof Attr);
        Attr perAttr = (Attr) perAttributeNode;
        assertEquals("http://www.person.org/1.0", perAttr.getValue());
    }
}
