/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Base class for OpenSeach tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOTestSupport extends GeoServerSystemTestSupport {

    private static Schema OS_SCHEMA;

    private SimpleNamespaceContext namespaceContext;

    static {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            OS_SCHEMA = factory
                    .newSchema(OSEOTestSupport.class.getResource("/schemas/OpenSearch.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the OpenSearch schemas", e);
        }
    }

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

    protected Matcher<Node> hasXPath(String xPath, Matcher<String> valueMatcher) {
        return Matchers.hasXPath(xPath, namespaceContext, valueMatcher);
    }

    protected void checkValidOSDD(Document d) throws SAXException, IOException {
        checkValidationErrors(d, OS_SCHEMA);
    }
}
