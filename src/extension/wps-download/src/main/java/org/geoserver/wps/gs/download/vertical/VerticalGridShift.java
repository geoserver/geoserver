/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical;

import org.geotools.geometry.GeneralEnvelope;

/**
 * A Vertical Grid Shift is based on a Grid file containing a Grid of values where each position in
 * the grid contains the value of the shift to be applied
 */
public interface VerticalGridShift {

    /** The width (in pixels) of the Grid */
    int getWidth();

    /** The height (in pixels) of the Grid */
    int getHeight();

    /** Return true if the specified coordinate is within the valid area of the Grid */
    boolean isInValidArea(double x, double y);

    /** Return the valid area (the bbox) of the Grid */
    GeneralEnvelope getValidArea();

    /** Return the resolution of the Grid */
    double[] getResolution();

    /** Return the CRS associated to the Grid */
    int getCRSCode();

    /**
     * Apply the shift to the z value at the given position, returning false if the shift hasn't be
     * applied
     */
    boolean shift(double x, double y, double[] z);

    /** Release resources associated to the Grid */
    void dispose();
}
