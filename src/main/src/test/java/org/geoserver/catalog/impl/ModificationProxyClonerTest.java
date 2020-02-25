/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.jdbc.VirtualTable;
import org.junit.Test;

public class ModificationProxyClonerTest {

    @Test
    public void testCloneNull() throws Exception {
        Object copy = ModificationProxyCloner.clone(null);
        assertNull(copy);
    }

    @Test
    public void testCloneString() throws Exception {
        String source = new String("abc");
        String copy = ModificationProxyCloner.clone(source);
        assertSame(source, copy);
    }

    @Test
    public void testCloneDouble() throws Exception {
        Double source = Double.valueOf(12.56);
        Double copy = ModificationProxyCloner.clone(source);
        assertSame(source, copy);
    }

    @Test
    public void testCloneCloneable() throws Exception {
        TestCloneable source = new TestCloneable("test");
        TestCloneable copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
    }

    @Test
    public void testByCopyConstructor() throws Exception {
        VirtualTable source = new VirtualTable("test", "select * from tables");
        VirtualTable copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
    }

    @Test
    public void testNotCloneable() throws Exception {
        TestNotCloneable source = new TestNotCloneable("test");
        TestNotCloneable copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
    }

    @Test
    public void testDeepCopyMap() throws Exception {
        Map<String, Object> source = new HashMap<>();
        Map<String, String> subMap = new HashMap<>();
        subMap.put("a", "b");
        subMap.put("c", "d");
        source.put("submap", subMap);
        List<String> list = new ArrayList<>();
        list.add("x");
        list.add("y");
        list.add("z");
        source.put("list", list);
        Map<String, Object> copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
        assertNotSame(source.get("submap"), copy.get("submap"));
        assertEquals(source.get("submap"), copy.get("submap"));
        assertNotSame(source.get("list"), copy.get("list"));
        assertEquals(source.get("list"), copy.get("list"));
    }

    static class TestNotCloneable {

        private String myState;

        public TestNotCloneable(String myState) {
            this.myState = myState;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((myState == null) ? 0 : myState.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TestNotCloneable other = (TestNotCloneable) obj;
            if (myState == null) {
                if (other.myState != null) return false;
            } else if (!myState.equals(other.myState)) return false;
            return true;
        }
    }

    static class TestCloneable extends TestNotCloneable {

        public TestCloneable(String myState) {
            super(myState);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
