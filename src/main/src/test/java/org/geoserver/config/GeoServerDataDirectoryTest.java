package org.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

public class GeoServerDataDirectoryTest extends GeoServerSystemTestSupport {

    private GeoServerDataDirectory dd;

    private File root;

    private WorkspaceInfo ws;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do nothing, we don't need the test data
    }

    @Before
    public void retrieveDataDirectory() {
        ws = getCatalog().getDefaultWorkspace();
        dd = applicationContext.getBean(GeoServerDataDirectory.class);
        root = dd.root();
        assertTrue(root.getPath().startsWith("./target"));
    }

    @Test
    public void testFindDataRoot() throws IOException {
        File expected = new File(root, "data");
        if(!expected.exists()) {
            assertTrue(expected.mkdirs());
        }
        assertEquals(expected, dd.findDataRoot());
    }
    
    @Test
    public void testFindOrCreateDataRoot() throws IOException {
        File expected = new File(root, "data");
        if(expected.exists()) {
            FileUtils.deleteDirectory(expected);
        }
        assertEquals(expected, dd.findOrCreateDataRoot());
        assertTrue(expected.exists());
    }
    
    @Test
    public void testFindSecurityRoot() throws IOException {
        File expected = new File(root, "security");
        if(!expected.exists()) {
            assertTrue(expected.mkdirs());
        }
        assertEquals(expected, dd.findSecurityRoot());
    }
    
    @Test
    public void testFindOrCreateSecurityRoot() throws IOException {
        File expected = new File(root, "security");
        if(expected.exists()) {
            FileUtils.deleteDirectory(expected);
        }
        assertEquals(expected, dd.findOrCreateSecurityRoot());
        assertTrue(expected.exists());
    }

    @Test
    public void testFindWorkspacesDir() throws IOException {
        File expected = new File(new File(root, "workspaces"), ws.getName());
        if(!expected.exists()) {
            assertTrue(expected.mkdirs());
        }
        
        assertEquals(expected, dd.findWorkspaceDir(ws));
    }
    
    @Test
    public void testFindOrCreateWorkspacesDir() throws IOException {
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        File expected = new File(new File(root, "workspaces"), ws.getName());
        if(expected.exists()) {
            FileUtils.deleteDirectory(expected);
        }
        
        assertEquals(expected, dd.findOrCreateWorkspaceDir(ws));
    }
    
    @Test
    public void testCreateMasterPassword() throws IOException {
        File security = new File(root, "security");
        File expected = new File(security, "masterpw.txt");
        if(expected.exists()) {
            expected.delete();
        }
    }
   
}