/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;

import org.geoserver.ows.Dispatcher;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.junit.Test;

public class GetMapKvpRequestReaderTest extends KvpRequestReaderTestSupport {

    Dispatcher dispatcher;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        dispatcher = (Dispatcher) applicationContext.getBean("dispatcher");
    }

    /** One of the cite tests ensures that WMTVER is recognized as VERSION and the server does not complain */
    @Test
    public void testWmtVer() throws Exception {
        dispatcher.setCiteCompliant(true);
        String request =
                "wms?SERVICE=WMS&&WiDtH=200&FoRmAt=image/png&LaYeRs=cite:Lakes&StYlEs=&BbOx=0,-0.0020,0.0040,0&ReQuEsT=GetMap&HeIgHt=100&SrS=EPSG:4326&WmTvEr=1.1.1";
        assertEquals("image/png", getAsServletResponse(request).getContentType());
    }
}
