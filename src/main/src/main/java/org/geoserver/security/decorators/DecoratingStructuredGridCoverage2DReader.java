/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import it.geosolutions.imageio.maskband.DatasetLayout;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.media.jai.ImageLayout;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.geometry.GeneralBounds;
import org.geotools.util.factory.Hints;

/**
 * Delegates every method to the delegate structured grid coverage reader. Subclasses will override
 * selected methods to perform their "decoration" job
 *
 * @author Daniele Romagnoli
 */
public abstract class DecoratingStructuredGridCoverage2DReader
        implements StructuredGridCoverage2DReader {

    StructuredGridCoverage2DReader delegate;

    public DecoratingStructuredGridCoverage2DReader(StructuredGridCoverage2DReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public Format getFormat() {
        return delegate.getFormat();
    }

    @Override
    public Object getSource() {
        return delegate.getSource();
    }

    @Override
    public String[] getMetadataNames() throws IOException {
        return delegate.getMetadataNames();
    }

    @Override
    public GeneralBounds getOriginalEnvelope() {
        return delegate.getOriginalEnvelope();
    }

    @Override
    public GeneralBounds getOriginalEnvelope(String coverageName) {
        return delegate.getOriginalEnvelope(coverageName);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        return delegate.getCoordinateReferenceSystem(coverageName);
    }

    @Override
    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange();
    }

    @Override
    public String[] getMetadataNames(String coverageName) throws IOException {
        return delegate.getMetadataNames(coverageName);
    }

    @Override
    public GridEnvelope getOriginalGridRange(String coverageName) {
        return delegate.getOriginalGridRange(coverageName);
    }

    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(pixInCell);
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return delegate.read(parameters);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return delegate.read(coverageName, parameters);
    }

    @Override
    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(name);
    }

    @Override
    public String getMetadataValue(String coverageName, String name) throws IOException {
        return delegate.getMetadataValue(coverageName, name);
    }

    @Override
    public void dispose() throws IOException {
        delegate.dispose();
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters();
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        return delegate.getDynamicParameters(coverageName);
    }

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(policy, requestedResolution);
    }

    @Override
    public double[] getReadingResolutions(
            String coverageName, OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    @Override
    public String[] getGridCoverageNames() throws IOException {
        return delegate.getGridCoverageNames();
    }

    @Override
    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    @Override
    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout();
    }

    @Override
    public ImageLayout getImageLayout(String coverageName) throws IOException {
        return delegate.getImageLayout(coverageName);
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels();
    }

    @Override
    public double[][] getResolutionLevels(String coverageName) throws IOException {
        return delegate.getResolutionLevels(coverageName);
    }

    @Override
    public GranuleSource getGranules(String coverageName, boolean readOnly)
            throws IOException, UnsupportedOperationException {
        return delegate.getGranules(coverageName, readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public void createCoverage(String coverageName, SimpleFeatureType schema)
            throws IOException, UnsupportedOperationException {
        delegate.createCoverage(coverageName, schema);
    }

    @Override
    public boolean removeCoverage(String coverageName, boolean delete)
            throws IOException, UnsupportedOperationException {
        return delegate.removeCoverage(coverageName, delete);
    }

    @Override
    public void delete(boolean deleteData) throws IOException {
        delegate.delete(deleteData);
    }

    @Override
    public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source, Hints hints)
            throws IOException, UnsupportedOperationException {
        return delegate.harvest(defaultTargetCoverage, source, hints);
    }

    @Override
    public List<DimensionDescriptor> getDimensionDescriptors(String coverageName)
            throws IOException {
        return delegate.getDimensionDescriptors(coverageName);
    }

    @Override
    public DatasetLayout getDatasetLayout() {
        return delegate.getDatasetLayout();
    }

    @Override
    public DatasetLayout getDatasetLayout(String coverageName) {
        return delegate.getDatasetLayout(coverageName);
    }
}
