/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class ResourceConfigurationPageTest extends GeoServerWicketTestSupport {
    
    @Test
    public void testBasic() {
        LayerInfo layer = getGeoServerApplication().getCatalog().getLayerByName(getLayerId(MockData.BASIC_POLYGONS));

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        tester.assertLabel("resourcename", layer.getResource().getPrefixedName());
        tester.assertComponent("resource:tabs:panel:theList:0:content", BasicResourceConfig.class);
    }
}
