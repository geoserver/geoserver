/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 *           (c) 2002-2011 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * A modified version of ScaleRatioDecorationTest from GeoTools (LGPL).
 */
package org.geoserver.wms.decoration;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ScaleRatioDecorationTest extends DecorationTestSupport {

    @Test
    public void testRatio() throws Exception {
        ScaleRatioDecoration d = new ScaleRatioDecoration();

        // 90 is DPI default value
        assertEquals(3975, d.getScale(createMapContent(-1)), 1);
        assertEquals(3975, d.getScale(createMapContent(25.4 / 0.28)), 1);

        assertEquals(13147, d.getScale(createMapContent(300)), 1);
        assertEquals(26295, d.getScale(createMapContent(600)), 1);
        assertEquals(78887, d.getScale(createMapContent(1800)), 1);
    }

    @Test
    public void testDefaultFormat() throws Exception {
        ScaleRatioDecoration d = new ScaleRatioDecoration();
        assertEquals("1 : 13148", d.getScaleText(createMapContent(300)));
    }

    @Test
    public void testCustomFormat() throws Exception {
        ScaleRatioDecoration d = new ScaleRatioDecoration();
        Map<String, String> options = new HashMap<>();
        options.put("format", "#,###");
        options.put("formatLanguage", "en");
        d.loadOptions(options);
        assertEquals("1 : 13,148", d.getScaleText(createMapContent(300)));

        options.put("formatLanguage", "it");
        d.loadOptions(options);
        assertEquals("1 : 13.148", d.getScaleText(createMapContent(300)));
    }
}
