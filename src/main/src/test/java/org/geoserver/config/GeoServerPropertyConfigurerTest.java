/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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

public class GeoServerPropertyConfigurerTest {

    ClassPathXmlApplicationContext ctx;

    @Before
    public void setUp() throws Exception {
        File f = new File("target/foo.properties");
        if (f.exists()) f.delete();

        ctx =
                new ClassPathXmlApplicationContext(
                        "GeoServerPropertyConfigurerTest-applicationContext.xml", getClass());
        ctx.refresh();
    }

    @After
    public void tearDown() throws Exception {
        ctx.close();
    }

    @Test
    public void testDefaults() {

        Foo f = (Foo) ctx.getBean("myBean");
        assertEquals("value1", f.getBar());
        assertEquals("value2", f.getBaz());
    }

    @Test
    public void testUserSpecified() throws Exception {
        Properties p = new Properties();
        p.put("prop1", "foobar");
        p.put("prop2", "barfoo");

        FileOutputStream out = new FileOutputStream("target/foo.properties");
        p.store(out, "");

        out.flush();
        out.close();

        ctx.refresh();
        Foo f = (Foo) ctx.getBean("myBean");
        assertEquals("foobar", f.getBar());
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
