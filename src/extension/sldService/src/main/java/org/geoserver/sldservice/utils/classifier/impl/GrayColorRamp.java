/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.*;

/**
 * Gray Color Ramp Implementation
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class GrayColorRamp extends SingleColorRamp {

    @Override
    protected Color getColorForIndex(double step, int i) {
        return new Color((int) (step * i + 30), (int) (step * i + 30), (int) (step * i + 30));
    }
}
