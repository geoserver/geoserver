/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.awt.Color;
import java.util.List;

/**
 * Colar Ramp is useful for bulding symbolizer in classified style
 *
 * @author kappu
 */
public interface ColorRamp {
    /**
     * Set the new classes number and update the color ramp
     *
     * @param numClass color ramp number of classes
     */
    public void setNumClasses(int numClass);

    /** @return int classes number */
    public int getNumClasses();

    /**
     * Return the color ramp
     *
     * @return Color[]
     */
    public List<Color> getRamp() throws Exception;

    /** revert color ramp order */
    public void revert();
}
