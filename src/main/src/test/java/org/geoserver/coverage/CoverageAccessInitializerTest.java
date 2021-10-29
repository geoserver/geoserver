/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.junit.Test;

public class CoverageAccessInitializerTest {

    @Test
    public void testChangePoolSize() throws Exception {
        // start point
        CoverageAccessInfoImpl caInfo = new CoverageAccessInfoImpl();
        caInfo.setCorePoolSize(5);
        caInfo.setMaxPoolSize(5);
        GeoServerInfoImpl gsInfo = new GeoServerInfoImpl();
        gsInfo.setCoverageAccess(caInfo);

        GeoServer gs = EasyMock.createNiceMock(GeoServer.class);
        Catalog catalog = EasyMock.createNiceMock(Catalog.class);
        ResourcePool rp = ResourcePool.create(catalog);
        expect(gs.getCatalog()).andReturn(catalog).anyTimes();
        expect(gs.getGlobal()).andReturn(gsInfo).anyTimes();
        expect(catalog.getResourcePool()).andReturn(rp).anyTimes();
        replay(gs);
        replay(catalog);

        CoverageAccessInitializer initializer = new CoverageAccessInitializer();
        initializer.initialize(gs);
        assertEquals(5, rp.getCoverageExecutor().getCorePoolSize());
        assertEquals(5, rp.getCoverageExecutor().getMaximumPoolSize());

        // going up
        caInfo.setCorePoolSize(32);
        caInfo.setMaxPoolSize(32);
        initializer.initialize(gs);
        assertEquals(32, rp.getCoverageExecutor().getCorePoolSize());
        assertEquals(32, rp.getCoverageExecutor().getMaximumPoolSize());

        // and back down
        caInfo.setCorePoolSize(5);
        caInfo.setMaxPoolSize(5);
        initializer.initialize(gs);
        assertEquals(5, rp.getCoverageExecutor().getCorePoolSize());
        assertEquals(5, rp.getCoverageExecutor().getMaximumPoolSize());
    }
}
