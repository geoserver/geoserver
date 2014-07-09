/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.awt.Dimension;

import org.geoserver.catalog.StyleInfo;

/**
 * Simple interface for a LegendSample manager. Currently it only allows getting
 * sample size (width x height) of a given StyleInfo object. In the future we
 * could add other sample related functionality (for example to create a cache
 * of samples to be used in GetLegendGraphic).
 * 
 * @author Mauro Bartolomeoli (mauro.bartolomeoli @ geo-solutions.it)
 */
public interface LegendSample {
    /**
     * Calculates sample icon size (width x height) for the given style.
     * 
     * @param style
     * @return
     */
    public Dimension getLegendURLSize(StyleInfo style);
}
