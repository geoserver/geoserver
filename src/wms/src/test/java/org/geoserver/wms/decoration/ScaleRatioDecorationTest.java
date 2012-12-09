/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 *           (c) 2002-2011 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 * 
 * A modified version of ScaleRatioDecorationTest from GeoTools (LGPL).
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

    public WMSMapContent createMapContent(double dpi) {
        GetMapRequest request = new GetMapRequest();
        request.setWidth(1000);
        request.setHeight(1000);
        request.setRawKvp(new HashMap<String, String>());
    
        if (dpi > 0) {
            request.getFormatOptions().put("dpi", dpi);
        }
    
        WMSMapContent map = new WMSMapContent(request);
        map.getViewport().setBounds(
                new ReferencedEnvelope(new Envelope(0, 0.01, 0, 0.01),
                        DefaultGeographicCRS.WGS84));
        return map;
    }
    
    @Test
    public void testRatio() throws Exception {
        ScaleRatioDecoration d = new ScaleRatioDecoration();
    
        // 90 is DPI default value
        Assert.assertEquals(3975, d.getScale(createMapContent(-1)), 1);
        Assert.assertEquals(3975, d.getScale(createMapContent(25.4 / 0.28)), 1);
    
        Assert.assertEquals(13147, d.getScale(createMapContent(300)), 1);
        Assert.assertEquals(26295, d.getScale(createMapContent(600)), 1);
        Assert.assertEquals(78887, d.getScale(createMapContent(1800)), 1);
    }
}