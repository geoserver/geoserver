/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.List;

import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.GrayColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.JetColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RedColorRamp;
import org.junit.Test;

public class ColorRampTest extends SLDServiceBaseTest {

    protected static int minColorInt = 49;

    protected static int maxColorInt = 224;

    @Test
    public void blueColorRampTest() throws Exception {
        BlueColorRamp blueRamp = new BlueColorRamp();
        blueRamp.setNumClasses(10);
        assertEquals(11, blueRamp.getNumClasses());
        List<Color> colors = blueRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals("Incorrect value for 1st color", new Color(0, 0, minColorInt), colors.get(0));
        assertEquals("Incorrect value for last color", new Color(0, 0, maxColorInt), colors.get(9));
        blueRamp.revert();
        List<Color> reverseColors = blueRamp.getRamp();
        assertEquals("Incorrect value for last reverse color", new Color(0, 0, minColorInt),
                reverseColors.get(9));
        assertEquals("Incorrect value for 1st reverse color", new Color(0, 0, maxColorInt),
                reverseColors.get(0));
    }

    @Test
    public void redColorRampTest() throws Exception {
        RedColorRamp redRamp = new RedColorRamp();
        redRamp.setNumClasses(10);
        assertEquals(11, redRamp.getNumClasses());
        List<Color> colors = redRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals("Incorrect value for 1st color", new Color(minColorInt, 0, 0), colors.get(0));
        assertEquals("Incorrect value for last color", new Color(maxColorInt, 0, 0), colors.get(9));
        redRamp.revert();
        List<Color> reverseColors = redRamp.getRamp();
        assertEquals("Incorrect value for last reverse color", new Color(maxColorInt, 0, 0),
                reverseColors.get(0));
        assertEquals("Incorrect value for 1st reverse color", new Color(minColorInt, 0, 0),
                reverseColors.get(9));
    }

    @Test
    public void grayColorRampTest() throws Exception {
        GrayColorRamp grayRamp = new GrayColorRamp();
        grayRamp.setNumClasses(10);
        assertEquals(11, grayRamp.getNumClasses());
        List<Color> colors = grayRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals("Incorrect value for 1st color",
                new Color(minColorInt, minColorInt, minColorInt), colors.get(0));
        assertEquals("Incorrect value for last color",
                new Color(maxColorInt, maxColorInt, maxColorInt), colors.get(9));
        grayRamp.revert();
        List<Color> reverseColors = grayRamp.getRamp();
        assertEquals("Incorrect value for last reverse color",
                new Color(minColorInt, minColorInt, minColorInt), reverseColors.get(9));
        assertEquals("Incorrect value for 1st reverse color",
                new Color(maxColorInt, maxColorInt, maxColorInt), reverseColors.get(0));
    }

    @Test
    public void jetColorRampTest() throws Exception {
        JetColorRamp jetRamp = new JetColorRamp();
        jetRamp.setNumClasses(10);
        assertEquals(11, jetRamp.getNumClasses());
        List<Color> colors = jetRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals("Incorrect value for 1st color", new Color(0, 0, 255), colors.get(0));
        assertEquals("Incorrect value for last color", new Color(255, 0, 0), colors.get(9));

        jetRamp.revert();
        List<Color> reverseColors = jetRamp.getRamp();
        assertEquals("Incorrect value for last reverse color", new Color(0, 0, 255),
                reverseColors.get(9));
        assertEquals("Incorrect value for 1st reverse color", new Color(255, 0, 0),
                reverseColors.get(0));
    }

    @Override
    protected String getServiceUrl() {
        return null;
    }

}