/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geofence.core.services.RuleReaderService;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.ShortRule;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Assert;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.w3c.dom.Document;

public abstract class GeofenceBaseTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    protected static XpathEngine xp;

    protected static Boolean IS_GEOFENCE_AVAILABLE = false;

    protected static GeofenceAccessManager accessManager;

    protected static GeoFenceConfigurationManager configManager;

    protected static RuleReaderService geofenceService;

    static GeoServerDataDirectory dd;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<>();

        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");

        testData.registerNamespaces(namespaces);
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        testData.setUp();
        //        testData.setUpDefault();

        addUser("area", "area", Collections.singletonList("USERS"), Collections.singletonList("ROLE_AUTHENTICATED"));
        addUser("cite", "cite", Collections.singletonList("USERS"), Collections.singletonList("ROLE_AUTHENTICATED"));
        addUser(
                "wms_user",
                "wms_user",
                Collections.singletonList("USERS"),
                Collections.singletonList("ROLE_AUTHENTICATED"));
        addUser("sf", "sf", Collections.singletonList("USERS"), Arrays.asList("ROLE_AUTHENTICATED", "ROLE_SF_ADMIN"));

        catalog = getCatalog();

        // add test geofence properties file to the temporary data dir. For testing purposes only
        dd = new GeoServerDataDirectory(testData.getDataDirectoryRoot());
        GeoServerExtensionsHelper.singleton("dataDirectory", dd, GeoServerDataDirectory.class);

        // get the beans we use for testing
        accessManager = applicationContext.getBean("geofenceRuleAccessManager", GeofenceAccessManager.class);

        configManager = applicationContext.getBean("geofenceConfigurationManager", GeoFenceConfigurationManager.class);

        Assert.assertNotNull(accessManager);
        Assert.assertNotNull(configManager);

        if (isGeoFenceAvailable()) {
            IS_GEOFENCE_AVAILABLE = true;
            System.setProperty("IS_GEOFENCE_AVAILABLE", "True");
        } else {
            LOGGER.warning("Skipping test in "
                    + getClass().getSimpleName()
                    + " as GeoFence service is down: "
                    + "in order to run this test you need the services to be running on port 9191");
        }
    }

    /** subclass hook to register additional namespaces. */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    @After
    public void after() {
        // used by catalog
        logout();

        // used by getAsDOM etc
        this.username = null;
        this.password = null;
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}", e);
        }
    }

    protected boolean isGeoFenceAvailable() {
        geofenceService = applicationContext
                .getBean("ruleReaderBackendFactory", RuleReaderServiceFactory.class)
                .getService();
        try {
            /**
             * In order to run live tests, you will need to run an instance of GeoFence on port 9191 and create two
             * rules:
             *
             * <p>1) User: admin - grant ALLOW ALL 2) User: * - grant Service: "WMS" ALLOW 3) * - DENY
             */
            final RuleFilter ruleFilter = new RuleFilter();
            ruleFilter.setService("WMS");
            final List<ShortRule> matchingRules = geofenceService.getMatchingRules(ruleFilter);
            if (geofenceService != null && matchingRules != null && !matchingRules.isEmpty()) {
                LOGGER.log(Level.WARNING, "GeoFence is active");
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error connecting to GeoFence", e);
            geofenceService = null;
        }

        LOGGER.log(Level.WARNING, "Not connecting to GeoFence");
        return false;
    }

    protected Authentication getUser(String username, String password, String... roles) {

        List<GrantedAuthority> l = new ArrayList<>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        return new UsernamePasswordAuthenticationToken(username, password, l);
    }

    /**
     * Fails, logging the raw response first, if {@code dom} is an OWS/WMS exception report.
     *
     * <p>An XPath {@code count(...)} assertion evaluates to 0 on any document lacking matching nodes, exception reports
     * included - so a request that actually failed (e.g. "No service") can silently masquerade as "0 results" further
     * down the test, with no indication of the real cause. Call this right after {@code getAsDOM(...)} before running
     * further assertions on the response.
     */
    protected void assertNotExceptionReport(Document dom) throws Exception {
        String rootName = dom.getDocumentElement().getNodeName();
        if (rootName.contains("ExceptionReport")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            print(dom, out);
            String xml = out.toString(java.nio.charset.StandardCharsets.UTF_8);
            LOGGER.severe("Expected a valid response but got an exception report:\n" + xml);
            Assert.fail("Expected a valid response, got " + rootName + ":\n" + xml);
        }
    }
}
