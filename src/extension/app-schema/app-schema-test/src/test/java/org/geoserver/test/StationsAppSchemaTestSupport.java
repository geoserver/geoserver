/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.w3c.dom.Document;

/** Test support for GML 3.1 and GML 3.2 Stations data use case */
public abstract class StationsAppSchemaTestSupport extends AbstractAppSchemaTestSupport {

    protected final Map<String, String> GML31_PARAMETERS =
            Collections.unmodifiableMap(
                    Stream.of(
                                    new SimpleEntry<>("GML_PREFIX", "gml31"),
                                    new SimpleEntry<>(
                                            "GML_NAMESPACE", "http://www.opengis.net/gml"),
                                    new SimpleEntry<>(
                                            "GML_LOCATION",
                                            "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd"))
                            .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    protected final Map<String, String> GML32_PARAMETERS =
            Collections.unmodifiableMap(
                    Stream.of(
                                    new SimpleEntry<>("GML_PREFIX", "gml32"),
                                    new SimpleEntry<>(
                                            "GML_NAMESPACE", "http://www.opengis.net/gml/3.2"),
                                    new SimpleEntry<>(
                                            "GML_LOCATION",
                                            "http://schemas.opengis.net/gml/3.2.1/gml.xsd"))
                            .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    // xpath engines used to check WFS responses
    protected XpathEngine WFS11_XPATH_ENGINE;
    protected XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE =
                StationsMockData.buildXpathEngine(
                        getTestData().getNamespaces(),
                        "wfs",
                        "http://www.opengis.net/wfs",
                        "gml",
                        "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE =
                StationsMockData.buildXpathEngine(
                        getTestData().getNamespaces(),
                        "wfs",
                        "http://www.opengis.net/wfs/2.0",
                        "gml",
                        "http://www.opengis.net/gml/3.2");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    /** * GetFeature tests ** */
    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new StationsMockData();
    }

    /**
     * Helper method that evaluates a xpath and checks if the number of nodes found correspond to
     * the expected number,
     */
    protected void checkCount(
            XpathEngine xpathEngine, Document document, int expectedCount, String xpath) {
        try {
            // evaluate the xpath and compare the number of nodes found
            assertThat(
                    xpathEngine.getMatchingNodes(xpath, document).getLength(), is(expectedCount));
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath: " + xpath, exception);
        }
    }
}
