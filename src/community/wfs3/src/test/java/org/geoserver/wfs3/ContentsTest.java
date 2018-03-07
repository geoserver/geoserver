/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.sf.json.JSON;
import org.junit.Test;

public class ContentsTest extends WFS3TestSupport {

    @Test
    public void testContentsJson() throws Exception {
        JSON json = getAsJSON("wfs3/");
        print(json);
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("wfs3/?f=application/x-yaml");
        System.out.println(yaml);
    }
}
