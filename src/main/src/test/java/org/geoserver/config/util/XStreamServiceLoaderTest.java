/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XStreamServiceLoaderTest {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testXstreamPersisterReusedIfCalledWithSameGeoServerInstance() throws Exception {
        GeoServerResourceLoader rl = new GeoServerResourceLoader(folder.getRoot());

        XStreamServiceLoader<ServiceInfo> loader =
                new XStreamServiceLoader<ServiceInfo>(rl, "test") {

                    @Override
                    public Class<ServiceInfo> getServiceClass() {
                        return ServiceInfo.class;
                    }

                    @Override
                    protected ServiceInfo createServiceFromScratch(GeoServer gs) {
                        return new ServiceInfoImpl();
                    }
                };
        loader = spy(loader);

        GeoServerImpl gs1 = new GeoServerImpl();
        GeoServerImpl gs2 = new GeoServerImpl();

        ServiceInfo service = loader.createServiceFromScratch(gs1);
        Resource datadirRoot = rl.get("");

        // initXStreamPersister first called on save for gs1
        loader.save(service, gs1, datadirRoot);
        verify(loader, times(1)).initXStreamPersister(any(XStreamPersister.class), same(gs1));

        // then called again when getting gs2 as argument, but only once
        loader.load(gs2);
        loader.load(gs2);
        verify(loader, times(1)).initXStreamPersister(any(XStreamPersister.class), same(gs2));

        // and called only once again when getting gs1 as argument
        loader.load(gs1);
        loader.load(gs1);
        verify(loader, times(2)).initXStreamPersister(any(XStreamPersister.class), same(gs1));
    }
}
