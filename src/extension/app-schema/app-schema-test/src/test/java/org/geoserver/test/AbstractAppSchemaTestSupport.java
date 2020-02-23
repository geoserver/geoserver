/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.WFSInfo;
import org.geotools.appschema.jdbc.NestedFilterToSQL;
import org.geotools.appschema.resolver.xml.AppSchemaValidator;
import org.geotools.appschema.resolver.xml.AppSchemaXSDRegistry;
import org.geotools.data.DataAccess;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.geotools.data.complex.DataAccessRegistry;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.PreparedFilterToSQL;
import org.geotools.jdbc.PreparedStatementSQLDialect;
import org.geotools.jdbc.SQLDialect;
import org.geotools.xml.resolver.SchemaCache;
import org.geotools.xml.resolver.SchemaCatalog;
import org.geotools.xml.resolver.SchemaResolver;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for WFS (and WMS) test cases that test integration of {@link
 * AppSchemaDataAccess} with GeoServer.
 *
 * <p>The implementation takes care to ensure that private {@link XMLUnit} namespace contexts are
 * used for each mock data instance, to avoid collisions. Use of static {@link XMLAssert} methods
 * risks collisions in the static namespace context. This class avoids such problems by providing
 * its own instance methods like those in XMLAssert.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public abstract class AbstractAppSchemaTestSupport extends GeoServerSystemTestSupport {

    /**
     * The namespace URI used internally in the DOM to qualify the name of an "xmlns:" attribute.
     * Note that "xmlns:" attributes are not accessible via XMLUnit XPathEngine, so testing these
     * can only be performed by examining the DOM.
     *
     * @see <a href="http://www.w3.org/2000/xmlns/">http://www.w3.org/2000/xmlns/</a>
     */
    protected static final String XMLNS = "http://www.w3.org/2000/xmlns/";

    /** WFS namespaces, for use by XMLUnit. A seen in WFSTestSupport, plus xlink. */
    @SuppressWarnings("serial")
    private final Map<String, String> WFS_NAMESPACES =
            Collections.unmodifiableMap(
                    new HashMap<String, String>() {
                        {
                            put("wfs", "http://www.opengis.net/wfs");
                            put("ows", "http://www.opengis.net/ows");
                            put("ogc", "http://www.opengis.net/ogc");
                            put("xs", "http://www.w3.org/2001/XMLSchema");
                            put("xsd", "http://www.w3.org/2001/XMLSchema");
                            put("gml", "http://www.opengis.net/gml");
                            put("xlink", "http://www.w3.org/1999/xlink");
                            put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
                            put(
                                    "wms",
                                    "http://www.opengis.net/wms"); // NC - wms added for wms tests
                        }
                    });

    /** The XpathEngine to be used for this namespace context. */
    private XpathEngine xpathEngine;

    /** SchemaCatalog to work with AppSchemaValidator for test requests validation. */
    private SchemaCatalog catalog;

    /**
     * Subclasses override this to construct the test data.
     *
     * <p>Override to narrow return type and remove checked exception.
     *
     * @see org.geoserver.test.GeoServerAbstractTestSupport#buildTestData()
     */
    @Override
    protected abstract AbstractAppSchemaMockData createTestData();

    /**
     * Configure WFS to encode canonical schema location and use featureMember.
     *
     * <p>FIXME: These settings should go in wfs.xml for the mock data when tests migrated to new
     * data directory format. Have to do it programmatically for now. To do this insert in wfs.xml
     * just after the <tt>featureBounding</tt> setting:
     *
     * <ul>
     *   <li><tt>&lt;canonicalSchemaLocation&gt;true&lt;/canonicalSchemaLocation&gt;<tt>
     *   <li><tt>&lt;encodeFeatureMember&gt;true&lt;/encodeFeatureMember&gt;<tt>
     * </ul>
     */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setCanonicalSchemaLocation(true);
        wfs.setEncodeFeatureMember(true);
        getGeoServer().save(wfs);
        // disable schema caching in tests, as schemas are expected to provided on the classpath
        SchemaCache.disableAutomaticConfiguration();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Unregister all data access from registry to avoid stale data access being used by other unit
     * tests.
     */
    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        DataAccessRegistry.unregisterAndDisposeAll();
        AppSchemaDataAccessRegistry.clearAppSchemaProperties();
        AppSchemaXSDRegistry.getInstance().dispose();
        catalog = null;
    }

    /**
     * Return the test data.
     *
     * <p>Override to narrow return type.
     *
     * @see org.geoserver.test.GeoServerAbstractTestSupport#getTestData()
     */
    @Override
    public AbstractAppSchemaMockData getTestData() {
        return (AbstractAppSchemaMockData) super.getTestData();
    }

    /** Returns the map of namespace prefix to URI configured in the test data. */
    public Map<String, String> getNamespaces() {
        return getTestData().getNamespaces();
    }

    /** Returns the namespace URI for a given prefix configured in the test data. */
    public String getNamespace(String prefix) {
        return getNamespaces().get(prefix);
    }

    /**
     * Return the response for a GET request for a path (starts with "wfs?").
     *
     * <p>Override to remove checked exception.
     *
     * @see org.geoserver.test.GeoServerAbstractTestSupport#get(java.lang.String)
     */
    @Override
    protected InputStream get(String path) {
        try {
            return super.get(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected InputStream getBinary(String path) {
        try {
            return getBinaryInputStream(getAsServletResponse(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the response for a GET request for a path (starts with "wfs?").
     *
     * <p>Override to remove checked exception.
     *
     * @see org.geoserver.test.GeoServerAbstractTestSupport#getAsDOM(java.lang.String)
     */
    @Override
    protected Document getAsDOM(String path) {
        try {
            return super.getAsDOM(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the response for a POST request to a path (typically "wfs"). The request XML is a
     * String.
     *
     * <p>Override to remove checked exception.
     *
     * @see org.geoserver.test.GeoServerAbstractTestSupport#post(java.lang.String, java.lang.String)
     */
    @Override
    protected InputStream post(String path, String xml) {
        try {
            return super.post(path, xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the response for a POST request to a path (typically "wfs"). The request XML is a
     * String.
     *
     * <p>Override to remove checked exception.
     *
     * @see org.geoserver.test.GeoServerAbstractTestSupport#postAsDOM(java.lang.String,
     *     java.lang.String)
     */
    @Override
    protected Document postAsDOM(String path, String xml) {
        try {
            return super.postAsDOM(path, xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the XpathEngine, configured for this namespace context.
     *
     * <p>Note that the engine is configured lazily, to ensure that the mock data has been created
     * and is ready to report data namespaces, which are then put into the namespace context.
     *
     * @return configured XpathEngine
     */
    private XpathEngine getXpathEngine() {
        if (xpathEngine == null) {
            xpathEngine = XMLUnit.newXpathEngine();
            Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.putAll(WFS_NAMESPACES);
            namespaces.putAll(getTestData().getNamespaces());
            xpathEngine.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        }
        return xpathEngine;
    }

    /**
     * Return the SchemaCatalog to resolve local schemas.
     *
     * @return SchemaCatalog
     */
    private SchemaCatalog getSchemaCatalog() {
        if (catalog == null) {
            if (testData instanceof AbstractAppSchemaMockData) {
                catalog = ((AbstractAppSchemaMockData) testData).getSchemaCatalog();
            }
        }
        return catalog;
    }

    /**
     * Return the flattened value corresponding to an XPath expression from a document.
     *
     * @param xpath XPath expression
     * @param document the document under test
     * @return flattened string value
     */
    protected String evaluate(String xpath, Document document) {
        try {
            return getXpathEngine().evaluate(xpath, document);
        } catch (XpathException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the list of nodes in a document that match an XPath expression.
     *
     * @param xpath XPath expression
     * @param document the document under test
     * @return list of matching nodes
     */
    protected NodeList getMatchingNodes(String xpath, Document document) {
        try {
            return getXpathEngine().getMatchingNodes(xpath, document);
        } catch (XpathException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Assertion that the flattened value of an XPath expression in document is equal to the
     * expected value.
     *
     * @param expected expected value of expression
     * @param xpath XPath expression
     * @param document the document under test
     */
    protected void assertXpathEvaluatesTo(String expected, String xpath, Document document) {
        assertEquals(expected, evaluate(xpath, document));
    }

    /**
     * Assert that there are count matches of and XPath expression in a document.
     *
     * @param count expected number of matches
     * @param xpath XPath expression
     * @param document document under test
     */
    protected void assertXpathCount(int count, String xpath, Document document) {
        assertEquals(count, getMatchingNodes(xpath, document).getLength());
    }

    /**
     * Assert that the flattened value of an XPath expression in a document matches a regular
     * expression.
     *
     * @param regex regular expression that must be matched
     * @param xpath XPath expression
     * @param document document under test
     */
    protected void assertXpathMatches(String regex, String xpath, Document document) {
        assertTrue(evaluate(xpath, document).matches(regex));
    }

    /**
     * Assert that the flattened value of an XPath expression in a document doe not match a regular
     * expression.
     *
     * @param regex regular expression that must not be matched
     * @param xpath XPath expression
     * @param document document under test
     */
    protected void assertXpathNotMatches(String regex, String xpath, Document document) {
        assertFalse(evaluate(xpath, document).matches(regex));
    }

    /**
     * Return {@link Document} as a pretty-printed string.
     *
     * @param document document to be prettified
     * @return the prettified string
     */
    protected String prettyString(Document document) {
        OutputStream output = new ByteArrayOutputStream();
        prettyPrint(document, output);
        return output.toString();
    }

    /**
     * Pretty-print a {@link Document} to an {@link OutputStream}.
     *
     * @param document document to be prettified
     * @param output stream to which output is written
     */
    protected void prettyPrint(Document document, OutputStream output) {
        try {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.INDENT, "yes");
            tx.transform(new DOMSource(document), new StreamResult(output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find the first file matching the supplied path, starting from the supplied root. This doesn't
     * support multiple matching files.
     *
     * @param path Supplied path
     * @param root Directory to start searching from
     * @return Matching file
     */
    protected File findFile(String path, File root) {
        File target = null;
        List<File> files = Arrays.asList(root.listFiles());
        String[] steps = path.split("/");
        for (int i = 0; i < steps.length; i++) {
            for (File file : files) {
                if (file.getName().equals(steps[i])) {
                    if (i < steps.length - 1) {
                        return findFile(path.substring(steps[i].length() + 1, path.length()), file);
                    } else {
                        return file;
                    }
                }
            }
        }
        return target;
    }

    /**
     * Schema-validate the response for a GET request for a path (starts with "wfs?"). Validation is
     * against schemas found on the classpath. See {@link
     * SchemaResolver#getSimpleHttpResourcePath(java.net.URI)} for URL-to-classpath convention.
     *
     * <p>If validation fails, a {@link RuntimeException} is thrown with detail containing the
     * failure messages. The failure messages are also logged.
     *
     * @param path GET request (starts with "wfs?")
     * @throws RuntimeException if validation fails
     */
    protected void validateGet(String path) {
        try {
            AppSchemaValidator.validate(get(path), getSchemaCatalog());
        } catch (RuntimeException e) {
            LOGGER.severe(e.getMessage());
            throw e;
        }
    }

    /**
     * Schema-validate the response for a POST request to a path (typically "wfs"). Validation is
     * against schemas found on the classpath. See {@link
     * SchemaResolver#getSimpleHttpResourcePath(java.net.URI)} for URL-to-classpath convention.
     *
     * <p>If validation fails, a {@link RuntimeException} is thrown with detail containing the
     * failure messages. The failure messages are also logged.
     *
     * @param path request path (typically "wfs")
     * @param xml the request XML document
     * @throws RuntimeException if validation fails
     */
    protected void validatePost(String path, String xml) {
        try {
            AppSchemaValidator.validate(post(path, xml), getSchemaCatalog());
        } catch (RuntimeException e) {
            LOGGER.severe(e.getMessage());
            throw e;
        }
    }

    /**
     * Schema-validate an XML instance document in a string. Validation is against schemas found on
     * the classpath. See {@link AppSchemaResolver#getSimpleHttpResourcePath(java.net.URI)} for
     * URL-to-classpath convention.
     *
     * <p>If validation fails, a {@link RuntimeException} is thrown with detail containing the
     * failure messages. The failure messages are also logged.
     *
     * @param path request path (typically "wfs")
     * @param xml the XML instance document
     * @throws RuntimeException if validation fails
     */
    protected void validate(String xml) {
        try {
            AppSchemaValidator.validate(xml, getSchemaCatalog());
        } catch (RuntimeException e) {
            LOGGER.severe(e.getMessage());
            throw e;
        }
    }

    /**
     * For WMS tests.
     *
     * <p>Asserts that the image is not blank, in the sense that there must be pixels different from
     * the passed background color.
     *
     * @param testName the name of the test to throw meaningfull messages if something goes wrong
     * @param image the imgage to check it is not "blank"
     * @param bgColor the background color for which differing pixels are looked for
     */
    protected void assertNotBlank(String testName, BufferedImage image, Color bgColor) {
        int pixelsDiffer = countNonBlankPixels(testName, image, bgColor);
        assertTrue(testName + " image is completely blank", 0 < pixelsDiffer);
    }

    /**
     * For WMS tests.
     *
     * <p>Counts the number of non black pixels
     */
    protected int countNonBlankPixels(String testName, BufferedImage image, Color bgColor) {
        int pixelsDiffer = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != bgColor.getRGB()) {
                    ++pixelsDiffer;
                }
            }
        }

        LOGGER.fine(
                testName
                        + ": pixel count="
                        + (image.getWidth() * image.getHeight())
                        + " non bg pixels: "
                        + pixelsDiffer);
        return pixelsDiffer;
    }

    /**
     * Checks that the identifiers of the features in the provided collection match the specified
     * ids.
     *
     * <p>Note that:
     *
     * <ul>
     *   <li>The method considers that feature identifiers follow the convention <code>
     *       [type name].[ID]</code> and only matches the ID part.
     *   <li>If the feature collection contains a feature whose identifier does not match any of the
     *       passed ids, the check will fail
     * </ul>
     *
     * @param featureSet the feature collection to check
     * @param fids the feature identifiers that must be present in the collection
     */
    protected void assertContainsFeatures(FeatureCollection featureSet, String... fids) {
        List<String> fidList = Arrays.asList(fids);

        try (FeatureIterator it = featureSet.features()) {
            int count = 0;
            while (it.hasNext()) {
                Feature f = it.next();
                String[] parts = f.getIdentifier().getID().split("\\.");
                String fid = parts[parts.length - 1];
                assertTrue(fidList.contains(fid));
                count++;
            }
            assertEquals(fidList.size(), count);
        }
    }

    /**
     * Checks that all the pre-conditions for SQL encoding filters on nested attributes are met:
     *
     * <ol>
     *   <li>Source datastore is backed by a RDBMS
     *   <li>Joining support is enabled
     *   <li>Nested filters encoding is enabled
     * </ol>
     *
     * <p>If the method returns <code>false</code> the test should be skipped.
     *
     * @param rootMapping the feature type being queried
     * @return <code>true</code> if nested filters encoding can be tested, <code>false</code>
     *     otherwise.
     */
    protected boolean shouldTestNestedFiltersEncoding(FeatureTypeMapping rootMapping) {
        if (!(rootMapping.getSource().getDataStore() instanceof JDBCDataStore)) return false;
        if (!AppSchemaDataAccessConfigurator.isJoining()) return false;
        if (!AppSchemaDataAccessConfigurator.shouldEncodeNestedFilters()) return false;
        return true;
    }

    /**
     * Creates a properly configured {@link NestedFilterToSQL} instance to enable testing the SQL
     * encoding of filters on nested attributes.
     *
     * <p>Note: before calling this method, clients should verify that nested filters encoding is
     * enabled by calling {@link #shouldTestNestedFiltersEncoding(FeatureTypeMapping)}.
     *
     * @param mapping the feature type being queried
     * @return nested filter encoder
     */
    protected NestedFilterToSQL createNestedFilterEncoder(FeatureTypeMapping mapping) {
        DataAccess<?, ?> source = mapping.getSource().getDataStore();
        if (!(source instanceof JDBCDataStore)) {
            throw new IllegalArgumentException(
                    "nested filters encoding requires the source datastore be a JDBCDataStore");
        }
        JDBCDataStore store = (JDBCDataStore) source;
        SQLDialect dialect = store.getSQLDialect();
        FilterToSQL original = null;
        if (dialect instanceof BasicSQLDialect) {
            original = ((BasicSQLDialect) dialect).createFilterToSQL();
        } else if (dialect instanceof PreparedStatementSQLDialect) {
            original = ((PreparedStatementSQLDialect) dialect).createPreparedFilterToSQL();
            // disable prepared statements to have literals actually encoded in the SQL
            ((PreparedFilterToSQL) original).setPrepareEnabled(false);
        }
        original.setFeatureType((SimpleFeatureType) mapping.getSource().getSchema());

        NestedFilterToSQL nestedFilterToSQL = new NestedFilterToSQL(mapping, original);
        nestedFilterToSQL.setInline(true);
        return nestedFilterToSQL;
    }

    /**
     * Utility method that converts a XML document object to a string.
     *
     * @param document Xml Document to parse
     * @return String representation of xml document
     */
    protected static String toString(Document document) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Helper method that reads a resource from the class path converting it to text.
     *
     * @param resourcePath non relative path to the class path resource
     * @return the content of the resource as text
     */
    protected String readResource(String resourcePath) {
        try (InputStream input =
                NormalizedMultiValuesTest.class.getResourceAsStream(resourcePath)) {
            return IOUtils.toString(input);
        } catch (Exception exception) {
            throw new RuntimeException(String.format("Error reading resource '%s'.", resourcePath));
        }
    }

    /** Drills into nested JSON objects (won't traverse arrays though) */
    protected JSONObject getNestedObject(JSONObject root, String... keys) {
        JSONObject curr = root;
        for (String key : keys) {
            if (!curr.has(key)) {
                fail("Could not find property " + key + " in " + curr);
            }
            curr = curr.getJSONObject(key);
        }
        return curr;
    }

    /**
     * Helper method that just extracts \ looks for a station in the provided GeoJSON response based
     * on its ID.
     */
    protected JSONObject getFeaturePropertiesById(JSON geoJson, String id) {
        assertThat(geoJson, instanceOf(JSONObject.class));
        JSONObject json = (JSONObject) geoJson;
        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            if (Objects.equals(id, feature.get("id"))) {
                // we found the feature we are looking for
                return feature.getJSONObject("properties");
            }
        }
        // feature matching the provided ID not found
        return null;
    }
}
