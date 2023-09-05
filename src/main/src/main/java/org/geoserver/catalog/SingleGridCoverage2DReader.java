/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import it.geosolutions.imageio.maskband.DatasetLayout;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.media.jai.ImageLayout;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.geometry.GeneralBounds;

/** A wrapper restricting the provided reader to return a single grid coverage */
public class SingleGridCoverage2DReader implements GridCoverage2DReader {

    protected GridCoverage2DReader delegate;

    protected String coverageName;

    public static SingleGridCoverage2DReader wrap(
            GridCoverage2DReader delegate, String coverageName) {
        if (delegate instanceof StructuredGridCoverage2DReader) {
            return new StructuredSingleGridCoverage2DReader(
                    (StructuredGridCoverage2DReader) delegate, coverageName);
        } else {
            return new SingleGridCoverage2DReader(delegate, coverageName);
        }
    }

    public SingleGridCoverage2DReader(GridCoverage2DReader delegate, String coverageName) {
        if (delegate == null) {
            throw new IllegalArgumentException("The delegate coverage reader cannot be null");
        }
        this.delegate = delegate;
        if (coverageName == null) {
            throw new IllegalArgumentException("The coverage name must be specified");
        }
        this.coverageName = coverageName;
    }

    /** Checks the specified name is the one we are expecting */
    protected void checkCoverageName(String coverageName) {
        if (!this.coverageName.equals(coverageName)) {
            throw new IllegalArgumentException(
                    "Unknown coverage named "
                            + coverageName
                            + ", the only valid value is: "
                            + this.coverageName);
        }
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
        return delegate.getMetadataNames(coverageName);
    }

    @Override
    public String[] getMetadataNames(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataNames(coverageName);
    }

    @Override
    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(coverageName, name);
    }

    @Override
    public String getMetadataValue(String coverageName, String name) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataValue(coverageName, name);
    }

    @Override
    public void dispose() throws IOException {
        delegate.dispose();
    }

    @Override
    public GeneralBounds getOriginalEnvelope() {
        return delegate.getOriginalEnvelope(coverageName);
    }

    @Override
    public GeneralBounds getOriginalEnvelope(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalEnvelope(coverageName);
    }

    @Override
    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange(coverageName);
    }

    @Override
    public GridEnvelope getOriginalGridRange(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridRange(coverageName);
    }

    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return delegate.read(coverageName, parameters);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        checkCoverageName(coverageName);
        return delegate.read(coverageName, parameters);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem(this.coverageName);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getCoordinateReferenceSystem(coverageName);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters(this.coverageName);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getDynamicParameters(coverageName);
    }

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(this.coverageName, policy, requestedResolution);
    }

    @Override
    public double[] getReadingResolutions(
            String coverageName, OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    @Override
    public String[] getGridCoverageNames() throws IOException {
        return new String[] {
            coverageName
        }; // Being a singleGridCoverage reader, I can return the only coverage
    }

    @Override
    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    @Override
    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout(coverageName);
    }

    @Override
    public ImageLayout getImageLayout(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getImageLayout(coverageName);
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels(coverageName);
    }

    @Override
    public double[][] getResolutionLevels(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getResolutionLevels(coverageName);
    }

    @Override
    public DatasetLayout getDatasetLayout() {
        return delegate.getDatasetLayout();
    }

    @Override
    public DatasetLayout getDatasetLayout(String coverageName) {
        return delegate.getDatasetLayout(coverageName);
    }

    @Override
    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        return delegate.getInfo(coverageName);
    }
}
