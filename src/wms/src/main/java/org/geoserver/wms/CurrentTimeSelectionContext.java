/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.Date;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.feature.FeatureCollection;

/**
 * Implements a Context of the current time selection Strategy
 * as well as the Factory method for getting a Singleton
 * TimeSelectionContext for selecting the current time 
 * appropriate for the given DimensionInfo.
 * 
 * Note: As the DimensionInfo does not yet contain a setting
 * for the current time selection strategy, the system property
 * "org.geoserver.wms.currentTimeSelectionStrategy" will be used
 * instead for deciding the strategy. If this property has value
 * "nearest", the "current" time will be the time closest to the
 * current system time. Otherwise the traditional strategy of
 * selecting the maximum value of the available times will be used.
 *  
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 *
 */
public class CurrentTimeSelectionContext {
    private static CurrentTimeSelectionContext latestCtx;
    private static CurrentTimeSelectionContext nearestCtx;
    
    /**
     * Returns a singleton CurrentTimeSelectionContext appropriate
     * for the given DimensionInfo.
     * 
     * @param dimensionInfo
     * @return
     */
    public synchronized static CurrentTimeSelectionContext getInstance(DimensionInfo dimensionInfo){
        CurrentTimeSelectionContext retval = null;

        //TODO: Use the configuration options in DimensionInfo (hopefully to-be-added) 
        //to choose the strategy.        
        //Until then choose the strategy based on the system property value:        
        if (System.getProperty("org.geoserver.wms.currentTimeSelectionStrategy", "latest").equals("nearest")){
            if (nearestCtx == null){
                nearestCtx = new CurrentTimeSelectionContext(new NearestCurrentTimeSelectionStrategy());
            }
            retval = nearestCtx;
        }
        else {        
            if (latestCtx == null){
                latestCtx = new CurrentTimeSelectionContext(new LatestCurrentTimeSelectionStrategy());
            }
            retval = latestCtx;
        }
        return retval;
    }
    
    private CurrentTimeSelectionStrategy strategy;
    
    /**
     * Creates a new CurrentTimeSelectionContext with the given strategy.
     * 
     * @param strategy
     */
    public CurrentTimeSelectionContext(CurrentTimeSelectionStrategy strategy){
        this.strategy = strategy;
    }
    
    /**
     * Returns the current time for the specified type info
     * @param typeInfo
     * @param dimensionCollection
     * @return
     * @throws IOException
     */
    public Date getCurrentTime(FeatureTypeInfo typeInfo, FeatureCollection<?,?> dimensionCollection) throws IOException {
        return this.strategy.getCurrentTime(typeInfo, dimensionCollection);
    }
    
    /**
     * Returns the current time for the specified coverage
     * @param coverage
     * @param dimensions
     * @return
     * @throws IOException
     */
    public Date getCurrentTime(CoverageInfo coverage, ReaderDimensionsAccessor dimensions) throws IOException{
        return this.strategy.getCurrentTime(coverage, dimensions);
    }
}
