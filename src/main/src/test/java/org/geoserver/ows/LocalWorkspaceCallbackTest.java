/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalWorkspaceCallbackTest extends GeoServerSystemTestSupport {

    private LocalWorkspaceCallback callback = null;

    private Request request = null;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        GeoServer geoserver = getGeoServer();
        GeoServerInfo global = geoserver.getGlobal();
        global.setGlobalServices(false);
        geoserver.save(global);

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
    public void initCallback() {
        this.callback = new LocalWorkspaceCallback(getGeoServer());
        this.request = new Request();
    }

    @After
    public void removeThreadLocals() {
        this.callback.finished(this.request);
    }

    @Test(expected = ServiceException.class)
    public void testInitRequest_WithoutWorkspace() {
        this.request.setContext("ows");
        this.callback.init(this.request);
    }

    @Test
    public void testInitRequest_WorkspaceExists() {
        this.request.setContext("cite/ows");
        this.callback.init(this.request);
        assertNotNull(LocalWorkspace.get());
        assertNull(LocalPublished.get());
    }

    @Test(expected = ServiceException.class)
    public void testInitRequest_WorkspaceMissing() {
        this.request.setContext("ws/ows");
        this.callback.init(this.request);
    }

    @Test
    public void testInitRequest_LayerExists() {
        this.request.setContext("cite/BasicPolygons/ows");
        this.callback.init(this.request);
        assertNotNull(LocalWorkspace.get());
        assertNotNull(LocalPublished.get());
    }

    @Test
    public void testInitRequest_LayerMissing() {
        this.request.setContext("cite/MissingLayer/ows");
        this.callback.init(this.request);
        assertNotNull(LocalWorkspace.get());
        assertNull(LocalPublished.get());
    }

    @Test
    public void testInitRequest_LayerMissingInWorkspace() {
        this.request.setContext("cite/Fifteen/ows");
        this.callback.init(this.request);
        assertNotNull(LocalWorkspace.get());
        assertNull(LocalPublished.get());
    }

    @Test
    public void testInitRequest_WorkspacedLayerGroupExists() {
        this.request.setContext("cite/LayerGroup2/ows");
        this.callback.init(this.request);
        assertNotNull(LocalWorkspace.get());
        assertNotNull(LocalPublished.get());
    }

    @Test
    public void testInitRequest_WorkspacedLayerGroupMissing() {
        this.request.setContext("cite/lg/ows");
        this.callback.init(this.request);
        assertNotNull(LocalWorkspace.get());
        assertNull(LocalPublished.get());
    }

    @Test
    public void testInitRequest_LayerGroupExists() {
        this.request.setContext("LayerGroup1/ows");
        this.callback.init(this.request);
        assertNull(LocalWorkspace.get());
        assertNotNull(LocalPublished.get());
    }

    @Test(expected = ServiceException.class)
    public void testInitRequest_LayerGroupMissing() {
        this.request.setContext("lg/ows");
        this.callback.init(this.request);
    }

    @Test(expected = ServiceException.class)
    public void testInitRequest_NotAGlobalLayerGroup() {
        this.request.setContext("cite:LayerGroup2/ows");
        this.callback.init(this.request);
    }
}
