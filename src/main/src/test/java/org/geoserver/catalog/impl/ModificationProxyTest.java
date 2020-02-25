/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.impl;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

public class ModificationProxyTest {

    @Test
    public void testRewrapNoProxyIdentity() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");

        TestBean result = ModificationProxy.rewrap(bean, b -> b, TestBean.class);

        assertThat(result, sameInstance(bean));
    }

    @Test
    public void testRewrapNoProxyInnerChange() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");

        TestBean result = ModificationProxy.rewrap(bean, b -> newBean, TestBean.class);

        assertThat(result, sameInstance(newBean));
    }

    @Test
    public void testRewrapEmptyProxyIdentity() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        TestBean result = ModificationProxy.rewrap(proxy, b -> b, TestBean.class);
        assertThat(result, modProxy(sameInstance(bean)));

        assertThat(result.getValue(), equalTo("Mr. Bean"));
        assertThat(result.getListValue(), contains("Uhh", "Bean"));
    }

    @Test
    public void testRewrapChangedProxyIdentity() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        proxy.setValue("Edmond Blackadder");
        proxy.setListValue(Arrays.asList("Cunning", "Plan"));

        TestBean result = ModificationProxy.rewrap(proxy, b -> b, TestBean.class);

        // Should be a new wrapper
        assertThat(result, not(sameInstance(proxy)));
        // Wrapping the same object
        assertThat(result, modProxy(sameInstance(bean)));

        // With the same changes
        assertThat(result.getValue(), equalTo("Edmond Blackadder"));
        assertThat(result.getListValue(), contains("Cunning", "Plan"));

        // The changes should not have been committed
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
    }

    @Test
    public void testRewrapChangedProxyInnerChange() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        proxy.setValue("Edmond Blackadder");
        proxy.setListValue(Arrays.asList("Cunning", "Plan"));

        // Swap the old bean for the new one
        TestBean result = ModificationProxy.rewrap(proxy, b -> newBean, TestBean.class);

        // Should be a new wrapper
        assertThat(result, not(sameInstance(proxy)));
        // Wrapping the new object
        assertThat(result, modProxy(sameInstance(newBean)));

        // With the same changes
        assertThat(result.getValue(), equalTo("Edmond Blackadder"));
        assertThat(result.getListValue(), contains("Cunning", "Plan"));

        // The changes should not have been committed to either bean
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
        assertThat(newBean.getValue(), equalTo("Johnny English"));
        assertThat(newBean.getListValue(), contains("Not", "Bond"));
    }

    @Test
    public void testRewrapEmptyProxyInnerChange() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        // Swap the old bean for the new one
        TestBean result = ModificationProxy.rewrap(proxy, b -> newBean, TestBean.class);

        // Should be a new wrapper
        assertThat(result, not(sameInstance(proxy)));
        // Wrapping the new object
        assertThat(result, modProxy(sameInstance(newBean)));

        // Should show the properties of the new bean
        assertThat(result.getValue(), equalTo("Johnny English"));
        assertThat(result.getListValue(), contains("Not", "Bond"));

        // No changes should not have been committed to either bean
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
        assertThat(newBean.getValue(), equalTo("Johnny English"));
        assertThat(newBean.getListValue(), contains("Not", "Bond"));
    }

    @Test
    public void testRewrapCommitToNew() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        proxy.setValue("Edmond Blackadder");
        proxy.setListValue(Arrays.asList("Cunning", "Plan"));

        // Swap the old bean for the new one
        TestBean result = ModificationProxy.rewrap(proxy, b -> newBean, TestBean.class);

        // Commit the changes
        ModificationProxy.handler(result).commit();

        // The changes should not have been committed to either bean
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
        assertThat(newBean.getValue(), equalTo("Edmond Blackadder"));
        assertThat(newBean.getListValue(), contains("Cunning", "Plan"));
    }

    /** Matches a modification proxy wrapping an object matching the given matcher */
    public static <T> Matcher<T> modProxy(Matcher<T> objectMatcher) {
        return new BaseMatcher<T>() {

            @Override
            public boolean matches(Object item) {
                ModificationProxy handler = ModificationProxy.handler(item);
                if (handler == null) {
                    return false;
                }
                return objectMatcher.matches(handler.getProxyObject());
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("ModificationProxy wrapping ")
                        .appendDescriptionOf(objectMatcher);
            }
        };
    }

    static interface TestBean {
        public String getValue();

        public void setValue(String value);

        public List<String> getListValue();

        public void setListValue(List<String> listValue);
    }

    static class TestBeanImpl implements TestBean {
        String value;
        List<String> listValue;

        public TestBeanImpl(String value, List<String> listValue) {
            super();
            this.value = value;
            this.listValue = new ArrayList<>(listValue);
        }

        public TestBeanImpl(String value, String... listValues) {
            this(value, Arrays.asList(listValues));
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public List<String> getListValue() {
            return listValue;
        }

        public void setListValue(List<String> listValue) {
            this.listValue = listValue;
        }
    }
}
