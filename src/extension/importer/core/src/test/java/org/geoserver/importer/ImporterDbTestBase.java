/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.data.jdbc.JDBCUtils;
import org.junit.Test;

public abstract class ImporterDbTestBase extends ImporterDbTestSupport {

    @Override
    protected void doSetUpInternal() throws Exception {
        Connection cx = getConnection();
        try {
            Statement st = cx.createStatement();
            try {
                dropTable("widgets", st);
                dropTable("archsites", st);
                dropTable("bugsites", st);

                createWidgetsTable(st);
            } finally {
                JDBCUtils.close(st);
            }
        } finally {
            JDBCUtils.close(cx, null, null);
        }
    }

    protected String tableName(String name) {
        return name;
    }

    protected abstract void createWidgetsTable(Statement st) throws Exception;

    protected void dropTable(String tableName, Statement st) throws Exception {
        runSafe("DROP TABLE " + tableName(tableName), st);
    }

    @Test
    public void testDirectImport() throws Exception {
        Database db = new Database(getConnectionParams());

        ImportContext context = importer.createContext(db);
        assertEquals(ImportContext.State.PENDING, context.getState());

        assertEquals(1, context.getTasks().size());

        importer.run(context);
        runChecks("gs:" + tableName("widgets"));
    }

    @Test
    public void testIndirectToShapefile() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir));
        importer.run(context);

        runChecks("gs:archsites");
        runChecks("gs:bugsites");

        DataStoreInfo store = (DataStoreInfo) context.getTasks().get(0).getStore();
        assertNotNull(store);
        assertEquals(2, getCatalog().getFeatureTypesByDataStore(store).size());

        context = importer.createContext(new Database(getConnectionParams()), store);
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());

        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        assertEquals(3, getCatalog().getFeatureTypesByDataStore(store).size());
        runChecks("gs:" + tableName("widgets"));
    }

    @Test
    public void testIndirectToDb() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("oracle");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);
        ds.getConnectionParameters().putAll(getConnectionParams());
        cat.add(ds);

        assertEquals(0, cat.getFeatureTypesByDataStore(ds).size());
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(2, context.getTasks().size());

        assertEquals(ImportTask.State.READY, context.getTasks().get(0).getState());
        assertEquals(ImportTask.State.READY, context.getTasks().get(1).getState());

        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        assertEquals(2, cat.getFeatureTypesByDataStore(ds).size());
        runChecks("gs:" + tableName("archsites"));
        runChecks("gs:" + tableName("bugsites"));
    }
}
