/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;

/**
 * Base class for functional testing of the regionating code; sets up a proper testing enviroment
 * with a real data dir and a connection to a postgis data store
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public abstract class RegionatingTestSupport extends GeoServerSystemTestSupport {

    public static QName STACKED_FEATURES =
            new QName(MockData.SF_URI, "Stacked", MockData.SF_PREFIX);
    public static QName DISPERSED_FEATURES =
            new QName(MockData.SF_URI, "Dispersed", MockData.SF_PREFIX);
    public static QName TILE_TESTS = new QName(MockData.SF_URI, "Tiles", MockData.SF_PREFIX);
    public static QName CENTERED_POLY =
            new QName(MockData.SF_URI, "CenteredPoly", MockData.SF_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData data) throws Exception {
        super.setUpTestData(data);

        // register some basic namespaces for xmlunit tests
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        data.registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        data.setUpVectorLayer(STACKED_FEATURES, null, "Stacked.properties", getClass());
        data.setUpVectorLayer(DISPERSED_FEATURES, null, "Dispersed.properties", getClass());

        HashMap extra = new HashMap();
        extra.put(MockData.KEY_SRS_HANDLINGS, ProjectionPolicy.FORCE_DECLARED.getCode());

        data.setUpVectorLayer(TILE_TESTS, null, "TileTests.properties", getClass());

        data.setUpVectorLayer(CENTERED_POLY, null, "CenteredPoly.properties", getClass());
    }
}
