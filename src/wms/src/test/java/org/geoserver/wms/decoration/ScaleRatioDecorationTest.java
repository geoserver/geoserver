/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.util.HashMap;

import junit.framework.Assert;

import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;


public class ScaleRatioDecorationTest {

    @SuppressWarnings("unchecked")
    public WMSMapContent createMapContent(double dpi) {
        GetMapRequest request = new GetMapRequest();
        request.setWidth(1000);
        request.setHeight(1000);
        request.setRawKvp(new HashMap<String, String>());
        
        if (dpi > 0) {
            request.getFormatOptions().put("dpi", dpi);
        }
        
        WMSMapContent map = new WMSMapContent(request);
        map.getViewport().setBounds(new ReferencedEnvelope(new Envelope(0, 0.01, 0, 0.01), DefaultGeographicCRS.WGS84));
        return map;
    }

    @Test
    public void testRatio() throws Exception {
        ScaleRatioDecoration d = new ScaleRatioDecoration();
        
        Assert.assertEquals(3962, d.getScale(createMapContent(-1)), 0.5);
        
        // 90 is DPI default value
        Assert.assertEquals(3962, d.getScale(createMapContent(25.4 / 0.28)), 0.5);
        
        Assert.assertEquals(13104, d.getScale(createMapContent(300)), 0.5);
        Assert.assertEquals(26208, d.getScale(createMapContent(600)), 0.5);        
        Assert.assertEquals(78624, d.getScale(createMapContent(1800)), 0.5);
    }
}