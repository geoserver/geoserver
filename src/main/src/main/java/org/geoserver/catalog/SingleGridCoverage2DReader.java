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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

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
            return new SingleGridCoverage2DReader((GridCoverage2DReader) delegate, coverageName);
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

    public Format getFormat() {
        return delegate.getFormat();
    }

    public Object getSource() {
        return delegate.getSource();
    }

    public String[] getMetadataNames() throws IOException {
        return delegate.getMetadataNames(coverageName);
    }

    public String[] getMetadataNames(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataNames(coverageName);
    }

    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(coverageName, name);
    }

    public String getMetadataValue(String coverageName, String name) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataValue(coverageName, name);
    }

    public void dispose() throws IOException {
        delegate.dispose();
    }

    public GeneralEnvelope getOriginalEnvelope() {
        return delegate.getOriginalEnvelope(coverageName);
    }

    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalEnvelope(coverageName);
    }

    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange(coverageName);
    }

    public GridEnvelope getOriginalGridRange(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridRange(coverageName);
    }

    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return delegate.read(coverageName, parameters);
    }

    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        checkCoverageName(coverageName);
        return delegate.read(coverageName, parameters);
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem(this.coverageName);
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getCoordinateReferenceSystem(coverageName);
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters(this.coverageName);
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getDynamicParameters(coverageName);
    }

    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(this.coverageName, policy, requestedResolution);
    }

    public double[] getReadingResolutions(
            String coverageName, OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    public String[] getGridCoverageNames() throws IOException {
        return new String[] {
            coverageName
        }; // Being a singleGridCoverage reader, I can return the only coverage
    }

    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout(coverageName);
    }

    public ImageLayout getImageLayout(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getImageLayout(coverageName);
    }

    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels(coverageName);
    }

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
