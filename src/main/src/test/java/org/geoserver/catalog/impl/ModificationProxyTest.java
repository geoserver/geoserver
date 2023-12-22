/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CatalogVisitorAdapter;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
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

    @Test
    public void testCatalogVisitorCalledWithProxyObject() {
        CatalogFactory factory = new CatalogImpl().getFactory();

        testCatalogVisitor(factory.createNamespace(), NamespaceInfo.class);
        testCatalogVisitor(factory.createWorkspace(), WorkspaceInfo.class);

        testCatalogVisitor(factory.createCoverageStore(), CoverageStoreInfo.class);
        testCatalogVisitor(factory.createDataStore(), DataStoreInfo.class);
        testCatalogVisitor(factory.createWebMapServer(), WMSStoreInfo.class);
        testCatalogVisitor(factory.createWebMapTileServer(), WMTSStoreInfo.class);

        testCatalogVisitor(factory.createCoverage(), CoverageInfo.class);
        testCatalogVisitor(factory.createFeatureType(), FeatureTypeInfo.class);
        testCatalogVisitor(factory.createWMSLayer(), WMSLayerInfo.class);
        testCatalogVisitor(factory.createWMTSLayer(), WMTSLayerInfo.class);

        testCatalogVisitor(factory.createLayer(), LayerInfo.class);
        testCatalogVisitor(factory.createLayerGroup(), LayerGroupInfo.class);
        testCatalogVisitor(factory.createStyle(), StyleInfo.class);
    }

    private <T extends CatalogInfo> void testCatalogVisitor(T info, Class<T> type) {
        T proxy = ModificationProxy.create(info, type);
        T visited = visitAndCapture(proxy, type);
        assertThat(visited, sameInstance(proxy));
    }

    private <T extends CatalogInfo> T visitAndCapture(T proxy, Class<T> type) {

        AtomicReference<CatalogInfo> captured = new AtomicReference<>();

        CatalogVisitor visitor =
                new CatalogVisitorAdapter() {
                    @Override
                    public void visit(WorkspaceInfo workspace) {
                        captured.set(workspace);
                    }

                    @Override
                    public void visit(NamespaceInfo namespace) {
                        captured.set(namespace);
                    }

                    @Override
                    public void visit(DataStoreInfo dataStore) {
                        captured.set(dataStore);
                    }

                    @Override
                    public void visit(CoverageStoreInfo coverageStore) {
                        captured.set(coverageStore);
                    }

                    @Override
                    public void visit(WMSStoreInfo wmsStore) {
                        captured.set(wmsStore);
                    }

                    @Override
                    public void visit(WMTSStoreInfo wmtsStore) {
                        captured.set(wmtsStore);
                    }

                    @Override
                    public void visit(FeatureTypeInfo featureType) {
                        captured.set(featureType);
                    }

                    @Override
                    public void visit(CoverageInfo coverage) {
                        captured.set(coverage);
                    }

                    @Override
                    public void visit(WMSLayerInfo wmsLayer) {
                        captured.set(wmsLayer);
                    }

                    @Override
                    public void visit(WMTSLayerInfo wmtsLayer) {
                        captured.set(wmtsLayer);
                    }

                    @Override
                    public void visit(LayerInfo layer) {
                        captured.set(layer);
                    }

                    @Override
                    public void visit(StyleInfo style) {
                        captured.set(style);
                    }

                    @Override
                    public void visit(LayerGroupInfo layerGroup) {
                        captured.set(layerGroup);
                    }
                };
        proxy.accept(visitor);
        return type.cast(captured.get());
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

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public List<String> getListValue() {
            return listValue;
        }

        @Override
        public void setListValue(List<String> listValue) {
            this.listValue = listValue;
        }
    }
}
