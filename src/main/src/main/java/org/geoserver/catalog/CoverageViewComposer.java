/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.geoserver.catalog.CoverageView.EnvelopeCompositionType.INTERSECTION;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.JAI;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.media.mosaic.MosaicDescriptor;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.eclipse.imagen.media.range.Range;
import org.eclipse.imagen.media.range.RangeFactory;
import org.eclipse.imagen.media.utilities.ImageLayout2;
import org.geoserver.data.util.CoverageUtils;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;

/** Utility class supporting the composition of coverage views by handling input coverages and filling strategies. */
class CoverageViewComposer {

    /**
     * Represents a layout configuration for filling missing data in a coverage composing a coverageView. Holds
     * information about no-data values, image layout, and availability of the layout.
     */
    static class FillingLayout {
        private Double noData;
        private ImageLayout imageLayout;

        public FillingLayout(Double noData, ImageLayout imageLayout) {
            this.noData = noData;
            this.imageLayout = imageLayout;
        }
    }

    private final CoverageViewHandler handler;
    private final GridCoverageFactory coverageFactory;

    private final HashMap<String, GridCoverage2D> inputCoverages = new HashMap<>();
    private final HashMap<String, FillingLayout> noDataCoverages = new HashMap<>();
    private final HashMap<String, GridCoverage2DReader> inputReaders = new HashMap<>();
    private int nonNullCoverages;
    private GridCoverage2D dynamicAlphaSource;
    private boolean fillMissingBands;

    public CoverageViewComposer(
            boolean fillMissingBands, CoverageViewHandler handler, GridCoverageFactory coverageFactory) {
        this.fillMissingBands = fillMissingBands;
        this.handler = handler;
        this.coverageFactory = coverageFactory;
    }

    /**
     * Checks if a specific coverage name exists in the input coverages map.
     *
     * @param coverageName The name of the coverage to check
     * @return true if the coverage name is present in the input coverages, false otherwise
     */
    public boolean containsCoverage(String coverageName) {
        return inputCoverages.containsKey(coverageName);
    }

    /**
     * Adds a GridCoverage2DReader to the composing input readers.
     *
     * @param coverageName The name of the coverage associated with the reader
     * @param reader The GridCoverage2DReader to be added to the input readers
     */
    public void putReader(String coverageName, GridCoverage2DReader reader) {
        inputReaders.put(coverageName, reader);
    }

    /** Retrieves the set of coverage names from the composing readers. */
    public Set<String> getCoverageNames() {
        return inputReaders.keySet();
    }

    /**
     * Retrieves the composing GridCoverage2DReader associated with a specific coverage name.
     *
     * @param coverageName The name of the coverage for which to retrieve the reader
     * @return The GridCoverage2DReader for the specified coverage name, or null if not found
     */
    public GridCoverage2DReader getReader(String coverageName) {
        return inputReaders.get(coverageName);
    }

    /**
     * Prepares the view inputs for a coverage view by packaging necessary components.
     *
     * @param bands The list of coverage bands to be included in the view
     * @return A ViewInputs object containing the configuration for rendering the coverage view
     */
    public CoverageViewReader.ViewInputs prepareViewInputs(List<CoverageView.CoverageBand> bands) {
        return new CoverageViewReader.ViewInputs(
                bands, inputReaders, inputCoverages, dynamicAlphaSource, nonNullCoverages);
    }

    /**
     * Determines whether a coverage should be processed in a coverage view.
     *
     * @throws IOException if an error occurs during processing
     */
    boolean shouldProcessCoverage(String coverageName, GridCoverage2DReader reader, GridCoverage2D coverage)
            throws IOException {
        if (coverage != null) {
            nonNullCoverages++;
            if (dynamicAlphaSource == null && hasDynamicAlpha(coverage, reader)) {
                dynamicAlphaSource = coverage;
            }
            inputCoverages.put(coverageName, coverage);
            return true;
        }

        if (!(handler.isHomogeneousCoverages() || handler.getEnvelopeCompositionType() == INTERSECTION)) {
            return true;
        }

        // A null coverage should stop the processing if we are not supporting the nodata fill.
        if (!fillMissingBands) {
            return false;
        }

        // If we are here, we are going to prepare the filling layout for the coverage.
        FillingLayout fillingLayout = getFillingLayout(reader, coverageName);
        if (fillingLayout == null) {
            // We can't do the filling, so we stop the processing
            return false;
        }

        noDataCoverages.put(coverageName, fillingLayout);
        return true;
    }

    /**
     * Determines if the coverage view can be composed by checking input coverages and handling missing bands.
     *
     * <p>This method ensures that: 1. There are input coverages available 2. If filling missing bands is enabled, it
     * creates no-data coverages for missing bands 3. Prepares a consistent image layout for composing coverage views
     *
     * @return true if the coverage view can be composed, false otherwise
     */
    boolean canCompose() {
        if (inputCoverages.isEmpty()) {
            // Nothing to compose. we cannot proceed.
            return false;
        }
        int coveragesSize = getCoverageNames().size();
        int missingCoverages = coveragesSize - inputCoverages.size();
        if (missingCoverages == 0) {
            // We have all the coverages, we can proceed.
            return true;
        }
        if (!fillMissingBands) {
            // We are missing some bands but we are not filling them, we cannot proceed.
            return false;
        }

        // We have available input coverage
        if (!noDataCoverages.isEmpty() && noDataCoverages.size() == missingCoverages) {
            GridCoverage2D referenceCoverage =
                    inputCoverages.values().iterator().next();
            GridGeometry2D gridGeometry = referenceCoverage.getGridGeometry();
            GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
            ReferencedEnvelope envelope = referenceCoverage.getEnvelope2D();
            RenderedImage refImage = referenceCoverage.getRenderedImage();
            Range zeroRange = RangeFactory.create(0, 0);
            for (Map.Entry<String, FillingLayout> entry : noDataCoverages.entrySet()) {
                String coverageName = entry.getKey();
                FillingLayout fillingLayout = entry.getValue();
                double noDataValue = fillingLayout.noData;
                final ImageLayout2 il = prepareImageLayout(refImage, gridRange, fillingLayout);
                final RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);

                // Prepare a constant image, filled with nodata value,
                // consistent with how ImageMosaic handles it
                double threshold = CoverageUtilities.getMosaicThreshold(
                        il.getSampleModel(null).getDataType());
                ImageWorker w = new ImageWorker(renderingHints);
                w.setBackground(new double[] {noDataValue});
                w.mosaic(
                        new RenderedImage[0],
                        MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                        null,
                        null,
                        new double[][] {{threshold}},
                        new Range[] {zeroRange});
                RenderedImage image = w.getRenderedImage();
                GridCoverage2D noDataCoverage = coverageFactory.create(
                        coverageName, image, envelope, referenceCoverage.getSampleDimensions(), null, null);
                inputCoverages.put(coverageName, noDataCoverage);
            }
            if (coveragesSize == inputCoverages.size()) {
                return true;
            }
        }
        return false;
    }

    private ImageLayout2 prepareImageLayout(
            RenderedImage refImage, GridEnvelope2D gridRange, FillingLayout fillingLayout) {
        ImageLayout2 il = new ImageLayout2();
        final int width = gridRange.width;
        final int height = gridRange.height;
        final int tileHeight = refImage.getTileHeight();
        final int tileWidth = refImage.getTileWidth();
        final int minX = refImage.getMinX();
        final int minY = refImage.getMinY();

        ColorModel cm = fillingLayout.imageLayout.getColorModel(null);
        SampleModel sm = fillingLayout.imageLayout.getSampleModel(null);
        il.setColorModel(cm);
        il.setTileHeight(tileHeight);
        il.setTileWidth(tileWidth);
        il.setMinX(minX);
        il.setMinY(minY);
        il.setWidth(width);
        il.setHeight(height);
        il.setSampleModel(sm.createCompatibleSampleModel(width, height));
        return il;
    }

    /**
     * Checks if a reader added a alpha channel on the fly as a result of a read parameter. We want to preserve this
     * alpha channel because the user never got a chance to select its presence in the output (e.g. footprint management
     * in mosaic)
     */
    private boolean hasDynamicAlpha(GridCoverage2D coverage, GridCoverage2DReader reader) throws IOException {
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

    /**
     * Retrieves the filling layout information for a specific coverage from a grid coverage reader.
     *
     * <p>This method determines the no-data characteristics and image layout for a given coverage. by performing a
     * limited read operation to extract such information.
     *
     * @param reader The GridCoverage2DReader to extract layout information from
     * @param coverageName The name of the coverage to analyze
     * @return A FillingLayout containing no-data and image layout details
     * @throws IOException If there are issues reading the coverage
     */
    private FillingLayout getFillingLayout(GridCoverage2DReader reader, String coverageName) throws IOException {
        FillingLayout info = null;
        Format format = reader.getFormat();
        final ParameterValueGroup readParams = format.getReadParameters();
        final Map<String, Serializable> parameters = CoverageUtils.getParametersKVP(readParams);

        final GridCoverage2D gc;
        try {
            gc = CoverageUtils.readSampleGridCoverage(reader, readParams, parameters, null, true);
        } catch (TransformException e) {
            throw new IOException("Exception occurred reading the sample coverage", e);
        }
        NoDataContainer noDataContainer;
        if (gc != null && (noDataContainer = CoverageUtilities.getNoDataProperty(gc)) != null) {
            ImageLayout imageLayout = reader.getImageLayout(coverageName);
            info = new FillingLayout(noDataContainer.getAsSingleValue(), imageLayout);
            gc.dispose(true);
            if (gc.getRenderedImage() instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) gc.getRenderedImage());
            }
        }
        return info;
    }
}
