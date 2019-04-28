/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import javax.script.ScriptEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.script.ScriptTestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.h2.tools.DeleteDbFiles;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.context.ApplicationContext;

public class PyCatalogDataTest extends ScriptTestSupport {

    ByteArrayOutputStream out;

    public void setUp() throws Exception {
        super.setUp();
        setUpMock();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        tearDownMock();
    }

    void setUpMock() throws Exception {
        DeleteDbFiles.execute("target", "acme", true);

        H2DataStoreFactory fac = new H2DataStoreFactory();
        HashMap params = new HashMap();

        params.put(H2DataStoreFactory.DATABASE.key, "target/acme");
        params.put(H2DataStoreFactory.DBTYPE.key, "h2");

        DataStore ds = fac.createDataStore(params);

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("widgets");
        tb.add("type", String.class);
        tb.add("price", Double.class);
        tb.add("geom", Point.class);
        SimpleFeatureType ft = tb.buildFeatureType();
        ds.createSchema(ft);

        FeatureWriter fw = ds.getFeatureWriter("widgets", Transaction.AUTO_COMMIT);

        WKTReader wkt = new WKTReader();

        fw.hasNext();
        SimpleFeature next = (SimpleFeature) fw.next();
        next.setAttribute("type", "anvil");
        next.setAttribute("price", 10.99);
        next.setAttribute("geom", wkt.read("POINT(12.5 13.7)"));
        fw.write();

        fw.hasNext();
        next = (SimpleFeature) fw.next();
        next.setAttribute("type", "dynamite");
        next.setAttribute("price", 99.99);
        next.setAttribute("geom", wkt.read("POINT(11.8 16.7)"));
        fw.write();

        fw.close();

        Catalog cat = createNiceMock(Catalog.class);

        // workspaces
        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(ws.getName()).andReturn("acme").anyTimes();
        replay(ws);

        expect(cat.getWorkspaces()).andReturn(Arrays.asList(ws)).anyTimes();
        expect(cat.getDefaultWorkspace()).andReturn(ws).anyTimes();
        expect(cat.getWorkspaceByName("acme")).andReturn(ws).anyTimes();

        // data stores
        DataStoreInfo acme = createNiceMock(DataStoreInfo.class);
        expect(acme.getName()).andReturn("acme").anyTimes();
        expect(acme.getWorkspace()).andReturn(ws).anyTimes();
        expect(acme.getDataStore(null)).andReturn((DataAccess) ds).anyTimes();
        replay(acme);

        expect(cat.getDataStoresByWorkspace(ws)).andReturn(Arrays.asList(acme)).anyTimes();
        expect(cat.getDataStoresByWorkspace("acme")).andReturn(Arrays.asList(acme)).anyTimes();

        expect(cat.getDataStoreByName("acme", "acme")).andReturn(acme).anyTimes();
        expect(cat.getDataStoreByName(ws, "acme")).andReturn(acme).anyTimes();

        // feature types
        FeatureTypeInfo widgets = createNiceMock(FeatureTypeInfo.class);
        expect(widgets.getName()).andReturn("widgets");
        expect(widgets.getFeatureSource(null, null))
                .andReturn((FeatureSource) ds.getFeatureSource("widgets"));
        expect(widgets.getFeatureType()).andReturn(ds.getSchema("widgets"));
        expect(widgets.getStore()).andReturn(acme);
        replay(widgets);

        expect(cat.getFeatureTypeByDataStore(acme, "widgets")).andReturn(widgets).anyTimes();

        // app context
        ApplicationContext app = createNiceMock(ApplicationContext.class);
        expect(app.getBean("catalog")).andReturn(cat).anyTimes();

        replay(cat);
        replay(app);

        GeoServerExtensionsHelper.init(app);
    }

    void tearDownMock() throws Exception {
        GeoServerExtensionsHelper.init(null);
        DeleteDbFiles.execute("target", "acme", true);
    }

    public void testLayerData() throws Exception {
        ScriptEngine e = scriptMgr.createNewEngine("py");

        e.eval("from geoserver.catalog import Workspace");
        e.eval("acme = Workspace('acme')['acme']");

        clear(e);
        e.eval("widgets = acme['widgets']");
        e.eval("print widgets.data.count()");
        _assert("2");

        clear(e);
        e.eval("print widgets.data.bounds()");
        _assert("(11.8, 13.7, 12.5, 16.7)");

        clear(e);
        e.eval("for f in widgets.data.features(): print f.geom");
        _assert("POINT (12.5 13.7)\nPOINT (11.8 16.7)");
    }

    void print() {
        System.out.println(new String(out.toByteArray()));
    }

    void clear(ScriptEngine e) {
        out = new ByteArrayOutputStream();
        e.getContext().setWriter(new OutputStreamWriter(out));
    }

    void _assert(String result) {
        assertEquals(result, new String(out.toByteArray()).trim());
    }
}
