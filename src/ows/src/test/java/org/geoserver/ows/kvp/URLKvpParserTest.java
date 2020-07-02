/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class URLKvpParserTest {
    URLKvpParser parser;

    @Before
    public void setUp() {
        parser = new URLKvpParser("url");
    }

    @Test
    public void testValidUrl() {
        try {
            String validUrl =
                    "http://localhost:8080/geoserver/rest/sldservice/topp:states/classify.xml?attribute=PERSONS&intervals=8&method=quantile&ramp=custom&colors=%23000000%2C%23240000%2C%23490000%2C%236d0000%2C%23920000%2C%23b60000%2C%23db0000%2C%23ff0000&open=false&strokeWeight=-1&strokeColor=%23ff0000&customClasses=1.1011%2C22.518%2C%23000000%3B22.518%2C48.445%2C%23240000%3B48.445%2C81.193%2C%23490000%3B81.193%2C118.905%2C%236D0000%3B118.905%2C168.246%2C%23920000%3B168.246%2C232.83%2C%23B60000%3B232.83%2C338.04%2C%23DB0000%3B338.04%2C16597.660000000003%2C%23FF0000&fullSLD=true";
            URL url = (URL) parser.parse(validUrl);
            assertEquals(
                    "http://localhost:8080/geoserver/rest/sldservice/topp:states/classify.xml?attribute=PERSONS&intervals=8&method=quantile&ramp=custom&colors=%23000000%2C%23240000%2C%23490000%2C%236d0000%2C%23920000%2C%23b60000%2C%23db0000%2C%23ff0000&open=false&strokeWeight=-1&strokeColor=%23ff0000&customClasses=1.1011%2C22.518%2C%23000000%3B22.518%2C48.445%2C%23240000%3B48.445%2C81.193%2C%23490000%3B81.193%2C118.905%2C%236D0000%3B118.905%2C168.246%2C%23920000%3B168.246%2C232.83%2C%23B60000%3B232.83%2C338.04%2C%23DB0000%3B338.04%2C16597.660000000003%2C%23FF0000&fullSLD=true",
                    url.toExternalForm());
            assertNotEquals(URLKvpParser.fixURL(validUrl), url.toExternalForm());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testLegacyBehaviourUrl() {
        try {
            System.setProperty("org.geoserver.kvp.urlfix", "true");
            URLKvpParser legacyParser = new URLKvpParser("url");
            String validUrl =
                    "http://localhost:8080/geoserver/rest/sldservice/topp:states/classify.xml?attribute=PERSONS&intervals=8&method=quantile&ramp=custom&colors=%23000000%2C%23240000%2C%23490000%2C%236d0000%2C%23920000%2C%23b60000%2C%23db0000%2C%23ff0000&open=false&strokeWeight=-1&strokeColor=%23ff0000&customClasses=1.1011%2C22.518%2C%23000000%3B22.518%2C48.445%2C%23240000%3B48.445%2C81.193%2C%23490000%3B81.193%2C118.905%2C%236D0000%3B118.905%2C168.246%2C%23920000%3B168.246%2C232.83%2C%23B60000%3B232.83%2C338.04%2C%23DB0000%3B338.04%2C16597.660000000003%2C%23FF0000&fullSLD=true";
            URL legacyUrl = (URL) legacyParser.parse(validUrl);
            assertEquals(legacyUrl.toExternalForm(), URLKvpParser.fixURL(validUrl));
        } catch (Exception e) {
            fail();
        } finally {
            System.setProperty("org.geoserver.kvp.urlfix", "false");
        }
    }
}
