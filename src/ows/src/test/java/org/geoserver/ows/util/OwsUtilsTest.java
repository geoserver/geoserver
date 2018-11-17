/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class OwsUtilsTest extends TestCase {

    public void testSimple() throws Exception {
        Foo foo = new Foo();
        foo.setA("a");

        assertEquals("a", OwsUtils.get(foo, "a"));
        assertNull(OwsUtils.get(foo, "b"));

        OwsUtils.set(foo, "b", 5);
        assertEquals(5, OwsUtils.get(foo, "b"));

        assertEquals(0f, OwsUtils.get(foo, "c"));
        OwsUtils.set(foo, "c", 5f);
        assertEquals(5f, OwsUtils.get(foo, "c"));
    }

    public void testExtended() throws Exception {
        Bar bar = new Bar();
        assertNull(OwsUtils.get(bar, "foo"));
        assertNull(OwsUtils.get(bar, "foo.a"));

        Foo foo = new Foo();
        bar.setFoo(foo);
        assertEquals(foo, OwsUtils.get(bar, "foo"));
        assertNull(OwsUtils.get(bar, "foo.a"));

        foo.setA("abc");
        assertEquals("abc", OwsUtils.get(bar, "foo.a"));

        OwsUtils.set(bar, "foo.b", 123);
        assertEquals(123, OwsUtils.get(bar, "foo.b"));
    }

    public void testPut() throws Exception {
        Baz baz = new Baz();
        try {
            OwsUtils.put(baz, "map", "k", "v");
            fail("null map should cause exception");
        } catch (NullPointerException e) {
        }

        baz.map = new HashMap();
        try {
            OwsUtils.put(baz, "xyz", "k", "v");
            fail("bad property should cause exception");
        } catch (IllegalArgumentException e) {
        }

        assertTrue(baz.map.isEmpty());
        OwsUtils.put(baz, "map", "k", "v");
        assertEquals("v", baz.map.get("k"));
    }

    class Foo {
        String a;
        Integer b;
        float c;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public Integer getB() {
            return b;
        }

        public void setB(Integer b) {
            this.b = b;
        }

        public float getC() {
            return c;
        }

        public void setC(float c) {
            this.c = c;
        }
    }

    class Bar {
        Foo foo;
        Double d;

        public Foo getFoo() {
            return foo;
        }

        public void setFoo(Foo foo) {
            this.foo = foo;
        }

        public Double getD() {
            return d;
        }

        public void setD(Double d) {
            this.d = d;
        }
    }

    class Baz {
        Map map;

        public Map getMap() {
            return map;
        }
    }
}
