/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.jayway.jsonpath.DocumentContext;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

public class MapsTest extends MapsTestSupport {
    @Test
    public void testDatetimeJson() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        Integer[] values = {1, 1, 1, 1, 0, 1};
        // work with different time resolutions
        String[] dates = {
            "2012", "2012-02", "2012-02-11", "2012-02-11T00:00:00Z", "2012-02-14T00:00:00.000Z", "2012-02-12T00:00:00Z"
        };
        for (int i = 0; i < 6; i++) {
            DocumentContext json = getAsJSONPath(
                    "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map/info?i=50&j=50&f=application%2Fjson&datetime="
                            + dates[i],
                    200);
            assertEquals(values[i], json.read("$.numberReturned", Integer.class));
        }
    }

    @Test
    public void testDatetimeHTMLMapsFormat() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        Document document = getAsJSoup(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=html&datetime=2012-02-12T00:00:00Z");
        assertEquals("2012-02-12T00:00:00Z", getParameterValue(document, "datetime"));
    }

    @Test
    public void testHTMLNoDatetime() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        // failed here when no datetime provided, FTL processing error, null on js_string
        Document document = getAsJSoup("ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=html");
        assertNull(getParameterValue(document, "datetime"));
    }

    private static String getParameterValue(Document document, String key) {
        Elements parameters = document.select("input[type='hidden'][title='" + key + "']");
        if (parameters.isEmpty()) return null;
        if (parameters.size() > 1) fail("Found more than one element with key " + key + ": " + parameters);
        Element parameter = parameters.first();
        return parameter.attr("value");
    }
}
