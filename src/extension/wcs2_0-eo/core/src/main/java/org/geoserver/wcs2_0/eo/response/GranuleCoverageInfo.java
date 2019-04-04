/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.security.decorators.DecoratingCoverageInfo;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

/**
 * Builds a view of the coverage that contains only the specified coverage
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GranuleCoverageInfo extends DecoratingCoverageInfo {
    private static final long serialVersionUID = 7877565589262804385L;
    private SimpleFeature feature;
    private List<DimensionDescriptor> dimensionDescriptors;

    public GranuleCoverageInfo(
            CoverageInfo delegate,
            SimpleFeature feature,
            List<DimensionDescriptor> dimensionDescriptors) {
        super(delegate);
        this.feature = feature;
        this.dimensionDescriptors = dimensionDescriptors;
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException {
        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) super.getGridCoverageReader(listener, hints);
        return new SingleGranuleGridCoverageReader(reader, feature, dimensionDescriptors);
    }
}
