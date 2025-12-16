/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.outputformat;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.junit.Test;

/** Unit test suite for {@link WFSOutputFormatCallback} */
public class WFSOutputFormatCallbackTest {
    @Test(expected = ServiceException.class)
    public void testOperationDispatchedNoPermission() {
        WFSInfo wfs = createNiceMock(WFSInfo.class);
        expect(wfs.isGetFeatureOutputTypeCheckingEnabled()).andReturn(true).anyTimes();
        expect(wfs.getGetFeatureOutputTypes())
                .andReturn(Collections.singleton("text/xml; subtype=gml/2.0"))
                .anyTimes();
        replay(wfs);
        GeoServer geoServer = createNiceMock(GeoServer.class);
        expect(geoServer.getCatalog()).andReturn(null).anyTimes();
        expect(geoServer.getService(WFSInfo.class)).andReturn(wfs).anyTimes();
        replay(geoServer);
        WFSOutputFormatCallback wfsOutputFormatCallback = new WFSOutputFormatCallback(geoServer);
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        request.setOutputFormat("text/xml; subtype=gml/3.2");
        Service service = new Service("wfs", null, null, null);
        Operation operationIn = new Operation("GetFeature", service, null, null);
        wfsOutputFormatCallback.operationDispatched(request, operationIn);
    }

    @Test
    public void testOperationDispatchedWithPermission() {
        WFSInfo wfs = createNiceMock(WFSInfo.class);
        expect(wfs.isGetFeatureOutputTypeCheckingEnabled()).andReturn(true).anyTimes();
        expect(wfs.getGetFeatureOutputTypes())
                .andReturn(Collections.singleton("text/xml; subtype=gml/3.2"))
                .anyTimes();
        replay(wfs);
        GeoServer geoServer = createNiceMock(GeoServer.class);
        expect(geoServer.getCatalog()).andReturn(null).anyTimes();
        expect(geoServer.getService(WFSInfo.class)).andReturn(wfs).anyTimes();
        replay(geoServer);
        WFSOutputFormatCallback wfsOutputFormatCallback = new WFSOutputFormatCallback(geoServer);
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        request.setOutputFormat("text/xml; subtype=gml/3.2");
        Service service = new Service("wfs", null, null, null);
        Operation operationIn = new Operation("GetFeature", service, null, null);
        Operation operation = wfsOutputFormatCallback.operationDispatched(request, operationIn);
        assertEquals("GetFeature", operation.getId());
    }
}
