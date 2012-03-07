package org.geoserver.catalog.impl;

import java.util.Arrays;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;

import junit.framework.TestCase;
import static org.easymock.EasyMock.*;

public class LocalWorkspaceCatalogTest extends TestCase {

    LocalWorkspaceCatalog catalog;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        WorkspaceInfo ws1 = createNiceMock(WorkspaceInfo.class);
        expect(ws1.getName()).andReturn("ws1").anyTimes();
        replay(ws1);

        WorkspaceInfo ws2 = createNiceMock(WorkspaceInfo.class);
        expect(ws2.getName()).andReturn("ws2").anyTimes();
        replay(ws2);

        StyleInfo s1 = createNiceMock(StyleInfo.class);
        expect(s1.getName()).andReturn("s1").anyTimes();
        expect(s1.getWorkspace()).andReturn(ws1).anyTimes();
        replay(s1);

        StyleInfo s2 = createNiceMock(StyleInfo.class);
        expect(s2.getName()).andReturn("s2").anyTimes();
        expect(s2.getWorkspace()).andReturn(ws2).anyTimes();
        replay(s2);

        LayerGroupInfo lg1 = createNiceMock(LayerGroupInfo.class);
        expect(lg1.getName()).andReturn("lg1").anyTimes();
        expect(lg1.getWorkspace()).andReturn(ws1).anyTimes();
        replay(lg1);

        LayerGroupInfo lg2 = createNiceMock(LayerGroupInfo.class);
        expect(lg2.getName()).andReturn("lg2").anyTimes();
        expect(lg2.getWorkspace()).andReturn(ws2).anyTimes();
        replay(lg2);

        Catalog cat = createNiceMock(Catalog.class);

        expect(cat.getWorkspaces()).andReturn(Arrays.asList(ws1,ws2)).anyTimes();
        expect(cat.getWorkspaceByName("ws1")).andReturn(ws1).anyTimes();
        expect(cat.getWorkspaceByName("ws2")).andReturn(ws2).anyTimes();

        expect(cat.getStyleByName("ws1", "s1")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName(ws1, "s1")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName("s1")).andReturn(null).anyTimes();
        
        expect(cat.getStyleByName("ws2", "s2")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName(ws2, "s2")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName("s2")).andReturn(null).anyTimes();
        
        expect(cat.getLayerGroupByName("ws1", "lg1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName(ws1, "lg1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName("lg1")).andReturn(null).anyTimes();
        
        expect(cat.getLayerGroupByName("ws2", "lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName(ws2, "lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName("lg2")).andReturn(null).anyTimes();

        replay(cat);

        catalog = new LocalWorkspaceCatalog(cat);
    }

    public void testGetStyleByName() throws Exception {
        assertNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");
        
        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));

        LocalWorkspace.remove();
        assertNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));

        LocalWorkspace.set(ws2);
        assertNull(catalog.getStyleByName("s1"));
        assertNotNull(catalog.getStyleByName("s2"));

        LocalWorkspace.remove();
        assertNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));
    }

    public void testGetLayerGroupByName() throws Exception {
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");
        
        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.remove();
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.set(ws2);
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNotNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.remove();
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));
    }
}
