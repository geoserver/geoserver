/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A single coverage wrapper for structured coverage readers. The structured extra operations are
 * not limited to the single coverage though.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StructuredSingleGridCoverage2DReader extends SingleGridCoverage2DReader
        implements StructuredGridCoverage2DReader {

    private StructuredGridCoverage2DReader structuredDelegate;

    public StructuredSingleGridCoverage2DReader(
            StructuredGridCoverage2DReader delegate, String coverageName) {
        super(delegate, coverageName);
        this.structuredDelegate = delegate;
    }

    @Override
    public GranuleSource getGranules(String coverageName, boolean readOnly)
            throws IOException, UnsupportedOperationException {
        if (coverageName == null) {
            coverageName = this.coverageName;
        }
        return structuredDelegate.getGranules(coverageName, readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return structuredDelegate.isReadOnly();
    }

    @Override
    public void createCoverage(String coverageName, SimpleFeatureType schema)
            throws IOException, UnsupportedOperationException {
        structuredDelegate.createCoverage(coverageName, schema);
    }

    @Override
    public boolean removeCoverage(String coverageName, boolean delete)
            throws IOException, UnsupportedOperationException {
        return structuredDelegate.removeCoverage(coverageName, delete);
    }

    @Override
    public void delete(boolean deleteData) throws IOException, UnsupportedOperationException {
        structuredDelegate.delete(deleteData);
    }

    @Override
    public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source, Hints hints)
            throws IOException, UnsupportedOperationException {
        return structuredDelegate.harvest(defaultTargetCoverage, source, hints);
    }

    @Override
    public List<DimensionDescriptor> getDimensionDescriptors(String coverageName)
            throws IOException {
        if (coverageName == null) {
            coverageName = this.coverageName;
        }
        return structuredDelegate.getDimensionDescriptors(coverageName);
    }
}
