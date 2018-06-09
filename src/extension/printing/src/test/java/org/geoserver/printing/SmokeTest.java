/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class SmokeTest extends GeoServerSystemTestSupport {

    @Test
    public void testServiceExists() throws Exception {
        // place the configuration file in the data dir (whicih is also used as the servlet context
        // lookup)
        GeoServerDataDirectory dd = getDataDirectory();
        try (InputStream is = getClass().getResourceAsStream("/test.yaml")) {
            FileUtils.copyInputStreamToFile(is, new File(dd.root(), "test.yaml"));
        }
        JSON json = getAsJSON("/pdf/info.json?app=test");

        assertTrue(json instanceof JSONObject);

        JSONObject obj = (JSONObject) json;
        assertTrue(obj.containsKey("scales"));
        assertTrue(obj.containsKey("dpis"));
        assertTrue(obj.containsKey("layouts"));
        assertTrue(obj.containsKey("printURL"));
        assertTrue(obj.containsKey("createURL"));

        json =
                json(
                        postAsServletResponse(
                                "/pdf/create.json",
                                "{\"app\":\"test\",\"units\":\"m\",\"srs\":\"EPSG:900913\",\"layout\":\"A4 portrait\",\"dpi\":75,\"outputFilename\":\"mapstore-print\",\"layers\":[],\"legends\":[],\"pages\":[{\"mapTitle\":\"\",\"center\":[1263949.7576605,5859225.6448425],\"scale\":200000,\"rotation\":0,\"comment\":\"\"}]}",
                                "application/json"));

        assertTrue(json instanceof JSONObject);

        obj = (JSONObject) json;
        assertTrue(obj.containsKey("getURL"));
    }
}
