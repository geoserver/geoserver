/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.bdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.geoserver.importer.CountingVisitor;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ImporterInfoDAO;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.SearchingVisitor;
import org.geoserver.importer.bdb.BDBImportStore.BindingType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BDBImportStoreTest extends ImporterTestSupport {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> result = new ArrayList<>();
        result.add(new Object[] {"serial", BindingType.SERIAL});
        result.add(new Object[] {"xstream", BindingType.XSTREAM});

        return result;
    }

    BDBImportStore store;
    File dbRoot;

    private BindingType bindingType;

    public BDBImportStoreTest(String name, BindingType bindingType) {
        this.bindingType = bindingType;
    }

    // The test has been written assuming the importer uses a memory store while a separate, test
    // managed store is used to run checks... keeping it that way, wasted enough time trying to
    // make it just run with the importer one
    @BeforeClass
    public static void setupMemoryStore() {
        System.setProperty(Importer.IMPORTER_STORE_KEY, "memory");
    }

    @AfterClass
    public static void clearMemoryStore() {
        System.clearProperty(Importer.IMPORTER_STORE_KEY);
    }

    @Before
    public void setupStoreField() throws Exception {
        store = new BDBImportStore(importer);
        store.setBinding(bindingType);
        store.init();
        dbRoot = new File(importer.getImportRoot(), "bdb");
    }

    // in order to test this, run once, then change the serialVersionUID of ImportContext2
    @Test
    public void testSerialVersionUIDChange() throws Exception {
        Importer imp =
                new Importer(null, new ImporterInfoDAO()) {

                    @Override
                    public File getImportRoot() {
                        File root = new File("target");
                        root.mkdirs();
                        return root;
                    }
                };
        ImportContext ctx = new ImportContext2();
        ctx.setState(ImportContext.State.PENDING);
        ctx.setUser("fooboo");
        BDBImportStore store = new BDBImportStore(imp);
        try {
            store.init();
            store.add(ctx);

            Iterator<ImportContext> iterator = store.iterator();
            while (iterator.hasNext()) {
                ctx = iterator.next();
                assertEquals("fooboo", ctx.getUser());
            }

            store.add(ctx);
        } finally {
            store.destroy();
        }
    }

    public static class ImportContext2 extends ImportContext {
        private static final long serialVersionUID = 12345;
    }

    @Test
    public void testAdd() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));

        assertEquals(1, context.getTasks().size());
        for (int i = 0; i < context.getTasks().size(); i++) {
            assertNotNull(context.getTasks().get(i).getStore());
            assertNotNull(context.getTasks().get(i).getStore().getCatalog());
        }

        // @todo commented these out as importer.createContext adds to the store
        //        assertNull(context.getId());

        CountingVisitor cv = new CountingVisitor();
        //        store.query(cv);
        //        assertEquals(0, cv.getCount());

        store.add(context);
        assertNotNull(context.getId());
        assertNotNull(context.getTasks().get(0).getLayer());

        ImportContext context2 = store.get(context.getId());
        assertNotNull(context2);
        assertEquals(context.getId(), context2.getId());

        store.query(cv);
        assertEquals(1, cv.getCount());

        SearchingVisitor sv = new SearchingVisitor(context.getId());
        store.query(sv);
        assertTrue(sv.isFound());

        importer.reattach(context2);

        // ensure various transient bits are set correctly on deserialization
        assertEquals(1, context2.getTasks().size());
        for (int i = 0; i < context2.getTasks().size(); i++) {
            assertNotNull(context2.getTasks().get(i).getStore());
            assertNotNull(context2.getTasks().get(i).getStore().getCatalog());
        }
        assertNotNull(context2.getTasks().get(0).getLayer());
    }

    @Test
    public void testSaveRemoteData() throws Exception {
        ImportContext context = importer.registerContext(null);
        RemoteData data = new RemoteData("ftp://geoserver.org");
        data.setUsername("geoserver");
        data.setPassword("gisIsCool");
        context.setData(data);

        store.add(context);
        assertNotNull(context.getId());

        ImportContext context2 = store.get(context.getId());
        assertEquals(data, context2.getData());
    }

    @Test
    public void testSave() throws Exception {
        testAdd();

        ImportContext context = store.get(0);
        assertNotNull(context);

        assertEquals(ImportContext.State.PENDING, context.getState());
        context.setState(ImportContext.State.COMPLETE);

        ImportContext context2 = store.get(0);
        assertNotNull(context2);
        assertEquals(ImportContext.State.PENDING, context2.getState());

        store.save(context);
        context2 = store.get(0);
        assertNotNull(context2);
        assertEquals(ImportContext.State.COMPLETE, context2.getState());
    }

    @Test
    public void testDatabaseRecovery() throws Exception {}

    @Test
    public void testIDManagement() throws Exception {
        // verify base - first one is zero
        ImportContext zero = new ImportContext();
        store.add(zero);
        assertEquals(Long.valueOf(0), zero.getId());

        // try for zero again (less than current case - client out of sync)
        Long advanceId = store.advanceId(0L);
        assertEquals(Long.valueOf(1), advanceId);

        // and again for current (equals current case - normal mode)
        advanceId = store.advanceId(2L);
        assertEquals(Long.valueOf(2), advanceId);

        // now jump ahead (client advances case - server out of sync)
        advanceId = store.advanceId(666L);
        assertEquals(Long.valueOf(666), advanceId);

        // the next created import should be one higher
        ImportContext dumby = new ImportContext();
        store.add(dumby);
        assertEquals(Long.valueOf(667), dumby.getId());
    }

    @After
    public void destroyStore() throws Exception {
        store.destroy();
        // clean up the databse
        FileUtils.deleteDirectory(dbRoot);
    }
}
