/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.jdbc.JDBCDataStore;
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
    
    private static Schema ATOM_SCHEMA;

    private SimpleNamespaceContext namespaceContext;

    static {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            OS_SCHEMA = factory
                    .newSchema(OSEOTestSupport.class.getResource("/schemas/OpenSearch.xsd"));
            ATOM_SCHEMA = factory
                    .newSchema(OSEOTestSupport.class.getResource("/schemas/searchResults.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the OpenSearch schemas", e);
        }
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data to setup
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        setupBasicOpenSearch(testData, getCatalog(), getGeoServer());
    }

    /**
     * Sets up a H2 based OpenSearchAccess and configures OpenSearch for EO to use it
     * @param testData
     * @param cat
     * @param gs
     * @throws IOException
     * @throws SQLException
     */
    public static void setupBasicOpenSearch(SystemTestData testData, Catalog cat, GeoServer gs) throws IOException, SQLException {
        // create the plain database
        DataStoreInfo jdbcDs = cat.getFactory().createDataStore();
        jdbcDs.setName("oseo_jdbc");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        jdbcDs.setWorkspace(ws);
        jdbcDs.setEnabled(true);

        Map params = jdbcDs.getConnectionParameters();
        params.put("dbtype", "h2");
        File dbFolder = new File(testData.getDataDirectoryRoot(), "oseo_db");
        FileUtils.deleteQuietly(dbFolder);
        dbFolder.mkdir();
        File dbFile = new File(dbFolder, "oseo_db");
        params.put("database", dbFile.getAbsolutePath());
        cat.add(jdbcDs);

        JDBCDataStore h2 = (JDBCDataStore) jdbcDs.getDataStore(null);
        JDBCOpenSearchAccessTest.createTables(h2);
        JDBCOpenSearchAccessTest.populateCollections(h2);
        
        // create the OpenSeach wrapper store
        DataStoreInfo osDs = cat.getFactory().createDataStore();
        osDs.setName("oseo");
        osDs.setWorkspace(ws);
        osDs.setEnabled(true);

        params = osDs.getConnectionParameters();
        params.put("dbtype", "opensearch-eo-jdbc");
        params.put("database", jdbcDs.getWorkspace().getName() + ":" + jdbcDs.getName());
        params.put("store", jdbcDs.getWorkspace().getName() + ":" + jdbcDs.getName());
        params.put("repository", null);
        cat.add(osDs);
        
        // configure opensearch for EO to use it
        OSEOInfo service  = gs.getService(OSEOInfo.class);
        service.setOpenSearchAccessStoreId(osDs.getId());
        gs.save(service);
        
        // configure contact info
        GeoServerInfo global = gs.getGlobal();
        global.getSettings().getContact().setContactOrganization("GeoServer");
        gs.save(global);
    }

    @Before
    public void setupNamespaces() {
        this.namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("os", "http://a9.com/-/spec/opensearch/1.1/");
        namespaceContext.bindNamespaceUri("param", "http://a9.com/-/spec/opensearch/extensions/parameters/1.0/");
        namespaceContext.bindNamespaceUri("at", "http://www.w3.org/2005/Atom");
        namespaceContext.bindNamespaceUri("gml", "http://www.opengis.net/gml");
        namespaceContext.bindNamespaceUri("georss", "http://www.georss.org/georss");
        namespaceContext.bindNamespaceUri("eo", OpenSearchAccess.EO_NAMESPACE);
        namespaceContext.bindNamespaceUri("geo", OpenSearchAccess.GEO_NAMESPACE);
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
    
    protected void checkValidAtomFeed(Document d) throws SAXException, IOException {
        // TODO: we probably need to enrich this with EO specific elements check
        checkValidationErrors(d, ATOM_SCHEMA);
    }
}
