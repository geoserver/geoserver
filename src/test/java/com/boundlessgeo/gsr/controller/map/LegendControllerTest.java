/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.map;

import static org.junit.Assert.fail;

import com.boundlessgeo.gsr.controller.ControllerTest;
import org.junit.Ignore;

public class LegendControllerTest extends ControllerTest {
    @Ignore
    public void testStreamsLegend() throws Exception {
        String result = getAsString(baseURL + "cite/MapServer/legend?f=json");
        fail(result);
    }
}
