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
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;

/**
 * This strategy implements the current time selection based on finding the maximum available timestamp.
 * 
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 * 
 */
public class LatestCurrentTimeSelectionStrategy implements CurrentTimeSelectionStrategy {

    /**
     * Default constructor.
     */
    public LatestCurrentTimeSelectionStrategy() {
    }

    @Override
    public Date getCurrentTime(FeatureTypeInfo typeInfo, FeatureCollection<?, ?> dimensionCollection)
            throws IOException {
        if (dimensionCollection == null) {
            throw new ServiceException(
                    "No dimension collection given, cannot select 'current' value for time dimension");
        }

        // check the time metadata
        DimensionInfo time = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.prefixedName()
                    + " does not have time support enabled");
        }

        // current is the max time we have
        final MaxVisitor max = new MaxVisitor(time.getAttribute());
        dimensionCollection.accepts(max, null);
        if (max.getResult() != CalcResult.NULL_RESULT) {
            return (Date) max.getMax();
        } else {
            return null;
        }
    }

    @Override
    public Date getCurrentTime(CoverageInfo coverage, ReaderDimensionsAccessor dimensions)
            throws IOException {
        // The below is the content previously contained in org.geoserver.wms.WMS#GetCurrentTime(CoverageInfo, ReaderDimensionsAccessor):
        // check the time metadata
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        String name = coverage.prefixedName();
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + name + " does not have time support enabled");
        }

        // get and parse the current time
        return dimensions.getMaxTime();
    }

}
