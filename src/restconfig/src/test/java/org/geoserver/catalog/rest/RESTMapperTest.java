/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.DataUtilities;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Unit test for evaluating the Default REST PathMapper.
 */
public class RESTMapperTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testGlobalConfig() throws Exception {
        // Selection of the root directory
        File rootFile = getRootDirectory();
        String root = rootFile.getAbsolutePath();

        // Setting of the global configuration
        GeoServerInfo global = getGeoServer().getGlobal();
        // Selections of the SettingsInfo associated to the GlobalSettings
        SettingsInfoImpl info = (SettingsInfoImpl) ModificationProxy.unwrap(global.getSettings());
        // If no metadata map is present, then a new one is added
        if (info.getMetadata() == null) {
            info.setMetadata(new MetadataMap());
        }
        // Selection of the metadata
        MetadataMap map = info.getMetadata();
        // Addition of the key associated to the root directory
        map.put(RESTUtils.ROOT_KEY, root);
        // Insertion of the settings inside the global ones
        global.setSettings(info);
        // Save settings
        getGeoServer().save(global);
        // Test of the input file
        testFile(rootFile, "sf", "usa");
    }

    @Test
    public void testWorkspaceConfig() throws Exception {
        // Selection of the root directory
        File rootFile = getRootDirectory();
        String root = rootFile.getAbsolutePath();

        // Creation of a new Workspace called "test"
        String xml = "<workspace>" + "<name>test</name>" + "</workspace>";

        // Add the workspace "test" to geoserver
        MockHttpServletResponse responseBefore = postAsServletResponse("/rest/workspaces", xml,
                "text/xml");
        assertEquals(201, responseBefore.getStatusCode());
        assertNotNull(responseBefore.getHeader("Location"));
        assertTrue(responseBefore.getHeader("Location").endsWith("/workspaces/test"));

        // Setting of the workspace configuration
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("test");
        assertNotNull(ws);

        // Creation of new Settings to test 
        SettingsInfoImpl info = new SettingsInfoImpl();
        
        // Setting of the metadata map if not present
        if (info.getMetadata() == null) {
            info.setMetadata(new MetadataMap());
        }
        // Selection of the metadata map
        MetadataMap map = info.getMetadata();
        // Addition of the key associated to the root directory 
        map.put(RESTUtils.ROOT_KEY, root);
        // Associate the workspace to the settings
        info.setWorkspace(ws);
        // Add the new Settings
        getGeoServer().add(info);
        // Test of the input file
        testFile(rootFile, "test", "usa2");
    }

    @Test
    public void testStoreConfig() throws Exception {
        // Selection of the root directory
        File rootFile = getRootDirectory();
        String root = rootFile.getAbsolutePath();
        // Adding a new default coverage
        getTestData().addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
        // Modifying the coverageStore by setting it to WorldImage
        String xml = "<coverageStore>" + "<name>BlueMarble</name>" + "<type>WorldImage</type>"
                + "</coverageStore>";
        MockHttpServletResponse responseBefore = putAsServletResponse(
                "/rest/workspaces/wcs/coveragestores/BlueMarble", xml, "text/xml");
        assertEquals(200, responseBefore.getStatusCode());
        // Selection of the coverage store
        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName("wcs", "BlueMarble");
        // Setting of the store configuration
        MetadataMap map = cs.getMetadata();
        // Addition of the key associated to the root directory
        map.put(RESTUtils.ROOT_KEY, root);
        // Saving the store
        getCatalog().save(cs);
        // Test of the input file
        testFile(rootFile, "wcs", "BlueMarble");
    }

    /**
     * Create a fake root directory where the sample data are stored. This directory contains a path with spaces
     * in order to check that no error are thrown with URL encoding.
     * 
     * @return
     */
    private File getRootDirectory() {
        File dataDirectoryRoot = getTestData().getDataDirectoryRoot();
        File newroot = new File(dataDirectoryRoot, "test data");
        return newroot;
    }

    /**
     * Private method for adding the selected coverage inside the defined workspace via REST and then checking if the coverage has been placed inside
     * the right directory
     * 
     * @param root
     * @param workspace
     * @param coverageStore
     * @throws Exception
     */
    private void testFile(File root, String workspace, String coverageStore) throws Exception {
        // Selection of a zip file
        URL zip = getClass().getResource("test-data/usa.zip");
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));
        // Uploading the file via rest
        MockHttpServletResponse response = putAsServletResponse("/rest/workspaces/" + workspace
                + "/coveragestores/" + coverageStore + "/file.worldimage", bytes, "application/zip");
        assertEquals(201, response.getStatusCode());
        // Check if the coverage is present
        String content = response.getOutputStreamContent();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("coverageStore", d.getDocumentElement().getNodeName());
        // Control if the coverage store is present
        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName(workspace, coverageStore);
        assertNotNull(cs);
        // Control if the associated info are present
        CoverageInfo ci = getCatalog().getCoverageByName(workspace, "usa");
        assertNotNull(ci);
        // Check if the defined root is present        
        // Using DataUtilities.fileToURL in order to have the same URL encoding
        String urlString = cs.getURL();
        File urlFile = new File(urlString);
        URL url = DataUtilities.fileToURL(urlFile);
        File urlFileRoot = new File(DataUtilities.fileToURL(root).getPath());
        URL urlRoot = DataUtilities.fileToURL(urlFileRoot);
        
        assertTrue(url.toExternalForm().contains(urlRoot.toExternalForm()));
    }
}
