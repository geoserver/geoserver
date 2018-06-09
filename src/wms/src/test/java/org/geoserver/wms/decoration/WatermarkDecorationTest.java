/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 *           (c) 2002-2011 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * A modified version of ScaleRatioDecorationTest from GeoTools (LGPL).
 */
package org.geoserver.wms.decoration;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.URLs;
import org.junit.Test;

public class WatermarkDecorationTest extends GeoServerSystemTestSupport {

    @Test
    public void testAbsolutePath() throws Exception {
        WatermarkDecoration d = new WatermarkDecoration();
        Map<String, String> options = new HashMap<String, String>();
        File file = new File("src/test/resources/org/geoserver/wms/world.png");
        options.put(
                "url", URLs.fileToUrl(file.getAbsoluteFile().getCanonicalFile()).toExternalForm());
        d.loadOptions(options);
        BufferedImage logo = d.getLogo();

        assertNotNull(logo);
        assertEquals(180, logo.getWidth());
        assertEquals(90, logo.getHeight());
    }

    @Test
    public void testRelativePath() throws Exception {
        WatermarkDecoration d = new WatermarkDecoration();
        Map<String, String> options = new HashMap<String, String>();
        File file = new File("src/test/resources/org/geoserver/wms/world.png");
        File styles = getDataDirectory().findOrCreateDir("styles");
        File logoFile = new File(styles, "world.png").getAbsoluteFile();
        FileUtils.copyFile(file, logoFile);

        options.put("url", "file:styles/world.png");
        d.loadOptions(options);
        BufferedImage logo = d.getLogo();

        assertNotNull(logo);
        assertEquals(180, logo.getWidth());
        assertEquals(90, logo.getHeight());
    }

    @Test
    public void testRelativeUnqualifiedPath() throws Exception {
        WatermarkDecoration d = new WatermarkDecoration();
        Map<String, String> options = new HashMap<>();
        File file = new File("src/test/resources/org/geoserver/wms/world.png");
        File styles = getDataDirectory().findOrCreateDir("styles");
        File logoFile = new File(styles, "world.png").getAbsoluteFile();
        FileUtils.copyFile(file, logoFile);

        options.put("url", "styles/world.png");
        d.loadOptions(options);
        BufferedImage logo = d.getLogo();

        assertNotNull(logo);
        assertEquals(180, logo.getWidth());
        assertEquals(90, logo.getHeight());
    }
}
