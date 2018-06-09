package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import org.geotools.jdbc.VirtualTable;
import org.junit.Test;

public class ModificationProxyClonerTest {

    @Test
    public void testCloneNull() {
        Object copy = ModificationProxyCloner.clone(null);
        assertNull(copy);
    }

    @Test
    public void testCloneString() {
        String source = new String("abc");
        String copy = ModificationProxyCloner.clone(source);
        assertSame(source, copy);
    }

    @Test
    public void testCloneDouble() {
        Double source = new Double(12.56);
        Double copy = ModificationProxyCloner.clone(source);
        assertSame(source, copy);
    }

    @Test
    public void testCloneCloneable() {
        TestCloneable source = new TestCloneable("test");
        TestCloneable copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
    }

    @Test
    public void testByCopyConstructor() {
        VirtualTable source = new VirtualTable("test", "select * from tables");
        VirtualTable copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
    }

    @Test
    public void testNotCloneable() {
        TestNotCloneable source = new TestNotCloneable("test");
        TestNotCloneable copy = ModificationProxyCloner.clone(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
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
