package org.geoserver.security.decorators;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.data.ows.HTTPClient;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wmts.WebMapTileServer;
import org.junit.Rule;
import org.junit.Test;

public class SecuredWebMapTileServerTest {

    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Test
    public void testCanSecure() throws IOException, ServiceException {
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

        WebMapTileServer wmts = new WebMapTileServer(serverURL, client, null);
        assertNotNull(wmts);

        wmts = new SecuredWebMapTileServer(wmts);
        assertNotNull(wmts);
    }
}
