/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.Date;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.feature.FeatureCollection;

/**
 * Strategy for selecting the value of the default "current" time for TIME dimension, when
 * the user has not made any selection in the request.
 * 
 * The concrete selection strategies all implement this interface according to the 
 * Strategy design pattern (http://en.wikipedia.org/wiki/Strategy_pattern)
 * 
 * @author Ilkka Rinne
 *
 */
public interface CurrentTimeSelectionStrategy {
    
    /**
     * Returns the current time for the specified feature type
     * 
     * @param typeInfo
     * @return
     * @throws IOException
     */
    public Date getCurrentTime(FeatureTypeInfo typeInfo, FeatureCollection<?,?> dimensionCollection) throws IOException;
    
    /**
     * Returns the current time for the specified coverage 
     * 
     * @param typeInfo
     * @return
     * @throws IOException
     */
    public Date getCurrentTime(CoverageInfo coverage, ReaderDimensionsAccessor dimensions) throws IOException;
}
