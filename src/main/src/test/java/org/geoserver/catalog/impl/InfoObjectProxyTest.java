/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class InfoObjectProxyTest {

    @Test
    public void test() throws Exception {
        BeanImpl bean = new BeanImpl();
        ModificationProxy handler = new ModificationProxy(bean);

        Bean proxy =
                (Bean)
                        Proxy.newProxyInstance(
                                Bean.class.getClassLoader(), new Class[] {Bean.class}, handler);

        bean.setFoo("one");
        bean.setBar(1);

        proxy.setFoo("two");
        proxy.setBar(2);

        proxy.getScratch().add("x");
        proxy.getScratch().add("y");

        assertEquals("one", bean.getFoo());
        assertEquals(Integer.valueOf(1), bean.getBar());
        assertTrue(bean.getScratch().isEmpty());

        assertEquals("two", proxy.getFoo());
        assertEquals(Integer.valueOf(2), proxy.getBar());
        assertEquals(2, proxy.getScratch().size());

        handler.commit();
        assertEquals("two", bean.getFoo());
        assertEquals(Integer.valueOf(2), bean.getBar());
        assertEquals(2, bean.getScratch().size());
    }

    static interface Bean {

        String getFoo();

        void setFoo(String foo);

        Integer getBar();

        void setBar(Integer bar);

        List getScratch();
    }

    static class BeanImpl implements Bean {

        String foo;
        Integer bar;
        List scratch = new ArrayList();

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public Integer getBar() {
            return bar;
        }

        public void setBar(Integer bar) {
            this.bar = bar;
        }

        public List getScratch() {
            return scratch;
        }
    }
}
