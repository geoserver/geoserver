/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.geoserver.jdbcconfig.JDBCConfigTestSupport.*;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerFactoryImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.jdbcconfig.config.JDBCGeoServerFacade;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.wms.WMSXStreamLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.web.context.WebApplicationContext;

@RunWith(Parameterized.class)
public class JDBCGeoServerLoaderTest {

    JDBCConfigTestSupport testSupport;

    public JDBCGeoServerLoaderTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        testSupport =
                new JDBCConfigTestSupport(dbConfig) {
                    @Override
                    protected void configureAppContext(WebApplicationContext appContext) {
                        expect(appContext.getBeanNamesForType(XStreamServiceLoader.class))
                                .andReturn(new String[] {"wmsLoader"})
                                .anyTimes();
                        expect(appContext.getBeanNamesForType((Class) anyObject()))
                                .andReturn(new String[] {})
                                .anyTimes();
                        expect(appContext.getBean("wmsLoader"))
                                .andReturn(new WMSXStreamLoader(getResourceLoader()))
                                .anyTimes();
                    }
                };
    }

    @Parameters(name = "JDBCGeoServerLoaderTest-{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    @Before
    public void setUp() throws Exception {
        testSupport.setUp();
    }

    @After
    public void tearDown() throws Exception {
        testSupport.tearDown();
    }

    @Test
    public void testLoadEmptyNoImport() throws Exception {
        JDBCConfigProperties config = createNiceMock(JDBCConfigProperties.class);
        expect(config.isEnabled()).andReturn(true).anyTimes();
        expect(config.isInitDb()).andReturn(true).anyTimes();
        expect(config.isImport()).andReturn(false).anyTimes();
        replay(config);

        JDBCGeoServerLoader loader =
                new JDBCGeoServerLoader(testSupport.getResourceLoader(), config);
        loader.setGeoServerFacade(new JDBCGeoServerFacade(testSupport.getDatabase()));
        loader.setApplicationContext(testSupport.getApplicationContext());

        // create a mock and ensure a global, logging, and service config are set
        GeoServerImpl geoServer = createNiceMock(GeoServerImpl.class);
        expect(geoServer.getFactory()).andReturn(new GeoServerFactoryImpl(geoServer)).anyTimes();

        geoServer.setGlobal((GeoServerInfo) anyObject());
        expectLastCall().once();

        geoServer.setLogging((LoggingInfo) anyObject());
        expectLastCall().once();

        geoServer.add((ServiceInfo) anyObject());
        expectLastCall().once();

        replay(geoServer);
        loader.postProcessBeforeInitialization(geoServer, "geoServer");

        verify(geoServer);
    }
}
