/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import org.geoserver.wms.WMSMapContent;

/**
 * The MapDecoration class encapsulates the rendering code for an overlay to be used to enhance a
 * WMS response. Decorations know how to determine their appropriate size, and how to render into a
 * given area, but leave the actual layout calculations to the {MapDecorationLayout} class.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public interface MapDecoration {
    /**
     * Load in configuration parameters from a map. All subsequent paint operations should use the
     * provided parameters. Implementations do not need to respect multiple calls to this method.
     *
     * @param options a Map<String,String> containing the configuration parameters
     * @throws Exception if required parameters are missing from the configuration
     */
    public void loadOptions(Map<String, String> options) throws Exception;

    /**
     * Determine the 'best' size for this decoration, given the request parameters.
     *
     * @param g2d the Graphics2D context in which this Decoration will be rendered
     * @param mapContent the map context for the request
     * @throws InvalidStateException if loadOptions() has not been called yet
     */
    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) throws Exception;

    /**
     * Render the contents of this decoration onto the provided graphics object within the specified
     * bounds. The WMSMapContext object can be used to provide additional info about the map for
     * context-sensitive decorations.
     *
     * @param g2d the Graphics2D object onto which the decoration should be drawn
     * @param paintArea the bounds within the graphics object where the decoration should be drawn
     * @param context the mapContent for the image being rendered
     * @throws InvalidStateException if loadOptions() has not been called yet
     */
    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent context) throws Exception;
}
