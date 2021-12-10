/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.WMSMockData.DummyRasterMapProducer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.function.EnvFunction;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.map.WMTSMapLayer;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Unit test for {@link GetMap}
 *
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.5.x
 */
public class GetMapTest {

    private WMSMockData mockData;

    private GetMapRequest request;

    private GetMap getMapOp;

    @Before
    public void setUp() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        request = mockData.createRequest();
        // add a layer so its a valid request
        MapLayerInfo layer = mockData.addFeatureTypeLayer("testType", Point.class);
        request.setLayers(Arrays.asList(layer));

        getMapOp = new GetMap(mockData.getWMS());
    }

    @Test
    public void testExecuteNoExtent() {
        request.setBbox(null);
        assertInvalidMandatoryParam("MissingBBox");
    }

    @Test
    public void testExecuteEmptyExtent() {
        request.setBbox(new Envelope());
        assertInvalidMandatoryParam("InvalidBBox");
    }

    @Test
    public void testSingleVectorLayer() throws IOException {
        request.setFormat(DummyRasterMapProducer.MIME_TYPE);

        MapLayerInfo layer = mockData.addFeatureTypeLayer("testSingleVectorLayer", Point.class);
        request.setLayers(Arrays.asList(layer));

        final DummyRasterMapProducer producer = new DummyRasterMapProducer();
        final WMS wms =
                new WMS(mockData.getGeoServer()) {
                    @Override
                    public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                        if (DummyRasterMapProducer.MIME_TYPE.equals(mimeType)) {
                            return producer;
                        }
                        return null;
                    }
                };
        getMapOp = new GetMap(wms);
        getMapOp.run(request);
        assertTrue(producer.produceMapCalled);
    }

    @Test
    public void testExecuteNoLayers() throws Exception {
        request.setLayers(null);
        assertInvalidMandatoryParam("LayerNotDefined");
    }

    @Test
    public void testExecuteNoWidth() {
        request.setWidth(0);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");

        request.setWidth(-1);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");
    }

    @Test
    public void testExecuteNoHeight() {
        request.setHeight(0);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");

        request.setHeight(-1);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");
    }

    @Test
    public void testExecuteInvalidFormat() {
        request.setFormat("non-existent-output-format");
        assertInvalidMandatoryParam("InvalidFormat");
    }

    @Test
    public void testExecuteNoFormat() {
        request.setFormat(null);
        assertInvalidMandatoryParam("InvalidFormat");
    }

    @Test
    public void testExecuteNoStyles() {
        request.setStyles(null);
        assertInvalidMandatoryParam("StyleNotDefined");
    }

    @Test
    public void testEnviroment() {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        EnvFunction.setLocalValues(Collections.singletonMap("myParam", 23));

        final DummyRasterMapProducer producer =
                new DummyRasterMapProducer() {
                    @Override
                    public WebMap produceMap(WMSMapContent ctx)
                            throws ServiceException, IOException {
                        assertEquals(23, ff.function("env", ff.literal("myParam")).evaluate(null));
                        assertEquals(
                                10,
                                ff.function("env", ff.literal("otherParam"), ff.literal(10))
                                        .evaluate(null));
                        super.produceMapCalled = true;
                        return null;
                    }
                };
        final WMS wms =
                new WMS(mockData.getGeoServer()) {
                    @Override
                    public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                        if (DummyRasterMapProducer.MIME_TYPE.equals(mimeType)) {
                            return producer;
                        }
                        return null;
                    }
                };

        getMapOp = new GetMap(wms);
        getMapOp.run(request);
        assertTrue(producer.produceMapCalled);
        // there used to be a test that the values are reset right after
        // GetMap, but this is wrong, the producer can be streaming and thus
        // the env variable must stay until the full request lifecycle is done,
        // we now use a DispatcherCallback to clean up the env variables:
        // EnvVariableCleaner
    }

    private void assertInvalidMandatoryParam(String expectedExceptionCode) {
        try {
            getMapOp.run(request);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(expectedExceptionCode, e.getCode());
        }
    }

    @Test
    public void addWMTSLayerIsAddingNativeCRSAsSource() throws IOException, FactoryException {
        Request request = new Request();
        request.setRawKvp(new HashMap<>());
        Dispatcher.REQUEST.set(request);
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();
        Catalog catalog = mockData.getGeoServer().getCatalog();
        URL descURL = new URL(baseURL + "/wmts?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        client.expectGet(
                descURL,
                new MockHttpResponse(getClass().getResource("wmts_getCaps.xml"), "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        WMTSStoreInfo storeInfo = new MockWMTSStoreInfo(catalog);
        storeInfo.setName("Another Mock WMTS Store");
        storeInfo.setCapabilitiesURL(descURL.toString());
        storeInfo.setConnectTimeout(60);
        storeInfo.setMaxConnections(10);
        storeInfo.setDateCreated(new Date());
        storeInfo.setDateModified(new Date());
        catalog.add(storeInfo);
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        WMTSLayerInfo wmtsInfo =
                xp.load(getClass().getResourceAsStream("wmtsLayerInfo.xml"), WMTSLayerInfo.class);
        wmtsInfo.setStore(storeInfo);
        catalog.add(wmtsInfo);
        LayerInfo layerInfo =
                xp.load(getClass().getResourceAsStream("wmtsLayer.xml"), LayerInfo.class);
        layerInfo.setResource(wmtsInfo);
        GetMap op = new GetMap(mockData.getWMS());
        WMSMapContent mapContent = new WMSMapContent();
        MapLayerInfo mli = new MapLayerInfo(layerInfo);
        op.addWMTSLayer(mapContent, mli);
        WMTSMapLayer layer = (WMTSMapLayer) mapContent.layers().get(0);
        SimpleFeature sf = layer.toFeatureCollection().features().next();
        GeneralParameterValue[] params =
                new AttributeExpressionImpl("params").evaluate(sf, GeneralParameterValue[].class);
        CoordinateReferenceSystem crs =
                (CoordinateReferenceSystem) ((ParameterValue) params[0]).getValue();
        assertEquals(wmtsInfo.getNativeCRS(), crs);
    }

    class MockWMTSStoreInfo extends WMTSStoreInfoImpl {

        MockWMTSStoreInfo(Catalog catalog) {
            super(catalog);
        }

        @Override
        public WebMapTileServer getWebMapTileServer(ProgressListener listener) throws IOException {
            try {
                return new WebMapTileServer(
                        getClass().getResource("wmts_getCaps.xml").toURI().toURL());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
