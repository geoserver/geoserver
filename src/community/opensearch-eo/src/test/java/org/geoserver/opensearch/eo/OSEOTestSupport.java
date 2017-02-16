/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Node;

/**
 * Base class for OpenSeach tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOTestSupport extends GeoServerSystemTestSupport {

    private SimpleNamespaceContext namespaceContext;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data to setup
    }
    
    @Before
    public void setupNamespaces() {
        this.namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("os", "http://a9.com/-/spec/opensearch/1.1/");
    }
    
    protected Matcher<Node> hasXPath(String xPath) {
        return Matchers.hasXPath(xPath, namespaceContext);
    }
}
