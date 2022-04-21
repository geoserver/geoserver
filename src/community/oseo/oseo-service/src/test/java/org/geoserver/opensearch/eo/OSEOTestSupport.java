/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport.setupBasicOpenSearch;
import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest.GS_PRODUCT;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.Filter;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletResponse;
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

    private static Schema ATOM_SCHEMA;

    protected SimpleNamespaceContext namespaceContext;

    static {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            OS_SCHEMA =
                    factory.newSchema(OSEOTestSupport.class.getResource("/schemas/OpenSearch.xsd"));
            ATOM_SCHEMA =
                    factory.newSchema(
                            OSEOTestSupport.class.getResource("/schemas/searchResults.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the OpenSearch schemas", e);
        }
    }

    private static Schema getOsSchema() {
        if (OS_SCHEMA == null) {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                OS_SCHEMA =
                        factory.newSchema(
                                OSEOTestSupport.class.getResource("/schemas/OpenSearch.xsd"));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse the OpenSearch schemas", e);
            }
        }

        return OS_SCHEMA;
    }

    private static Schema getAtomSchema() {
        if (ATOM_SCHEMA == null) {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                ATOM_SCHEMA =
                        factory.newSchema(
                                OSEOTestSupport.class.getResource("/schemas/searchResults.xsd"));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse the OpenSearch schemas", e);
            }
        }
        return ATOM_SCHEMA;
    }

    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList(new OSEOFilter());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data to setup
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.ENGLISH);
        super.onSetUp(testData);

        GeoServer geoServer = getGeoServer();
        setupBasicOpenSearch(testData, getCatalog(), geoServer, populateGranulesTable());

        // add the custom product class and attribution
        OSEOInfo oseo = geoServer.getService(OSEOInfo.class);
        oseo.getProductClasses().add(JDBCOpenSearchAccessTest.GS_PRODUCT);
        oseo.setAttribution("Copyright 2020-2030, GeoServer");
        geoServer.save(oseo);
    }

    /** Allows subclasses to decide if to populate the granules table, or not */
    protected boolean populateGranulesTable() {
        return false;
    }

    @BeforeClass
    public static void checkOnLine() {
        GeoServerOpenSearchTestSupport.checkOnLine();
    }

    @Before
    public void setupNamespaces() {
        this.namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("atom", "http://www.w3.org/2005/Atom");
        namespaceContext.bindNamespaceUri("os", "http://a9.com/-/spec/opensearch/1.1/");
        namespaceContext.bindNamespaceUri(
                "param", "http://a9.com/-/spec/opensearch/extensions/parameters/1.0/");
        namespaceContext.bindNamespaceUri("at", "http://www.w3.org/2005/Atom");
        namespaceContext.bindNamespaceUri("gml", "http://www.opengis.net/gml/3.2");
        namespaceContext.bindNamespaceUri("georss", "http://www.georss.org/georss");
        namespaceContext.bindNamespaceUri("eo", OpenSearchAccess.EO_NAMESPACE);
        namespaceContext.bindNamespaceUri("geo", OpenSearchAccess.GEO_NAMESPACE);
        namespaceContext.bindNamespaceUri("gmi", "http://www.isotc211.org/2005/gmi");
        namespaceContext.bindNamespaceUri("gmd", "http://www.isotc211.org/2005/gmd");
        namespaceContext.bindNamespaceUri("gco", "http://www.isotc211.org/2005/gco");
        namespaceContext.bindNamespaceUri("gmx", "http://www.isotc211.org/2005/gmx");
        namespaceContext.bindNamespaceUri("time", "http://a9.com/-/opensearch/extensions/time/1.0");
        namespaceContext.bindNamespaceUri("owc", "http://www.opengis.net/owc/1.0");
        namespaceContext.bindNamespaceUri("dc", "http://purl.org/dc/elements/1.1/");
        namespaceContext.bindNamespaceUri("media", "http://search.yahoo.com/mrss/");
        namespaceContext.bindNamespaceUri("opt", "http://www.opengis.net/opt/2.1");
        namespaceContext.bindNamespaceUri("om", "http://www.opengis.net/om/2.0");
        namespaceContext.bindNamespaceUri("eop", "http://www.opengis.net/eop/2.1");
        namespaceContext.bindNamespaceUri("ows", "http://www.opengis.net/ows/2.0");
        namespaceContext.bindNamespaceUri("xlink", "http://www.w3.org/1999/xlink");
        for (ProductClass pc : ProductClass.DEFAULT_PRODUCT_CLASSES) {
            namespaceContext.bindNamespaceUri(pc.getPrefix(), pc.getNamespace());
        }
        namespaceContext.bindNamespaceUri(GS_PRODUCT.getPrefix(), GS_PRODUCT.getNamespace());
    }

    protected Matcher<Node> hasXPath(String xPath) {
        return Matchers.hasXPath(xPath, namespaceContext);
    }

    protected Matcher<Node> hasXPath(String xPath, Matcher<String> valueMatcher) {
        return Matchers.hasXPath(xPath, namespaceContext, valueMatcher);
    }

    protected void checkValidOSDD(Document d) throws SAXException, IOException {
        checkValidationErrors(d, getOsSchema());
    }

    protected void checkValidAtomFeed(Document d) throws SAXException, IOException {
        // TODO: we probably need to enrich this with EO specific elements check
        checkValidationErrors(d, getAtomSchema());
    }

    /** Checks the response is a RSS and */
    protected Document getAsOpenSearchException(String path, int expectedStatus) throws Exception {
        return getAsDOM(path, expectedStatus, "application/xml"); // OSEOExceptionHandler.RSS_MIME);
    }

    /**
     * Returns the DOM after checking the status code is 200 and the returned mime type is the
     * expected one
     */
    protected Document getAsDOM(String path, int expectedStatusCode, String expectedMimeType)
            throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(expectedMimeType, response.getContentType());
        assertEquals(expectedStatusCode, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        return dom;
    }

    /**
     * Copies the given template from the classpath to the data directory. Lookup is relative to the
     * test class.
     */
    protected void copyTemplate(String template) throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();
        Resource target = dd.get("templates/os-eo/", template);
        if (getClass().getResource(template) == null)
            throw new IllegalArgumentException(
                    "Could not find " + template + " relative to " + getClass());
        try (InputStream is = getClass().getResourceAsStream(template);
                OutputStream os = target.out()) {
            IOUtils.copy(is, os);
        }
    }
}
