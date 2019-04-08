package org.geoserver.security.decorators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.data.ows.HTTPClient;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.map.WMTSCoverageReader;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.junit.Rule;
import org.junit.Test;

public class SecuredWMTSLayerTest {

    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    protected final WrapperPolicy policy =
            WrapperPolicy.readOnlyHide(new AccessLimits(CatalogMode.HIDE));

    // First layer in nasa.getcapa.xml
    protected static final String LAYER_TITLE = "Snow Water Equivalent (AMSR2, GCOM-W1)";

    @Test
    public void testCanSecure() {
        WMTSLayer layer = new WMTSLayer(LAYER_TITLE);
        layer = new SecuredWMTSLayer(layer, policy);
        assertNotNull(layer);
        assertEquals(LAYER_TITLE, layer.getTitle());
    }

    @Test
    public void testCoverageReader() throws IOException, ServiceException {
        String capabilitiesURL =
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        URL serverURL = new URL(capabilitiesURL);
        MockHttpClient mockClient = new MockHttpClient();
        mockClient.expectGet(
                serverURL,
                new MockHttpResponse(
                        WMTSStoreInfoImpl.class.getResource("nasa.getcapa.xml"), "text/xml"));
        TestHttpClientProvider.bind(mockClient, serverURL);
        HTTPClient client = TestHttpClientProvider.get(capabilitiesURL);

        WebMapTileServer wmts =
                new SecuredWebMapTileServer(new WebMapTileServer(serverURL, client, null));
        WMTSLayer layer =
                new SecuredWMTSLayer(
                        wmts.getCapabilities().getLayerList().iterator().next(), policy);

        WMTSCoverageReader coverageReader = new WMTSCoverageReader(wmts, layer);
    }
}
