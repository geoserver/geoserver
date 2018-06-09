/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import javax.script.ScriptEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.script.ScriptTestSupport;
import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.h2.tools.DeleteDbFiles;
import org.springframework.context.ApplicationContext;

public class PyCatalogModTest extends ScriptTestSupport {

    Catalog cat;
    ScriptEngine e;

    public void setUp() throws Exception {
        super.setUp();
        GeoServerExtensionsHelper.init(null); // suppress extension lookup warnings

        DeleteDbFiles.execute("target", "bar", false);
        GeoServerResourceLoader resourceLoader = scriptMgr.getDataDirectory().getResourceLoader();

        cat = new CatalogImpl();
        cat.setResourceLoader(resourceLoader);

        ApplicationContext context = createNiceMock(ApplicationContext.class);
        expect(context.getBean("catalog")).andReturn(cat).anyTimes();
        expect(context.getBean("resourceLoader")).andReturn(resourceLoader);
        expect(context.getBeanNamesForType(GeoServerResourceLoader.class))
                .andReturn(new String[] {"resourceLoader"})
                .anyTimes();
        expect(context.getBeanNamesForType((Class<?>) anyObject()))
                .andReturn(new String[] {})
                .anyTimes();
        replay(context);

        GeoServerExtensionsHelper.init(context); // mock context to use for test
        e = scriptMgr.createNewEngine("py");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        GeoServerExtensionsHelper.init(null); // clear mock context
        DeleteDbFiles.execute("target", "bar", false);
    }

    public void testAddWorkspace() throws Exception {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        assertNotNull("resourceLoader", loader);

        assertEquals(0, cat.getWorkspaces().size());

        e.eval("from geoserver.catalog import Workspace");
        e.eval("ws = Workspace('foo')");
        e.eval("ws.uri = 'http://foo.org'");
        e.eval("ws.save()");

        assertEquals(1, cat.getWorkspaces().size());
        assertNotNull(cat.getWorkspaceByName("foo"));
    }

    public void testSaveWorkspace() throws Exception {
        testAddWorkspace();

        assertNotNull(cat.getWorkspaceByName("foo"));

        e.eval("from geoserver.catalog import Workspace");
        e.eval("ws = Workspace('foo')");
        e.eval("ws.name = 'bar'");
        e.eval("ws.save()");

        assertNull(cat.getWorkspaceByName("foo"));
        assertNotNull(cat.getWorkspaceByName("bar"));
    }

    public void testAddStore() throws Exception {
        testAddWorkspace();

        assertNull(cat.getDataStoreByName("foo", "bar"));

        e.eval("from geoserver.catalog import Store");
        e.eval("st = Store('bar')");
        e.eval("st.connectionParameters.putAll({'database':'target/bar', 'dbtype': 'h2'})");
        e.eval("st.save()");

        DataStoreInfo ds = cat.getDataStoreByName("foo", "bar");
        assertNotNull(ds);
        assertNotNull(ds.getDataStore(null));
    }

    public void testAddStoreNonDefaultWorkspace() throws Exception {
        testAddWorkspace();

        assertNull(cat.getDataStoreByName("acme", "bar"));

        e.eval("from geoserver.catalog import Workspace, Store");
        e.eval("ws = Workspace('acme')");
        e.eval("ws.save()");

        e.eval("st = Store('bar', ws)");
        e.eval("st.connectionParameters.putAll({'database':'target/bar', 'dbtype': 'h2'})");
        e.eval("st.save()");

        assertNotNull(cat.getDataStoreByName("acme", "bar"));
    }

    public void testSaveStore() throws Exception {
        testAddStore();

        assertNull(cat.getDataStoreByName("foo", "bar").getDescription());

        e.eval("from geoserver.catalog import Store");
        e.eval("st = Store('bar', 'foo')");
        e.eval("st.description = 'foobar'");
        e.eval("st.save()");

        assertEquals("foobar", cat.getDataStoreByName("foo", "bar").getDescription());
    }

    public void testAddFeatureType() throws Exception {
        testAddStore();

        DataStoreInfo ds = cat.getDataStoreByName("foo", "bar");

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("baz");
        tb.add("x", Integer.class);
        tb.add("y", String.class);
        ((DataStore) ds.getDataStore(null)).createSchema(tb.buildFeatureType());

        assertNull(cat.getFeatureTypeByDataStore(ds, "baz"));

        e.eval("from geoserver.catalog import Catalog");
        e.eval("from geoserver.catalog import Layer");
        e.eval("cat = Catalog()");
        e.eval("st = cat['foo']['bar']");
        e.eval("l = Layer('baz', st)");
        e.eval("l.save()");

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(ds, "baz");

        assertNotNull(ft);
        assertNotNull(ft.getFeatureType());
        assertNotNull(ft.getFeatureSource(null, null));
    }

    public void testSaveFeatureType() throws Exception {
        testAddStore();
    }
}
