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

public class ColorRampTest {

    protected static final int MIN_COLOR_INT = 52;

    protected static final int MAX_COLOR_INT = 255;

    @Test
    public void blueColorRampTest() throws Exception {
        BlueColorRamp blueRamp = new BlueColorRamp();
        blueRamp.setNumClasses(10);
        assertEquals(10, blueRamp.getNumClasses());
        List<Color> colors = blueRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals(
                "Incorrect value for 1st color", new Color(0, 0, MIN_COLOR_INT), colors.get(0));
        assertEquals(
                "Incorrect value for last color", new Color(0, 0, MAX_COLOR_INT), colors.get(9));
        blueRamp.revert();
        List<Color> reverseColors = blueRamp.getRamp();
        assertEquals(
                "Incorrect value for last reverse color",
                new Color(0, 0, MIN_COLOR_INT),
                reverseColors.get(9));
        assertEquals(
                "Incorrect value for 1st reverse color",
                new Color(0, 0, MAX_COLOR_INT),
                reverseColors.get(0));
    }

    @Test
    public void redColorRampTest() throws Exception {
        RedColorRamp redRamp = new RedColorRamp();
        redRamp.setNumClasses(10);
        assertEquals(10, redRamp.getNumClasses());
        List<Color> colors = redRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals(
                "Incorrect value for 1st color", new Color(MIN_COLOR_INT, 0, 0), colors.get(0));
        assertEquals(
                "Incorrect value for last color", new Color(MAX_COLOR_INT, 0, 0), colors.get(9));
        redRamp.revert();
        List<Color> reverseColors = redRamp.getRamp();
        assertEquals(
                "Incorrect value for last reverse color",
                new Color(MAX_COLOR_INT, 0, 0),
                reverseColors.get(0));
        assertEquals(
                "Incorrect value for 1st reverse color",
                new Color(MIN_COLOR_INT, 0, 0),
                reverseColors.get(9));
    }

    @Test
    public void grayColorRampTest() throws Exception {
        GrayColorRamp grayRamp = new GrayColorRamp();
        grayRamp.setNumClasses(10);
        assertEquals(10, grayRamp.getNumClasses());
        List<Color> colors = grayRamp.getRamp();
        assertEquals("Incorrect size for color ramp", 10, colors.size());
        assertEquals(
                "Incorrect value for 1st color",
                new Color(MIN_COLOR_INT, MIN_COLOR_INT, MIN_COLOR_INT),
                colors.get(0));
        assertEquals(
                "Incorrect value for last color",
                new Color(MAX_COLOR_INT, MAX_COLOR_INT, MAX_COLOR_INT),
                colors.get(9));
        grayRamp.revert();
        List<Color> reverseColors = grayRamp.getRamp();
        assertEquals(
                "Incorrect value for last reverse color",
                new Color(MIN_COLOR_INT, MIN_COLOR_INT, MIN_COLOR_INT),
                reverseColors.get(9));
        assertEquals(
                "Incorrect value for 1st reverse color",
                new Color(MAX_COLOR_INT, MAX_COLOR_INT, MAX_COLOR_INT),
                reverseColors.get(0));
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
        assertEquals(
                "Incorrect value for last reverse color",
                new Color(0, 0, 255),
                reverseColors.get(9));
        assertEquals(
                "Incorrect value for 1st reverse color",
                new Color(255, 0, 0),
                reverseColors.get(0));
    }
}
