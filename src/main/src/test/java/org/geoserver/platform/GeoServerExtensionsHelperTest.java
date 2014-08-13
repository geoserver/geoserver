/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GeoServerExtensionsHelperTest {

    @Before
    public void setUp() throws Exception {
        System.setProperty("TEST_PROPERTY", "ABC");
    }

    @After
    public void tearDown() throws Exception {
        System.setProperty("TEST_PROPERTY", "");
        GeoServerExtensionsHelper.init(null);
    }
    

    @Test
    public void helperProperty() {
        assertEquals("ABC", GeoServerExtensions.getProperty("TEST_PROPERTY"));

        GeoServerExtensionsHelper.property("TEST_PROPERTY", "abc");
        assertEquals("abc", GeoServerExtensions.getProperty("TEST_PROPERTY"));
        
        GeoServerExtensionsHelper.clear();
        assertEquals("ABC", GeoServerExtensions.getProperty("TEST_PROPERTY"));
    }

    @Test
    public void helperSingleton() {
        GeoServerExtensionsHelper.singleton("bean", this);
        assertSame(this, GeoServerExtensions.bean("bean"));
        assertSame(this, GeoServerExtensions.bean(GeoServerExtensionsHelperTest.class));

        GeoServerExtensionsHelper.clear();
        assertNull(GeoServerExtensions.bean("bean"));
        assertNull(GeoServerExtensions.bean(GeoServerExtensionsHelperTest.class));
    }  
    
    @Test
    public void helperFile(){
        File webxml = new File("web.xml"); // we are not touching the file so anywhere is fine
        
        GeoServerExtensionsHelper.file("WEB-INF/web.xml",  webxml );
        assertSame( webxml, GeoServerExtensions.file("WEB-INF/web.xml") );
        
    }
}
