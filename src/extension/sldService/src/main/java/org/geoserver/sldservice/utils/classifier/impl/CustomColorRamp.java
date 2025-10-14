/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.sldservice.utils.classifier.ColorRamp;
import org.geotools.util.logging.Logging;

/**
 * Custom Color Ramp Implementation
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class CustomColorRamp implements ColorRamp {

    static final Logger LOGGER = Logging.getLogger(CustomColorRamp.class);

    private int classNum = 0;

    private List<Color> colors = new ArrayList<>();

    private Color startColor = null;

    private Color endColor = null;

    private Color midColor = null;

    private List<Color> inputColors = null;

    @Override
    public int getNumClasses() {
        return classNum;
    }

    @Override
    public List<Color> getRamp() throws Exception {
        if (colors == null) throw new Exception("Class num not setted, color ramp null");
        return colors;
    }

    @Override
    public void revert() {
        Collections.reverse(colors);
    }

    @Override
    public void setNumClasses(int numClass) {
        classNum = numClass + 1; // +1 for transparent
        try {
            createRamp();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
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

    public void setInputColors(List<Color> inputColors) {
        this.inputColors = inputColors;
    }

    protected void createRamp() throws Exception {
        int classes = classNum - 1;
        if (inputColors != null) {
            if (classes == inputColors.size()) {
                colors = inputColors;
            } else if (classes > inputColors.size()) {
                int slices = inputColors.size() - 1;
                int sliceSize = classes / slices;
                colors = new ArrayList<>();
                int total = 0;
                for (int i = 0; i < slices - 1; i++) {
                    total += sliceSize - 1;
                    interpolate(colors, inputColors.get(i), inputColors.get(i + 1), sliceSize, i > 0);
                }
                interpolate(
                        colors,
                        inputColors.get(inputColors.size() - 2),
                        inputColors.get(inputColors.size() - 1),
                        classes - total,
                        true);
            } else {
                colors = inputColors.subList(0, classes);
            }
        } else {
            int mid;
            if (startColor == null || endColor == null)
                throw new Exception("Start or end color not setted unable to build color ramp");

            if (midColor == null) {
                interpolate(colors, startColor, endColor, classNum - 1);
            } else {
                if (classes == 1) {
                    colors.add(startColor);
                } else if (classes == 2) {
                    colors.add(startColor);
                    colors.add(endColor);
                } else if (classes == 3) {
                    colors.add(startColor);
                    colors.add(midColor);
                    colors.add(endColor);
                } else {
                    mid = classes / 2;
                    int rest = classes - mid;
                    interpolate(colors, startColor, midColor, mid);
                    interpolate(colors, midColor, endColor, rest, true);
                }
            }
        }
    }

    private void interpolate(List<Color> result, Color start, Color end, int samples, boolean offset) {
        if (offset) {
            double sRed = ((double) end.getRed() - start.getRed()) / samples;
            double sGreen = ((double) end.getGreen() - start.getGreen()) / samples;
            double sBlue = ((double) end.getBlue() - start.getBlue()) / samples;

            start = new Color(
                    (int) (start.getRed() + sRed), (int) (start.getGreen() + sGreen), (int) (start.getBlue() + sBlue));
        }
        interpolate(colors, start, end, samples);
    }

    private void interpolate(List<Color> result, Color start, Color end, int samples) {
        int red;
        int green;
        int blue;
        double sRed = ((double) end.getRed() - start.getRed()) / (samples - 1);
        double sGreen = ((double) end.getGreen() - start.getGreen()) / (samples - 1);
        double sBlue = ((double) end.getBlue() - start.getBlue()) / (samples - 1);
        for (int i = 0; i < samples; i++) {
            red = (int) (sRed * i + start.getRed());
            green = (int) (sGreen * i + start.getGreen());
            blue = (int) (sBlue * i + start.getBlue());
            result.add(new Color(red, green, blue));
        }
    }
}
