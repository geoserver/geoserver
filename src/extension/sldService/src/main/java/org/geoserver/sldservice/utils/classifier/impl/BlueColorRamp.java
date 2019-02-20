/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.*;

/**
 * Blue Color Ramp Implementation
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class BlueColorRamp extends SingleColorRamp {

    protected Color getColorForIndex(double step, int idx) {
        return new Color(0, 0, (int) (step * idx + 30));
    }
}
