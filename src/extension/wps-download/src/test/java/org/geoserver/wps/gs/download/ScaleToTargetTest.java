/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.media.jai.Interpolation;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.util.NullProgressListener;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.CropCoverage;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

public class ScaleToTargetTest {

    private static final String TEST_COVERAGE = "/org/geoserver/data/test/tazbm.tiff";

    private static final double DELTA = 1E-06;

    private static final double[] EXP_NATIVE_RES = new double[] {0.0041666667, 0.0041666667};

    private static final ReferencedEnvelope ROI =
            new ReferencedEnvelope(146.5, 148, -43.5, -43, DefaultGeographicCRS.WGS84);

    private File inputTempFile = null;

    @Before
    public void setUpInput() throws IOException {
        inputTempFile = File.createTempFile("scale2target_", "_in");
        try (FileOutputStream fos = new FileOutputStream(inputTempFile)) {
            IOUtils.copy(getClass().getResourceAsStream(TEST_COVERAGE), fos);
        }
    }

    @After
    public void cleanUpInput() {
        if (inputTempFile != null) {
            inputTempFile.delete();
        }
    }

    @Test
    public void testAdjustSize() throws Exception {
        GeoTiffReader reader = null;

        try {
            reader = new GeoTiffReader(inputTempFile);
            assertNotNull(reader);

            Envelope fullSizeEnvelope = reader.getOriginalEnvelope();
            ScaleToTarget scalingFullSize = new ScaleToTarget(reader, fullSizeEnvelope);
            scalingFullSize.setTargetSize(160, null);
            Integer[] targetSize = scalingFullSize.getTargetSize();
            assertEquals(160, targetSize[0].intValue());
            assertEquals(160, targetSize[1].intValue());
            scalingFullSize.setTargetSize(null, 200);
            targetSize = scalingFullSize.getTargetSize();
            assertEquals(200, targetSize[0].intValue());
            assertEquals(200, targetSize[1].intValue());

            // test with ROI with a different aspect ratio
            ScaleToTarget scalingRoi = new ScaleToTarget(reader, ROI);
            scalingRoi.setTargetSize(150, null);
            targetSize = scalingRoi.getTargetSize();
            assertEquals(150, targetSize[0].intValue());
            assertEquals(50, targetSize[1].intValue());
            scalingRoi.setTargetSize(null, 100);
            targetSize = scalingRoi.getTargetSize();
            assertEquals(300, targetSize[0].intValue());
            assertEquals(100, targetSize[1].intValue());
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    @Test
    public void testNoScalingJustInterpolation() throws Exception {
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;

        try {
            reader = new GeoTiffReader(inputTempFile);
            assertNotNull(reader);

            Envelope fullSizeEnvelope = reader.getOriginalEnvelope();

            ScaleToTarget noScaling = new ScaleToTarget(reader, fullSizeEnvelope);
            // I deliberately omit setting the target size: only interpolation will be performed
            // set interpolation method to something other than NEAREST
            noScaling.setInterpolation(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            gc = noScaling.scale(reader.read(null));
            assertNotNull(gc);
            // TODO: this only proves the code ran without throwing exceptions: how do I actually
            // test that the interpolation was done?
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
        }
    }

    @Test
    public void testFullSizeScale2X() throws Exception {
        final int targetSizeX = 720, targetSizeY = 720;
        final double[] expectedRequestedResolution =
                new double[] {EXP_NATIVE_RES[0] / 2, EXP_NATIVE_RES[1] / 2};
        final double[] expectedReadResolution = EXP_NATIVE_RES;
        final int[] expectedGridSize = new int[] {360, 360}; // full size image

        testFullSize(
                targetSizeX,
                targetSizeY,
                expectedRequestedResolution,
                expectedReadResolution,
                expectedGridSize);
    }

    @Test
    public void testFullSizeTargetSizeMatchesOverview() throws Exception {
        final int targetSizeX = 90, targetSizeY = 90;
        final double[] expectedRequestedResolution = new double[] {0.0166666667, 0.0166666667};
        final double[] expectedReadResolution = expectedRequestedResolution;
        final int[] expectedGridSize =
                new int[] {targetSizeX, targetSizeY}; // matches 90x90 overview

        testFullSize(
                targetSizeX,
                targetSizeY,
                expectedRequestedResolution,
                expectedReadResolution,
                expectedGridSize);
    }

    @Test
    public void testFullSizeTargetSizeDoesNotMatchOverview() throws Exception {
        final int targetSizeX = 110, targetSizeY = 110;
        final double[] expectedRequestedResolution = new double[] {0.0136363636, 0.0136363636};
        final double[] expectedReadResolution = new double[] {0.0166666667, 0.0166666667};
        final int[] expectedGridSize = new int[] {90, 90}; // closest overview: 90x90

        testFullSize(
                targetSizeX,
                targetSizeY,
                expectedRequestedResolution,
                expectedReadResolution,
                expectedGridSize);
    }

    private void testFullSize(
            int targetSizeX,
            int targetSizeY,
            final double[] expectedRequestedResolution,
            final double[] expectedReadResolution,
            final int[] expectedGridSize)
            throws Exception {
        GeoTiffReader inputReader = null, outputReader = null;
        GeoTiffWriter writer = null;
        File outputTempFile = null;
        GridCoverage2D gc = null;
        try {
            inputReader = new GeoTiffReader(inputTempFile);
            assertNotNull(inputReader);

            // read the entire coverage
            Envelope fullSizeEnvelope = inputReader.getOriginalEnvelope();
            ScaleToTarget oneFourth = new ScaleToTarget(inputReader, fullSizeEnvelope);
            oneFourth.setTargetSize(targetSizeX, targetSizeY);

            // check resolution computations
            double[] nativeResolution = oneFourth.computeNativeResolution();
            checkResolution(EXP_NATIVE_RES, nativeResolution);

            double[] requestedResolution = oneFourth.computeRequestedResolution();
            checkResolution(expectedRequestedResolution, requestedResolution);

            double[] readResolution = oneFourth.computeReadingResolution(requestedResolution);
            checkResolution(expectedReadResolution, readResolution);

            // check grid geometry size at the picked read resolution
            GridGeometry2D gridGeometry = oneFourth.getGridGeometry();
            assertEquals(expectedGridSize[0], gridGeometry.getGridRange2D().width);
            assertEquals(expectedGridSize[1], gridGeometry.getGridRange2D().height);

            // do the actual scaling
            gc = oneFourth.scale(new GeneralParameterValue[] {});

            // write scaled coverage to temp file
            outputTempFile = File.createTempFile("scale2target_", "_out");
            writer = new GeoTiffWriter(outputTempFile);
            writer.write(gc, null);

            // verify coverage has been scaled
            outputReader = new GeoTiffReader(outputTempFile);
            GridEnvelope2D outputGrid = (GridEnvelope2D) outputReader.getOriginalGridRange();
            assertEquals(targetSizeX, outputGrid.width);
            assertEquals(targetSizeY, outputGrid.height);
            assertTrue(outputReader.getOriginalEnvelope().equals(fullSizeEnvelope, DELTA, false));
        } finally {
            // final cleanup
            finalCleanUp(inputReader, outputReader, writer, gc, null, outputTempFile);
        }
    }

    @Test
    public void testROITargetSizeMatchesOverview() throws Exception {
        final int targetSizeX = 180, targetSizeY = 60; // matches 180x180 overview
        final double[] expectedRequestedResolution = new double[] {0.0083333333, 0.0083333333};
        final double[] expectedReadResolution = expectedRequestedResolution;
        final int[] expectedGridSize = new int[] {180, 60}; // matches 180x180 overview

        testROI(
                targetSizeX,
                targetSizeY,
                expectedRequestedResolution,
                expectedReadResolution,
                expectedGridSize);
    }

    @Test
    public void testROITargetSizeDoesNotMatchOverview() throws Exception {
        final int targetSizeX = 150, targetSizeY = 50; // closest overview is 180x180
        final double[] expectedRequestedResolution = new double[] {0.01, 0.01};
        final double[] expectedReadResolution = new double[] {0.0083333333, 0.0083333333};
        final int[] expectedGridSize = new int[] {180, 60}; // targetSize * requestedRes / readRes

        testROI(
                targetSizeX,
                targetSizeY,
                expectedRequestedResolution,
                expectedReadResolution,
                expectedGridSize);
    }

    private void testROI(
            int targetSizeX,
            int targetSizeY,
            final double[] expectedRequestedResolution,
            final double[] expectedReadResolution,
            final int[] expectedGridSize)
            throws Exception {
        GeoTiffReader inputReader = null, outputReader = null;
        GeoTiffWriter writer = null;
        File outputTempFile = null;
        GridCoverage2D gc = null, croppedGC = null;
        try {
            inputReader = new GeoTiffReader(inputTempFile);
            assertNotNull(inputReader);

            // read ROI
            ScaleToTarget scaleToROI = new ScaleToTarget(inputReader, ROI);
            scaleToROI.setTargetSize(targetSizeX, targetSizeY);

            // check resolution computations
            double[] nativeResolution = scaleToROI.computeNativeResolution();
            checkResolution(EXP_NATIVE_RES, nativeResolution);

            double[] requestedResolution = scaleToROI.computeRequestedResolution();
            checkResolution(expectedRequestedResolution, requestedResolution);

            double[] readResolution = scaleToROI.computeReadingResolution(requestedResolution);
            checkResolution(expectedReadResolution, readResolution);

            // check grid geometry size at the picked read resolution
            GridGeometry2D gridGeometry = scaleToROI.getGridGeometry();
            assertEquals(expectedGridSize[0], gridGeometry.getGridRange2D().width);
            assertEquals(expectedGridSize[1], gridGeometry.getGridRange2D().height);

            // setup reader parameters to have it exploit overviews
            GeneralParameterValue[] readParameters = getReaderParams(inputReader, gridGeometry);

            // read input
            gc = inputReader.read(readParameters);

            // crop first
            CropCoverage crop = new CropCoverage();
            croppedGC = crop.execute(gc, JTS.toGeometry(ROI), new NullProgressListener());

            // do the actual scaling: actually, no need to scale
            gc = scaleToROI.scale(croppedGC);

            // write scaled coverage to temp file
            outputTempFile = File.createTempFile("scale2target_", "_out");
            writer = new GeoTiffWriter(outputTempFile);
            writer.write(gc, null);

            // verify coverage has been scaled
            outputReader = new GeoTiffReader(outputTempFile);
            GridEnvelope2D outputGrid = (GridEnvelope2D) outputReader.getOriginalGridRange();
            assertEquals(targetSizeX, outputGrid.width);
            assertEquals(targetSizeY, outputGrid.height);
            assertTrue(outputReader.getOriginalEnvelope().equals(ROI, DELTA, false));
        } finally {
            // final cleanup
            finalCleanUp(inputReader, outputReader, writer, gc, croppedGC, outputTempFile);
        }
    }

    private void finalCleanUp(
            GeoTiffReader inputReader,
            GeoTiffReader outputReader,
            GeoTiffWriter writer,
            GridCoverage2D gc,
            GridCoverage2D croppedGC,
            File outputTempFile) {
        if (inputReader != null) {
            inputReader.dispose();
        }
        if (outputReader != null) {
            outputReader.dispose();
        }
        if (writer != null) {
            writer.dispose();
        }
        if (gc != null) {
            CoverageCleanerCallback.disposeCoverage(gc);
        }
        if (croppedGC != null) {
            CoverageCleanerCallback.disposeCoverage(croppedGC);
        }
        if (outputTempFile != null) {
            outputTempFile.delete();
        }
    }

    private void checkResolution(double[] expectedResolution, double[] actualResolution) {
        assertEquals(expectedResolution[0], actualResolution[0], DELTA);
        assertEquals(expectedResolution[1], actualResolution[1], DELTA);
    }

    private GeneralParameterValue[] getReaderParams(
            GridCoverage2DReader reader, GridGeometry2D gridGeometry) {
        // setup reader parameters to have it exploit overviews
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        final List<GeneralParameterDescriptor> parameterDescriptors =
                readParametersDescriptor.getDescriptor().descriptors();
        GeneralParameterValue[] readParameters = new GeneralParameterValue[] {};
        readParameters =
                CoverageUtils.mergeParameter(
                        parameterDescriptors,
                        readParameters,
                        gridGeometry,
                        AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().getCode());

        return readParameters;
    }
}
