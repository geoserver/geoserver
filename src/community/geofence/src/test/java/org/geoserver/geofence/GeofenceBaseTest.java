/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.test.GeoServerTestSupport;

public abstract class GeofenceBaseTest extends GeoServerTestSupport {

    private static Boolean IS_GEOFENCE_AVAILABLE;

    protected GeofenceAccessManager accessManager;

    protected GeoFenceConfigurationManager configManager;

    protected RuleReaderService geofenceService;

    @Override
    public void oneTimeSetUp() throws Exception {
        try {
            super.oneTimeSetUp();
        } catch (Exception e) {
            LOGGER.severe(
                    "Error in OneTimeSetup: it may be due to GeoFence not running, please check the logs -- "
                            + e.getMessage());
            LOGGER.log(
                    Level.FINE,
                    "Error in OneTimeSetup: it may be due to GeoFence not running, please check the logs",
                    e);
        }

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // get the beans we use for testing
        accessManager =
                (GeofenceAccessManager) applicationContext.getBean("geofenceRuleAccessManager");
        geofenceService =
                (RuleReaderService)
                        applicationContext.getBean(
                                applicationContext
                                        .getBeanFactory()
                                        .resolveEmbeddedValue("${ruleReaderBackend}"));
        configManager =
                (GeoFenceConfigurationManager)
                        applicationContext.getBean("geofenceConfigurationManager");
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        // populate the users
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();

        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,ROLE_DUMMY");
        props.put("wmsuser", "wmsuser,ROLE_DUMMY");
        props.put("area", "area,ROLE_DUMMY");
        props.store(new FileOutputStream(users), "");
    }

    @Override
    protected void runTest() throws Throwable {

        if (IS_GEOFENCE_AVAILABLE == null) {
            IS_GEOFENCE_AVAILABLE = isGeoFenceAvailable();
        }

        if (IS_GEOFENCE_AVAILABLE) {
            super.runTest();
        } else {
            System.out.println(
                    "Skipping test in "
                            + getClass().getSimpleName()
                            + " as GeoFence service is down: "
                            + "in order to run this test you need the services to be running on port 9191");
            // TODO: use Assume when using junit >=3
        }
    }

    protected boolean isGeoFenceAvailable() {
        try {
            geofenceService.getMatchingRules(null, null, null, null, null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error connecting to GeoFence", e);
            return false;
        }
    }
}
