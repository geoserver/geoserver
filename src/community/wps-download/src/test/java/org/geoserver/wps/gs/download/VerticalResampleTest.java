/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ExtremaDescriptor;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.web.wps.VerticalCRSConfigurationPanel;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.util.NullProgressListener;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class VerticalResampleTest extends WPSTestSupport {

    private static final double DELTA = 1E-6;

    @Rule public ExpectedException thrown = ExpectedException.none();

    private CoordinateReferenceSystem sourceVerticalCRS;
    private GeneralEnvelope verticalGridEnvelope;

    private static QName HETEROGENEOUS_CRS2 = new QName(WCS_URI, "hcrs2", WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add limits properties file
        File dataDir = testData.getDataDirectoryRoot();
        File userProjections = new File(dataDir, "user_projections");
        if (!userProjections.exists()) {
            userProjections.mkdir();
        }
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/epsg.properties"),
                "user_projections/epsg.properties");
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream(
                                "download-process/vertical/epsg_operations.properties"),
                "user_projections/epsg_operations.properties");
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/verticalgrid.tif"),
                "user_projections/verticalgrid.tif");
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/verticalgrid2.tif"),
                "user_projections/verticalgrid2.tif");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addRasterLayer(HETEROGENEOUS_CRS2, "heterogeneous_crs2.zip", null, getCatalog());
        CRS.reset("all");
        sourceVerticalCRS = CRS.decode("EPSG:5778");
        setVerticalCRS();
        final File file =
                new File(
                        this.getTestData().getDataDirectoryRoot(),
                        "user_projections/verticalgrid.tif");
        GeoTiffReader reader = new GeoTiffReader(file);
        verticalGridEnvelope = reader.getOriginalEnvelope();
        reader.dispose();
    }

    @Test
    public void testVerticalResamplingMissingSourceVerticalCRS()
            throws ParseException, FactoryException {
        final WPSResourceManager resourceManager =
                GeoServerExtensions.bean(WPSResourceManager.class);
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        DownloadProcess downloadProcess = new DownloadProcess(geoserver, limits, resourceManager);
        unsetVerticalCRS();
        try {
            String roiWkt =
                    "POLYGON ((-102583.25 262175.25, -100000.25 262175.25, -100000.25 260300.25, -102583.25 260300.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            thrown.expect(ProcessException.class);
            thrown.expectMessage(CoreMatchers.containsString("no source VerticalCRS"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS2), // layerName
                            null, // filter
                            "image/tiff", // outputFormat
                            targetCRS, // targetCRS
                            targetCRS,
                            bboxRoi, // roi
                            false, // cropToGeometry
                            null, // interpolation
                            null, // targetSizeX
                            null, // targetSizeY
                            null, // bandSelectIndices
                            parameters, // Writing params
                            false,
                            false,
                            0d,
                            CRS.decode("EPSG:9999", true),
                            new NullProgressListener() // progressListener
                            );

        } finally {
            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
            setVerticalCRS();
        }
    }

    @Test
    public void testVerticalResamplingOutsideOfTheGrid()
            throws ParseException, IOException, FactoryException {
        final WPSResourceManager resourceManager =
                GeoServerExtensions.bean(WPSResourceManager.class);
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        DownloadProcess downloadProcess = new DownloadProcess(geoserver, limits, resourceManager);

        // Requesting an area out of the vertical grid validity area. Pixels won't be modified
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            String roiWkt =
                    "POLYGON ((-101000.25 262175.25, -100000.25 262175.25, -100000.25 260300.25, -101000.25 260300.25, -101000.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS2), // layerName
                            null, // filter
                            "image/tiff", // outputFormat
                            targetCRS, // targetCRS
                            targetCRS,
                            bboxRoi, // roi
                            false, // cropToGeometry
                            null, // interpolation
                            null, // targetSizeX
                            null, // targetSizeY
                            null, // bandSelectIndices
                            parameters, // Writing params
                            true,
                            true,
                            0d,
                            CRS.decode("EPSG:9998", true),
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = DownloadProcessTest.extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            RenderedImage ri = gc.getRenderedImage();
            RenderedOp extremaOp = ExtremaDescriptor.create(ri, null, 1, 1, false, 1, null);

            // The original file has only pixel with values in range [1 , 2]
            // Check that the vertical resampling didn't happen due to the vertical grid not
            // covering that area
            double[][] extrema = (double[][]) extremaOp.getProperty("Extrema");
            assertEquals(extrema[0][0], 1, DELTA);
            assertEquals(extrema[1][0], 2, DELTA);

        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    @Test
    public void testVerticalResampling() throws Exception {
        final WPSResourceManager resourceManager =
                GeoServerExtensions.bean(WPSResourceManager.class);
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        DownloadProcess downloadProcess = new DownloadProcess(geoserver, limits, resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;

        try {
            String roiWkt =
                    "POLYGON ((-102500.25 260000.25, -101000.25 260000.25, -101000.25 262500.25, -102500.25 262500.25, -102500.25 260000.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS2), // layerName
                            null, // filter
                            "image/tiff", // outputFormat
                            targetCRS, // targetCRS
                            targetCRS,
                            bboxRoi, // roi
                            false, // cropToGeometry
                            null, // interpolation
                            null, // targetSizeX
                            null, // targetSizeY
                            null, // bandSelectIndices
                            parameters, // Writing params
                            true,
                            true,
                            0d,
                            CRS.decode("EPSG:9999", true),
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = DownloadProcessTest.extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            RenderedImage ri = gc.getRenderedImage();
            RenderedOp extremaOp = ExtremaDescriptor.create(ri, null, 1, 1, false, 1, null);

            // The original file has only pixel with values in range [1 , 2]
            // On that ROI, the vertical grid has values around [60 , 160]
            // Grid values have been exaggerated for testing purposes

            // Check that the vertically interpolated values are coherent.
            // Not using equals check to allow some tolerance for the bilinear interpolation on the
            // grid values
            double[][] extrema = (double[][]) extremaOp.getProperty("Extrema");
            assertTrue(extrema[0][0] > 60);
            assertTrue(extrema[1][0] < 163);

        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    private void setVerticalCRS() {
        Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(HETEROGENEOUS_CRS2.getLocalPart());
        coverageInfo.getMetadata().put(VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY, "EPSG:5778");
        catalog.save(coverageInfo);
    }

    private void unsetVerticalCRS() {
        Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(HETEROGENEOUS_CRS2.getLocalPart());
        coverageInfo.getMetadata().remove(VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY);
        catalog.save(coverageInfo);
    }
}
