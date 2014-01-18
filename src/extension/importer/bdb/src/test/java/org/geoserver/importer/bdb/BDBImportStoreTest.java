/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.bdb;

import java.io.File;
import java.util.Iterator;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportStore.ImportVisitor;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ImporterTestSupport;


public class BDBImportStoreTest extends ImporterTestSupport {

    BDBImportStore store;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        store = new BDBImportStore(importer);
        store.init();
    }
    
    // in order to test this, run once, then change the serialVersionUID of ImportContext2
    public void testSerialVersionUIDChange() throws Exception {
        Importer imp =  new Importer(null) {

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
        store = new BDBImportStore(imp);
        store.init();
        store.add(ctx);
        
        Iterator<ImportContext> iterator = store.iterator();
        while (iterator.hasNext()) {
            ctx = iterator.next();
            assertEquals("fooboo", ctx.getUser());
        }
        
        store.add(ctx);
        
        store.destroy();
    }
    
    public static class ImportContext2 extends ImportContext {
        private static final long serialVersionUID = 12345;
    }

    public void testAdd() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));

        assertEquals(1,context.getTasks().size());
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
        assertEquals(1,context2.getTasks().size());
        for (int i = 0; i < context2.getTasks().size(); i++) {
            assertNotNull(context2.getTasks().get(i).getStore());
            assertNotNull(context2.getTasks().get(i).getStore().getCatalog());
        }
        assertNotNull(context2.getTasks().get(0).getLayer());
    }

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

    public void testDatabaseRecovery() throws Exception {
        
    }
    
    public void testIDManagement() throws Exception {
        // verify base - first one is zero
        ImportContext zero = new ImportContext();
        store.add(zero);
        assertEquals(new Long(0), zero.getId());

        // try for zero again (less than current case - client out of sync)
        Long advanceId = store.advanceId(0L);
        assertEquals(new Long(1), advanceId);

        // and again for current (equals current case - normal mode)
        advanceId = store.advanceId(2L);
        assertEquals(new Long(2), advanceId);

        // now jump ahead (client advances case - server out of sync)
        advanceId = store.advanceId(666L);
        assertEquals(new Long(666), advanceId);

        // the next created import should be one higher
        ImportContext dumby = new ImportContext();
        store.add(dumby);
        assertEquals(new Long(667), dumby.getId());
    }

    class SearchingVisitor implements ImportVisitor {
        long id;
        boolean found = false;

        SearchingVisitor(long id) {
            this.id = id;
        }
        public void visit(ImportContext context) {
            if (context.getId().longValue() == id) {
                found = true;
            }
        }
        public boolean isFound() {
            return found;
        }
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        store.destroy();

//        Environment env = db.getEnvironment();
//        db.close();
//        classDb.close();
//        env.close();
    }
}
