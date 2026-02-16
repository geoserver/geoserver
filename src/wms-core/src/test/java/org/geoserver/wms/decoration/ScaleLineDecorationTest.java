/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import static org.geoserver.wms.decoration.MapDecorationLayout.FF;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.filter.expression.Expression;
import org.junit.Test;

public class ScaleLineDecorationTest extends DecorationTestSupport {

    @Test
    public void testTransparency() throws Exception {
        ScaleLineDecoration d = new ScaleLineDecoration();
        BufferedImage bi = paintOnImage(d);

        // ImageIO.write(bi, "PNG", new File("/tmp/test.png"));

        assertPixel(bi, 180, 160, Color.WHITE);

        // setup for transparent background
        Map<String, Expression> options = new HashMap<>();
        options.put("transparent", FF.literal("true"));
        d.loadOptions(options);

        // check we get a transparent background in the same location
        BufferedImage bi2 = paintOnImage(d);
        // ImageIO.write(bi2, "PNG", new File("/tmp/test.png"));
        assertPixel(bi2, 180, 160, new Color(0, 0, 0, 0));
    }

    @Test
    public void testMeasurementOption() throws Exception {
        ScaleLineDecoration d = new ScaleLineDecoration();

        // setup for metric
        Map<String, Expression> options = new HashMap<>();
        options.put("measurement-system", FF.literal("metric"));
        d.loadOptions(options);
        BufferedImage bi = paintOnImage(d);

        // ImageIO.write(bi, "PNG", new File("/tmp/test1.png"));
        // Check for metric
        assertPixel(bi, 109, 139, Color.black);
        // Check that we do not have imperial
        assertPixel(bi, 109, 157, Color.white);

        // setup for imperial
        options.clear();
        options.put("measurement-system", FF.literal("imperial"));
        d.loadOptions(options);
        bi = paintOnImage(d);

        // ImageIO.write(bi, "PNG", new File("/tmp/test2.png"));
        // Check for imperial
        assertPixel(bi, 109, 157, Color.black);
        // Check that we do not have metric
        assertPixel(bi, 109, 139, Color.white);

        // setup for both
        options.clear();
        options.put("measurement-system", FF.literal("both"));
        d.loadOptions(options);
        bi = paintOnImage(d);

        // ImageIO.write(bi, "PNG", new File("/tmp/test3.png"));
        // Check for imperial
        assertPixel(bi, 109, 157, Color.black);
        // Check for metric
        assertPixel(bi, 109, 139, Color.black);

        // setup for default(both)
        options.clear();
        d.loadOptions(options);
        bi = paintOnImage(d);

        // ImageIO.write(bi, "PNG", new File("/tmp/test4.png"));
        // Check for imperial
        assertPixel(bi, 109, 157, Color.black);
        // Check for metric
        assertPixel(bi, 109, 139, Color.black);
    }

    private BufferedImage paintOnImage(ScaleLineDecoration d) throws Exception {
        BufferedImage bi = new BufferedImage(300, 300, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = bi.createGraphics();
        d.paint(g2d, new Rectangle(300, 300), createMapContent(300));
        g2d.dispose();
        return bi;
    }
}
