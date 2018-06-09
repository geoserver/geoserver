/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.xml.v1_0_0.WFSConfiguration;

/**
 * New Base support class for wfs tests.
 *
 * <p>Deriving from this test class provides the test case with preconfigured geoserver and wfs
 * objects.
 *
 * @author Niels Charlier
 */
public abstract class WFSTestSupport extends GeoServerSystemTestSupport {
    /** @return The global wfs instance from the application context. */
    protected WFSInfo getWFS() {
        return getGeoServer().getService(WFSInfo.class);
    }

    /** @return The 1.0 service descriptor. */
    protected Service getServiceDescriptor10() {
        return (Service) GeoServerExtensions.bean("wfsService-1.0.0");
    }

    /** @return The 1.1 service descriptor. */
    protected Service getServiceDescriptor11() {
        return (Service) GeoServerExtensions.bean("wfsService-1.1.0");
    }

    /** @return The 1.0 xml configuration. */
    protected WFSConfiguration getXmlConfiguration10() {
        return (WFSConfiguration) applicationContext.getBean("wfsXmlConfiguration-1.0");
    }

    /** @return The 1.1 xml configuration. */
    protected org.geoserver.wfs.xml.v1_1_0.WFSConfiguration getXmlConfiguration11() {
        return (org.geoserver.wfs.xml.v1_1_0.WFSConfiguration)
                applicationContext.getBean("wfsXmlConfiguration-1.1");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("gs", "http://geoserver.org");

        CiteTestData.registerNamespaces(namespaces);

        setUpNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        setUpInternal(testData);
    }

    protected void setUpInternal(SystemTestData testData) throws Exception {}

    protected void setUpNamespaces(Map<String, String> namespaces) {}

    protected List<String> getSupportedSpatialOperatorsList(boolean wfs1_0_0) {
        return Arrays.asList(
                new String[] {
                    "Disjoint",
                    "Equals",
                    "DWithin",
                    "Beyond",
                    "Intersect" + (wfs1_0_0 ? "" : "s"),
                    "Touches",
                    "Crosses",
                    "Within",
                    "Contains",
                    "Overlaps",
                    "BBOX"
                });
    }
}
