/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.sldservice.utils.classifier.ColorRamp;

/**
 * Custom Color Ramp Implementation
 * 
 * @author Alessio Fabiani, GeoSolutions SAS
 *
 */
public class CustomColorRamp implements ColorRamp {

    private int classNum = 0;

    private List<Color> colors = new ArrayList<Color>();

    private Color startColor = null;

    private Color endColor = null;

    private Color midColor = null;

    public int getNumClasses() {
        return classNum;
    }

    public List<Color> getRamp() throws Exception {
        if (colors == null)
            throw new Exception("Class num not setted, color ramp null");
        return colors;
    }

    public void revert() {
        Collections.reverse(colors);
    }

    public void setNumClasses(int numClass) {
        classNum = numClass + 1;// +1 for transparent
        try {
            createRamp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStartColor(Color start) {
        startColor = start;
    }

    public void setEndColor(Color end) {
        endColor = end;
    }

    public void setMid(Color mid) {
        midColor = mid;
    }

    protected void createRamp() throws Exception {
        int red, green, blue;
        double sRed, sGreen, sBlue;
        int mid;
        if (startColor == null || endColor == null)
            throw new Exception("Start or end color not setted unable to build color ramp");

        if (midColor == null) {
            sRed = ((double) endColor.getRed() - startColor.getRed()) / (double) (classNum - 1);
            sGreen = ((double) endColor.getGreen() - startColor.getGreen())
                    / (double) (classNum - 1);
            sBlue = ((double) endColor.getBlue() - startColor.getBlue()) / (double) (classNum - 1);
            for (int i = 0; i < classNum - 1; i++) {
                red = (int) (sRed * i + startColor.getRed());
                green = (int) (sGreen * i + startColor.getGreen());
                blue = (int) (sBlue * i + startColor.getBlue());
                colors.add(new Color(red, green, blue));
            }
        } else {
            mid = (classNum - 1) / 2;
            int rest = (classNum - 1) % 2;
            sRed = ((double) midColor.getRed() - startColor.getRed()) / (double) (mid);
            sGreen = ((double) midColor.getGreen() - startColor.getGreen()) / (double) (mid);
            sBlue = ((double) midColor.getBlue() - startColor.getBlue()) / (double) (mid);
            for (int i = 0; i < mid; i++) {
                red = (int) (sRed * i + startColor.getRed());
                green = (int) (sGreen * i + startColor.getGreen());
                blue = (int) (sBlue * i + startColor.getBlue());
                colors.add(new Color(red, green, blue));
            }
            int count = mid;
            sRed = ((double) endColor.getRed() - midColor.getRed()) / (double) (mid + rest - 1);
            sGreen = ((double) endColor.getGreen() - midColor.getGreen())
                    / (double) (mid + rest - 1);
            sBlue = ((double) endColor.getBlue() - midColor.getBlue()) / (double) (mid + rest - 1);
            for (int i = 0; i < (mid + rest); i++) {
                red = (int) (sRed * i + midColor.getRed());
                green = (int) (sGreen * i + midColor.getGreen());
                blue = (int) (sBlue * i + midColor.getBlue());
                colors.add(new Color(red, green, blue));
                count++;
            }
        }
    }

}
