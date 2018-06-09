/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class LocalWorkspaceCatalogTest {

    LocalWorkspaceCatalog catalog;

    @Before
    public void setUp() throws Exception {
        WorkspaceInfo ws1 = createNiceMock(WorkspaceInfo.class);
        expect(ws1.getName()).andReturn("ws1").anyTimes();
        replay(ws1);

        NamespaceInfo ns1 = createNiceMock(NamespaceInfo.class);
        expect(ns1.getPrefix()).andReturn("ws1").anyTimes();
        expect(ns1.getURI()).andReturn("ws1").anyTimes();
        replay(ns1);

        WorkspaceInfo ws2 = createNiceMock(WorkspaceInfo.class);
        expect(ws2.getName()).andReturn("ws2").anyTimes();
        replay(ws2);

        NamespaceInfo ns2 = createNiceMock(NamespaceInfo.class);
        expect(ns2.getPrefix()).andReturn("ws2").anyTimes();
        expect(ns2.getURI()).andReturn("ws2").anyTimes();
        replay(ns2);

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

        FeatureTypeInfo ft1 = createNiceMock(FeatureTypeInfo.class);
        expect(ft1.getName()).andReturn("l1").anyTimes();
        expect(ft1.getNamespace()).andReturn(ns1).anyTimes();
        replay(ft1);

        LayerInfo l1 = createNiceMock(LayerInfo.class);
        expect(l1.getName()).andReturn("ws1:l1").anyTimes();
        expect(l1.getResource()).andReturn(ft1).anyTimes();
        replay(l1);

        FeatureTypeInfo ft2 = createNiceMock(FeatureTypeInfo.class);
        expect(ft2.getName()).andReturn("l2").anyTimes();
        expect(ft2.getNamespace()).andReturn(ns2).anyTimes();
        replay(ft2);

        LayerInfo l2 = createNiceMock(LayerInfo.class);
        expect(l2.getName()).andReturn("ws2:l2").anyTimes();
        expect(l2.getResource()).andReturn(ft2).anyTimes();
        replay(l2);

        // set up layer name collisions: lc
        // use same name, but different featuretypeinfo objects
        // pointing to different workspaces
        LayerInfo lc1 = createNiceMock(LayerInfo.class);
        expect(lc1.getName()).andReturn("ws1:lc").anyTimes();
        expect(lc1.getResource()).andReturn(ft1).anyTimes();
        replay(lc1);

        LayerInfo lc2 = createNiceMock(LayerInfo.class);
        expect(lc2.getName()).andReturn("ws2:lc").anyTimes();
        expect(lc2.getResource()).andReturn(ft2).anyTimes();
        replay(lc2);

        Catalog cat = createNiceMock(Catalog.class);

        expect(cat.getWorkspaces()).andReturn(Arrays.asList(ws1, ws2)).anyTimes();
        expect(cat.getWorkspaceByName("ws1")).andReturn(ws1).anyTimes();
        expect(cat.getWorkspaceByName("ws2")).andReturn(ws2).anyTimes();
        expect(cat.getNamespaceByPrefix("ws1")).andReturn(ns1).anyTimes();
        expect(cat.getNamespaceByPrefix("ws2")).andReturn(ns2).anyTimes();

        expect(cat.getStyleByName("ws1", "s1")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName(ws1, "s1")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName("s1")).andReturn(null).anyTimes();

        expect(cat.getStyleByName("ws2", "s2")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName(ws2, "s2")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName("s2")).andReturn(null).anyTimes();

        expect(cat.getLayerGroupByName("ws1", "lg1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName(ws1, "lg1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName("ws1:lg1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName("lg1")).andReturn(null).anyTimes();

        expect(cat.getLayerGroupByName("ws2", "lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName(ws2, "lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName("ws2:lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName("lg2")).andReturn(null).anyTimes();

        // expect(cat.getLayerByName("ws1", "l1")).andReturn(l1).anyTimes();
        // expect(cat.getLayerByName(ws1, "l1")).andReturn(l1).anyTimes();
        expect(cat.getLayerByName(new NameImpl("ws1", "l1"))).andReturn(l1).anyTimes();
        expect(cat.getLayerByName("l1")).andReturn(null).anyTimes();

        // expect(cat.getLayerByName("ws2", "l2")).andReturn(l2).anyTimes();
        // expect(cat.getLayerByName(ws2, "l2")).andReturn(l2).anyTimes();
        expect(cat.getLayerByName(new NameImpl("ws2", "l2"))).andReturn(l2).anyTimes();
        expect(cat.getLayerByName("l2")).andReturn(null).anyTimes();

        // with namespace prefixes, return the appropriate layer info
        expect(cat.getLayerByName(new NameImpl("ws1", "lc"))).andReturn(lc1).anyTimes();
        expect(cat.getLayerByName(new NameImpl("ws2", "lc"))).andReturn(lc2).anyTimes();
        // return back the first one without a namespace prefix
        expect(cat.getLayerByName("lc")).andReturn(lc1).anyTimes();

        List<LayerInfo> layers = new ArrayList<LayerInfo>(2);
        layers.add(l1);
        layers.add(l2);
        layers.add(lc1);
        layers.add(lc2);
        expect(cat.getLayers()).andReturn(layers).anyTimes();
        List<LayerInfo> layers2 = new ArrayList<>(layers);
        layers2.add(null);
        expect(
                        cat.list(
                                LayerInfo.class,
                                Filter.INCLUDE,
                                (Integer) null,
                                (Integer) null,
                                (SortBy) null))
                .andReturn(new CloseableIteratorAdapter<LayerInfo>(layers2.iterator()))
                .anyTimes();
        replay(cat);

        catalog = new LocalWorkspaceCatalog(cat);
    }

    @After
    public void tearDown() {
        LocalWorkspace.remove();
    }

    @Test
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

    @Test
    public void testGetLayerGroupByName() throws Exception {
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");

        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getLayerGroupByName("lg1"));
        assertNotNull(catalog.getLayerGroupByName("ws1:lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.remove();
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.set(ws2);
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNotNull(catalog.getLayerGroupByName("ws2:lg2"));
        assertNotNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.remove();
        assertNull(catalog.getLayerGroupByName("lg1"));
        assertNull(catalog.getLayerGroupByName("lg2"));
    }

    @Test
    public void testGetLayerByName() throws Exception {
        assertNull(catalog.getLayerByName("l1"));
        assertNull(catalog.getLayerByName("l2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");

        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getLayerByName("l1"));
        assertNull(catalog.getLayerByName("l2"));
        LocalWorkspace.remove();

        LocalWorkspace.set(ws2);
        assertNull(catalog.getLayerByName("l1"));
        assertNotNull(catalog.getLayerByName("l2"));
        LocalWorkspace.remove();

        assertNull(catalog.getLayerByName("l1"));
        assertNull(catalog.getLayerByName("l2"));
    }

    @Test
    public void testGetLayersWithSameName() throws Exception {
        LayerInfo layerInfo1 = catalog.getLayerByName(new NameImpl("ws1", "lc"));
        ResourceInfo resource1 = layerInfo1.getResource();
        NamespaceInfo namespace1 = resource1.getNamespace();
        String nsPrefix1 = namespace1.getPrefix();

        LayerInfo layerInfo2 = catalog.getLayerByName(new NameImpl("ws2", "lc"));
        ResourceInfo resource2 = layerInfo2.getResource();
        NamespaceInfo namespace2 = resource2.getNamespace();
        String nsPrefix2 = namespace2.getPrefix();

        assertEquals("Invalid namespace prefix", "ws1", nsPrefix1);
        assertEquals("Invalid namespace prefix", "ws2", nsPrefix2);
    }

    /** The setting says to not include the prefix. This is default behaviour */
    @Test
    public void testGetNonPrefixedLayerNames() {
        boolean includePrefix = false;
        boolean setLocalWorkspace = true;
        boolean createGeoServer = true;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /**
     * No geoserver instance has been set. This means there is no access to geoserver. this should
     * not happen but we want to protect against this consideration. In this case we have a local
     * workspace set and we will use the default behaviour (no prefix)
     */
    @Test
    public void testGetNoGeoserverPrefixedLayerNameBehaviour() {
        boolean includePrefix = false;
        boolean setLocalWorkspace = true;
        boolean createGeoServer = false;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /**
     * No local workspace is set this means the prefix should be included since the global
     * capabilities is probably being created.
     *
     * <p>The No Geoserver part is just to verify there are no nullpointer exceptions because of a
     * coding error
     */
    @Test
    public void testGetNoGeoserverLocalWorkspacePrefixedLayerNameBehaviour() {
        boolean includePrefix = true;
        boolean setLocalWorkspace = false;
        boolean createGeoServer = false;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /**
     * No localworkspace so prefix should be included since the global capabilities is probably
     * being created.
     */
    @Test
    public void testGetNoLocalWorkspacePrefixedLayerNameBehaviour() {
        boolean includePrefix = true;
        boolean setLocalWorkspace = false;
        boolean createGeoServer = true;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /** The setting is set to include the prefixes. */
    @Test
    public void testGetPrefixedLayerNames() {
        boolean includePrefix = true;
        boolean setLocalWorkspace = true;
        boolean createGeoServer = true;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    private void assertPrefixInclusion(
            boolean includePrefix, boolean setLocalWorkspace, boolean createGeoServer) {
        if (createGeoServer) {
            SettingsInfo settings = createNiceMock(SettingsInfo.class);
            expect(settings.isLocalWorkspaceIncludesPrefix()).andReturn(includePrefix).anyTimes();
            replay(settings);

            GeoServer geoServer = createNiceMock(GeoServer.class);
            expect(geoServer.getSettings()).andReturn(settings).anyTimes();
            replay(geoServer);

            catalog.setGeoServer(geoServer);
        }

        if (setLocalWorkspace) {
            WorkspaceInfo workspaceByName = catalog.getWorkspaceByName("ws1");
            LocalWorkspace.set(workspaceByName);
        }

        checkLayerNamePrefixInclusion(includePrefix, catalog.getLayers().iterator());

        checkLayerNamePrefixInclusion(includePrefix, catalog.list(LayerInfo.class, Filter.INCLUDE));
    }

    private void checkLayerNamePrefixInclusion(boolean includePrefix, Iterator<LayerInfo> layers) {
        while (layers.hasNext()) {
            LayerInfo layerInfo = layers.next();
            if (layerInfo == null) {
                continue;
            }
            String message;
            if (includePrefix) {
                message =
                        layerInfo.getName()
                                + " should contain a : because the prefix should have been kept";
            } else {
                message =
                        layerInfo.getName()
                                + " should contain not a : because the prefix should have been removed";
            }
            assertEquals(message, includePrefix, layerInfo.getName().contains(":"));
        }
    }
}
