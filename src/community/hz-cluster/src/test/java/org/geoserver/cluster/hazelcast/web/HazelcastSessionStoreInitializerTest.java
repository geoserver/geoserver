/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.pageStore.InSessionPageStore;
import org.apache.wicket.settings.StoreSettings;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.geoserver.web.GeoServerApplication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class HazelcastSessionStoreInitializerTest {

    private HzCluster cluster;
    private HazelcastSessionStoreInitializer initializer;
    private GeoServerApplication application;

    @Before
    public void setUp() {
        cluster = mock(HzCluster.class);
        initializer = new HazelcastSessionStoreInitializer(cluster);
        application = mock(GeoServerApplication.class);
        when(application.getStoreSettings()).thenReturn(new StoreSettings(application));
    }

    @Test
    public void testInitEnabled() throws Exception {
        // Explicitly enable session sharing
        when(cluster.isEnabled()).thenReturn(true);
        when(cluster.isSessionSharing()).thenReturn(true);

        initializer.init(application);

        ArgumentCaptor<DefaultPageManagerProvider> captor = ArgumentCaptor.forClass(DefaultPageManagerProvider.class);
        verify(application).setPageManagerProvider(captor.capture());

        DefaultPageManagerProvider provider = captor.getValue();
        assertNotNull(provider);

        // Verify it's using the InSessionPageStore using reflection as the method is protected
        Method method = DefaultPageManagerProvider.class.getDeclaredMethod("newPersistentStore");
        method.setAccessible(true);
        IPageStore store = (IPageStore) method.invoke(provider);
        assertTrue(
                "Expected InSessionPageStore, but got: "
                        + (store == null ? "null" : store.getClass().getName()),
                store instanceof InSessionPageStore);
    }

    @Test
    public void testInitDisabled() {
        // Explicitly disable clustering
        when(cluster.isEnabled()).thenReturn(false);
        when(cluster.isSessionSharing()).thenReturn(true);

        initializer.init(application);

        verify(application, never()).setPageManagerProvider(any());
    }

    @Test
    public void testInitSessionSharingDisabled() {
        // Explicitly disable session sharing
        when(cluster.isEnabled()).thenReturn(true);
        when(cluster.isSessionSharing()).thenReturn(false);

        initializer.init(application);

        verify(application, never()).setPageManagerProvider(any());
    }
}
