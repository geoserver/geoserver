/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class GeoServerPropertyOverrideConfigurerTest {

    ClassPathXmlApplicationContext ctx;
    
    @Before
    public void setUp() throws Exception {
        File f = new File("target/foo.properties");
        if (f.exists()) f.delete();
        
        Properties p = new Properties();
        p.put("myBean.bar", "${GEOSERVER_DATA_DIR}/foobar");
        p.put("myBean.baz", "barfoo");
        
        FileOutputStream out = new FileOutputStream("target/foo.properties");
        p.store(out, ""); 
        
        out.flush(); out.close();
        
        ctx = new ClassPathXmlApplicationContext(
            "GeoServerPropertyOverrideConfigurerTest-applicationContext.xml", getClass());
        ctx.refresh();
    }
    
    @After
    public void tearDown() throws Exception {
        ctx.destroy();
    }
 
    @Test 
    public void testUserSpecified() throws Exception {
        ctx.refresh();
        Foo f = (Foo) ctx.getBean("myBean");
        assertNotEquals("foobar", f.getBar());
        assertTrue(f.getBar().endsWith("/foobar"));
        assertEquals("barfoo", f.getBaz());
    }
    
    static class Foo {
        String bar;
        String baz;
        
        public String getBar() {
            return bar;
        }
        
        public void setBar(String bar) {
            this.bar = bar;
        }
        
        public void setBaz(String baz) {
            this.baz = baz;
        }
        
        public String getBaz() {
            return baz;
        }
        
    }
}
