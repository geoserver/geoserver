/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApplicationListenerImplTest {

    @Test
    public void buildLayerGroupCache() {
        ApplicationListenerImpl applicationListenerImpl = new ApplicationListenerImpl();
        ContextRefreshedEvent contextRefreshedEvent = mock(ContextRefreshedEvent.class);

        SecureCatalogImpl secureCatalog = mock(SecureCatalogImpl.class);
        ResourceAccessManager resourceAccessManager = mock(ResourceAccessManager.class);
        try (MockedStatic<GeoServerExtensions> mocked = mockStatic(GeoServerExtensions.class)) {
            mocked.when(() -> GeoServerExtensions.bean(ArgumentMatchers.<Class<Object>>any()))
                    .thenReturn(secureCatalog);
            when(secureCatalog.getResourceAccessManager()).thenReturn(resourceAccessManager);

            applicationListenerImpl.onApplicationEvent(contextRefreshedEvent);

            verify(resourceAccessManager, times(1)).buildLayerGroupCache();
        }
    }
}
