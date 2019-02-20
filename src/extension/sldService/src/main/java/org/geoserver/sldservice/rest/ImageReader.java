/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.ArrayList;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.rest.RestException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

/**
 * Helper class that reads the best image out of the provided CoverageInfo, taking into account
 * subsampling, overviews, band selection and bounding box restrictions
 */
class ImageReader {
    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();

    private ArrayList<GeneralParameterValue> readParameters;
    private final ReferencedEnvelope envelope;
    private CoverageInfo coverageInfo;
    private int selectedBand;
    private boolean bandSelected;
    private RenderedImage image;
    private int maxPixels;
    private GridCoverage2D coverage;

    public ImageReader(
            CoverageInfo coverageInfo,
            int selectedBand,
            int maxPixels,
            ReferencedEnvelope envelope) {
        this.coverageInfo = coverageInfo;
        this.selectedBand = selectedBand;
        this.envelope = envelope;
        this.maxPixels = maxPixels;
    }

    public ImageReader invoke() throws IOException, TransformException, FactoryException {
        // grab the raster, for the time being, read fully trying to force deferred loading where
        // possible
        readParameters = new ArrayList<>();
        GridCoverage2DReader reader =
                (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        ParameterValue<Boolean> useImageRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useImageRead.setValue(true);
        readParameters.add(useImageRead);

        // grab the original grid geometry
        GridEnvelope originalGridrange = reader.getOriginalGridRange();
        GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
        MathTransform g2w = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
        CoordinateReferenceSystem crs = originalEnvelope.getCoordinateReferenceSystem();
        GridGeometry2D originalGeometry =
                new GridGeometry2D(originalGridrange, PixelInCell.CELL_CORNER, g2w, crs, null);
        GridGeometry2D readGeometry = new GridGeometry2D(originalGeometry);

        // do we need to restrict to a specific bounding box?
        ReferencedEnvelope readEnvelope = null;
        if (envelope != null) {
            ReferencedEnvelope envelopeInNativeCRS = envelope.transform(crs, true);
            readEnvelope =
                    envelopeInNativeCRS.intersection(
                            ReferencedEnvelope.reference(originalEnvelope));
            if (readEnvelope.isEmpty() || readEnvelope.isNull()) {
                throw new RestException(
                        "Specified bounding box does not match the data original envelope "
                                + originalEnvelope,
                        HttpStatus.BAD_REQUEST);
            }

            GeneralEnvelope ers = CRS.transform(g2w.inverse(), readEnvelope);
            GridEnvelope gridToRead =
                    new GridEnvelope2D(
                            (int) Math.floor(ers.getMinimum(0)),
                            (int) Math.floor(ers.getMinimum(1)),
                            (int) Math.ceil(ers.getSpan(0)),
                            (int) Math.ceil(ers.getSpan(1)));
            readGeometry = new GridGeometry2D(gridToRead, PixelInCell.CELL_CORNER, g2w, crs, null);
        }

        // subsample the raster if needed
        GridEnvelope2D gridRange = readGeometry.getGridRange2D();
        long pixels = gridRange.getSpan(0) * (long) gridRange.getSpan(1);
        if (pixels > maxPixels) {
            double pixelRatio = Math.sqrt(pixels / (double) maxPixels);

            int readWidth = (int) Math.max(1, Math.round(gridRange.getSpan(0) / pixelRatio));
            int readHeight = (int) Math.max(1, Math.round(gridRange.getSpan(1) / pixelRatio));
            GridEnvelope2D reducedRange =
                    new GridEnvelope2D(gridRange.x, gridRange.y, readWidth, readHeight);
            readGeometry = new GridGeometry2D(reducedRange, readGeometry.getEnvelope());
        }

        if (!readGeometry.equals(originalGeometry)) {
            ParameterValue<GridGeometry2D> ggParam =
                    AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            ggParam.setValue(readGeometry);
            readParameters.add(ggParam);
        }

        // can we delegate band selection to the reader? if so, it can help a lot, especially
        // on coverage views merging bands coming from different sources
        bandSelected = false;
        if (reader.getFormat()
                .getReadParameters()
                .getDescriptor()
                .descriptors()
                .contains(AbstractGridFormat.BANDS)) {
            SampleModel sampleModel = reader.getImageLayout().getSampleModel(null);
            if (sampleModel != null) {
                verifyBandSelection(selectedBand, sampleModel);
                if (sampleModel.getNumBands() > 1) {
                    ParameterValue<int[]> value = AbstractGridFormat.BANDS.createValue();
                    // this param is zero based, the service is like SLD, 1-based
                    value.setValue(new int[] {selectedBand - 1});
                    readParameters.add(value);
                    bandSelected = true;
                }
            }
        }

        // finally perform the read
        coverage =
                reader.read(
                        readParameters.toArray(new GeneralParameterValue[readParameters.size()]));

        // the reader can do a best-effort on grid geometry, might not have cut it
        if (readEnvelope != null && isUncut(coverage, readGeometry)) {
            ReferencedEnvelope bounds = ReferencedEnvelope.reference(readGeometry.getEnvelope2D());
            Polygon polygon = JTS.toGeometry(bounds);
            Geometry roi = polygon.getFactory().createMultiPolygon(new Polygon[] {polygon});

            // perform the crops
            final ParameterValueGroup param =
                    PROCESSOR.getOperation("CoverageCrop").getParameters();
            param.parameter("Source").setValue(coverage);
            param.parameter("Envelope").setValue(bounds);
            param.parameter("ROI").setValue(roi);

            coverage = (GridCoverage2D) PROCESSOR.doOperation(param);
        }

        image = coverage.getRenderedImage();

        // do we need to perform band selection?
        SampleModel sampleModel = image.getSampleModel();
        if (!bandSelected && sampleModel.getNumBands() > 1) {
            verifyBandSelection(selectedBand, sampleModel);
            ImageWorker iw = new ImageWorker(image);
            // this param is zero based, the service is like SLD, 1-based
            iw.retainBands(new int[] {selectedBand - 1});
            image = iw.getRenderedImage();
            bandSelected = true;
        }

        return this;
    }

    private boolean isUncut(GridCoverage2D coverage, GridGeometry2D targetGridGeometry) {
        Envelope2D actual = coverage.getEnvelope2D();
        Envelope2D expected = targetGridGeometry.getEnvelope2D();
        AffineTransform2D at = (AffineTransform2D) targetGridGeometry.getGridToCRS2D();
        double resX = at.getScaleX();
        double resY = at.getScaleY();

        return Math.abs(actual.getMinimum(0) - expected.getMinimum(0)) > resX
                || Math.abs(actual.getMinimum(1) - expected.getMinimum(1)) > resY
                || Math.abs(actual.getMaximum(0) - expected.getMaximum(0)) > resX
                || Math.abs(actual.getMaximum(1) - expected.getMaximum(1)) > resY;
    }

    /**
     * Returns true if a band has been selected (and as such, we'll need to add a channel selection
     * in the raster symbolizer)
     *
     * @return
     */
    public boolean isBandSelected() {
        return bandSelected;
    }

    /** Returns the image to be classified */
    public RenderedImage getImage() {
        return image;
    }

    ArrayList<GeneralParameterValue> getReadParameters() {
        return readParameters;
    }

    GridCoverage2D getCoverage() {
        return coverage;
    }

    private void verifyBandSelection(int selectedBand, SampleModel sampleModel) {
        int numBands = sampleModel.getNumBands();
        if (selectedBand < 0 || selectedBand > numBands) {
            throw new RestException(
                    "Invalid property value for raster layer, must be a valid band number, between 0 and "
                            + (numBands - 1)
                            + ", but was "
                            + selectedBand,
                    HttpStatus.BAD_REQUEST);
        }
    }
}
