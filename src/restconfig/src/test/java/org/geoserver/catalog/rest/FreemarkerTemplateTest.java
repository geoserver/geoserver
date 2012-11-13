/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;


public class FreemarkerTemplateTest extends GeoServerSystemTestSupport {

    public void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    private String getIndexAsString(String childPath, String format) throws Exception {
        String indexUrl = childPath.substring(0, childPath.lastIndexOf("/"));
        if (format != null) {
            indexUrl += "." + format;            
        }
        String indexContent = getAsString(indexUrl);
        return indexContent;
    }
    
    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }
    
    private void testGetPutGetDeleteGet(String path, String content) throws Exception {
        String name = getName(path);
        
        String htmlIndexToken = "geoserver" + path + "\">" + name + "</a></li>";
        String xmlIndexToken = "<name>" + name + "</name>";
        String jsonIndexToken = "{\"name\":\"" + name + "\"";
        
        // GET
        Assert.assertEquals("File Not Found", getAsString(path).trim());
        Assert.assertFalse(getIndexAsString(path, null).contains(htmlIndexToken));        
        Assert.assertFalse(getIndexAsString(path, "html").contains(htmlIndexToken));
        Assert.assertFalse(getIndexAsString(path, "xml").contains(xmlIndexToken));
        Assert.assertFalse(getIndexAsString(path, "json").contains(jsonIndexToken));        
                
        // PUT
        put(path, content).close();
        Assert.assertTrue(getIndexAsString(path, null).contains(htmlIndexToken));                
        Assert.assertTrue(getIndexAsString(path, "html").contains(htmlIndexToken));
        Assert.assertTrue(getIndexAsString(path, "xml").contains(xmlIndexToken));
        Assert.assertTrue(getIndexAsString(path, "json").contains(jsonIndexToken));        
        
        // GET
        Assert.assertEquals(content, getAsString(path).trim());
        
        // DELETE
        Assert.assertEquals(200, deleteAsServletResponse(path).getStatusCode());
        
        // GET
        Assert.assertEquals("File Not Found", getAsString(path).trim());
        Assert.assertFalse(getIndexAsString(path, null).contains(htmlIndexToken));        
        Assert.assertFalse(getIndexAsString(path, "html").contains(htmlIndexToken));
        Assert.assertFalse(getIndexAsString(path, "xml").contains(xmlIndexToken));
        Assert.assertFalse(getIndexAsString(path, "json").contains(jsonIndexToken));                
    }
    
    @Test
    public void testGetPutGetDeleteGet() throws Exception {
        String path = "/rest/templates/my_template.ftl";
        testGetPutGetDeleteGet(path, "hello world");
    }

    private List<String> getAllPaths() {
        List<String> paths = new ArrayList<String>();
        
        paths.add("/rest/templates/aTemplate.ftl");
        paths.add("/rest/templates/anotherTemplate.ftl");
        
        paths.add("/rest/workspaces/topp/templates/aTemplate.ftl");
        paths.add("/rest/workspaces/topp/templates/anotherTemplate.ftl");

        paths.add("/rest/workspaces/topp/datastores/states_shapefile/templates/aTemplate.ftl");
        paths.add("/rest/workspaces/topp/datastores/states_shapefile/templates/anotherTemplate.ftl");

        paths.add("/rest/workspaces/topp/datastores/states_shapefile/featuretypes/states/templates/aTemplate.ftl");
        paths.add("/rest/workspaces/topp/datastores/states_shapefile/featuretypes/states/templates/anotherTemplate.ftl");
        
        paths.add("/rest/workspaces/wcs/coveragestores/DEM/templates/aTemplate.ftl");
        paths.add("/rest/workspaces/wcs/coveragestores/DEM/templates/anotherTemplate.ftl");
        
        paths.add("/rest/workspaces/wcs/coveragestores/DEM/coverages/tazdem.tiff/templates/aTemplate.ftl");
        paths.add("/rest/workspaces/wcs/coveragestores/DEM/coverages/tazdem.tiff/templates/anotherTemplate.ftl");

        return paths;
    }
    
    @Test
    public void testAllPathsSequentially() throws Exception {
        Random random = new Random();        
        for (String path : getAllPaths()) {
            testGetPutGetDeleteGet(path, "hello test " + random.nextInt(1000));
        }
    }

    @Test
    public void testAllPaths() throws Exception {
        String contentHeader = "hello path ";                
        List<String> paths = getAllPaths();       
        
        for (String path : paths) {
            // GET
            Assert.assertEquals("File Not Found", getAsString(path).trim());            
        }        
        
        for (String path : paths) {
            // PUT
            put(path, contentHeader + path).close();
        }                
        
        for (String path : paths) {
            // GET
            Assert.assertEquals(contentHeader + path, getAsString(path).trim());
        }                        
        
        for (String path : paths) {
            // DELETE
            Assert.assertEquals(200, deleteAsServletResponse(path).getStatusCode());
        }                                
        
        for (String path : paths) {
            // GET
            Assert.assertEquals("File Not Found", getAsString(path).trim());            
        }
    }
}
