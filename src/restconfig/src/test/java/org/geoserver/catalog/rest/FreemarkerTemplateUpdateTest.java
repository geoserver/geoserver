/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;

import junit.framework.Assert;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;


public class FreemarkerTemplateUpdateTest extends GeoServerSystemTestSupport {

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
    
    @Test
    public void testUpdate() throws Exception {
        String fooTemplate = "/rest/templates/foo.ftl";
        String barTemplate = "/rest/templates/bar.ftl";
        
        String fooContent = "hello foo";
        String barContent = "hello bar";
        
        // PUT
        put(fooTemplate, fooContent).close();
        put(barTemplate, barContent).close();
        
        // GET
        Assert.assertEquals(fooContent, getAsString(fooTemplate).trim());
        Assert.assertEquals(barContent, getAsString(barTemplate).trim());
        
        fooContent = "goodye foo";
        
        // PUT
        put(fooTemplate, fooContent).close();
        
        // verify nothing bad happened to workspaces (GEOS-5533)
        File ws = new File(new File(testData.getDataDirectoryRoot(), "workspaces"), "sf");
        Assert.assertTrue(ws.exists());
        Assert.assertTrue(ws.isDirectory());
        
        // GET
        Assert.assertEquals(fooContent, getAsString(fooTemplate).trim());
        Assert.assertEquals(barContent, getAsString(barTemplate).trim());
    }
}
