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
import it.geosolutions.jaiext.jiffleop.JiffleDescriptor;
import it.geosolutions.jaiext.range.NoDataContainer;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.utilities.ImageLayout2;
import java.awt.RenderingHints;
import java.awt.Transparency;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.media.jai.ColorModelFactory;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageViewHandler.CoveragesConsistencyChecker;
import org.geotools.api.coverage.SampleDimensionType;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.TypeMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.PAMResourceInfo;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.GridCoverage2DRIA;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.DefaultResourceInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * A {@link CoverageView} reader which takes care of doing underlying coverage read operations and recompositions.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class CoverageViewReader implements GridCoverage2DReader {

    private static final int HETEROGENEOUS_RASTER_GUTTER = 10;

    public static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();

    private static final Logger LOGGER = Logging.getLogger(CoverageViewReader.class);

    /**
     * Executor for parallel loading of coverages that do support multithreaded loading to start with (e.g., image
     * mosaic). It's not limiting the number of threads, because when MT loading is one, that's already limited in the
     * readers. It cannot be the same executor used by the image mosaic (ResourcePool#getCoverageExecutor) because that
     * would cause deadlock (each read would use at least 2 threads, one inside the image mosaic and one inside the
     * coverage view). Should we switch to allow parellel loading of coverages that do not support MT natively, a
     * separate executor should be used.
     */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /** The CoverageView containing definition */
    CoverageView coverageView;

    private CoverageViewHandler handler;

    boolean canSupportHeterogeneousCoverages = false;

    /** The name of the reference coverage, we can remove/revisit it once we relax some constraint */
    String referenceName;

    private String coverageName;

    private GridCoverage2DReader delegate;

    private Hints hints;

    private GridCoverageFactory coverageFactory;

    private ImageLayout imageLayout;

    public CoverageViewReader(
            GridCoverage2DReader delegate, CoverageView coverageView, CoverageInfo coverageInfo, Hints hints) {
        this.coverageName = coverageView.getName();
        this.delegate = delegate;
        this.coverageView = coverageView;
        this.hints = hints;
        referenceName = coverageView.getBand(0).getInputCoverageBands().get(0).getCoverageName();
        canSupportHeterogeneousCoverages = JAIExt.isJAIExtOperation("BandMerge");

        this.handler = new CoverageViewHandler(canSupportHeterogeneousCoverages, delegate, referenceName, coverageView);

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
            SampleModel sampleModel = RasterFactory.createBandedSampleModel(
                    originalSampleModel.getDataType(),
                    originalSampleModel.getWidth(),
                    originalSampleModel.getHeight(),
                    coverageView.getCoverageBands().size());

            ColorModel colorModel = ImageIOUtilities.createColorModel(sampleModel);
            this.imageLayout = new ImageLayout2(
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
    public GridCoverage2D read(GeneralParameterValue... parameters) throws IllegalArgumentException, IOException {

        // did we get a request within the bounds of the coverage?
        Optional<GeneralParameterValue> ggParameter = Optional.empty();
        if (parameters != null && parameters.length > 0) { // beware a no-args call means an empty array
            ggParameter = Arrays.stream(parameters)
                    .filter(parameter -> matches(parameter, AbstractGridFormat.READ_GRIDGEOMETRY2D))
                    .findFirst();
        }
        GridGeometry2D requestedGridGeometry = null;
        if (ggParameter.isPresent()) {
            ParameterValue value = (ParameterValue) ggParameter.get();
            requestedGridGeometry = (GridGeometry2D) value.getValue();
            ReferencedEnvelope requestedEnvelope = ReferencedEnvelope.reference(requestedGridGeometry.getEnvelope());
            ReferencedEnvelope dataEnvelope = ReferencedEnvelope.reference(handler.getOriginalEnvelope());
            if (CRS.equalsIgnoreMetadata(requestedEnvelope, dataEnvelope)) {
                if (!requestedEnvelope.intersects((BoundingBox) dataEnvelope)) {
                    return null;
                }
            } else {
                ReferencedEnvelope re84;
                try {
                    re84 = requestedEnvelope.transform(DefaultGeographicCRS.WGS84, true);
                    ReferencedEnvelope de84 = dataEnvelope.transform(DefaultGeographicCRS.WGS84, true);
                    if (!re84.intersects((BoundingBox) de84)) {
                        return null;
                    }
                } catch (TransformException | FactoryException e) {
                    LOGGER.log(
                            Level.FINE,
                            "Cannot determine if the requested BBOX intersects the " + "data one, continuing",
                            e);
                }
            }

            // expand the read area if we are in the heterogeneous case... it's a resampling
            // one similar to reprojection, prone to off-by-one issues at the borders
            if (!handler.isHomogeneousCoverages()) {
                GridEnvelope2D range = requestedGridGeometry.getGridRange2D();
                GridEnvelope2D expandedRange = new GridEnvelope2D(
                        (int) range.getMinX() - HETEROGENEOUS_RASTER_GUTTER,
                        (int) range.getMinY() - HETEROGENEOUS_RASTER_GUTTER,
                        (int) range.getWidth() + HETEROGENEOUS_RASTER_GUTTER * 2,
                        (int) range.getHeight() + HETEROGENEOUS_RASTER_GUTTER * 2);
                GridGeometry2D expandedGG = new GridGeometry2D(
                        expandedRange,
                        requestedGridGeometry.getGridToCRS(),
                        requestedGridGeometry.getCoordinateReferenceSystem());
                value.setValue(expandedGG);
            }
        }

        List<CoverageBand> bands = coverageView.getCoverageBands();
        CoverageView.CompositionType compositionType = coverageView.getCompositionType();
        if (compositionType == null) {
            // Fallback on the legacy behavior where compositionType was tied to the band
            // and the only possible value at that time was BandSelect
            compositionType = CoverageView.CompositionType.BAND_SELECT;
        }
        CoveragesConsistencyChecker checker = null;
        ArrayList<Integer> selectedBandIndices = getBandIndices(parameters, bands);

        // Since composition of a raster band using a formula applied on individual bands has not
        // been implemented, the normal case is that each CoverageBand is in fact a single band from
        // an input coverage. When band composition will be implemented, this will be the point
        // where
        // band composition will occur, before the final BandSelect/BandMerge ops

        // This is a good spot to read coverages. Reading a coverage is done only once, it is
        // cached to be used for its other bands that possibly take part in the CoverageView
        // definition
        boolean fillMissingBands = coverageView.getFillMissingBands();
        ViewInputs inputAlphaNonNull = getInputAlphaNonNullCoverages(
                parameters, selectedBandIndices, bands, checker, true, compositionType, fillMissingBands);
        if (inputAlphaNonNull == null) return null;

        // all readers returned null?
        if (inputAlphaNonNull.nonNullCoverages == 0 || inputAlphaNonNull.inputCoverages.isEmpty()) {
            return null;
        }

        if (compositionType == CoverageView.CompositionType.BAND_SELECT) {
            return readUsingBandSelect(inputAlphaNonNull, bands, selectedBandIndices, requestedGridGeometry);
        } else if (compositionType == CoverageView.CompositionType.JIFFLE) {
            return readUsingJiffle(inputAlphaNonNull);
        } else {
            throw new UnsupportedOperationException("Unsupported composition type: " + compositionType);
        }
    }

    public GridCoverage2D readUsingBandSelect(
            ViewInputs inputAlphaNonNull,
            List<CoverageBand> bands,
            ArrayList<Integer> selectedBandIndices,
            GridGeometry2D requestedGridGeometry)
            throws IOException {
        List<GridCoverage2D> coverages = new ArrayList<>();

        // some returned null?
        if (inputAlphaNonNull.nonNullCoverages < inputAlphaNonNull.inputCoverages.size()) {
            float width, height;
            if (requestedGridGeometry != null) {
                GridEnvelope2D range = requestedGridGeometry.getGridRange2D();
                width = (float) range.getWidth();
                height = (float) range.getHeight();
            } else {
                GridCoverage2D reference = inputAlphaNonNull.inputCoverages.values().stream()
                        .filter(c -> c != null)
                        .findFirst()
                        .get();
                RenderedImage ri = reference.getRenderedImage();
                width = ri.getWidth();
                height = ri.getHeight();
            }

            // build empty coverages for the missing bits
            for (String name : inputAlphaNonNull.inputCoverages.keySet()) {
                GridCoverage2DReader reader = SingleGridCoverage2DReader.wrap(delegate, name);
                ImageLayout layout = reader.getImageLayout();
                int numBands = layout.getSampleModel(null).getNumBands();
                Number[] bandValues = new Number[numBands]; // all zeroes
                Arrays.fill(bandValues, Double.valueOf(0));
                ConstantDescriptor.create(width, height, bandValues, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout));
            }
        }

        ArrayList<CoverageBand> mergedBands = getMergedBands(selectedBandIndices, bands);

        // perform the band selects as needed
        int index = 0;
        int transformationChoice = index;
        CoverageViewHandler.CoverageResolutionChooser resolutionChooser = handler.getCoverageResolutionChooser();
        for (CoverageBand band : mergedBands) {
            List<InputCoverageBand> selectedBands = band.getInputCoverageBands();

            // Peek for coverage name
            String coverageName = selectedBands.get(0).getCoverageName();

            // Get band indices for band selection
            ArrayList<Integer> bandIndices = new ArrayList<>(selectedBands.size());
            for (InputCoverageBand icb : selectedBands) {
                int bandIdx = 0;
                final String bandString = icb.getBand();
                if (bandString != null && !bandString.isEmpty()) {
                    bandIdx = Integer.parseInt(bandString);
                }
                bandIndices.add(bandIdx);
            }

            GridCoverage2D coverage = inputAlphaNonNull.inputCoverages.get(coverageName);

            // special case for dynamic alpha on single input, no need to actually select away the
            // alpha
            Hints localHints = new Hints(hints);
            if (inputAlphaNonNull.dynamicAlphaSource != null
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
                        "Read coverage " + coverageName + ", result has envelope " + coverage.getEnvelope2D());
            }
        }

        GridCoverage2D result;
        if (coverages.size() > 1) {
            // dynamic alpha but more than one source
            Hints localHints = new Hints(hints);
            if (inputAlphaNonNull.dynamicAlphaSource != null) {
                int currentBandCount = countBands(coverages);
                // and the output is suitable for getting an alpha band
                if (currentBandCount == 1 || currentBandCount == 3) {
                    final int alphaBandIndex = getAlphaBandIndex(inputAlphaNonNull.dynamicAlphaSource);
                    GridCoverage2D alphaBandCoverage =
                            retainBands(Arrays.asList(alphaBandIndex), inputAlphaNonNull.dynamicAlphaSource, hints);
                    coverages.add(alphaBandCoverage);

                    addAlphaColorModelHint(localHints, currentBandCount);
                }
            }

            // perform final band merge
            String operationName = "BandMerge";
            final ParameterValueGroup param =
                    PROCESSOR.getOperation(operationName).getParameters();
            if (!handler.isHomogeneousCoverages()) {
                param.parameter("transform_choice").setValue("index");
                param.parameter("coverage_idx").setValue(transformationChoice);
            }
            param.parameter("sources").setValue(coverages);
            localHints.put(JAI.KEY_COLOR_MODEL_FACTORY, (ColorModelFactory) (sampleModel, sources, configuration) -> {
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

                return RasterFactory.createComponentColorModel(dataType, cs, false, false, Transparency.OPAQUE);
            });
            result = (GridCoverage2D) PROCESSOR.doOperation(param, localHints);
        } else {
            // optimize out, no need to do a band merge
            result = coverages.get(0);
        }

        return result;
    }

    private GridCoverage2D readUsingJiffle(ViewInputs viewInputs) {
        HashMap<String, GridCoverage2D> inputCoverages = viewInputs.getInputCoverages();
        List<String> coverageOrder = new ArrayList<>(inputCoverages.keySet());
        GridCoverage2D[] coverages = inputCoverages.values().toArray(new GridCoverage2D[0]);
        GridCoverage2D reference = coverages[0];

        String destName = coverageView.getOutputName();
        String script = coverageView.getDefinition();
        List<CoverageBand> outputBands = coverageView.getCoverageBands();
        List<InputCoverageBand> inputBands = outputBands.get(0).getInputCoverageBands();
        Set<String> usedBandNames =
                inputBands.stream().map(InputCoverageBand::getCoverageName).collect(Collectors.toSet());

        // Make sure sourceBands are sorted the same way as the coverages
        List<String> sourceBands =
                coverageOrder.stream().filter(usedBandNames::contains).collect(Collectors.toList());

        RenderedImage[] sources = new RenderedImage[inputCoverages.size()];
        sources[0] = reference.getRenderedImage();
        for (int i = 1; i < sources.length; i++) {
            GridCoverage2D coverage = coverages[i];
            if (coverage.getGridGeometry().equals(reference.getGridGeometry())) {
                sources[i] = coverage.getRenderedImage();
            } else {
                double[] nodata = CoverageUtilities.getBackgroundValues(coverage);
                ROI roi = CoverageUtilities.getROIProperty(coverage);
                sources[i] = GridCoverage2DRIA.create(coverage, reference, nodata, hints, roi);
            }
        }

        String[] sourceNames = sourceBands.toArray(new String[0]);
        int outputBandCount = outputBands.size();
        Range[] nodatas = new Range[sources.length];
        nodatas = getNodatas(nodatas, coverages);
        RenderedOp result = JiffleDescriptor.create(
                sources, sourceNames, destName, script, null, null, outputBandCount, null, null, nodatas, hints);

        GridSampleDimension[] sampleDimensions = getSampleDimensions(result, destName);
        GridCoverageFactory factory = new GridCoverageFactory(hints);
        return factory.create("jiffle", result, reference.getEnvelope(), sampleDimensions, coverages, null);
    }

    private GridSampleDimension[] getSampleDimensions(RenderedOp result, String destName) {
        SampleModel sm = result.getSampleModel();
        Stream<String> names = getBandNames(sm.getNumBands(), destName);
        SampleDimensionType sourceType = TypeMap.getSampleDimensionType(sm, 0);
        NumberRange<? extends Number> range = TypeMap.getRange(sourceType);
        double[] nodata = null; // {Double.NaN};
        double min = range.getMinimum();
        double max = range.getMaximum();
        return names.map(n -> new GridSampleDimension(n, sourceType, null, nodata, min, max, 1, 0, null))
                .toArray(n -> new GridSampleDimension[n]);
    }

    private Stream<String> getBandNames(int numBands, String destName) {
        if (numBands == 1) {
            return Stream.of(destName);
        } else {
            return IntStream.range(1, numBands + 1).mapToObj(n -> destName + n);
        }
    }

    private Range[] getNodatas(Range[] nodatas, GridCoverage2D[] coverages) {
        for (int i = 0; i < coverages.length; i++) {
            GridCoverage2D coverage = coverages[i];
            NoDataContainer coverageNoData = CoverageUtilities.getNoDataProperty(coverage);
            if (coverageNoData != null) {
                nodatas[i] = coverageNoData.getAsRange();
            }
        }
        if (Arrays.stream(nodatas).filter(n -> n != null).count() == 0) {
            nodatas = null;
        }
        return nodatas;
    }

    private ViewInputs getInputAlphaNonNullCoverages(
            GeneralParameterValue[] parameters,
            ArrayList<Integer> selectedBandIndices,
            List<CoverageBand> bands,
            CoveragesConsistencyChecker checker,
            Boolean includeCoverages,
            CoverageView.CompositionType compositionType,
            boolean fillMissingBands)
            throws IOException {

        // bands selection parameter inside on final bands so they should not be propagated
        // to the delegate reader
        final GeneralParameterValue[] filteredParameters;
        boolean multiThreadedLoading = false;
        if (parameters != null) {
            // creating a copy of parameters excluding the bands parameter
            filteredParameters = Arrays.stream(parameters)
                    .filter(parameter -> !matches(parameter, AbstractGridFormat.BANDS))
                    .toArray(GeneralParameterValue[]::new);
            // check if multi-threaded loading is enabled
            multiThreadedLoading = Arrays.stream(parameters)
                    .filter(parameter -> matches(parameter, ImageMosaicFormat.ALLOW_MULTITHREADING))
                    .map(p -> Boolean.TRUE.equals(((ParameterValue) p).getValue()))
                    .findFirst()
                    .orElse(false);
        } else {
            filteredParameters = null;
        }
        CoverageViewComposer composer = new CoverageViewComposer(fillMissingBands, handler, coverageFactory);
        for (int bIdx : selectedBandIndices) {
            CoverageBand band = bands.get(bIdx);
            List<InputCoverageBand> selectedBands = band.getInputCoverageBands();
            List<InputCoverageBand> inputBands = compositionType == CoverageView.CompositionType.BAND_SELECT
                    ? Collections.singletonList(selectedBands.get(0))
                    : selectedBands;
            // Peek for coverage name
            for (InputCoverageBand inputBand : inputBands) {
                String coverageName = inputBand.getCoverageName();
                if (!composer.containsCoverage(coverageName)) {
                    GridCoverage2DReader reader = SingleGridCoverage2DReader.wrap(delegate, coverageName);
                    composer.putReader(coverageName, reader);
                    // Remove this when removing constraints
                    if (checker == null) {
                        checker = new CoveragesConsistencyChecker(reader, canSupportHeterogeneousCoverages);
                    } else {
                        checker.checkConsistency(reader);
                    }
                }
            }
        }

        if (Boolean.TRUE.equals(includeCoverages)) {
            if (!multiThreadedLoading || EXECUTOR == null) {
                for (String coverageName : composer.getCoverageNames()) {
                    GridCoverage2DReader reader = composer.getReader(coverageName);
                    GridCoverage2D coverage = reader.read(filteredParameters);
                    if (!composer.shouldProcessCoverage(coverageName, reader, coverage)) {
                        return null;
                    }
                }
                if (!composer.canCompose()) {
                    return null;
                }
            } else {
                // parallel read
                List<Future<ParallelLoadingResult>> futures = new ArrayList<>();
                for (String coverageName : composer.getCoverageNames()) {
                    Future<ParallelLoadingResult> future = EXECUTOR.submit(() -> {
                        GridCoverage2DReader reader = composer.getReader(coverageName);
                        GridCoverage2D coverage = reader.read(filteredParameters);
                        return new ParallelLoadingResult(coverageName, reader, coverage);
                    });
                    futures.add(future);
                }

                // sequential post-processing
                for (Future<ParallelLoadingResult> future : futures) {
                    try {
                        ParallelLoadingResult result = future.get();
                        if (!composer.shouldProcessCoverage(result.coverageName, result.reader, result.coverage)) {
                            return null;
                        }
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
                if (!composer.canCompose()) {
                    return null;
                }
            }
        }
        return composer.prepareViewInputs(bands);
    }

    static class ParallelLoadingResult {
        String coverageName;
        GridCoverage2DReader reader;
        GridCoverage2D coverage;

        public ParallelLoadingResult(String coverageName, GridCoverage2DReader reader, GridCoverage2D coverage) {
            this.coverageName = coverageName;
            this.reader = reader;
            this.coverage = coverage;
        }
    }

    static class ViewInputs {
        private final List<CoverageBand> bands;
        private final HashMap<String, GridCoverage2DReader> inputReaders;
        private final HashMap<String, GridCoverage2D> inputCoverages;
        private final GridCoverage2D dynamicAlphaSource;
        private final int nonNullCoverages;

        public ViewInputs(
                List<CoverageBand> bands,
                HashMap<String, GridCoverage2DReader> inputReaders,
                HashMap<String, GridCoverage2D> inputCoverages,
                GridCoverage2D dynamicAlphaSource,
                int nonNullCoverages) {
            this.bands = bands;
            this.inputCoverages = inputCoverages;
            this.dynamicAlphaSource = dynamicAlphaSource;
            this.nonNullCoverages = nonNullCoverages;
            this.inputReaders = inputReaders;
        }

        public List<CoverageBand> getBands() {
            return bands;
        }

        public HashMap<String, GridCoverage2D> getInputCoverages() {
            return inputCoverages;
        }

        public GridCoverage2D getDynamicAlphaSource() {
            return dynamicAlphaSource;
        }

        public int getNonNullCoverages() {
            return nonNullCoverages;
        }

        public HashMap<String, GridCoverage2DReader> getInputReaders() {
            return inputReaders;
        }
    }

    /**
     * Get the band indices to read
     *
     * @param parameters the parameters to read
     * @param bands the bands to read
     * @return the band indices to read
     */
    private static ArrayList<Integer> getBandIndices(GeneralParameterValue[] parameters, List<CoverageBand> bands) {
        int coverageBandsSize = bands.size();

        // Check params, populate band indices to read if BANDS param has been defined
        ArrayList<Integer> selectedBandIndices = new ArrayList<>();
        for (int m = 0; m < coverageBandsSize; m++) {
            selectedBandIndices.add(m);
        }

        if (parameters != null) {
            for (GeneralParameterValue parameter : parameters) {
                final ParameterValue param = (ParameterValue) parameter;
                if (AbstractGridFormat.BANDS
                        .getName()
                        .equals(param.getDescriptor().getName())) {
                    int[] bandIndicesParam = (int[]) param.getValue();
                    if (bandIndicesParam != null) {
                        selectedBandIndices = new ArrayList<>();
                        for (int j : bandIndicesParam) {
                            selectedBandIndices.add(j);
                        }
                        break;
                    }
                }
            }
        }
        return selectedBandIndices;
    }

    /**
     * Get the merged bands, grouping together bands that come from the same coverage
     *
     * @param selectedBandIndices the indices of the bands to merge
     * @param bands the bands to merge
     * @return the merged bands
     */
    private static ArrayList<CoverageBand> getMergedBands(
            ArrayList<Integer> selectedBandIndices, List<CoverageBand> bands) {
        // Group together bands that come from the same coverage
        ArrayList<CoverageBand> mergedBands = new ArrayList<>();

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
            String coverageName = bands.get(selectedBandIndices.get(idx))
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
                ArrayList<InputCoverageBand> groupBands = new ArrayList<>();
                groupBands.addAll(mBand.getInputCoverageBands());
                groupBands.addAll(bands.get(selectedBandIndices.get(idx + 1)).getInputCoverageBands());
                mBand.setInputCoverageBands(groupBands);
            } else {
                mergedBands.add(mBand);
                mBand = null;
            }
            idx++;
        }
        return mergedBands;
    }

    private boolean matches(GeneralParameterValue parameter, ParameterDescriptor<?> expected) {
        return parameter.getDescriptor().getName().equals(expected.getName());
    }

    /**
     * The BandMerge operation takes indexed images and expands them, however in the context of coverage view band
     * merging we don't normally want that, e.g., raster mask bands are represented as indexed but we really want to
     * keep them in their binary, single band form. To do so, the IndexColorModel is replaced by a ComponentColorModel
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
            return new ComponentColorModel(cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        } else if (currentBandCount == 1) {
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {8, 8};
            return new ComponentColorModel(cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        } else {
            throw new IllegalArgumentException(
                    "Cannot create a color model with alpha support starting with " + currentBandCount + " bands");
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
                    "The source coverage does not have an alpha band, cannot extract an " + "alpha band");
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

    private GridCoverage2D retainBands(List<Integer> bandIndices, GridCoverage2D coverage, Hints hints) {
        final ParameterValueGroup param =
                PROCESSOR.getOperation("SelectSampleDimension").getParameters();
        param.parameter("Source").setValue(coverage);
        final int[] sampleDimensionArray = ArrayUtils.toPrimitive(bandIndices.toArray(new Integer[bandIndices.size()]));
        param.parameter("SampleDimensions").setValue(sampleDimensionArray);
        coverage = (GridCoverage2D) PROCESSOR.doOperation(param, hints);
        return coverage;
    }

    /** @param coverageName */
    protected void checkCoverageName(String coverageName) {
        if (!this.coverageName.equalsIgnoreCase(coverageName)) {
            throw new IllegalArgumentException("The specified coverageName isn't the one of this coverageView");
        }
    }

    @Override
    public void dispose() throws IOException {
        delegate.dispose();
    }

    /** Get a {@link GridCoverage2DReader} wrapping the provided delegate reader */
    public static GridCoverage2DReader wrap(
            GridCoverage2DReader reader, CoverageView coverageView, CoverageInfo coverageInfo, Hints hints) {
        if (reader instanceof StructuredGridCoverage2DReader) {
            return new StructuredCoverageViewReader(
                    (StructuredGridCoverage2DReader) reader, coverageView, coverageInfo, hints);
        } else {
            return new CoverageViewReader(reader, coverageView, coverageInfo, hints);
        }
    }

    @Override
    public Format getFormat() {
        return new AbstractGridFormat() {

            private final AbstractGridFormat delegateFormat = (AbstractGridFormat) delegate.getFormat();

            @Override
            public ParameterValueGroup getWriteParameters() {
                return delegateFormat.getWriteParameters();
            }

            @Override
            public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
                return delegateFormat.getDefaultImageIOWriteParameters();
            }

            @Override
            public GridCoverageWriter getWriter(Object o, Hints hints) {
                return delegateFormat.getWriter(o, hints);
            }

            @Override
            public String getVersion() {
                return delegateFormat.getVersion();
            }

            @Override
            public AbstractGridCoverage2DReader getReader(Object o) {
                return delegateFormat.getReader(o);
            }

            @Override
            public AbstractGridCoverage2DReader getReader(Object o, Hints hints) {
                return delegateFormat.getReader(o, hints);
            }

            @Override
            public GridCoverageWriter getWriter(Object o) {
                return delegateFormat.getWriter(o);
            }

            @Override
            public boolean accepts(Object o, Hints hints) {
                return delegateFormat.accepts(o, hints);
            }

            @Override
            public String getVendor() {
                return delegateFormat.getVendor();
            }

            @Override
            public ParameterValueGroup getReadParameters() {
                HashMap<String, String> info = new HashMap<>();

                info.put("name", getName());
                info.put("description", getDescription());
                info.put("vendor", getVendor());
                info.put("docURL", getDocURL());
                info.put("version", getVersion());

                List<GeneralParameterDescriptor> delegateFormatParams = new ArrayList<>();
                delegateFormatParams.addAll(
                        delegateFormat.getReadParameters().getDescriptor().descriptors());
                // add bands parameter descriptor only if the delegate reader doesn't have it
                // already
                if (!checkIfDelegateReaderSupportsBands()) {
                    delegateFormatParams.add(AbstractGridFormat.BANDS);
                }

                return new ParameterGroup(new DefaultParameterDescriptorGroup(
                        info,
                        delegateFormatParams.toArray(new GeneralParameterDescriptor[delegateFormatParams.size()])));
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
    public GeneralBounds getOriginalEnvelope(String coverageName) {
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
    public GridCoverage2D read(String coverageName, GeneralParameterValue... parameters) throws IOException {
        checkCoverageName(coverageName);
        return read(parameters);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getDynamicParameters(referenceName);
    }

    @Override
    public double[] getReadingResolutions(String coverageName, OverviewPolicy policy, double[] requestedResolution)
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
        List<CoverageBand> bands = coverageView.getCoverageBands();
        ArrayList<Integer> selectedBandIndices = getBandIndices(null, bands);
        try {
            CoverageView.CompositionType compositionType = coverageView.getCompositionType() != null
                    ? coverageView.getCompositionType()
                    : CoverageView.CompositionType.BAND_SELECT;
            ViewInputs viewInputs = getInputAlphaNonNullCoverages(
                    null, selectedBandIndices, bands, null, false, compositionType, false);

            if (viewHasPAM(viewInputs)) {
                return new CoverageViewPamResourceInfo(viewInputs);
            } else {
                return new DefaultResourceInfo();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while getting input coverages from Coverage View", e);
        }
        return null;
    }

    private boolean viewHasPAM(ViewInputs info) {
        Map<String, GridCoverage2DReader> readers = info.getInputReaders();
        // iterate over the view bands and check for PAMs
        for (CoverageView.CoverageBand band : info.getBands()) {
            for (CoverageView.InputCoverageBand inputCoverageBand : band.getInputCoverageBands()) {
                // get the reader for the coverage associated with this input coverage band
                GridCoverageReader reader1 = readers.get(inputCoverageBand.getCoverageName());
                if (reader1 instanceof GridCoverage2DReader) {
                    GridCoverage2DReader bandReader = (GridCoverage2DReader) reader1;
                    ResourceInfo resourceInfoBand = bandReader.getInfo(inputCoverageBand.getCoverageName());
                    // reader is associated with a PAM
                    if (resourceInfoBand instanceof PAMResourceInfo) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution) throws IOException {
        return handler.getReadingResolutions(policy, requestedResolution);
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return handler.getResolutionLevels();
    }

    @Override
    public GeneralBounds getOriginalEnvelope() {
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
