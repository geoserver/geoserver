/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.junit.Test;

public class OWSHandlerMappingTest extends GeoServerSystemTestSupport {

    private OWSHandlerMapping mapping = null;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(new NameImpl(CiteTestData.BASIC_POLYGONS));
        StyleInfo style = catalog.getStyleByName(CiteTestData.DEFAULT_VECTOR_STYLE);
        CatalogFactory factory = catalog.getFactory();

        LayerGroupInfo lg1 = factory.createLayerGroup();
        lg1.setName("LayerGroup1");
        lg1.getLayers().add(layer);
        lg1.getStyles().add(style);
        catalog.add(lg1);

        LayerGroupInfo lg2 = factory.createLayerGroup();
        lg2.setName("LayerGroup2");
        lg2.setWorkspace(catalog.getWorkspaceByName(CiteTestData.CITE_PREFIX));
        lg2.getLayers().add(layer);
        lg2.getStyles().add(style);
        catalog.add(lg2);
    }

    @Before
    public void initHandlerMapping() {
        this.mapping = new OWSHandlerMapping(getCatalog());
        this.mapping.setUrlMap(Collections.singletonMap("/test", new Object()));
        this.mapping.setApplicationContext(applicationContext);
        this.mapping.initApplicationContext();
    }

    @Test
    public void testLookupHandler_WithoutWorkspace() throws Exception {
        assertNotNull(this.mapping.lookupHandler("/test", null));
    }

    @Test
    public void testLookupHandler_WorkspaceExists() throws Exception {
        assertNotNull(this.mapping.lookupHandler("/cite/test", null));
    }

    @Test
    public void testLookupHandler_WorkspaceMissing() throws Exception {
        assertNull(this.mapping.lookupHandler("/ws/test", null));
    }

    @Test
    public void testLookupHandler_LayerExists() throws Exception {
        assertNotNull(this.mapping.lookupHandler("/cite/BasicPolygons/test", null));
    }

    @Test
    public void testLookupHandler_LayerMissing() throws Exception {
        assertNull(this.mapping.lookupHandler("/cite/MissingLayer/test", null));
    }

    @Test
    public void testLookupHandler_LayerMissingInWorkspace() throws Exception {
        assertNull(this.mapping.lookupHandler("/cite/Fifteen/test", null));
    }

    @Test
    public void testLookupHandler_WorkspacedLayerGroupExists() throws Exception {
        assertNotNull(this.mapping.lookupHandler("/cite/LayerGroup2/test", null));
    }

    @Test
    public void testLookupHandler_WorkspacedLayerGroupMissing() throws Exception {
        assertNull(this.mapping.lookupHandler("/cite/lg/test", null));
    }

    @Test
    public void testLookupHandler_LayerGroupExists() throws Exception {
        assertNotNull(this.mapping.lookupHandler("/LayerGroup1/test", null));
    }

    @Test
    public void testLookupHandler_LayerGroupMissing() throws Exception {
        assertNull(this.mapping.lookupHandler("/lg/test", null));
    }

    @Test
    public void testLookupHandler_NotAGlobalLayerGroup() throws Exception {
        assertNull(this.mapping.lookupHandler("/cite:LayerGroup2/test", null));
    }
}
