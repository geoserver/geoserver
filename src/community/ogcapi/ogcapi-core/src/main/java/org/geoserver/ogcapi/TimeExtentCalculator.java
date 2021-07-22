/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.IOException;
import java.util.Date;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.util.DateRange;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Utility to support calculation of time ranges for {@link org.geotools.data.ResourceInfo} objects.
 * Current implementation supports only {@link org.geoserver.catalog.FeatureTypeInfo}.
 */
public class TimeExtentCalculator {

    private TimeExtentCalculator() {};

    public static DateRange getTimeExtent(ResourceInfo ri) throws IOException {
        // does it have a time dimension enabled?
        DimensionInfo time = ri.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null) return null;

        if (ri instanceof FeatureTypeInfo) {
            return getTimeExtent((FeatureTypeInfo) ri, time);
        } else {
            return getTimeExtent((CoverageInfo) ri, time);
        }
    }

    private static DateRange getTimeExtent(FeatureTypeInfo ft, DimensionInfo time)
            throws IOException {
        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                ft.getFeatureSource(null, null);
        FeatureCollection<? extends FeatureType, ? extends Feature> collection = fs.getFeatures();

        final MinVisitor min = new MinVisitor(time.getAttribute());
        collection.accepts(min, null);
        CalcResult minResult = min.getResult();
        // check calcresult first to avoid potential IllegalStateException if no features are in
        // collection
        if (minResult != CalcResult.NULL_RESULT) {
            Date minDate = (Date) min.getMin();
            final MaxVisitor max = new MaxVisitor(time.getAttribute());
            collection.accepts(max, null);
            Date maxDate = (Date) max.getMax();

            if (minDate != null && maxDate != null) return new DateRange(minDate, maxDate);
        }

        return null;
    }

    private static DateRange getTimeExtent(CoverageInfo ci, DimensionInfo time) throws IOException {
        ReaderDimensionsAccessor accessor =
                new ReaderDimensionsAccessor(
                        (GridCoverage2DReader) ci.getGridCoverageReader(null, null));
        Date minTime = accessor.getMinTime();
        Date maxTime = accessor.getMaxTime();
        return new DateRange(minTime, maxTime);
    }
}
