/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WMS1_0_0;
import org.geotools.ows.wms.request.GetMapRequest;
import org.junit.Test;

public class SecuredGetMapRequestTest extends SecureObjectsTest {

    /**
     * Test for GEOS-6362: getFinalURL had side effects and gave different results if called
     * multiple times.
     */
    @Test
    public void testNoSideEffectsOnGetFinalUrl() throws Exception {
        GetMapRequest request = new WMS1_0_0().createGetMapRequest(new URL("http://test?"));
        SecuredGetMapRequest securedRequest = new SecuredGetMapRequest(request);
        Layer wmsLayer = new Layer();
        wmsLayer.setName("layer1");
        Layer layer = new SecuredWMSLayer(wmsLayer, WrapperPolicy.hide(null));
        securedRequest.addLayer(layer);
        String firstCallURL = securedRequest.getFinalURL().toExternalForm();
        String secondCallURL = securedRequest.getFinalURL().toExternalForm();
        assertEquals(firstCallURL, secondCallURL);
    }
}
