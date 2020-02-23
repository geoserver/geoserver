/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.util.HashMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;

public class DecorationTestSupport {

    protected WMSMapContent createMapContent(double dpi) {
        GetMapRequest request = new GetMapRequest();
        request.setWidth(1000);
        request.setHeight(1000);
        request.setRawKvp(new HashMap<String, String>());

        if (dpi > 0) {
            request.getFormatOptions().put("dpi", dpi);
        }

        WMSMapContent map = new WMSMapContent(request);
        map.setMapWidth(request.getWidth());
        map.setMapHeight(request.getHeight());
        map.getViewport()
                .setBounds(
                        new ReferencedEnvelope(
                                new Envelope(0, 0.01, 0, 0.01), DefaultGeographicCRS.WGS84));
        return map;
    }

    /** Checks the pixel i/j has the specified color */
    protected void assertPixel(BufferedImage image, int i, int j, Color color) {
        Color actual = getPixelColor(image, i, j);

        assertEquals(color, actual);
    }

    /** Gets a specific pixel color from the specified buffered image */
    protected Color getPixelColor(BufferedImage image, int i, int j) {
        ColorModel cm = image.getColorModel();
        Raster raster = image.getRaster();
        Object pixel = raster.getDataElements(i, j, null);

        Color actual;
        if (cm.hasAlpha()) {
            actual =
                    new Color(
                            cm.getRed(pixel),
                            cm.getGreen(pixel),
                            cm.getBlue(pixel),
                            cm.getAlpha(pixel));
        } else {
            actual = new Color(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), 255);
        }
        return actual;
    }
}
