/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import it.geosolutions.imageio.utilities.ImageIOUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.CatalogTimeStampUpdater;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.Before;

public abstract class CatalogRESTTestSupport extends GeoServerSystemTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    /** Clears up a coverage and its rendered image, important to get builds working on Windows */
    protected static void dispose(GridCoverage2D coverage) {
        try {
            ImageIOUtilities.disposeImage(coverage.getRenderedImage());
            coverage.dispose(true);
        } catch (Throwable t) {
            // Does nothing;
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // addUser("admin", "geoxserver", null, Arrays.asList("ROLE_ADMINISTRATOR"));
        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
        // attach time stamp listener
        new CatalogTimeStampUpdater(catalog);
    }

    protected final void setUpUsers(Properties props) {}

    protected final void setUpLayerRoles(Properties properties) {}

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
}
