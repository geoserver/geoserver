/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;
import javax.media.jai.operator.BandMergeDescriptor;

import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.DataSourceException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.filter.FilterFactory2;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * A {@link CoverageView} reader which takes care of doing underlying coverage read operations and recompositions.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class CoverageViewReader implements GridCoverage2DReader {

    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(CoverageViewReader.class);

    public final static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private static final CoverageProcessor PROCESSOR;

    private static Operation BANDMERGE;

    private static Operation BANDSELECT;

    static {
        PROCESSOR = CoverageProcessor.getInstance();
        try {
            BANDMERGE = PROCESSOR.getOperation("BandMergeOp");
            BANDSELECT = PROCESSOR.getOperation("SelectSampleDimension");
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("MultiInputs BandMerge operation unavailable. Band Merge will be made through standard JAI BandMerge operations:\n "
                        + e.getLocalizedMessage());
            }
            BANDMERGE = null;
            BANDSELECT = null;
        }
    }

    /**
     * A CoveragesConsistencyChecker checks if the composing coverages respect the constraints which currently are:
     * <UL>
     * <LI>same CRS</LI>
     * <LI>same resolution</LI>
     * <LI>same bbox</LI>
     * <LI>same data type</LI>
     * <LI>same dimensions (same number of dimension, same type, and same name)</LI>
     * </UL>
     */
    static class CoveragesConsistencyChecker {

        private static double DELTA = 1E-10;

        private Set<ParameterDescriptor<List>> dynamicParameters;

        private String[] metadataNames;

        private GridEnvelope gridRange;

        private GeneralEnvelope envelope;

        private CoordinateReferenceSystem crs;

        private ImageLayout layout;

        public CoveragesConsistencyChecker(GridCoverage2DReader reader) throws IOException {
            envelope = reader.getOriginalEnvelope();
            gridRange = reader.getOriginalGridRange();
            crs = reader.getCoordinateReferenceSystem();
            metadataNames = reader.getMetadataNames();
            dynamicParameters = reader.getDynamicParameters();
            layout = reader.getImageLayout();
        }

        /**
         * Check whether the coverages associated to the provided reader is consistent with the reference coverage.
         * 
         * @param reader
         * @throws IOException
         */
        public void checkConsistency(GridCoverage2DReader reader) throws IOException {
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            GridEnvelope gridRange = reader.getOriginalGridRange();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
            String[] metadataNames = reader.getMetadataNames();
            Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();

            // Checking envelope equality
            if (!envelope.equals(this.envelope, DELTA, true)) {
                throw new IllegalArgumentException("The coverage envelope must be the same");
            }

            // Checking gridRange equality
            final Rectangle thisRectangle = new Rectangle(this.gridRange.getLow(0),
                    this.gridRange.getLow(1), this.gridRange.getSpan(0), this.gridRange.getSpan(1));
            final Rectangle thatRectangle = new Rectangle(gridRange.getLow(0), gridRange.getLow(1),
                    gridRange.getSpan(0), gridRange.getSpan(1));
            if (!thisRectangle.equals(thatRectangle)) {
                throw new IllegalArgumentException("The coverage gridRange should be the same");
            }

            // Checking dimensions
            if (metadataNames.length != this.metadataNames.length) {
                throw new IllegalArgumentException(
                        "The coverage metadataNames should have the same size");
            }

            final Set<String> metadataSet = new HashSet<String>(Arrays.asList(metadataNames));
            for (String metadataName : this.metadataNames) {
                if (!metadataSet.contains(metadataName)) {
                    throw new IllegalArgumentException("The coverage metadata are different");
                }
            }

            // TODO: Add check for dynamic parameters

            // Checking CRS
            MathTransform destinationToSourceTransform = null;
            if (!CRS.equalsIgnoreMetadata(crs, this.crs)) {
                try {
                    destinationToSourceTransform = CRS.findMathTransform(crs, this.crs, true);
                } catch (FactoryException e) {
                    throw new DataSourceException("Unable to inspect request CRS", e);
                }
            }

            // now transform the requested envelope to source crs
            if (destinationToSourceTransform != null && !destinationToSourceTransform.isIdentity()) {
                throw new IllegalArgumentException(
                        "The coverage coordinateReferenceSystem should be the same");
            }

            // Checking data type
            if (layout.getSampleModel(null).getDataType() != this.layout.getSampleModel(null)
                    .getDataType()) {
                throw new IllegalArgumentException("The coverage dataType should be the same");
            }

        }
    }

    /**
     * A simple reader which will apply coverages band customizations to the {@link CoverageView}
     */
    static class CoverageDimensionCustomizerViewReader extends CoverageDimensionCustomizerReader {

        public CoverageDimensionCustomizerViewReader(GridCoverage2DReader delegate,
                String coverageName, CoverageInfo info) {
            super(delegate, coverageName, info);
        }

        protected GridSampleDimension[] wrapDimensions(SampleDimension[] dims) {
            GridSampleDimension[] wrappedDims = null;
            CoverageInfo info = getInfo();
            if (info != null) {
                List<CoverageDimensionInfo> storedDimensions = info.getDimensions();
                MetadataMap map = info.getMetadata();
                if (map.containsKey(CoverageView.COVERAGE_VIEW)) {
                    CoverageView coverageView = (CoverageView) map
                            .get(CoverageView.COVERAGE_VIEW);
                    List<CoverageBand> coverageBands = coverageView.getBands(getCoverageName());
                    wrappedDims = (coverageBands != null && !coverageBands.isEmpty()) ? new GridSampleDimension[coverageBands.size()] : null;
                    int i = 0;
                    for (CoverageBand band : coverageBands) {
                        if (storedDimensions != null && storedDimensions.size() > 0) {
                            CoverageDimensionInfo dimensionInfo = storedDimensions.get(band.getIndex());
                            wrappedDims[i] = new WrappedSampleDimension((GridSampleDimension) dims[i],
                                    dimensionInfo);
                        } else {
                            CoverageDimensionInfo dimensionInfo = new CoverageDimensionImpl();
                            dimensionInfo.setName(band.getDefinition());
                            wrappedDims[i] = new WrappedSampleDimension((GridSampleDimension) dims[i],
                                    dimensionInfo);
                        }
                        i++;
                    }
                } else {
                    super.wrapDimensions(wrappedDims);
                }
            }
            return wrappedDims;
        }
    }

    static class CoverageDimensionCustomizerViewStructuredReader extends
            CoverageDimensionCustomizerViewReader {

        public CoverageDimensionCustomizerViewStructuredReader(GridCoverage2DReader delegate,
                String coverageName, CoverageInfo info) {
            super(delegate, coverageName, info);
        }

    }

    /** The CoverageView containing definition */
    CoverageView coverageView;

    /** The name of the reference coverage, we can remove/revisit it once we relax some constraint */
    String referenceName;

    private String coverageName;

    private GridCoverage2DReader delegate;

    private Hints hints;

    /** The CoverageInfo associated to the CoverageView */
    private CoverageInfo coverageInfo;

    private GridCoverageFactory coverageFactory;

    public CoverageViewReader(GridCoverage2DReader delegate, CoverageView coverageView,
            CoverageInfo coverageInfo, Hints hints) {
        this.coverageName = coverageView.getName();
        this.delegate = delegate;
        this.coverageView = coverageView;
        this.coverageInfo = coverageInfo;
        this.hints = hints;
        // Refactor this once supporting heterogeneous elements
        referenceName = coverageView.getBand(0).getInputCoverageBands().get(0).getCoverageName();
        if (this.hints != null && this.hints.containsKey(Hints.GRID_COVERAGE_FACTORY)) {
            final Object factory = this.hints.get(Hints.GRID_COVERAGE_FACTORY);
            if (factory != null && factory instanceof GridCoverageFactory) {
                this.coverageFactory = (GridCoverageFactory) factory;
            }
        }
        if (this.coverageFactory == null) {
            this.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);
        }
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException,
            IOException {

        List<CoverageBand> bands = coverageView.getCoverageBands();
        List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
        List<SampleDimension> dims = new ArrayList<SampleDimension>();

        // Use composition rule specific implementation
        CoveragesConsistencyChecker checker = null;
        for (CoverageBand band : bands) {
            // Refactor this once supporting complex compositions
            final InputCoverageBand inputBand = band.getInputCoverageBands().get(0); 
            final String coverageName = inputBand.getCoverageName();
            final GridCoverage2DReader reader = wrap(delegate, coverageName, coverageInfo);

            // Remove this when removing constraints
            if (checker == null) {
                checker = new CoveragesConsistencyChecker(reader);
            } else {
                checker.checkConsistency(reader);
            }

            GridCoverage2D coverage = (GridCoverage2D) reader.read(parameters);
            if (coverage != null) {

                // We may consider revisiting this to use integers instead of String
                // For the moment, let's continue using String
                String selectedBand = inputBand.getBand();
                if (BANDSELECT != null) {
                    final ParameterValueGroup param = BANDSELECT.getParameters();
                    param.parameter("Source").setValue(coverage);
                    param.parameter("SampleDimensions").setValue(new int[]{Integer.valueOf(selectedBand)});
                    coverage = (GridCoverage2D) PROCESSOR.doOperation(param, hints);
                }

                coverages.add(coverage);
                dims.addAll(Arrays.asList(coverage.getSampleDimensions()));
            }
        }

        if (coverages.isEmpty()) {
            return null;
        }
        GridCoverage2D sampleCoverage = coverages.get(0);

        RenderedImage image = null;
        if (coverages.size() > 1) {
            if (BANDMERGE != null) {
                final ParameterValueGroup param = BANDMERGE.getParameters();
                param.parameter("sources").setValue(coverages);
                GridCoverage2D merge = (GridCoverage2D) PROCESSOR.doOperation(param, hints);
                image = merge.getRenderedImage();

            } else {
                final int coveragesSize = coverages.size();
                image = sampleCoverage.getRenderedImage();
                for (int i = 1; i < coveragesSize; i++) {
                    image = BandMergeDescriptor.create(image, coverages.get(i).getRenderedImage(),
                            hints);
                }
            }
        } else {
            image = sampleCoverage.getRenderedImage();
        }
        return coverageFactory.create(coverageInfo.getName(), image,
                sampleCoverage.getGridGeometry(),
                dims.toArray(new GridSampleDimension[dims.size()]), null, /* props */null);
    }


    /**
     * @param coverageName
     */
    protected void checkCoverageName(String coverageName) {
        if (!this.coverageName.equalsIgnoreCase(coverageName)) {
            throw new IllegalArgumentException("The specified coverageName isn't the one of this coverageView");
        }
    }

    @Override
    public void dispose() throws IOException {
        delegate.dispose();
    }

    /**
     * Get a {@link GridCoverage2DReader} wrapping the provided delegate reader
     */
    private static GridCoverage2DReader wrap(GridCoverage2DReader delegate, String coverageName,
            CoverageInfo info) {
        GridCoverage2DReader reader = delegate;
        if (coverageName != null) {
            reader = SingleGridCoverage2DReader.wrap(delegate, coverageName);
        }
        if (reader instanceof StructuredGridCoverage2DReader) {
            return new CoverageDimensionCustomizerViewStructuredReader(
                    (StructuredGridCoverage2DReader) reader, coverageName, info);
        } else {
            return new CoverageDimensionCustomizerViewReader(reader, coverageName, info);
        }
    }

    /**
     * Get a {@link GridCoverage2DReader} wrapping the provided delegate reader
     */
    public static GridCoverage2DReader wrap(GridCoverage2DReader reader, CoverageView coverageView,
            CoverageInfo coverageInfo, Hints hints) {
        if (reader instanceof StructuredGridCoverage2DReader) {
            return new StructuredCoverageViewReader((StructuredGridCoverage2DReader) reader,
                    coverageView, coverageInfo, hints);
        } else {
            return new CoverageViewReader((GridCoverage2DReader) reader, coverageView,
                    coverageInfo, hints);
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
    public String[] getMetadataNames(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataNames(referenceName);
    }

    @Override
    public String getMetadataValue(String coverageName, String name) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataValue(referenceName, name);
    }

    @Override
    public String[] listSubNames() throws IOException {
        return delegate.listSubNames();
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
    public String getCurrentSubname() throws IOException {
        return delegate.getCurrentSubname();
    }

    @Override
    public boolean hasMoreGridCoverages() throws IOException {
        return delegate.hasMoreGridCoverages();
    }

    @Override
    public void skip() throws IOException {
        delegate.skip();
        
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalEnvelope(referenceName);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getCoordinateReferenceSystem(referenceName);
    }

    @Override
    public GridEnvelope getOriginalGridRange(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridRange(referenceName);
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridToWorld(referenceName, pixInCell);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IOException {
        checkCoverageName(coverageName);
        return read(parameters);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getDynamicParameters(referenceName);
    }

    @Override
    public double[] getReadingResolutions(String coverageName, OverviewPolicy policy,
            double[] requestedResolution) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getReadingResolutions(referenceName, policy, requestedResolution);
    }

    @Override
    public int getNumOverviews(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getNumOverviews(referenceName);
    }

    @Override
    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout(referenceName);
    }

    @Override
    public ImageLayout getImageLayout(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getImageLayout(referenceName);
    }

    @Override
    public double[][] getResolutionLevels(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getResolutionLevels(referenceName);
    }

    @Override
    public String[] getMetadataNames() throws IOException {
        return delegate.getMetadataNames(referenceName);
    }

    @Override
    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(referenceName, name);
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope() {
        return delegate.getOriginalEnvelope(referenceName);
    }

    @Override
    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange(referenceName);
    }

    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(referenceName, pixInCell);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem(referenceName);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters(referenceName);
    }

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(referenceName, policy, requestedResolution);
    }

    @Override
    public int getNumOverviews() {
        return delegate.getNumOverviews(referenceName);
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels(referenceName);
    }

}
