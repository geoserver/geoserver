/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.junit.After;
import org.junit.Test;

public class TextDecorationTest {

    @After
    public void clearRequest() {
        Dispatcher.REQUEST.remove();
    }

    @Test
    public void testExpandRequestVariable() throws Exception {
        // setup environment
        Request request = new Request();
        Map kvp = new CaseInsensitiveMap(new HashMap());
        kvp.put("time", "2008-10-31T00:00:00.000Z");
        request.setRawKvp(kvp);
        Dispatcher.REQUEST.set(request);

        TextDecoration decoration = new TextDecoration();
        Map<String, String> options = new HashMap<>();
        options.put(
                "message",
                "<#setting datetime_format=\"yyyy-MM-dd'T'HH:mm:ss.SSSX\">\n"
                        + "<#setting locale=\"en_US\">\n"
                        + "<#setting time_zone=\"GMT\">"
                        + "<#if time??>\n"
                        + "${time?datetime?string[\"dd.MM.yyyy\"]}"
                        + "</#if>");
        decoration.loadOptions(options);

        GetMapRequest getMap = new GetMapRequest();
        WMSMapContent wmsMapContent = new WMSMapContent(getMap);
        String message = decoration.evaluateMessage(wmsMapContent);
        assertEquals("31.10.2008", message);
    }
}
