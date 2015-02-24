/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.geoserver.gss.GSSCore.*;
import static org.geotools.data.DataUtilities.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.LiveDbmsData;
import org.geoserver.data.test.TestData;
import org.geoserver.gss.GSSInfo.GSSMode;
import org.geoserver.gss.xml.GSSConfiguration;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geotools.data.FeatureStore;
import org.geotools.data.VersioningDataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Base class for gss functional testing, sets up a proper testing enviroment for gss test with a
 * real data dir and a connection to a postgis data store
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public abstract class GSSTestSupport extends GeoServerAbstractTestSupport {

    static XpathEngine xpath;

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    DefaultGeoServerSynchronizationService gss;

    VersioningDataStore synchStore;

    // protected String getLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    @Override
    public TestData buildTestData() throws Exception {
        File base = new File("./src/test/resources/");
        LiveDbmsData data = new LiveDbmsData(new File(base, "data_dir"), "unit", new File(base,
                "unit.sql"));
        List<String> filteredPaths = data.getFilteredPaths();
        filteredPaths.clear();
        filteredPaths.add("workspaces/topp/synch/datastore.xml");
        return data;
    }

    @Override
    protected void setUpInternal() throws Exception {
        // configure the GSS service
        GeoServer gs = getGeoServer();
        GSSInfo gssInfo = gs.getService(GSSInfo.class);
        gssInfo.setMode(GSSMode.Unit);
        gssInfo.setVersioningDataStore(getCatalog().getDataStoreByName("synch"));
        gs.save(gssInfo);

        // initialize the GSS service
        Map gssBeans = applicationContext
                .getBeansOfType(DefaultGeoServerSynchronizationService.class);
        gss = (DefaultGeoServerSynchronizationService) gssBeans.values().iterator().next();
        gss.core.ensureUnitEnabled();

        // mark some tables as version enabled
        synchStore = (VersioningDataStore) getCatalog().getDataStoreByName("synch").getDataStore(
                null);
        FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) synchStore
                .getFeatureSource(SYNCH_TABLES);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(fs.getSchema());
        fs.addFeatures(collection(fb.buildFeature(null, new Object[] { "restricted", "2" })));
        fs.addFeatures(collection(fb.buildFeature(null, new Object[] { "roads", "2" })));
        synchStore.setVersioned("restricted", true, null, null);
        synchStore.setVersioned("roads", true, null, null);

        assertNotNull(synchStore.getSchema(SYNCH_HISTORY));
        assertFalse(synchStore.isVersioned(SYNCH_HISTORY));
        assertNotNull(synchStore.getSchema(SYNCH_TABLES));
        assertFalse(synchStore.isVersioned(SYNCH_TABLES));

        assertEquals(0, gss.getActiveConflicts("restricted").size());
        assertEquals(0, gss.getActiveConflicts("roads").size());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wfs", "http://www.opengis.net/gss");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wfsv", "http://www.opengis.net/wfsv");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://www.openplans.org/spearfish");
        namespaces.put("gss", "http://geoserver.org/gss");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("", "http://www.opengis.net/ogc");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        xpath = XMLUnit.newXpathEngine();
    }

    /**
     * Returns the url of the WFSV entry point
     * 
     * @return
     */
    protected String root() {
        return root(false);
    }

    protected String root(boolean validate) {
        return "ows?" + (validate ? "strict=true&" : "");
    }

    /**
     * Validates
     * 
     * @param document
     * @throws Exception
     */
    protected void validate(MockHttpServletResponse response) throws Exception {
        GSSConfiguration configuration = (GSSConfiguration) applicationContext
                .getBean("gssXmlConfiguration");
        Parser parser = new Parser(configuration);
        parser.validate(new StringReader(response.getOutputStreamContent()));
        if (parser.getValidationErrors().size() > 0) {
            for (Iterator it = parser.getValidationErrors().iterator(); it.hasNext();) {
                SAXParseException se = (SAXParseException) it.next();
                System.out.println(se);
            }
            // print(dom(response));
            fail("Document is not valid, see standard output for a document dump and validation exceptions");
        }
    }

    /**
     * Parses the mock response into a DOM tree
     */
    protected Document dom(MockHttpServletResponse response) throws IOException, SAXException,
            ParserConfigurationException {
        return dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));
    }

    /**
     * Loads a text file in the classpath into a String
     * 
     * @param path
     *            Path relative to the calling class
     * @return
     * @throws Exception
     */
    protected String loadTextResource(String path) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass()
                .getResourceAsStream(path)));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    /**
     * Makes sure the response is a valid and positive PostDiff one
     */
    protected Document checkPostDiffSuccessResponse(MockHttpServletResponse response)
            throws Exception {
        validate(response);
        Document dom = dom(response);
        // print(dom);

        // basic response checks
        assertXpathExists("/gss:PostDiffResponse", dom);
        assertXpathEvaluatesTo("true", "/gss:PostDiffResponse/@success", dom);

        return dom;
    }

}
