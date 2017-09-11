/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.easymock.EasyMock;
import org.geoserver.config.SystemInfo;
import org.geoserver.platform.ExtensionFilter;
import org.geoserver.platform.ExtensionProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.SystemPanelInfo;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Unit tests for {@link SystemStatusImpl}
 * 
 * @author sandr
 *
 */

public class SystemPanelInfoTest {

    @Test
    public void testSystemStatus() {

        SystemPanelInfo<SystemInfo> system = createMock ("testSystem", SystemPanelInfo.class);
        expect(system.getTitleKey()).andReturn("name").atLeastOnce();

        ApplicationContext appContext = createMock(ApplicationContext.class);
        expect(appContext.getBean("testStatus")).andStubReturn(system);
        expect(appContext.getBeanNamesForType(SystemPanelInfo.class))
                .andStubReturn(new String[] { "testStatus" });
        expect(appContext.isSingleton((String) anyObject())).andReturn(true).anyTimes();
        expect(appContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(appContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
        EasyMock.replay(system, appContext);

        GeoServerExtensions gse = new GeoServerExtensions();
        gse.setApplicationContext(appContext);
        List<SystemPanelInfo> list = GeoServerExtensions.extensions(SystemPanelInfo.class);
        assertEquals(1, list.size());
        assertEquals("name", list.get(0).getTitleKey());

        EasyMock.verify(system);

    }

}
