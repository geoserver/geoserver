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
import java.util.concurrent.ThreadLocalRandom;
import org.geoserver.sldservice.utils.classifier.ColorRamp;

/**
 * Random Color Ramp Implementation
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class RandomColorRamp implements ColorRamp {

    private int classNum = 0;

    private List<Color> colors = new ArrayList<>();

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
        classNum = numClass;
        try {
            createRamp();
        } catch (Exception e) {

        }
    }

    private void createRamp() throws Exception {
        for (int i = 0; i < classNum; i++)
            colors.add(new Color(
                    (int) (ThreadLocalRandom.current().nextDouble() * 255),
                    (int) (ThreadLocalRandom.current().nextDouble() * 255),
                    (int) (ThreadLocalRandom.current().nextDouble() * 255)));
    }
}
