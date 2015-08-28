package org.geoserver.wms.decoration;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import javax.imageio.ImageIO;

public class ScaleLineDecorationTest extends DecorationTestSupport {

    @Test
    public void testTransparency() throws Exception {
        ScaleLineDecoration d = new ScaleLineDecoration();
        BufferedImage bi = paintOnImage(d);
        
        // ImageIO.write(bi, "PNG", new File("/tmp/test1.png"));

        assertPixel(bi, 180, 160, Color.WHITE);
        
        // setup for transparent background
        Map<String, String> options = new HashMap<String, String>();
        options.put("transparent", "true");
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
        Map<String, String> options = new HashMap<String, String>();
        options.put("measurement-system", "metric");
        d.loadOptions(options);
        BufferedImage bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test2.png"));
        //Check for metric
        assertPixel(bi, 163, 148, Color.black);
        //Check that we do not have imperial
        assertPixel(bi, 170, 148, Color.white);
        
        
        // setup for imperial
        options.clear();
        options.put("measurement-system", "imperial");
        d.loadOptions(options);
        bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test3.png"));
        //Check for imperial
        assertPixel(bi, 190, 148, Color.black);
        //Check that we do not have metric
        assertPixel(bi, 163, 148, Color.white);
        
        
        // setup for both
        options.clear();
        options.put("measurement-system", "both");
        d.loadOptions(options);
        bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test4.png"));
        //Check for imperial
        assertPixel(bi, 190, 157, Color.black);
        //Check for metric
        assertPixel(bi, 163, 144, Color.black);
        
        
        // setup for default(both)
        options.clear();
        d.loadOptions(options);
        bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test5.png"));
        //Check for imperial
        assertPixel(bi, 190, 157, Color.black);
        //Check for metric
        assertPixel(bi, 163, 144, Color.black);
    }

    @Test
    public void testMeasurementScale() throws Exception {
        ScaleLineDecoration d = new ScaleLineDecoration();

        // setup for metric
        Map<String, String> options = new HashMap<String, String>();
        options.put("measurement-system", "metric");
        options.put("scalewidthpercent", "200");
        d.loadOptions(options);
        BufferedImage bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test6.png"));
        //Check for metric
        assertPixel(bi, 217, 146, Color.black);
        //Check that center prong exists when 200 or more
        assertPixel(bi, 148, 146, Color.black);
        //Check that we do not have imperial
        assertPixel(bi, 222, 146, Color.white);


        // setup for imperial
        options.clear();
        options.put("measurement-system", "imperial");
        options.put("scalewidthpercent", "200");
        d.loadOptions(options);
        bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test7.png"));
        //Check for imperial
        assertPixel(bi, 163, 148, Color.black);
        //Check that we do not have metric
        assertPixel(bi, 168, 150, Color.white);


        // setup for both
        options.clear();
        options.put("measurement-system", "both");
        options.put("scalewidthpercent", "200");
        d.loadOptions(options);
        bi = paintOnImage(d);

        //ImageIO.write(bi, "PNG", new File("/tmp/test8.png"));
        //Check for imperial
        assertPixel(bi, 216, 145, Color.black);
        //Check for metric
        assertPixel(bi, 164, 155, Color.black);

    }



    private BufferedImage paintOnImage(ScaleLineDecoration d) throws Exception {
        BufferedImage bi = new BufferedImage(300, 300, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = bi.createGraphics();
        d.paint(g2d, new Rectangle(300, 300), createMapContent(300));
        g2d.dispose();
        return bi;
    }
}
