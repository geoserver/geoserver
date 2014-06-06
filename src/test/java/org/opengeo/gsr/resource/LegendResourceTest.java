/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.resource;

import static org.junit.Assert.fail;

import org.junit.Ignore;

public class LegendResourceTest extends ResourceTest {
    @Ignore
    public void testStreamsLegend() throws Exception {
        String result = getAsString(baseURL + "cite/MapServer/legend?f=json");
        fail(result);
    }
}
