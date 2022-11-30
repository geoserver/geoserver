/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;

import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.junit.Test;

public class HitsOutputFormatTest {

    @Test
    public void testMimeType() {
        HitsOutputFormat hitsOutputFormat = new HitsOutputFormat(null, null);

        // Operation and Service are final classes - difficult to mock
        Service servicewfs11 = new Service("wfs11", null, null, new Version("1.1.0"), null);
        Operation opwfs11 = new Operation("opwfs11", servicewfs11, null, null);

        String mime = hitsOutputFormat.getMimeType(null, opwfs11);
        assertEquals("text/xml; subtype=gml/3.1.1", mime);
    }
}
