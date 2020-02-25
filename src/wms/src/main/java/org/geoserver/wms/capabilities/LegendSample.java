/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.awt.Dimension;
import org.geoserver.catalog.StyleInfo;

/**
 * Acess to LegendSample information for a StyleInfo object. Currently it only allows getting sample
 * size (width x height) of a given StyleInfo object. In the future we could add other sample
 * related functionality (for example to create a cache of samples to be used in GetLegendGraphic).
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli @ geo-solutions.it)
 */
public interface LegendSample {
    /**
     * Calculates sample icon size (width x height) for the given style.
     *
     * @return legend dimensions
     */
    public Dimension getLegendURLSize(StyleInfo style) throws Exception;
}
