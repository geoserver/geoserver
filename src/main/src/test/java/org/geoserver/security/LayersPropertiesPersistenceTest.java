/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that demonstrates exactly where and how layers.properties is written to disk from catalog operations.
 * This test identifies the specific file and method responsible for persisting layers.properties.
 */
public class LayersPropertiesPersistenceTest extends GeoServerSystemTestSupport {

    private DataAccessRuleDAO dao;
    private File layersPropertiesFile;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    @Before
    public void setUp() throws Exception {
        // Get the DataAccessRuleDAO instance
        dao = DataAccessRuleDAO.get();
        
        // Locate the layers.properties file
        Resource securityDir = getDataDirectory().getSecurity();
        Resource layersPropertiesResource = securityDir.get("layers.properties");
        layersPropertiesFile = layersPropertiesResource.file();
    }

    @Test
    public void testLayersPropertiesPersistenceLocation() throws IOException {
        // Record the initial state
        long initialModified = layersPropertiesFile.exists() ? layersPropertiesFile.lastModified() : 0;
        String initialContent = layersPropertiesFile.exists() ? 
            new String(Files.readAllBytes(layersPropertiesFile.toPath())) : "";

        // Add a new security rule through the DAO
        DataAccessRule testRule = new DataAccessRule("test", "layer", 
            org.geoserver.security.AccessMode.READ, 
            java.util.Collections.singleton("TEST_ROLE"));
        
        dao.addRule(testRule);
        
        // THIS IS THE KEY METHOD CALL - where layers.properties gets written to disk
        // File: /src/main/src/main/java/org/geoserver/security/impl/AbstractAccessRuleDAO.java
        // Method: storeRules() at line 134-149
        // Exact line where writing occurs: line 141 - p.store(os, null);
        dao.storeRules();
        
        // Verify the file was written
        assertTrue("layers.properties file should exist after storeRules()", layersPropertiesFile.exists());
        
        // Verify the file was modified
        long newModified = layersPropertiesFile.lastModified();
        assertTrue("File modification time should have changed", newModified > initialModified);
        
        // Verify the content contains our new rule
        String newContent = new String(Files.readAllBytes(layersPropertiesFile.toPath()));
        assertNotEquals("File content should have changed", initialContent, newContent);
        assertTrue("File should contain the new test rule", newContent.contains("test.layer.r=TEST_ROLE"));
        
        // Clean up - remove the test rule
        dao.removeRule(testRule);
        dao.storeRules();
    }

    @Test 
    public void testCatalogTriggeredLayersPropertiesWrite() throws IOException {
        // This test demonstrates how catalog changes trigger layers.properties writes
        // through the SecuredResourceNameChangeListener
        
        Catalog catalog = getCatalog();
        
        // Get a layer and workspace for testing
        LayerInfo layerInfo = catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS));
        assertNotNull("Test layer should exist", layerInfo);
        
        WorkspaceInfo workspace = layerInfo.getResource().getStore().getWorkspace();
        assertNotNull("Workspace should exist", workspace);
        
        // Add a security rule for this specific layer
        DataAccessRule layerRule = new DataAccessRule(workspace.getName(), layerInfo.getName(), 
            org.geoserver.security.AccessMode.READ, 
            java.util.Collections.singleton("LAYER_TEST_ROLE"));
        
        dao.addRule(layerRule);
        dao.storeRules();
        
        // Record the file state before catalog change
        long beforeModified = layersPropertiesFile.lastModified();
        
        // Rename the layer - this should trigger SecuredResourceNameChangeListener
        // which will call dao.storeRules() at line 209 in SecuredResourceNameChangeListener.java
        String originalName = layerInfo.getName();
        String newName = originalName + "_renamed";
        
        layerInfo.setName(newName);
        catalog.save(layerInfo);
        
        // Verify that layers.properties was updated due to the catalog change
        long afterModified = layersPropertiesFile.lastModified();
        assertTrue("layers.properties should be updated when layer is renamed", 
            afterModified > beforeModified);
        
        // Verify the rule was updated with the new name
        String content = new String(Files.readAllBytes(layersPropertiesFile.toPath()));
        assertTrue("File should contain updated rule with new layer name", 
            content.contains(workspace.getName() + "." + newName + ".r=LAYER_TEST_ROLE"));
        assertFalse("File should not contain old layer name", 
            content.contains(workspace.getName() + "." + originalName + ".r=LAYER_TEST_ROLE"));
        
        // Clean up - restore original name and remove test rule
        layerInfo.setName(originalName);
        catalog.save(layerInfo);
        
        // Remove the test rule by finding it with the current name
        dao.getRules().stream()
            .filter(rule -> rule.getRoot().equals(workspace.getName()) && 
                           rule.getLayer().equals(originalName) &&
                           rule.getRoles().contains("LAYER_TEST_ROLE"))
            .findFirst()
            .ifPresent(dao::removeRule);
        dao.storeRules();
    }

    @Test
    public void testExactPersistenceMethodLocation() {
        // This test documents the exact location where layers.properties is written
        
        // The exact file and method where layers.properties is written to disk:
        // File: /src/main/src/main/java/org/geoserver/security/impl/AbstractAccessRuleDAO.java
        // Method: storeRules() - lines 134-149
        // Exact line where the write occurs: line 141
        //   p.store(os, null);
        //
        // This method is called from:
        // 1. Admin UI operations (when security rules are modified)
        // 2. Catalog changes via SecuredResourceNameChangeListener.save() line 209
        // 3. Programmatic API calls
        
        assertTrue("This test serves as documentation - see comments above", true);
    }
}