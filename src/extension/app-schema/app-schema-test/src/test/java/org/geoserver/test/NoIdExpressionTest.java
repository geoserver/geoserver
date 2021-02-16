/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.util.Collections;
import org.junit.Test;
import org.w3c.dom.Document;

/** Checks having two nested attributes with the same xpath and different FEATURE_LINK */
public class NoIdExpressionTest extends AbstractAppSchemaTestSupport {

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
                    "/test-data/stations/noIdExpr/stations.xml",
                    Collections.emptyMap(),
                    "/test-data/stations/noIdExpr/stations.xsd",
                    "/test-data/stations/noIdExpr/institutes.xml",
                    "/test-data/stations/noIdExpr/persons.xml",
                    "/test-data/stations/noIdExpr/stations.properties",
                    "/test-data/stations/noIdExpr/institutes.properties",
                    "/test-data/stations/noIdExpr/persons.properties");
        }
    }

    @Test
    public void testGetFeatureSimpleFilter() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=st:Station&maxFeatures=50&cql_filter=st:name = 'station1'");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=st:Station&maxFeatures=50&cql_filter=st:name = 'station1' Response:\n"
                        + prettyString(doc));

        assertXpathCount(1, "//st:Station", doc);
        assertXpathEvaluatesTo("station1", "//st:Station/st:name", doc);
    }

    @Test
    public void testGetFeatureNestedFilter() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=st:Station&maxFeatures=50&startIndex=2");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=st:Station&maxFeatures=50&startIndex=1 Response:\n"
                        + prettyString(doc));

        assertXpathCount(1, "//st:Station", doc);
        assertXpathEvaluatesTo("station3", "//st:Station/st:name", doc);
    }

    @Test
    public void testGetFeatureStartIndex() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=st:Station&maxFeatures=50&startIndex=2");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=st:Station&maxFeatures=50&startIndex=2 Response:\n"
                        + prettyString(doc));

        assertXpathCount(1, "//st:Station", doc);
        assertXpathEvaluatesTo("station3", "//st:Station/st:name", doc);
    }
}
