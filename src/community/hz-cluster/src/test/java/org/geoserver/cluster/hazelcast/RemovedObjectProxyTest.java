/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.junit.Test;

public class RemovedObjectProxyTest {

    @Test
    public void testVisitsCatalogInfoSubclasses() throws Exception {

        // Find all visit methods on CatalogVisitor that take a single parameter that's a subclass
        // of CatalogInfo
        for (Method method : CatalogVisitor.class.getMethods()) {
            if (!method.getName().equals("visit")) continue;
            if (method.getParameterTypes().length != 1) continue;
            Class<?> clazz = method.getParameterTypes()[0];
            if (CatalogInfo.class.isAssignableFrom(method.getParameterTypes()[0])) {

                @SuppressWarnings("unchecked")
                InvocationHandler handler =
                        new RemovedObjectProxy(
                                "Test", "Test", (Class<? extends CatalogInfo>) clazz);
                CatalogInfo info =
                        (CatalogInfo)
                                Proxy.newProxyInstance(
                                        clazz.getClassLoader(), new Class[] {clazz}, handler);

                // Check that the method is called by accept when the proxy is mimicking the
                // appropriate type
                CatalogVisitor visitor = createMock(CatalogVisitor.class);
                System.err.println(method);
                method.invoke(visitor, same(info));
                expectLastCall();

                replay(visitor);
                info.accept(visitor);
                verify(visitor);
            }
        }
    }

    @Test
    public void testReturnsCollaborators() throws Exception {
        DataStoreInfo ds = createMock(DataStoreInfo.class);
        replay(ds);

        RemovedObjectProxy handler =
                new RemovedObjectProxy(
                        "Test", "Test", (Class<? extends CatalogInfo>) FeatureTypeInfo.class);

        handler.addCatalogCollaborator("store", ds);

        FeatureTypeInfo info =
                (FeatureTypeInfo)
                        Proxy.newProxyInstance(
                                FeatureTypeInfo.class.getClassLoader(),
                                new Class[] {FeatureTypeInfo.class},
                                handler);

        // Added collaborator is returned by appropriate accessor
        assertThat(info.getStore(), sameInstance(ds));

        verify(ds);
    }
}
