/* Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.platform.resource.ResourceMatchers.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;

import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.geoserver.platform.resource.NullLockProvider;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.After;
import org.junit.Test;

/**
 * 
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 *
 */
public abstract class AbstractJDBCResourceStoreTest {
    
    DatabaseTestSupport support;
    
    @After
    public void cleanUp() throws Exception {
        support.close();
    }
    
    JDBCResourceStoreProperties getConfig(boolean enabled, boolean init) {
        JDBCResourceStoreProperties config = createMock(JDBCResourceStoreProperties.class);
        expect(config.isInitDb()).andStubReturn(init);
        expect(config.isEnabled()).andStubReturn(enabled);
        expect(config.isImport()).andStubReturn(init);
        expect(config.getIgnoreDirs()).andStubReturn(new String[] {"DirIgnore"});
        config.setInitDb(false);        
        expectLastCall();
        try {
            config.save();      
        } catch (Exception e) {}
        expectLastCall();
        support.stubConfig(config);
        replay(config);
        return config;
    }
    
    @Test
    public void testInitializeEmptyDB() throws Exception {
        JDBCResourceStoreProperties config = getConfig(true, true);
        
        @SuppressWarnings("unused")
        ResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        
        // Check that the database has a resources table with a root record
        
        ResultSet rs = support.getConnection().createStatement().executeQuery("SELECT * from resources where oid = 0");
        
        assertThat(rs.next(), describedAs("found root record",is(true)));
        assertThat(rs.getString("name"), equalTo(""));
        rs.getInt("parent");
        assertThat(rs.wasNull(), is(true));
        assertThat(rs.getBlob("content"), nullValue());
        assertThat(rs.next(), describedAs("only one root",is(false)));
    }
    
    void standardData() throws Exception {
        support.initialize();
        
        support.addFile("FileA", 0, "FileA Contents".getBytes());
        support.addFile("FileB", 0, "FileB Contents".getBytes());
        int c = support.addDir("DirC", 0);
        support.addFile("FileD", c, "FileD Contents".getBytes());
        support.addDir("DirE", 0);
        int f = support.addDir("DirF", c);
        int g = support.addDir("DirG", f);
        support.addFile("FileH", g, "FileH Contents".getBytes());
    }
    
    @Test
    public void testAcceptInitializedDB() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        {
            // Check that the database has a resources table with a root record
            
            ResultSet rs = support.getConnection().createStatement().executeQuery("SELECT * from resources where oid = 0");
            
            assertThat(rs.next(), describedAs("found root record",is(true)));
            assertThat(rs.getString("name"), equalTo(""));
            rs.getInt("parent");
            assertThat(rs.wasNull(), is(true));
            assertThat(rs.getBlob("content"), nullValue());
            assertThat(rs.next(), describedAs("only one root",is(false)));
        }
        {
            // Check that the database has one of the child nodes
            
            ResultSet rs = support.getConnection().createStatement().executeQuery("SELECT * from resources where parent = 0 and name='FileA'");
            
            assertThat(rs.next(), describedAs("found child FileA",is(true)));
            assertThat(rs.getString("name"), equalTo("FileA"));
            assertThat(rs.getInt("parent"), equalTo(0));
            assertThat(rs.wasNull(), is(false));
            assertThat(rs.getBinaryStream("content"), not(nullValue()));
            assertThat(rs.getInt("oid"), not(equalTo(0)));
        }
    }
    
    @Test
    public void testInitializeDatabaseWithIrrelevantTable() throws Exception {
        support.getConnection().createStatement().execute("CREATE TABLE foo (oid INTEGER PRIMARY KEY);");

        JDBCResourceStoreProperties config = getConfig(true, true);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        {
            // Check that the database has a resources table with a root record
            
            ResultSet rs = support.getConnection().createStatement().executeQuery("SELECT * from resources where oid = 0");
            
            assertThat(rs.next(), describedAs("found root record",is(true)));
            assertThat(rs.getString("name"), equalTo(""));
            rs.getInt("parent");
            assertThat(rs.wasNull(), is(true));
            assertThat(rs.getBlob("content"), nullValue());
            assertThat(rs.next(), describedAs("only one root",is(false)));
        }
        
    }
    
    @Test
    public void testBasicResourceQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        
        Resource r = store.get("FileA");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, resource());
        
    }
    @Test
    public void testBasicDirectoryQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        
        Resource r = store.get("DirE");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, directory());
        
    }
    
    @Test
    public void testBasicUndefinedQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        
        Resource r = store.get("DoesntExist");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, undefined());
        
    }
    
    @Test
    public void testLongQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        
        Resource r = store.get("DirC/DirF/DirG/FileH");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, resource());
        
    }
    @Test
    public void testBasicRead() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        JDBCResourceStore store = new JDBCResourceStore(support.getDataSource(), config);
        store.setLockProvider(new NullLockProvider());
        
        Resource r = store.get("FileA");
        
        byte[] expected = "FileA Contents".getBytes();
        
        InputStream in = r.in();
        try {
            byte[] result = new byte[expected.length];
            assertThat(in.read(result), describedAs("file contents same length",equalTo(expected.length)));
            assertThat(result, equalTo(expected));
            assertThat(in.read(), describedAs("stream is empty",equalTo(-1)));
        } finally {
            in.close();
        }
        
    }
    
    @Test
    public void testIgnoreDir() throws Exception {
        JDBCResourceStoreProperties config = getConfig(true, false);
        
        ResourceStore dataDirStore = new DataDirectoryResourceStore();
        ResourceStore store = new JDBCResourceStore(support.getDataSource(), config,
                dataDirStore);
                
        assertEquals(store.get("DirIgnore"), dataDirStore.get("DirIgnore"));
        assertNotEquals(store.get("DirDontIgnore"), dataDirStore.get("DirDontIgnore"));
    }
    
    private static class TestResourceListener implements ResourceListener {
        private ResourceNotification notify;
        
        @Override
        public void changed(ResourceNotification notify) {
            this.notify = notify;
        }

        public ResourceNotification getNotify() {
            return notify;
        }
        
        public void reset() {
            notify = null;
        }
    }
    
    @Test
    public void fileEvents() throws Exception {
        standardData();
        
        ResourceStore store = new JDBCResourceStore(support.getDataSource(), getConfig(false, false));   
        
        TestResourceListener listener = new TestResourceListener();
        
        Resource fileD = store.get("DirC/FileD");
        fileD.addListener(listener);
        
        long before = fileD.lastmodified();
        try (OutputStream out = fileD.out()) {
            out.write(1234);
        }
        long after = fileD.lastmodified();
        assertTrue(after>before);
        
        assertNotNull(listener.getNotify());
        assertEquals(Kind.ENTRY_MODIFY, listener.getNotify().getKind());
        assertEquals(1, listener.getNotify().events().size());
        long timeStamp = listener.getNotify().getTimestamp();
        assertTrue(timeStamp > before);
        
        listener.reset();
        fileD.delete();        
        
        assertNotNull(listener.getNotify());
        assertEquals(Kind.ENTRY_DELETE, listener.getNotify().getKind());
        assertEquals(1, listener.getNotify().events().size());

        listener.reset();
        try (OutputStream out = fileD.out()) {
            assertNotNull(listener.getNotify());
            assertEquals(Kind.ENTRY_CREATE, listener.getNotify().getKind());
            assertEquals(1, listener.getNotify().events().size());
        }          
        
        fileD.removeListener(listener);
    }
    
    @Test
    public void directoryEvents() throws Exception {
        standardData();
        
        ResourceStore store = new JDBCResourceStore(support.getDataSource(), getConfig(false, false));        
        Resource fileA = store.get("FileA");
        Resource fileB = store.get("FileB");
        Resource fileD = store.get("DirC/FileD");
        
        TestResourceListener listener = new TestResourceListener();
        store.get(Paths.BASE).addListener(listener);
        
        long before = fileB.lastmodified();
        try (OutputStream out = fileD.out()) {
            out.write(1234);
        }
        long after = fileD.lastmodified();
        assertTrue(after>before);
        
        assertNotNull(listener.getNotify());
        assertEquals(Kind.ENTRY_MODIFY, listener.getNotify().getKind());
        assertEquals(1, listener.getNotify().events().size());
        assertEquals(Paths.BASE, listener.getNotify().getPath());
        long timeStamp = listener.getNotify().getTimestamp();
        assertTrue(timeStamp > before);
        Event e = listener.getNotify().events().get(0);
        assertEquals(Kind.ENTRY_MODIFY, e.getKind());
        assertEquals("DirC/FileD", e.getPath());
        
        listener.reset();
        fileA.delete();

        assertNotNull(listener.getNotify());
        assertEquals(Kind.ENTRY_MODIFY, listener.getNotify().getKind());
        assertEquals(1, listener.getNotify().events().size());
        assertEquals(Paths.BASE, listener.getNotify().getPath());
        e = listener.getNotify().events().get(0);
        assertEquals(Kind.ENTRY_DELETE, e.getKind());
        assertEquals("FileA", e.getPath());
        
        listener.reset();
        try (OutputStream out = fileA.out()) {

            assertEquals(Kind.ENTRY_MODIFY, listener.getNotify().getKind());
            assertEquals(1, listener.getNotify().events().size());
            assertEquals(Paths.BASE, listener.getNotify().getPath());
            e = listener.getNotify().events().get(0);
            assertEquals(Kind.ENTRY_CREATE, e.getKind());
            assertEquals("FileA", e.getPath());
        }

        
        store.get(Paths.BASE).removeListener(listener);
    }


}
