/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

public class MapsTest extends MapsTestSupport {
    @Test
    public void testDatetimeJson() throws Exception {
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        Integer[] values = {1, 1, 1, 1, 0, 1};
        // work with different time resolutions
        String[] dates = {
            "2012",
            "2012-02",
            "2012-02-11",
            "2012-02-11T00:00:00Z",
            "2012-02-14T00:00:00.000Z",
            "2012-02-12T00:00:00Z"
        };
        for (int i = 0; i < 6; i++) {
            DocumentContext json =
                    getAsJSONPath(
                            "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map/info?i=50&j=50&f=application%2Fjson&datetime="
                                    + dates[i],
                            200);
            assertEquals(values[i], json.read("$.numberReturned", Integer.class));
        }
    }

    @Test
    public void testDatetimeHTMLMapsFormat() throws Exception {
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        Document document =
                getAsJSoup(
                        "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=html&datetime=2012-02-12T00:00:00Z");
        boolean found = searchParameter(document, "\"datetime\": '2012-02-12T00:00:00Z'");
        assertTrue(found);
    }

    @Test
    public void testHTMLNoDatetime() throws Exception {
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        // failed here when no datetime provided, FTL processing error, null on js_string
        Document document =
                getAsJSoup("ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=html");
        boolean found = searchParameter(document, "\"datetime\": '2012-02-12T00:00:00Z'");
        assertFalse(found);
    }

    private static boolean searchParameter(Document document, String keyValue) {
        Elements scriptsOnPage = document.select("script");
        Matcher matcher = null;
        // check that the datetime is in the javascript parameters
        String keyToFind = "datetime";
        Pattern pattern = Pattern.compile("\"" + keyToFind + "\":\\s*'(.*?)'");
        boolean found = false;
        for (Element element : scriptsOnPage) {
            for (DataNode node : element.dataNodes()) {
                matcher = pattern.matcher(node.getWholeData());
                while (matcher.find()) {
                    if (matcher.group().equals(keyValue)) {
                        found = true;
                    }
                }
            }
        }
        return found;
    }
}
