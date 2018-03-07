/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.sf.json.JSON;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class ApiTest extends WFS3TestSupport {

    @Test
    public void testApiJson() throws Exception {
        JSON json = getAsJSON("wfs3/api");
        print(json);
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("wfs3/api?f=application/x-yaml");
        System.out.println(yaml);
    }
}
