/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.monitor.MemoryMonitorDAO;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.MonitorTestData;
import org.geoserver.monitor.RequestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.mock.web.MockHttpServletResponse;

public class RESTMonitorCallbackTest extends GeoServerSystemTestSupport {

    static Monitor monitor;

    RESTMonitorCallback callback;
    RequestData data;
    static Catalog catalog;

    public static Filter parseFilter(String cql) {
        try {
            return CQL.toFilter(cql);
        } catch (CQLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @BeforeClass
    public static void setUpData() throws Exception {
        MonitorDAO dao = new MemoryMonitorDAO();
        new MonitorTestData(dao).setup();

        MonitorConfig mc =
                new MonitorConfig() {

                    @Override
                    public MonitorDAO createDAO() {
                        MonitorDAO dao = new MemoryMonitorDAO();
                        try {
                            new MonitorTestData(dao).setup();
                            return dao;
                        } catch (java.text.ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public BboxMode getBboxMode() {
                        return BboxMode.FULL;
                    }
                };

        GeoServer gs = createMock(GeoServer.class);
        monitor = new Monitor(mc);
        monitor.setServer(gs);

        catalog = new CatalogImpl();

        expect(gs.getCatalog()).andStubReturn(catalog);
        replay(gs);

        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        FeatureTypeInfo ftFoo = catalog.getFactory().createFeatureType();
        ftFoo.setName("foo");
        ftFoo.setSRS("EPSG:4326");
        ftFoo.setNamespace(ns);
        ftFoo.setStore(ds);
        catalog.add(ftFoo);
        FeatureTypeInfo ftBar = catalog.getFactory().createFeatureType();
        ftBar.setName("bar");
        ftBar.setSRS("EPSG:3348");
        ftBar.setNamespace(ns);
        ftBar.setStore(ds);
        catalog.add(ftBar);
    }

    @Before
    public void setUp() throws Exception {
        callback = new RESTMonitorCallback(monitor);
        data = monitor.start();
    }

    @After
    public void tearDown() throws Exception {
        monitor.complete();
    }

    @Test
    public void testURLEncodedRequestPathInfo() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/layers/foo");
        assertEquals(404, response.getStatus());

        assertEquals("foo", data.getResources().get(1));

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layers/acme:foo");
        assertEquals(404, response.getStatus());

        assertEquals("acme:foo", data.getResources().get(2));

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "acme:foo");
        assertEquals(404, response.getStatus());

        assertEquals("acme:foo", data.getResources().get(3));
    }
}
