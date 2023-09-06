/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import it.geosolutions.imageio.maskband.DatasetLayout;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.media.jai.ImageLayout;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.simple.SimpleFeature;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;

/**
 * Provides a view of a single granule to the DescribeCoverage encoder (to be used in
 * DescribeOECoverageSet response)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SingleGranuleGridCoverageReader implements StructuredGridCoverage2DReader {

    private StructuredGridCoverage2DReader reader;

    private SimpleFeature feature;

    GeneralBounds granuleEnvelope;

    ISO8601Formatter formatter = new ISO8601Formatter();

    Map<String, DimensionDescriptor> dimensionDescriptors;

    public SingleGranuleGridCoverageReader(
            StructuredGridCoverage2DReader reader,
            SimpleFeature feature,
            List<DimensionDescriptor> dimensionDescriptors) {
        this.reader = reader;
        this.feature = feature;
        this.dimensionDescriptors = new HashMap<>();
        for (DimensionDescriptor descriptor : dimensionDescriptors) {
            this.dimensionDescriptors.put(descriptor.getName().toUpperCase(), descriptor);
        }
        Geometry featureGeometry = lookupFeatureGeometry();
        ReferencedEnvelope re =
                new ReferencedEnvelope(
                        featureGeometry.getEnvelopeInternal(),
                        reader.getCoordinateReferenceSystem());
        this.granuleEnvelope = new GeneralBounds(re);
    }

    private Geometry lookupFeatureGeometry() {
        return (Geometry) feature.getDefaultGeometry();
    }

    @Override
    public Format getFormat() {
        return reader.getFormat();
    }

    @Override
    public Object getSource() {
        return reader.getSource();
    }

    @Override
    public String[] getMetadataNames() throws IOException {
        return reader.getMetadataNames();
    }

    @Override
    public GranuleSource getGranules(String coverageName, boolean readOnly)
            throws IOException, UnsupportedOperationException {
        return reader.getGranules(coverageName, readOnly);
    }

    @Override
    public String[] getMetadataNames(String coverageName) throws IOException {
        return reader.getMetadataNames(coverageName);
    }

    @Override
    public boolean isReadOnly() {
        return reader.isReadOnly();
    }

    @Override
    public void createCoverage(String coverageName, SimpleFeatureType schema)
            throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeCoverage(String coverageName, boolean delete)
            throws IOException, UnsupportedOperationException {
        return reader.removeCoverage(coverageName, delete);
    }

    @Override
    public String getMetadataValue(String name) throws IOException {
        if (name.endsWith("_DOMAIN_MINIMUM")
                || name.endsWith("_DOMAIN_MAXIMUM")
                || name.endsWith("_DOMAIN")) {
            String dimensionName = name.substring(0, name.indexOf("_DOMAIN"));
            DimensionDescriptor descriptor = dimensionDescriptors.get(dimensionName);
            if (descriptor != null) {
                Object start = feature.getAttribute(descriptor.getStartAttribute());
                Object end = null;
                if (descriptor.getEndAttribute() != null) {
                    end = feature.getAttribute(descriptor.getEndAttribute());
                }
                if (dimensionName.equalsIgnoreCase("TIME")) {
                    start = formatter.format((Date) start);
                    if (end != null) {
                        end = formatter.format((Date) end);
                    }
                }

                if (name.endsWith("_DOMAIN_MINIMUM")) {
                    return String.valueOf(start);
                }
                if (name.endsWith("_DOMAIN_MAXIMUM")) {
                    if (end != null) {
                        return String.valueOf(end);
                    } else {
                        return String.valueOf(start);
                    }
                }
                if (name.endsWith("_DOMAIN")) {
                    if (end != null) {
                        return start + "/" + end;
                    } else {
                        return String.valueOf(start);
                    }
                }
            }
        }

        return reader.getMetadataValue(name);
    }

    @Override
    public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source, Hints hints)
            throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMetadataValue(String coverageName, String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeneralBounds getOriginalEnvelope() {
        return granuleEnvelope;
    }

    @Override
    public GeneralBounds getOriginalEnvelope(String coverageName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getGridCoverageNames() throws IOException {
        return reader.getGridCoverageNames();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return reader.getCoordinateReferenceSystem();
    }

    @Override
    public int getGridCoverageCount() throws IOException {
        return reader.getGridCoverageCount();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        return reader.getCoordinateReferenceSystem(coverageName);
    }

    @Override
    public GridEnvelope getOriginalGridRange() {
        return reader.getOriginalGridRange();
    }

    @Override
    public GridEnvelope getOriginalGridRange(String coverageName) {
        return reader.getOriginalGridRange(coverageName);
    }

    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return reader.getOriginalGridToWorld(pixInCell);
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        return reader.getOriginalGridToWorld(coverageName, pixInCell);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        return reader.read(parameters);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IOException {
        return reader.read(coverageName, parameters);
    }

    @Override
    public void dispose() throws IOException {
        reader.dispose();
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return reader.getDynamicParameters();
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        return reader.getDynamicParameters(coverageName);
    }

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return reader.getReadingResolutions(policy, requestedResolution);
    }

    @Override
    public double[] getReadingResolutions(
            String coverageName, OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return reader.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    @Override
    public ImageLayout getImageLayout() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImageLayout getImageLayout(String coverageName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return reader.getResolutionLevels();
    }

    @Override
    public double[][] getResolutionLevels(String coverageName) throws IOException {
        return reader.getResolutionLevels(coverageName);
    }

    @Override
    public List<DimensionDescriptor> getDimensionDescriptors(String coverageName)
            throws IOException {
        return reader.getDimensionDescriptors(coverageName);
    }

    @Override
    public void delete(boolean deleteData) throws IOException, UnsupportedOperationException {
        reader.delete(deleteData);
    }

    @Override
    public DatasetLayout getDatasetLayout() {
        return reader.getDatasetLayout();
    }

    @Override
    public DatasetLayout getDatasetLayout(String coverageName) {
        return reader.getDatasetLayout(coverageName);
    }

    @Override
    public ServiceInfo getInfo() {
        return reader.getInfo();
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        return reader.getInfo(coverageName);
    }
}
