/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import static org.easymock.EasyMock.*;
import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.data.DataUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class GeoServerTemplateLoader2Test {

    static File root;

    @BeforeClass
    public static void createTmpDir() throws Exception {
        root = File.createTempFile("template", "tmp", new File("target"));
        root.delete();
        root.mkdir();
    }

    GeoServerDataDirectory createDataDirectoryMock() {
        GeoServerDataDirectory dd = createNiceMock(GeoServerDataDirectory.class);
        expect(dd.root()).andReturn(root).anyTimes();
        return dd;
    }

    @Test
    public void testRelativeToFeatureType() throws IOException {

        GeoServerDataDirectory dd = createDataDirectoryMock();
        replay(dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        Object source = tl.findTemplateSource("dummy.ftl");
        assertNull(source);

        reset(dd);

        FeatureTypeInfo ft = createMock(FeatureTypeInfo.class);
        expect(dd.findSuppResourceFile(ft, "dummy.ftl")).andReturn(new File("foo")).once();
        replay(ft, dd);

        tl.setFeatureType(ft);

        source = tl.findTemplateSource("dummy.ftl");
        assertNotNull(source);

        verify(ft, dd);
    }

    @Test
    public void testRelativeToStore() throws IOException {
        GeoServerDataDirectory dd = createDataDirectoryMock();
        replay(dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        Object source = tl.findTemplateSource("dummy.ftl");
        assertNull(source);

        reset(dd);

        DataStoreInfo s = createNiceMock(DataStoreInfo.class);
        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        expect(ft.getStore()).andReturn(s).anyTimes();
        tl.setFeatureType(ft);

        replay(ft, s, dd);

        assertNull(tl.findTemplateSource("dummy.ftl"));

        reset(dd);
        expect(dd.findSuppStoreFile(s, "dummy.ftl")).andReturn(new File("foo")).once();
        replay(dd);

        assertNotNull(tl.findTemplateSource("dummy.ftl"));
        verify(dd);
    }

    @Test
    public void testRelativeToWorkspace() throws IOException {
        GeoServerDataDirectory dd = createDataDirectoryMock();

        DataStoreInfo s = createNiceMock(DataStoreInfo.class);
        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        expect(ft.getStore()).andReturn(s).anyTimes();
        expect(s.getWorkspace()).andReturn(ws).anyTimes();

        replay(ft, s, ws, dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        tl.setFeatureType(ft);
        Object source = tl.findTemplateSource("dummy.ftl");
        assertNull(source);

        reset(dd);
        expect(dd.findSuppWorkspaceFile(ws, "dummy.ftl")).andReturn(new File("foo")).once();
        replay(dd);

        assertNotNull(tl.findTemplateSource("dummy.ftl"));
        verify(dd);
    }

    @Test
    public void testGlobal() throws IOException {
        GeoServerDataDirectory dd = createDataDirectoryMock();

        DataStoreInfo s = createNiceMock(DataStoreInfo.class);
        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        expect(ft.getStore()).andReturn(s).anyTimes();
        expect(s.getWorkspace()).andReturn(ws).anyTimes();
        replay(ft, s, ws, dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        tl.setResource(ft);
        assertNull(tl.findTemplateSource("dummy.ftl"));

        reset(dd);

        expect(dd.findSuppWorkspacesFile(ws, "dummy.ftl")).andReturn(new File("foo")).once();
        replay(dd);

        assertNotNull(tl.findTemplateSource("dummy.ftl"));
        verify(dd);
    }

    @Test
    public void testRemoteType() throws Exception {
        SimpleFeatureType ft =
                DataUtilities.createType(
                        "remoteType", "the_geom:MultiPolygon,FID:String,ADDRESS:String");

        GeoServerDataDirectory dd = createDataDirectoryMock();
        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getFeatureTypeByName(ft.getName())).andReturn(null).once();
        replay(dd, cat);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        tl.setCatalog(cat);
        tl.setFeatureType(ft);
        tl.findTemplateSource("header.ftl");

        verify(cat);
    }
}
