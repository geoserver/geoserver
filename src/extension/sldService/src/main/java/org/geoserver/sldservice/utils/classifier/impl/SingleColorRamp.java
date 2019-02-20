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
import org.geoserver.sldservice.utils.classifier.ColorRamp;

public abstract class SingleColorRamp implements ColorRamp {

    private int classNum = 0;

    private List<Color> colors = new ArrayList<>();

    public int getNumClasses() {
        return classNum;
    }

    public void revert() {
        Collections.reverse(colors);
    }

    public void setNumClasses(int numClass) {
        classNum = numClass;
        createRamp();
    }

    public List<Color> getRamp() throws Exception {
        if (colors == null) throw new Exception("Class num not set, color ramp null");
        return colors;
    }

    private void createRamp() {
        double step = (225.0 / (double) classNum);
        for (int i = 1; i <= classNum; i++) {
            colors.add(getColorForIndex(step, i));
        }
    }

    /**
     * Creates the color for the i-th class, using the provided color step
     *
     * @param step The color step
     * @param i The class counter
     * @return A color for this combination
     */
    protected abstract Color getColorForIndex(double step, int i);
}
