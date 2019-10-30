/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.geoserver.importer.CountingVisitor;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.SearchingVisitor;
import org.geotools.data.DataStoreFinder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.test.FixtureUtilities;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/** Abstract tests for the import store, based on a fixture */
public abstract class AbstractJDBCImportStoreTest extends ImporterTestSupport {

    private JDBCDataStore datastore;

    JDBCImportStore store;

    abstract String getFixtureId();

    @Before
    public void setupDataStore() throws IOException {
        // skip the test if the fixture is not found
        Properties props = getFixture();
        Assume.assumeNotNull(props);

        this.datastore = (JDBCDataStore) DataStoreFinder.getDataStore(props);
        if (datastore == null) {
            fail("Could not locate datastore with properties: " + props);
        }
        this.store = new JDBCImportStore(datastore, importer);
        this.store.init();
        // clean up before rather than after, to leave some evidence in the db, to help debugging
        store.removeAll();
    }

    protected Properties getFixture() {
        File fixtureFile = FixtureUtilities.getFixtureFile(getFixtureDirectory(), getFixtureId());
        if (fixtureFile.exists()) {
            return FixtureUtilities.loadProperties(fixtureFile);
        } else {
            Properties exampleFixture = createExampleFixture();
            if (exampleFixture != null) {
                File exFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
                if (!exFixtureFile.exists()) {
                    createExampleFixture(exFixtureFile, exampleFixture);
                }
            }
            FixtureUtilities.printSkipNotice(getFixtureId(), fixtureFile);
            return null;
        }
    }

    void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            FileOutputStream fout = new FileOutputStream(exFixtureFile);

            exampleFixture.store(
                    fout,
                    "This is an example fixture. Update the "
                            + "values and remove the .example suffix to enable the test");
            fout.flush();
            fout.close();
            // System.out.println("Wrote example fixture file to " + exFixtureFile);
        } catch (IOException ioe) {
            // System.out.println("Unable to write out example fixture " + exFixtureFile);
            java.util.logging.Logger.getGlobal().log(java.util.logging.Level.INFO, "", ioe);
        }
    }

    protected abstract Properties createExampleFixture();

    File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    @After
    public void shutdown() {
        // clean up the DB
        if (store != null) {
            store.destroy();
        }
        if (datastore != null) datastore.dispose();
    }

    @Test
    public void testAdd() throws Exception {
        runAddTest();
    }

    public Long runAddTest() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));

        assertEquals(1, context.getTasks().size());
        for (int i = 0; i < context.getTasks().size(); i++) {
            assertNotNull(context.getTasks().get(i).getStore());
            assertNotNull(context.getTasks().get(i).getStore().getCatalog());
        }

        CountingVisitor cv = new CountingVisitor();

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

        return context.getId();
    }

    @Test
    public void testSave() throws Exception {
        Long id = runAddTest();

        ImportContext context = store.get(id);
        assertNotNull(context);

        assertEquals(ImportContext.State.PENDING, context.getState());
        context.setState(ImportContext.State.COMPLETE);

        ImportContext context2 = store.get(id);
        assertNotNull(context2);
        assertEquals(ImportContext.State.PENDING, context2.getState());

        store.save(context);
        context2 = store.get(id);
        assertNotNull(context2);
        assertEquals(ImportContext.State.COMPLETE, context2.getState());
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
}
