/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test suite for {@link DescribeLayerKvpRequestReader}
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerKvpRequestReaderTest {

    private GeoServerImpl geoServerImpl;

    private WMS wms;

    private Map<String, String> params;

    @Before
    public void setUp() throws Exception {
        geoServerImpl = new GeoServerImpl();
        geoServerImpl.add(new WMSInfoImpl());
        wms = new WMS(geoServerImpl);
        params = new HashMap<String, String>();
    }

    @After
    public void tearDown() throws Exception {
        wms = null;
        params = null;
    }

    private DescribeLayerRequest getRequest(Map<String, String> rawKvp) throws Exception {
        return getRequest(rawKvp, new HashMap<String, Object>(rawKvp));
    }

    private DescribeLayerRequest getRequest(Map<String, String> rawKvp, Map<String, Object> kvp)
            throws Exception {

        DescribeLayerKvpRequestReader reader = new DescribeLayerKvpRequestReader(wms);
        DescribeLayerRequest req = (DescribeLayerRequest) reader.createRequest();
        return (DescribeLayerRequest) reader.read(req, kvp, rawKvp);
    }

    @Test
    public void testGetRequestNoVersion() throws Exception {
        params.put("LAYERS", "topp:states");
        try {
            getRequest(params);
            fail("expected ServiceException if version is not provided");
        } catch (ServiceException e) {
            assertEquals("NoVersionInfo", e.getCode());
        }
    }

    @Test
    public void testGetRequestInvalidVersion() throws Exception {
        params.put("LAYERS", "topp:states");
        params.put("VERSION", "fakeVersion");
        try {
            getRequest(params);
            fail("expected ServiceException if the wrong version is requested");
        } catch (ServiceException e) {
            assertEquals("InvalidVersion", e.getCode());
        }
    }

    @Test
    public void testGetRequestNoLayerRequested() throws Exception {
        params.put("VERSION", "1.1.1");
        try {
            getRequest(params);
            fail("expected ServiceException if no layer is requested");
        } catch (ServiceException e) {
            assertEquals("NoLayerRequested", e.getCode());
        }
    }

    @Test
    public void testGetRequest() throws Exception {
        CatalogImpl catalog = new CatalogImpl();
        geoServerImpl.setCatalog(catalog);
        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setPrefix("topp");
        ns.setURI("http//www.geoserver.org");

        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("fakeWs");
        workspace.setName("fakeWs");

        DataStoreInfoImpl dataStoreInfo = new DataStoreInfoImpl(catalog);
        dataStoreInfo.setName("fakeDs");
        dataStoreInfo.setId("fakeDs");
        dataStoreInfo.setWorkspace(workspace);

        FeatureTypeInfoImpl featureTypeInfo = new FeatureTypeInfoImpl(catalog);
        featureTypeInfo.setNamespace(ns);
        featureTypeInfo.setName("states");
        featureTypeInfo.setStore(dataStoreInfo);

        final LayerInfoImpl layerInfo = new LayerInfoImpl();
        layerInfo.setResource(featureTypeInfo);
        layerInfo.setId("states");
        layerInfo.setName("states");

        catalog.add(ns);
        catalog.add(workspace);
        catalog.add(dataStoreInfo);
        catalog.add(featureTypeInfo);
        catalog.add(layerInfo);

        params.put("VERSION", "1.1.1");

        CoverageStoreInfoImpl coverageStoreInfo = new CoverageStoreInfoImpl(catalog);
        coverageStoreInfo.setId("coverageStore");
        coverageStoreInfo.setName("coverageStore");
        coverageStoreInfo.setWorkspace(workspace);

        CoverageInfoImpl coverageInfo = new CoverageInfoImpl(catalog);
        coverageInfo.setNamespace(ns);
        coverageInfo.setName("fakeCoverage");
        coverageInfo.setStore(coverageStoreInfo);

        LayerInfoImpl layerInfo2 = new LayerInfoImpl();
        layerInfo2.setResource(coverageInfo);
        layerInfo2.setId("fakeCoverage");
        layerInfo2.setName("fakeCoverage");

        catalog.add(coverageStoreInfo);
        catalog.add(coverageInfo);
        catalog.add(layerInfo2);

        params.put("LAYERS", "topp:states,topp:fakeCoverage");
        Map<String, Object> kvp = new HashMap<String, Object>(params);
        kvp.put("LAYERS", Arrays.asList(new MapLayerInfo(layerInfo), new MapLayerInfo(layerInfo2)));
        DescribeLayerRequest describeRequest = getRequest(params, kvp);
        assertNotNull(describeRequest);
        assertNotNull(describeRequest.getLayers());
        assertEquals(2, describeRequest.getLayers().size());
    }
}
