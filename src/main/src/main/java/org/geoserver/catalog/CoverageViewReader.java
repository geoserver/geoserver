/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import com.sun.media.imageioimpl.common.BogusColorSpace;
import it.geosolutions.imageio.maskband.DatasetLayout;
import it.geosolutions.imageio.utilities.ImageIOUtilities;
import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.utilities.ImageLayout2;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ColorModelFactory;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.RasterFactory;
import javax.media.jai.operator.ConstantDescriptor;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.EnvelopeCompositionType;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageViewHandler.CoveragesConsistencyChecker;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A {@link CoverageView} reader which takes care of doing underlying coverage read operations and
 * recompositions.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class CoverageViewReader implements GridCoverage2DReader {

    private static final int HETEROGENEOUS_RASTER_GUTTER = 10;

    public static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();

    private static final Logger LOGGER = Logging.getLogger(CoverageViewReader.class);

    /** The CoverageView containing definition */
    CoverageView coverageView;

    private CoverageViewHandler handler;

    boolean canSupportHeterogeneousCoverages = false;

    /**
     * The name of the reference coverage, we can remove/revisit it once we relax some constraint
     */
    String referenceName;

    private String coverageName;

    private GridCoverage2DReader delegate;

    private Hints hints;

    private GridCoverageFactory coverageFactory;

    private ImageLayout imageLayout;

    public CoverageViewReader(
            GridCoverage2DReader delegate,
            CoverageView coverageView,
            CoverageInfo coverageInfo,
            Hints hints) {
        this.coverageName = coverageView.getName();
        this.delegate = delegate;
        this.coverageView = coverageView;
        this.hints = hints;
        referenceName = coverageView.getBand(0).getInputCoverageBands().get(0).getCoverageName();
        canSupportHeterogeneousCoverages = JAIExt.isJAIExtOperation("BandMerge");

        this.handler =
                new CoverageViewHandler(
                        canSupportHeterogeneousCoverages, delegate, referenceName, coverageView);

        if (this.hints != null && this.hints.containsKey(Hints.GRID_COVERAGE_FACTORY)) {
            final Object factory = this.hints.get(Hints.GRID_COVERAGE_FACTORY);
            if (factory != null && factory instanceof GridCoverageFactory) {
                this.coverageFactory = (GridCoverageFactory) factory;
            }
        }
        if (this.coverageFactory == null) {
            this.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);
        }
        ImageLayout layout;
        try {
            layout = delegate.getImageLayout(referenceName);
            SampleModel originalSampleModel = layout.getSampleModel(null);
            SampleModel sampleModel =
                    RasterFactory.createBandedSampleModel(
                            originalSampleModel.getDataType(),
                            originalSampleModel.getWidth(),
                            originalSampleModel.getHeight(),
                            coverageView.getCoverageBands().size());

            ColorModel colorModel = ImageIOUtilities.createColorModel(sampleModel);
            this.imageLayout =
                    new ImageLayout2(
                            layout.getMinX(null),
                            layout.getMinY(null),
                            originalSampleModel.getWidth(),
                            originalSampleModel.getHeight());
            imageLayout.setSampleModel(sampleModel);
            imageLayout.setColorModel(colorModel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {

        // did we get a request within the bounds of the coverage?
        Optional<GeneralParameterValue> ggParameter = Optional.empty();
        if (parameters != null) {
            ggParameter =
                    Arrays.stream(parameters)
                            .filter(
                                    parameter ->
                                            parameter
                                                    .getDescriptor()
                                                    .getName()
                                                    .equals(
                                                            AbstractGridFormat.READ_GRIDGEOMETRY2D
                                                                    .getName()))
                            .findFirst();
        }
        GridGeometry2D requestedGridGeometry = null;
        if (ggParameter.isPresent()) {
            ParameterValue value = (ParameterValue) ggParameter.get();
            requestedGridGeometry = (GridGeometry2D) value.getValue();
            ReferencedEnvelope requestedEnvelope =
                    ReferencedEnvelope.reference(requestedGridGeometry.getEnvelope());
            ReferencedEnvelope dataEnvelope =
                    ReferencedEnvelope.reference(handler.getOriginalEnvelope());
            if (CRS.equalsIgnoreMetadata(requestedEnvelope, dataEnvelope)) {
                if (!requestedEnvelope.intersects((BoundingBox) dataEnvelope)) {
                    return null;
                }
            } else {
                ReferencedEnvelope re84;
                try {
                    re84 = requestedEnvelope.transform(DefaultGeographicCRS.WGS84, true);
                    ReferencedEnvelope de84 =
                            dataEnvelope.transform(DefaultGeographicCRS.WGS84, true);
                    if (!re84.intersects((BoundingBox) de84)) {
                        return null;
                    }
                } catch (TransformException | FactoryException e) {
                    LOGGER.log(
                            Level.FINE,
                            "Cannot determine if the requested BBOX intersects the "
                                    + "data one, continuing",
                            e);
                }
            }

            // expand the read area if we are in the heterogeneous case... it's a resampling
            // one similar to reprojection, prone to off-by-one issues at the borders
            if (!handler.isHomogeneousCoverages()) {
                GridEnvelope2D range = requestedGridGeometry.getGridRange2D();
                GridEnvelope2D expandedRange =
                        new GridEnvelope2D(
                                (int) range.getMinX() - HETEROGENEOUS_RASTER_GUTTER,
                                (int) range.getMinY() - HETEROGENEOUS_RASTER_GUTTER,
                                (int) range.getWidth() + HETEROGENEOUS_RASTER_GUTTER * 2,
                                (int) range.getHeight() + HETEROGENEOUS_RASTER_GUTTER * 2);
                GridGeometry2D expandedGG =
                        new GridGeometry2D(
                                expandedRange,
                                requestedGridGeometry.getGridToCRS(),
                                requestedGridGeometry.getCoordinateReferenceSystem());
                value.setValue(expandedGG);
            }
        }

        List<CoverageBand> bands = coverageView.getCoverageBands();
        List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
        CoveragesConsistencyChecker checker = null;

        int coverageBandsSize = bands.size();

        // Check params, populate band indices to read if BANDS param has been defined
        ArrayList<Integer> selectedBandIndices = new ArrayList<Integer>();
        for (int m = 0; m < coverageBandsSize; m++) {
            selectedBandIndices.add(m);
        }

        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                final ParameterValue param = (ParameterValue) parameters[i];
                if (AbstractGridFormat.BANDS.getName().equals(param.getDescriptor().getName())) {
                    int[] bandIndicesParam = (int[]) param.getValue();
                    if (bandIndicesParam != null) {
                        selectedBandIndices = new ArrayList<Integer>();
                        for (int bIdx = 0; bIdx < bandIndicesParam.length; bIdx++) {
                            selectedBandIndices.add(bandIndicesParam[bIdx]);
                        }
                        break;
                    }
                }
            }
        }

        // Since composition of a raster band using a formula applied on individual bands has not
        // been implemented, the normal case is that each CoverageBand is in fact a single band from
        // an input coverage. When band composition will be implemented, this will be the point
        // where
        // band composition will occur, before the final BandSelect/BandMerge ops

        // This is a good spot to read coverages. Reading a coverage is done only once, it is
        // cached to be used for its other bands that possibly take part in the CoverageView
        // definition
        HashMap<String, GridCoverage2D> inputCoverages = new HashMap<String, GridCoverage2D>();
        GridCoverage2D dynamicAlphaSource = null;
        int nonNullCoverages = 0;
        for (int bIdx : selectedBandIndices) {
            CoverageBand band = bands.get(bIdx);
            List<InputCoverageBand> selectedBands = band.getInputCoverageBands();

            // Peek for coverage name
            String coverageName = selectedBands.get(0).getCoverageName();
            if (!inputCoverages.containsKey(coverageName)) {
                GridCoverage2DReader reader =
                        SingleGridCoverage2DReader.wrap(delegate, coverageName);
                // Remove this when removing constraints
                if (checker == null) {
                    checker =
                            new CoveragesConsistencyChecker(
                                    reader, canSupportHeterogeneousCoverages);
                } else {
                    checker.checkConsistency(reader);
                }
                // bands selection parameter inside on final bands so they should not be propagated
                // to the delegate reader
                GeneralParameterValue[] filteredParameters = parameters;
                if (parameters != null) {
                    // creating a copy of parameters excluding the bands parameter
                    filteredParameters =
                            Arrays.stream(parameters)
                                    .filter(
                                            parameter ->
                                                    !parameter
                                                            .getDescriptor()
                                                            .getName()
                                                            .equals(
                                                                    AbstractGridFormat.BANDS
                                                                            .getName()))
                                    .toArray(GeneralParameterValue[]::new);
                }
                GridCoverage2D coverage = reader.read(filteredParameters);
                if (coverage == null) {
                    if (handler.isHomogeneousCoverages()
                            || handler.getEnvelopeCompositionType()
                                    == EnvelopeCompositionType.INTERSECTION) {
                        return null;
                    }
                } else {
                    nonNullCoverages++;
                }
                if (dynamicAlphaSource == null && hasDynamicAlpha(coverage, reader)) {
                    dynamicAlphaSource = coverage;
                }
                inputCoverages.put(coverageName, coverage);
            }
        }

        // all readers returned null?
        if (nonNullCoverages == 0 || inputCoverages.isEmpty()) {
            return null;
        }

        // some returned null?
        if (nonNullCoverages < inputCoverages.size()) {
            float width, height;
            if (requestedGridGeometry != null) {
                GridEnvelope2D range = requestedGridGeometry.getGridRange2D();
                width = (float) range.getWidth();
                height = (float) range.getHeight();
            } else {
                GridCoverage2D reference =
                        inputCoverages.values().stream().filter(c -> c != null).findFirst().get();
                RenderedImage ri = reference.getRenderedImage();
                width = ri.getWidth();
                height = ri.getHeight();
            }

            // build empty coverages for the missing bits
            for (String name : inputCoverages.keySet()) {
                GridCoverage2DReader reader = SingleGridCoverage2DReader.wrap(delegate, name);
                ImageLayout layout = reader.getImageLayout();
                int numBands = layout.getSampleModel(null).getNumBands();
                Number[] bandValues = new Number[numBands]; // all zeroes
                Arrays.fill(bandValues, Double.valueOf(0));
                ConstantDescriptor.create(
                        width,
                        height,
                        bandValues,
                        new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout));
            }
        }

        // Group together bands that come from the same coverage
        ArrayList<CoverageBand> mergedBands = new ArrayList<CoverageBand>();

        int idx = 0;
        CoverageBand mBand = null;
        while (idx < selectedBandIndices.size()) {

            if (mBand == null) {
                // Create a temporary CoverageBand, to use later for SelectSampleDimension
                // operations
                mBand = new CoverageBand();
                mBand.setInputCoverageBands(
                        bands.get(selectedBandIndices.get(idx)).getInputCoverageBands());
            }

            // peek to the next band. Is it from the same coverage?
            String coverageName =
                    bands.get(selectedBandIndices.get(idx))
                            .getInputCoverageBands()
                            .get(0)
                            .getCoverageName();

            if (idx + 1 < selectedBandIndices.size()
                    && bands.get(selectedBandIndices.get(idx + 1))
                            .getInputCoverageBands()
                            .get(0)
                            .getCoverageName()
                            .equals(coverageName)) {
                // Same coverage, add its bands to the previous
                ArrayList<InputCoverageBand> groupBands = new ArrayList<InputCoverageBand>();
                groupBands.addAll(mBand.getInputCoverageBands());
                groupBands.addAll(
                        bands.get(selectedBandIndices.get(idx + 1)).getInputCoverageBands());
                mBand.setInputCoverageBands(groupBands);
            } else {
                mergedBands.add(mBand);
                mBand = null;
            }
            idx++;
        }

        // perform the band selects as needed
        int index = 0;
        int transformationChoice = index;
        CoverageViewHandler.CoverageResolutionChooser resolutionChooser =
                handler.getCoverageResolutionChooser();
        for (CoverageBand band : mergedBands) {
            List<InputCoverageBand> selectedBands = band.getInputCoverageBands();

            // Peek for coverage name
            String coverageName = selectedBands.get(0).getCoverageName();

            // Get band indices for band selection
            ArrayList<Integer> bandIndices = new ArrayList<Integer>(selectedBands.size());
            for (InputCoverageBand icb : selectedBands) {
                int bandIdx = 0;
                final String bandString = icb.getBand();
                if (bandString != null && !bandString.isEmpty()) {
                    bandIdx = Integer.parseInt(bandString);
                }
                bandIndices.add(bandIdx);
            }

            GridCoverage2D coverage = inputCoverages.get(coverageName);

            // special case for dynamic alpha on single input, no need to actually select away the
            // alpha
            Hints localHints = new Hints(hints);
            if (dynamicAlphaSource != null
                    && mergedBands.size() == 1
                    && (bandIndices.size() == 1 || bandIndices.size() == 3)) {
                final int alphaBandIndex = getAlphaBandIndex(coverage);
                addAlphaColorModelHint(localHints, bandIndices.size());
                bandIndices.add(alphaBandIndex);
            }

            coverage = retainBands(bandIndices, coverage, localHints);
            if (mergedBands.size() > 1) {
                coverage = prepareForBandMerge(coverage);
            }
            coverages.add(coverage);
            if (resolutionChooser.visit(coverage)) {
                transformationChoice = index;
            }
            index++;

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Read coverage "
                                + coverageName
                                + ", result has envelope "
                                + coverage.getEnvelope2D());
            }
        }

        GridCoverage2D result;
        if (coverages.size() > 1) {
            // dynamic alpha but more than one source
            Hints localHints = new Hints(hints);
            if (dynamicAlphaSource != null) {
                int currentBandCount = countBands(coverages);
                // and the output is suitable for getting an alpha band
                if (currentBandCount == 1 || currentBandCount == 3) {
                    final int alphaBandIndex = getAlphaBandIndex(dynamicAlphaSource);
                    GridCoverage2D alphaBandCoverage =
                            retainBands(Arrays.asList(alphaBandIndex), dynamicAlphaSource, hints);
                    coverages.add(alphaBandCoverage);

                    addAlphaColorModelHint(localHints, currentBandCount);
                }
            }

            // perform final band merge
            String operationName = "BandMerge";
            final ParameterValueGroup param = PROCESSOR.getOperation(operationName).getParameters();
            if (!handler.isHomogeneousCoverages()) {
                param.parameter("transform_choice").setValue("index");
                param.parameter("coverage_idx").setValue(transformationChoice);
            }
            param.parameter("sources").setValue(coverages);
            localHints.put(
                    JAI.KEY_COLOR_MODEL_FACTORY,
                    new ColorModelFactory() {
                        @Override
                        public ColorModel createColorModel(
                                SampleModel sampleModel, List sources, Map configuration) {
                            final int dataType = sampleModel.getDataType();
                            final int numBands = sampleModel.getNumBands();

                            ColorSpace cs;
                            switch (numBands) {
                                case 1:
                                case 2:
                                    cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                                    break;
                                case 3:
                                    cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                                    break;
                                default:
                                    cs = new BogusColorSpace(numBands);
                            }

                            return RasterFactory.createComponentColorModel(
                                    dataType, cs, false, false, Transparency.OPAQUE);
                        }
                    });
            result = (GridCoverage2D) PROCESSOR.doOperation(param, localHints);
        } else {
            // optimize out, no need to do a band merge
            result = coverages.get(0);
        }

        return result;
    }

    /**
     * The BandMerge operation takes indexed images and expands them, however in the context of
     * coverage view band merging we don't normally want that, e.g., raster mask bands are
     * represented as indexed but we really want to keep them in their binary, single band form. To
     * do so, the IndexColorModel is replaced by a ComponentColorModel
     */
    private GridCoverage2D prepareForBandMerge(GridCoverage2D coverage) {
        RenderedImage ri = coverage.getRenderedImage();
        SampleModel sampleModel = ri.getSampleModel();
        if (sampleModel.getNumBands() == 1 && ri.getColorModel() instanceof IndexColorModel) {
            ImageWorker worker = new ImageWorker(ri);
            worker.removeIndexColorModel();
            RenderedImage formatted = worker.getRenderedImage();

            return new GridCoverageFactory()
                    .create(
                            coverage.getName(),
                            formatted,
                            coverage.getGridGeometry(),
                            coverage.getSampleDimensions(),
                            new GridCoverage[] {coverage},
                            coverage.getProperties());
        }

        return coverage;
    }

    private void addAlphaColorModelHint(Hints localHints, int currentBandCount) {
        ImageLayout layout = new ImageLayout();
        ColorModel alphaModel = getColorModelWithAlpha(currentBandCount);
        layout.setColorModel(alphaModel);
        localHints.put(JAI.KEY_IMAGE_LAYOUT, layout);
    }

    private ColorModel getColorModelWithAlpha(int currentBandCount) {
        if (currentBandCount == 3) {
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            int[] nBits = {8, 8, 8, 8};
            return new ComponentColorModel(
                    cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        } else if (currentBandCount == 1) {
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {8, 8};
            return new ComponentColorModel(
                    cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        } else {
            throw new IllegalArgumentException(
                    "Cannot create a color model with alpha"
                            + "support starting with "
                            + currentBandCount
                            + " bands");
        }
    }

    private int countBands(List<GridCoverage2D> coverages) {
        int count = 0;
        for (GridCoverage2D coverage : coverages) {
            count += coverage.getRenderedImage().getSampleModel().getNumBands();
        }
        return count;
    }

    private int getAlphaBandIndex(GridCoverage2D coverage) {
        final ColorModel cm = coverage.getRenderedImage().getColorModel();
        if (!cm.hasAlpha() || cm.getNumComponents() == cm.getNumColorComponents()) {
            throw new IllegalArgumentException(
                    "The source coverage does not have an alpha band, cannot extract an "
                            + "alpha band");
        }
        // the alpha band is always the last (see ComponentColorModel.getAlphaRaster or the
        // getAlpha(object) code
        if (cm.getNumColorComponents() == 1) {
            // gray-alpha
            return 1;
        } else {
            // rgba/argb
            return 3;
        }
    }

    private GridCoverage2D retainBands(
            List<Integer> bandIndices, GridCoverage2D coverage, Hints hints) {
        final ParameterValueGroup param =
                PROCESSOR.getOperation("SelectSampleDimension").getParameters();
        param.parameter("Source").setValue(coverage);
        final int[] sampleDimensionArray =
                ArrayUtils.toPrimitive(bandIndices.toArray(new Integer[bandIndices.size()]));
        param.parameter("SampleDimensions").setValue(sampleDimensionArray);
        coverage = (GridCoverage2D) PROCESSOR.doOperation(param, hints);
        return coverage;
    }

    /**
     * Checks if a reader added a alpha channel on the fly as a result of a read parameter. We want
     * to preserve this alpha channel because the user never got a chance to select its presence in
     * the output (e.g. footprint management in mosaic)
     */
    private boolean hasDynamicAlpha(GridCoverage2D coverage, GridCoverage2DReader reader)
            throws IOException {
        // check if we have an alpha band in the coverage to stat with
        if (coverage == null) {
            return false;
        }
        ColorModel dynamicCm = coverage.getRenderedImage().getColorModel();
        if (!dynamicCm.hasAlpha() || !hasAlphaBand(dynamicCm)) {
            return false;
        }

        // check if we did not have one in the original layout
        ImageLayout readerLayout = reader.getImageLayout();
        if (readerLayout == null) {
            return false;
        }
        ColorModel nativeCm = readerLayout.getColorModel(null);
        if (nativeCm == null || nativeCm.hasAlpha()) {
            return false;
        }

        // the coverage has an alpha band, but the original reader does not advertise one?
        return !hasAlphaBand(nativeCm);
    }

    private boolean hasAlphaBand(ColorModel cm) {
        // num components returns the alpha, num _color_ components does not
        return (cm.getNumComponents() == 2 && cm.getNumColorComponents() == 1) /* gray-alpha case */
                || (cm.getNumComponents() == 4 && cm.getNumColorComponents() == 3) /* rgba case */;
    }

    /** @param coverageName */
    protected void checkCoverageName(String coverageName) {
        if (!this.coverageName.equalsIgnoreCase(coverageName)) {
            throw new IllegalArgumentException(
                    "The specified coverageName isn't the one of this coverageView");
        }
    }

    public void dispose() throws IOException {
        delegate.dispose();
    }

    /** Get a {@link GridCoverage2DReader} wrapping the provided delegate reader */
    public static GridCoverage2DReader wrap(
            GridCoverage2DReader reader,
            CoverageView coverageView,
            CoverageInfo coverageInfo,
            Hints hints) {
        if (reader instanceof StructuredGridCoverage2DReader) {
            return new StructuredCoverageViewReader(
                    (StructuredGridCoverage2DReader) reader, coverageView, coverageInfo, hints);
        } else {
            return new CoverageViewReader(reader, coverageView, coverageInfo, hints);
        }
    }

    @Override
    public Format getFormat() {
        return new Format() {

            private final Format delegateFormat = delegate.getFormat();

            @Override
            public ParameterValueGroup getWriteParameters() {
                return delegateFormat.getWriteParameters();
            }

            @Override
            public String getVersion() {
                return delegateFormat.getVersion();
            }

            @Override
            public String getVendor() {
                return delegateFormat.getVendor();
            }

            @Override
            public ParameterValueGroup getReadParameters() {
                HashMap<String, String> info = new HashMap<String, String>();

                info.put("name", getName());
                info.put("description", getDescription());
                info.put("vendor", getVendor());
                info.put("docURL", getDocURL());
                info.put("version", getVersion());

                List<GeneralParameterDescriptor> delegateFormatParams =
                        new ArrayList<GeneralParameterDescriptor>();
                delegateFormatParams.addAll(
                        delegateFormat.getReadParameters().getDescriptor().descriptors());
                // add bands parameter descriptor only if the delegate reader doesn't have it
                // already
                if (!checkIfDelegateReaderSupportsBands()) {
                    delegateFormatParams.add(AbstractGridFormat.BANDS);
                }

                return new ParameterGroup(
                        new DefaultParameterDescriptorGroup(
                                info,
                                delegateFormatParams.toArray(
                                        new GeneralParameterDescriptor
                                                [delegateFormatParams.size()])));
            }

            @Override
            public String getName() {
                return delegateFormat.getName();
            }

            @Override
            public String getDocURL() {
                return delegateFormat.getDocURL();
            }

            @Override
            public String getDescription() {
                return delegateFormat.getDescription();
            }
        };
    }

    /** Helper method that checks if the delegate reader support bands selection. */
    private boolean checkIfDelegateReaderSupportsBands() {
        List<GeneralParameterDescriptor> parameters =
                delegate.getFormat().getReadParameters().getDescriptor().descriptors();
        for (GeneralParameterDescriptor parameterDescriptor : parameters) {
            if (parameterDescriptor.getName().equals(AbstractGridFormat.BANDS.getName())) {
                return true;
            }
        }
        return false;
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
    public String[] getGridCoverageNames() throws IOException {
        return delegate.getGridCoverageNames();
    }

    @Override
    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getCoordinateReferenceSystem(referenceName);
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        checkCoverageName(coverageName);
        return this.getOriginalEnvelope();
    }

    @Override
    public GridEnvelope getOriginalGridRange(String coverageName) {
        checkCoverageName(coverageName);
        return this.getOriginalGridRange();
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        checkCoverageName(coverageName);
        return this.getOriginalGridToWorld(pixInCell);
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
    public double[] getReadingResolutions(
            String coverageName, OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getReadingResolutions(referenceName, policy, requestedResolution);
    }

    @Override
    public ImageLayout getImageLayout() throws IOException {
        return imageLayout;
    }

    @Override
    public ImageLayout getImageLayout(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return imageLayout;
    }

    @Override
    public double[][] getResolutionLevels(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return this.getResolutionLevels();
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
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem(referenceName);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters(referenceName);
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

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return handler.getReadingResolutions(policy, requestedResolution);
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return handler.getResolutionLevels();
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope() {
        return handler.getOriginalEnvelope();
    }

    @Override
    public GridEnvelope getOriginalGridRange() {
        return handler.getOriginalGridRange();
    }

    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return handler.getOriginalGridToWorld(pixInCell);
    }
}
