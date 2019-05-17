/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class PreviewLayerTest extends GeoServerWicketTestSupport {

    /** Test for layer group service support check */
    @Test
    public void testHasServiceSupport() throws Exception {
        Catalog cat = GeoServerApplication.get().getCatalog();
        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("linkgroup");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);
        cat.add(lg);
        PreviewLayer layer = new PreviewLayer(lg);
        assertTrue(layer.hasServiceSupport("WMS"));
    }
}
