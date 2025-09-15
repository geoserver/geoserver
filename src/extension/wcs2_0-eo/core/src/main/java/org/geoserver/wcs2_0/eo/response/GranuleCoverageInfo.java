/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.io.Serial;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.security.decorators.DecoratingCoverageInfo;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;

/**
 * Builds a view of the coverage that contains only the specified coverage
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GranuleCoverageInfo extends DecoratingCoverageInfo {
    @Serial
    private static final long serialVersionUID = 7877565589262804385L;

    private SimpleFeature feature;
    private List<DimensionDescriptor> dimensionDescriptors;

    public GranuleCoverageInfo(
            CoverageInfo delegate, SimpleFeature feature, List<DimensionDescriptor> dimensionDescriptors) {
        super(delegate);
        this.feature = feature;
        this.dimensionDescriptors = dimensionDescriptors;
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints) throws IOException {
        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) super.getGridCoverageReader(listener, hints);
        return new SingleGranuleGridCoverageReader(reader, feature, dimensionDescriptors);
    }

    @Override
    public CoordinateReferenceSystem getCRS() {
        try {
            return ((GridCoverage2DReader) getGridCoverageReader(null, null)).getCoordinateReferenceSystem();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ReferencedEnvelope boundingBox() throws Exception {
        return ReferencedEnvelope.reference(
                ((GridCoverage2DReader) getGridCoverageReader(null, null)).getOriginalEnvelope());
    }

    @Override
    public ReferencedEnvelope getNativeBoundingBox() {
        try {
            return boundingBox();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        return iface != null && iface.isAssignableFrom(this.getClass());
    }

    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        try {
            if (iface != null && iface.isAssignableFrom(this.getClass())) {
                return (T) this;
            }
            throw new java.sql.SQLException("Auto-generated unwrap failed; Revisit implementation");
        } catch (Exception e) {
            throw new java.sql.SQLException(e);
        }
    }
}
