/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.COMPRESSION;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.TILEHEIGHT;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.TILEWIDTH;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.TILING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.PrivateTIFFTagSet;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.ppio.WFSPPIO;
import org.geoserver.wps.ppio.ZipArchivePPIO;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.MathTransform2D;
import org.geotools.api.util.InternationalString;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.DataUtilities;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.geopkg.TileReader;
import org.geotools.image.test.ImageAssert;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.springframework.util.MimeType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class tests checks if the DownloadProcess class behaves correctly.
 *
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class DownloadProcessTest extends WPSTestSupport {

    public interface Parser {

        SimpleFeatureCollection parse(FileInputStream t) throws Exception;
    }

    private static final FilterFactory FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;
    private static final double EPS = 1e-6;

    private static QName MIXED_RES = new QName(WCS_URI, "mixedres", WCS_PREFIX);
    private static QName HETEROGENEOUS_CRS = new QName(WCS_URI, "hcrs", WCS_PREFIX);
    private static QName HETEROGENEOUS_CRS2 = new QName(WCS_URI, "hcrs2", WCS_PREFIX);
    private static QName HETEROGENEOUS_NODATA = new QName(WCS_URI, "hcrs_nodata", WCS_PREFIX);
    private static QName TIMESERIES = new QName(WCS_URI, "timeseries", WCS_PREFIX);
    private static QName SHORT = new QName(WCS_URI, "short", WCS_PREFIX);
    private static QName FLOAT = new QName(WCS_URI, "float", WCS_PREFIX);

    private static Set<String> GTIFF_EXTENSIONS = new HashSet<>();
    private static Set<String> PNG_EXTENSIONS = new HashSet<>();
    private static Set<String> JPEG_EXTENSIONS = new HashSet<>();
    private static Set<String> XML_EXTENSIONS = new HashSet<>();
    private static Set<String> JSON_EXTENSIONS = new HashSet<>();
    private static Map<String, Set<String>> FORMAT_TO_EXTENSIONS = new HashMap<>();

    private static final CoordinateReferenceSystem WGS84;
    private static final double DELTA = 1E-6;

    static {
        GTIFF_EXTENSIONS.add("tif");
        GTIFF_EXTENSIONS.add("tiff");
        GTIFF_EXTENSIONS.add("geotiff");
        FORMAT_TO_EXTENSIONS.put("GTIFF", GTIFF_EXTENSIONS);

        PNG_EXTENSIONS.add("png");
        FORMAT_TO_EXTENSIONS.put("PNG", PNG_EXTENSIONS);

        JPEG_EXTENSIONS.add("jpg");
        JPEG_EXTENSIONS.add("jpeg");
        FORMAT_TO_EXTENSIONS.put("JPEG", JPEG_EXTENSIONS);

        XML_EXTENSIONS.add("xml");
        FORMAT_TO_EXTENSIONS.put("XML", XML_EXTENSIONS);

        JSON_EXTENSIONS.add("json");
        FORMAT_TO_EXTENSIONS.put("JSON", JSON_EXTENSIONS);
        try {
            WGS84 = CRS.decode("EPSG:4326", true);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    public static class AutoDisposableGeoTiffReader extends GeoTiffReader implements AutoCloseable {

        public AutoDisposableGeoTiffReader(File file) throws DataSourceException {
            super(file);
        }

        @Override
        public void close() {
            dispose();
        }

        public AutoDisposableGridCoverage2D read() throws IOException {
            GridCoverage2D gc = super.read(null);
            assertNotNull(gc);
            return new AutoDisposableGridCoverage2D("", gc);
        }
    }

    /**
     * This method is used for decoding an input file.
     *
     * @param input the input stream to decode
     * @param tempDirectory temporary directory on where the file is decoded.
     * @return the object the decoded file
     * @throws Exception the exception TODO review
     */
    public static File decode(InputStream input, File tempDirectory) throws Exception {

        // unzip to the temporary directory
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry = null;

            // Copy the whole file in the new position
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(tempDirectory, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdir();
                } else {
                    int count;
                    byte[] data = new byte[4096];
                    // write the files to the disk

                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        while ((count = zis.read(data)) != -1) {
                            fos.write(data, 0, count);
                        }
                        fos.flush();
                    }
                }
                zis.closeEntry();
            }
        }

        return tempDirectory;
    }

    /** Test ROI used */
    static final Polygon roi;

    static final Polygon ROI2;

    static final Polygon ROI3;

    static {
        try {
            roi = (Polygon)
                    new WKTReader2()
                            .read(
                                    "POLYGON (( 500116.08576537756 499994.25579707103, 500116.08576537756 500110.1012210889, 500286.2657688021 500110.1012210889, 500286.2657688021 499994.25579707103, 500116.08576537756 499994.25579707103 ))");

            ROI2 = (Polygon) new WKTReader2().read("POLYGON (( -125 30, -116 30, -116 45, -125 45, -125 30))");

            ROI3 = (Polygon)
                    new WKTReader2()
                            .read(
                                    "POLYGON (( 356050 5520000, 791716 5520000, 791716 5655096, 356050 5655096, 356050 5520000))");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(MockData.USA_WORLDIMG, "usa.zip", MockData.PNG, getCatalog());
        testData.addRasterLayer(MockData.WORLD, "world.tiff", MockData.TIFF, getCatalog());
        testData.addRasterLayer(MIXED_RES, "mixedres.zip", null, getCatalog());
        testData.addRasterLayer(HETEROGENEOUS_CRS, "heterogeneous_crs.zip", null, getCatalog());
        testData.addRasterLayer(HETEROGENEOUS_CRS2, "heterogeneous_crs2.zip", null, getCatalog());
        testData.addRasterLayer(
                HETEROGENEOUS_NODATA, "hetero_nodata.zip", null, null, DownloadProcessTest.class, getCatalog());
        testData.addRasterLayer(SHORT, "short.zip", null, getCatalog());
        testData.addRasterLayer(FLOAT, "float.zip", null, getCatalog());
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add limits properties file
        testData.copyTo(
                DownloadProcessTest.class.getClassLoader().getResourceAsStream("download-process/download.properties"),
                "download.properties");
    }

    @Before
    public void clearPolygons() throws IOException {
        revertLayer(MockData.POLYGONS);
    }

    protected WPSResourceManager getResourceManager() {
        return GeoServerExtensions.bean(WPSResourceManager.class);
    }

    /**
     * Test get features as shapefile.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetFeaturesAsShapefile() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        // Download
        RawData shpeZip = executeVectorDownload(
                downloadProcess, MockData.POLYGONS, "application/zip", "application/zip", "EPSG:32615", roi, false);

        try (AutoCloseableResource resource = new AutoCloseableResource(getResourceManager(), shpeZip);
                InputStream is = new FileInputStream(resource.getFile())) {
            ShapefileDataStore store = decodeShape(is);
            SimpleFeatureCollection rawTarget = store.getFeatureSource().getFeatures();
            Assert.assertNotNull(rawTarget);
            Assert.assertEquals(rawSource.size(), rawTarget.size());
            store.dispose();
        }
    }

    @Test
    public void testGetFeaturesWithNoROIAsShapefile() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        // Download
        RawData shpeZip = executeVectorDownload(
                downloadProcess,
                MockData.POLYGONS,
                "application/zip",
                "application/zip",
                null,
                null,
                false,
                "EPSG:4326");

        try (AutoCloseableResource resource = new AutoCloseableResource(getResourceManager(), shpeZip);
                InputStream is = new FileInputStream(resource.getFile())) {
            ShapefileDataStore store = decodeShape(is);
            SimpleFeatureCollection rawTarget = store.getFeatureSource().getFeatures();
            Assert.assertNotNull(rawTarget);
            Assert.assertEquals(rawSource.size(), rawTarget.size());
            store.dispose();
        }
    }

    @Test
    public void testGetFeaturesAsGeoPackageZipped() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        // Download
        RawData gpkgZip = executeVectorDownload(
                downloadProcess,
                MockData.POLYGONS,
                GeopkgVectorPPIO.MIME_TYPE,
                "application/zip",
                "EPSG:32615",
                roi,
                false);

        try (AutoCloseableResource resource = new AutoCloseableResource(getResourceManager(), gpkgZip);
                InputStream is = new FileInputStream(resource.getFile());
                GeoPackage geoPackage = decodeGeoPackage(is)) {

            // Making sure the provided file has the expected file extension
            assertExpectedExtension(gpkgZip, "zip");

            FeatureEntry entry = geoPackage.feature("Polygons");
            assertNotNull(entry);
            try (SimpleFeatureReader reader = geoPackage.reader(entry, Filter.INCLUDE, Transaction.AUTO_COMMIT)) {
                assertEquals(rawSource.size(), DataUtilities.collection(reader).size());
            }
        }
    }

    @Test
    public void testGetFeaturesAsGeoPackageExtension() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        RawData gpkg = executeVectorDownload(
                downloadProcess,
                MockData.POLYGONS,
                GeopkgVectorPPIO.MIME_TYPE,
                GeopkgVectorPPIO.MIME_TYPE,
                "EPSG:32615",
                roi,
                false);

        try (AutoCloseableResource ignored = new AutoCloseableResource(getResourceManager(), gpkg)) {

            // Simply check that the output is in the expected format.
            // No need to validate the data, being done on another test
            assertExpectedExtension(gpkg, "gpkg");
        }
    }

    /**
     * Test downloading with a duplicate style
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadWithDuplicateStyle() throws Exception {
        String polygonsName = getLayerId(MockData.POLYGONS);
        LayerInfo li = getCatalog().getLayerByName(polygonsName);
        // setup an alternative equal to the main style
        li.getStyles().add(li.getDefaultStyle());
        getCatalog().save(li);

        testGetFeaturesAsShapefile();
    }

    /**
     * Test filtered clipped features.
     *
     * @throws Exception the exception
     */
    @Test
    public void testFilteredClippedFeatures() throws Exception {
        // Creates the new process for the download
        WPSResourceManager resourceManager = getResourceManager();
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // ROI object
        Polygon roi = (Polygon)
                new WKTReader2()
                        .read(
                                "POLYGON ((0.0008993124415341 0.0006854377923293, 0.0008437876520112 0.0006283489242283, 0.0008566913002806 0.0005341131898971, 0.0009642217025257 0.0005188634237605, 0.0011198475210477 0.000574779232928, 0.0010932581852198 0.0006572843779233, 0.0008993124415341 0.0006854377923293))");

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.BUILDINGS));
        // Download
        RawData shpeZip = null;
        ShapefileDataStore store = null;
        try {
            SimpleFeatureCollection rawSource =
                    (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();

            shpeZip = downloadProcess.execute(
                    getLayerId(MockData.BUILDINGS), // layerName
                    CQL.toFilter("ADDRESS = '123 Main Street'"), // filter
                    "application/zip",
                    "application/zip",
                    null, // targetCRS
                    DefaultGeographicCRS.WGS84, // roiCRS
                    roi, // roi
                    true, // cropToGeometry
                    null, // interpolation
                    null, // targetSizeX
                    null, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            // Final checks on the result
            Assert.assertNotNull(shpeZip);
            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, shpeZip);
                    InputStream in = new FileInputStream(resource.getFile())) {
                store = decodeShape(in);
                SimpleFeatureCollection rawTarget = store.getFeatureSource().getFeatures();
                Assert.assertNotNull(rawTarget);

                Assert.assertEquals(1, rawTarget.size());

                SimpleFeature srcFeature = rawSource.features().next();
                SimpleFeature trgFeature = rawTarget.features().next();

                Assert.assertEquals(srcFeature.getAttribute("ADDRESS"), trgFeature.getAttribute("ADDRESS"));

                // Final checks on the ROI
                Geometry srcGeometry = (Geometry) srcFeature.getDefaultGeometry();
                Geometry trgGeometry = (Geometry) trgFeature.getDefaultGeometry();

                Assert.assertTrue(
                        "Target geometry clipped and included into the source one", srcGeometry.contains(trgGeometry));
            }
        } finally {
            if (store != null) {
                store.dispose();
            }
        }
    }

    /** This method is used for extracting only the specified format files from a zipfile archive */
    public static File[] extractFilesFromResource(final RawData zipResource, String format)
            throws IOException, URISyntaxException {
        File tempDirectory = new File(DownloadProcessTest.class.getResource(".").toURI());
        tempDirectory = new File(tempDirectory, Long.toString(System.nanoTime()));
        Assert.assertTrue(tempDirectory.mkdir());

        try (InputStream in = zipResource.getInputStream()) {
            IOUtils.decompress(in, tempDirectory);
        }
        Set<String> extensions = FORMAT_TO_EXTENSIONS.get(format);

        File[] files = tempDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String ext = FilenameUtils.getExtension(name);
                for (String extension : extensions) {
                    if (ext.equalsIgnoreCase(extension)) {
                        return true;
                    }
                }
                return false;
            }
        });
        return files;
    }

    /**
     * Test get features as gml.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetFeaturesAsGML() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();

        // Download as GML 2
        RawData gml2Zip = executeVectorDownload(
                downloadProcess,
                MockData.POLYGONS,
                "application/wfs-collection-1.0",
                "application/zip",
                "EPSG:32615",
                roi,
                false);

        // Final checks on the result
        checkResult(rawSource, gml2Zip, "XML", is -> (SimpleFeatureCollection) new WFSPPIO.WFS10().decode(is));

        // Download as GML 3
        RawData gml3Zip = executeVectorDownload(
                downloadProcess,
                MockData.POLYGONS,
                "application/wfs-collection-1.1",
                "application/zip",
                "EPSG:32615",
                roi,
                false);

        // Final checks on the result
        checkResult(rawSource, gml3Zip, "XML", is -> (SimpleFeatureCollection) new WFSPPIO.WFS11().decode(is));
    }

    private RawData executeVectorDownload(
            DownloadProcess downloadProcess,
            QName polygons,
            String mimeType,
            String outputFormat,
            String roiCRS,
            Polygon roi,
            boolean cropToGeometry)
            throws FactoryException {
        return executeVectorDownload(
                downloadProcess, polygons, mimeType, outputFormat, roiCRS, roi, cropToGeometry, null);
    }

    private RawData executeVectorDownload(
            DownloadProcess downloadProcess,
            QName polygons,
            String mimeType,
            String outputFormat,
            String roiCRS,
            Polygon roi,
            boolean cropToGeometry,
            String targetCRS)
            throws FactoryException {
        return downloadProcess.execute(
                getLayerId(polygons), // layerName
                null, // filter
                mimeType,
                outputFormat,
                targetCRS == null ? null : CRS.decode(targetCRS), // targetCRS
                roiCRS == null ? null : CRS.decode(roiCRS), // roiCRS
                roi, // roi
                cropToGeometry, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );
    }

    /**
     * Test get features as geo json.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetFeaturesAsGeoJSON() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();
        // Download the file as Json
        RawData jsonZip = executeVectorDownload(
                downloadProcess, MockData.POLYGONS, "application/json", "application/zip", "EPSG:32615", roi, false);

        checkResult(rawSource, jsonZip, "JSON", fis -> new GeoJSONReader(fis).getFeatures());
    }

    private void checkResult(SimpleFeatureCollection rawSource, RawData result, String format, Parser parser)
            throws Exception {
        Assert.assertNotNull(result);
        File[] files = extractFilesFromResource(result, format);
        try (FileInputStream fis = new FileInputStream(files[0])) {
            SimpleFeatureCollection rawTarget = parser.parse(fis);
            Assert.assertNotNull(rawTarget);
            Assert.assertEquals(rawSource.size(), rawTarget.size());
        } finally {
            IOUtils.delete(files[0].getParentFile());
        }
    }

    /**
     * Test download of raster data.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadRaster() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // test ROI
        double firstXRoi = -127.57473954542964;
        double firstYRoi = 54.06575021619523;
        Polygon roi = (Polygon)
                new WKTReader2()
                        .read(
                                "POLYGON (( "
                                        + firstXRoi
                                        + " "
                                        + firstYRoi
                                        + ", -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);
        // ROI reprojection
        Polygon roiResampled =
                (Polygon) JTS.transform(roi, CRS.findMathTransform(WGS84, CRS.decode("EPSG:900913", true)));
        // Download the coverage as tiff (Not reprojected)
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                true, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            assertExpectedExtension(raster, "tiff");

            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.611129008700004, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -123.95304462109999, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(54.0861661371, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);

            // Take a pixel within the ROI
            byte[] result = (byte[]) gc.evaluate(new Position2D(new Point2D.Double(firstXRoi, firstYRoi - 1E-4)));
            assertNotEquals(0, result[0]);
            assertNotEquals(0, result[1]);
            assertNotEquals(0, result[2]);

            // Take a pixel outside of the ROI
            result = (byte[]) gc.evaluate(new Position2D(new Point2D.Double(firstXRoi - 2, firstYRoi - 0.5)));
            Assert.assertEquals(0, result[0]);
            Assert.assertEquals(0, result[1]);
            Assert.assertEquals(0, result[2]);
        }

        // Download the coverage as tiff with clipToROI set to False (Crop on envelope)
        raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff", // outputFormat
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.611129008700004, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -123.95304462109999, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(54.0861661371, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);

            // Take a pixel within the ROI
            byte[] result = (byte[]) gc.evaluate(new Position2D(new Point2D.Double(firstXRoi, firstYRoi - 1E-4)));
            assertNotEquals(0, result[0]);
            assertNotEquals(0, result[1]);
            assertNotEquals(0, result[2]);

            // Take a pixel outside of the ROI geometry but within the ROI's envelope
            // (We have set cropToROI = False)
            result = (byte[]) gc.evaluate(new Position2D(new Point2D.Double(firstXRoi - 2, firstYRoi - 0.5)));
            assertNotEquals(0, result[0]);
            assertNotEquals(0, result[1]);
            assertNotEquals(0, result[2]);
        }
        // Download the coverage as tiff (Reprojected)
        RawData resampled = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff", // outputFormat
                CRS.decode("EPSG:900913", true), // targetCRS
                CRS.decode("EPSG:900913", true), // roiCRS
                roiResampled, // roi
                true, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, resampled);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gcResampled = reader.read()) {

            Assert.assertEquals(
                    -1.457024062347863E7,
                    gcResampled.getEnvelope().getLowerCorner().getOrdinate(0),
                    DELTA);
            Assert.assertEquals(
                    6209706.404894806,
                    gcResampled.getEnvelope().getLowerCorner().getOrdinate(1),
                    DELTA);
            Assert.assertEquals(
                    -1.379838980949677E7,
                    gcResampled.getEnvelope().getUpperCorner().getOrdinate(0),
                    DELTA);
            Assert.assertEquals(
                    7187128.139081598,
                    gcResampled.getEnvelope().getUpperCorner().getOrdinate(1),
                    DELTA);
        }
    }

    @Test
    public void testDownloadRasterGeoPackage() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Download the coverage as GeoPackage (Not reprojected)
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                GeopkgPPIO.MIME_TYPE, // mimeType
                GeopkgPPIO.MIME_TYPE, // resultFormat
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                true, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                GeoPackage gpkg = new GeoPackage(resource.getFile())) {

            // Making sure the provided file has the expected file extension
            assertExpectedExtension(raster, "gpkg");

            TileEntry entry = gpkg.tile(MockData.USA_WORLDIMG.getLocalPart());
            assertNotNull(entry);
            ReferencedEnvelope bounds = entry.getBounds();
            CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(MockData.USA_WORLDIMG));

            // raster is not rescaled, should be the same
            org.geotools.api.geometry.Bounds envelope =
                    ci.getGridCoverage(null, null).getEnvelope();
            assertEquals(envelope.getMinimum(0), bounds.getMinimum(0), 0d);
            assertEquals(envelope.getMaximum(0), bounds.getMaximum(0), 0d);
            assertEquals(envelope.getMinimum(1), bounds.getMinimum(1), 0d);
            assertEquals(envelope.getMaximum(1), bounds.getMaximum(1), 0d);

            // the tile bounds are larger than the bounds (tile is bigger)
            assertTrue(entry.getTileMatrixSetBounds().contains(new ReferencedEnvelope(bounds)));

            List<TileMatrix> matrices = entry.getTileMatricies();
            assertEquals(1, matrices.size());
            TileMatrix matrix = matrices.get(0);
            assertEquals(0, (int) matrix.getZoomLevel());
            assertEquals(256, (int) matrix.getTileWidth());
            assertEquals(256, (int) matrix.getTileHeight());
            assertEquals(1, (int) matrix.getMatrixWidth());
            assertEquals(1, (int) matrix.getMatrixHeight());
            assertEquals(0.070037, matrix.getXPixelSize(), EPS);
            assertEquals(0.055868, matrix.getYPixelSize(), EPS);

            try (TileReader reader = gpkg.reader(entry, 0, 0, 0, 0, 0, 0)) {
                Tile tile = reader.next();
                assertEquals(0, (int) tile.getRow());
                assertEquals(0, (int) tile.getColumn());
                assertEquals(0, (int) tile.getZoom());
                byte[] data = tile.getData();
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));

                // image should have been expanded to tile size
                assertEquals(256, image.getWidth());
                assertEquals(256, image.getHeight());
            }
        }
    }

    @Test
    public void testDownloadRasterGeoPackageInZippedFormat() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Download the coverage as GeoPackage (Not reprojected)
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                GeopkgPPIO.MIME_TYPE, // mimeType
                "application/zip", // resultFormat
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                true, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource ignored = new AutoCloseableResource(resourceManager, raster)) {
            // Simply check that the output is in the expected format.
            // No need to validate the data, being done on another test
            assertExpectedExtension(raster, "zip");
        }
    }

    private void assertExpectedExtension(RawData raster, String extension) {
        assertEquals(
                "org.geoserver.wps.process.ResourceRawData", raster.getClass().getName());
        ResourceRawData rrd = (ResourceRawData) raster;
        assertEquals(extension, rrd.getFileExtension());
    }

    /**
     * Test Writing parameters are used, nodata not being set
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadWithWriteParametersWithoutNodata() throws Exception {
        testWriteParameters(false);
    }

    /**
     * Test Writing parameters are used
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadWithWriteParameters() throws Exception {
        testWriteParameters(true);
    }

    private void testWriteParameters(boolean writeNodata) throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // test ROI
        double firstXRoi = -127.57473954542964;
        double firstYRoi = 54.06575021619523;
        Polygon roi = (Polygon)
                new WKTReader2()
                        .read(
                                "POLYGON (( "
                                        + firstXRoi
                                        + " "
                                        + firstYRoi
                                        + ", -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        // Setting up custom writing parameters
        Parameters parameters = new Parameters();
        List<Parameter> parametersList = parameters.getParameters();
        final String tileWidthValue = "32";
        final String tileHeightValue = "32";
        final String compressionValue = "LZW";

        parametersList.add(new Parameter("tilewidth", tileWidthValue));
        parametersList.add(new Parameter("tileheight", tileHeightValue));
        parametersList.add(new Parameter("compression", compressionValue));
        parametersList.add(new Parameter("not_supported_ignore_this", "NOT_VALID_IGNORE_THIS"));

        // Note that nodata is written by default on GeoTiffWriter as soon
        // as a nodata is found on the input gridCoverage
        if (!writeNodata) {
            parametersList.add(new Parameter("writenodata", "false"));
        }

        // Download the coverage as tiff
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                true, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                parameters, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        // Final checks on the result
        Assert.assertNotNull(raster);

        final Map<String, String> expectedTiffTagValues = new HashMap<>();
        expectedTiffTagValues.put(Integer.toString(BaselineTIFFTagSet.TAG_TILE_WIDTH), tileWidthValue);
        expectedTiffTagValues.put(Integer.toString(BaselineTIFFTagSet.TAG_TILE_LENGTH), tileHeightValue);
        expectedTiffTagValues.put(Integer.toString(BaselineTIFFTagSet.TAG_COMPRESSION), compressionValue);
        expectedTiffTagValues.put(Integer.toString(PrivateTIFFTagSet.TAG_GDAL_NODATA), "0.0");

        int matchingStillRequired = expectedTiffTagValues.size();
        if (!writeNodata) {
            // we keep the map entry for scan but we make sure
            // the matching is missing
            matchingStillRequired--;
        }

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile())) {
            GeoTiffIIOMetadataDecoder metadata = reader.getMetadata();
            IIOMetadataNode rootNode = metadata.getRootNode();

            // Parsing metadata for check
            Node child = rootNode.getFirstChild().getFirstChild();

            if (child != null) {
                Set<String> expectedTiffTagKeys = expectedTiffTagValues.keySet();
                while (child != null) {
                    NamedNodeMap map = child.getAttributes();
                    if (map != null) { // print attribute values
                        int length = map.getLength();

                        // Loop over the TIFFTags
                        for (int i = 0; i < length; i++) {
                            Node attr = map.item(i);
                            String nodeValue = attr.getNodeValue();

                            if (expectedTiffTagKeys.contains(nodeValue)) {
                                // We have found a required TIFFTag
                                Node childNode = child.getFirstChild().getFirstChild();
                                NamedNodeMap attributesMap = childNode.getAttributes();
                                int attributesMapLength = attributesMap.getLength();
                                for (int k = 0; k < attributesMapLength; k++) {
                                    Node attrib = attributesMap.item(k);

                                    // Check the TIFFTag Value is matching the expected one
                                    if (expectedTiffTagValues.get(nodeValue).equals(attrib.getNodeValue())) {
                                        matchingStillRequired--;
                                    }
                                }
                            }
                        }
                    }
                    child = child.getNextSibling();
                }
            }
            Assert.assertEquals(0, matchingStillRequired);
        }
    }

    /**
     * Test download of selected bands of raster data. Result contains only bands 0 and 2.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadRasterSelectedBands() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        ///////////////////////////////////////
        //      test full coverage           //
        ///////////////////////////////////////

        // Download the coverage as tiff
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                new int[] {0, 2}, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // check bands
            Assert.assertEquals(2, gc.getNumSampleDimensions());

            // check visible band index for new coverage
            Assert.assertEquals(0, CoverageUtilities.getVisibleBand(gc));

            // check non existing band index
            assertNotEquals(3, gc.getNumSampleDimensions());
        }
    }

    /**
     * Test download of selected bands of raster data, scald and using a ROI area. Result contains only band 1.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadRasterSelectedBandsScaledWithROI() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        ///////////////////////////////////////
        //      test full coverage           //
        ///////////////////////////////////////

        Polygon roi = (Polygon) new WKTReader2()
                .read("POLYGON (( "
                        + "-127.57473954542964 54.06575021619523, "
                        + "-130.88669845369998 52.00807146727025, "
                        + "-129.50812897394974 49.85372324691927, "
                        + "-130.5300633861675 49.20465679591609, "
                        + "-129.25955033314003 48.60392508062591, "
                        + "-128.00975216684665 50.986137055052474, "
                        + "-125.8623089087404 48.63154492960477, "
                        + "-123.984159178178 50.68231871628503, "
                        + "-126.91186316993704 52.15307567440926, "
                        + "-125.3444367403868 53.54787804784162, "
                        + "-127.57473954542964 54.06575021619523 "
                        + "))");
        roi.setSRID(4326);

        // Download the coverage as tiff
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                false, // cropToGeometry
                null, // interpolation
                40, // targetSizeX
                40, // targetSizeY
                new int[] {1}, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // check bands
            Assert.assertEquals(1, gc.getNumSampleDimensions());

            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(40, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(40, Math.round(originalGridRange.getHeight()));

            // check envelope
            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(48.5552612829, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -124.05382943906582, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    54.00577111704634, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);
        }
    }

    @Test
    public void testMaskedGeoPackageRasterDownload() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // L shaped polygon covering top left, top right and bottom right quadrants (hand drew
        // in QGIS)
        Polygon roi = (Polygon)
                new WKTReader2()
                        .read(
                                "Polygon ((-131.15837491405085302 54.46008784574385686, -123.67994074117365244 54.40520025548421046, -123.61133125334909266 48.21662445370877492, -125.71078158078067588 48.2303463512736883, -125.99894142964383548 52.6762411623052671, -131.15837491405085302 52.82718203551930003, -131.15837491405085302 54.46008784574385686))");
        roi.setSRID(4326);

        // Download the coverage as tiff
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                GeopkgRasterPPIO.MIME_TYPE, // outputFormat
                GeopkgRasterPPIO.MIME_TYPE,
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                true, // cropToGeometry
                null, // interpolation
                512, // targetSizeX (get at least 4 tiles)
                512, // targetSizeY
                new int[] {1}, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                GeoPackage gpkg = new GeoPackage(resource.getFile())) {

            TileEntry entry = gpkg.tile(MockData.USA_WORLDIMG.getLocalPart());
            assertNotNull(entry);
            ReferencedEnvelope bounds = entry.getBounds();

            // the tile bounds are larger than the bounds (tile is bigger)
            assertTrue(entry.getTileMatrixSetBounds().contains(new ReferencedEnvelope(bounds)));

            List<TileMatrix> matrices = entry.getTileMatricies();
            assertEquals(1, matrices.size());
            TileMatrix matrix = matrices.get(0);
            assertEquals(0, (int) matrix.getZoomLevel());
            assertEquals(256, (int) matrix.getTileWidth());
            assertEquals(256, (int) matrix.getTileHeight());
            assertEquals(2, (int) matrix.getMatrixWidth());
            assertEquals(2, (int) matrix.getMatrixHeight());
            assertEquals(0.014713, matrix.getXPixelSize(), EPS);
            assertEquals(0.012198, matrix.getYPixelSize(), EPS);

            // collect all tiles and do minimal checks on them
            Set<Point> tiles = new HashSet<>();
            try (TileReader reader = gpkg.reader(entry, -1000, 1000, -1000, 1000, -1000, 1000)) {
                while (reader.hasNext()) {
                    Tile tile = reader.next();
                    tiles.add(new Point(tile.getColumn(), tile.getRow()));
                    assertEquals(0, (int) tile.getZoom());
                    byte[] data = tile.getData();
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
                    assertEquals(256, image.getWidth());
                    assertEquals(256, image.getHeight());
                    // gray + alpha, the alpha channel has been added to handle ROI
                    assertEquals(2, image.getSampleModel().getNumBands());
                }
            }

            // the lower left corner tile is not covered by the ROI, should have been skipped
            assertEquals(3, tiles.size());
            assertThat(tiles, CoreMatchers.hasItems(new Point(0, 0), new Point(1, 0), new Point(1, 1)));
        }
    }

    /**
     * Test download of raster data. The output is scaled to fit exactly the provided size.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadScaledRaster() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        ///////////////////////////////////////
        //      test full coverage           //
        ///////////////////////////////////////

        // Download the coverage as tiff
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                80, // targetSizeX
                80, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(80, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(80, Math.round(originalGridRange.getHeight()));

            // check envelope
            Assert.assertEquals(-130.8866985, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(48.5552613, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(-123.8830077, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(54.1420339, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);
        }

        ///////////////////////////////////////
        //      test partial input           //
        ///////////////////////////////////////

        // Download the coverage as tiff
        RawData largerDownload = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                160, // targetSizeX
                null, // targetSizeY not specified, will be calculated based on
                // targetSizeX and aspect ratio of the original image
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        // Final checks on the result
        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, largerDownload);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {
            assertNotNull(gc);

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(160, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(160, Math.round(originalGridRange.getHeight()));
        }

        //////////////////////////////////
        //      test with ROI           //
        //////////////////////////////////

        Polygon roi = (Polygon) new WKTReader2()
                .read("POLYGON (( -127.57473954542964 54.06575021619523, "
                        + "-130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927, "
                        + "-130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, "
                        + "-128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, "
                        + "-123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, "
                        + "-125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        // Download the coverage as tiff
        RawData resampled = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                true, // cropToGeometry
                null, // interpolation
                80, // targetSizeX
                80, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, resampled);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(80, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(80, Math.round(originalGridRange.getHeight()));

            // check envelope
            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.623544058877776, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -123.95304462109999, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(54.0861661371, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);
        }
    }

    /**
     * Test download of raster data. The output is scaled to fit exactly the provided size. Check that Datatype won't be
     * modified
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadScaledRasterPreservingDatatype() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Download the coverage as tiff
        RawData raster = downloadProcess.execute(
                getLayerId(SHORT), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                80, // targetSizeX
                80, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        // Final checks on the result. This test only focus on checking that the
        // datatype get preserved
        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(80, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(80, Math.round(originalGridRange.getHeight()));

            // This JUNIT test only focus on checking that the datatype get preserved
            Assert.assertEquals(
                    DataBuffer.TYPE_SHORT,
                    gc.getRenderedImage().getSampleModel().getDataType());
        }

        raster = downloadProcess.execute(
                getLayerId(FLOAT), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                80, // targetSizeX
                80, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // This JUNIT test only focus on checking that the datatype get preserved
            Assert.assertEquals(
                    DataBuffer.TYPE_FLOAT,
                    gc.getRenderedImage().getSampleModel().getDataType());
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a request with a
     * TargetCRS matching one of the underlying CRS of that mosaic should result in no reprojection on granules with
     * that CRS as native.
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSMinimizeReprojection() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 3 granules on
        // 3 different UTM zones (32631, 32632, 32633), being exposed as a 4326 Mosaic
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Getting one of the original files being used by this test: green.tif
        // a UTM 32632 granule with a green fill and a couple of white lines
        // [1 horizontal, 1 vertical crossing the first one and 2 oblique tying the vertexes]
        // having a pattern like this (let's call it the bow-tie :D ):
        //   /|
        //  / |
        // ---+---
        //    | /
        //    |/

        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);

        String roiWkt = "POLYGON((180000 600000, 820000 600000, 820000 1200000, 180000 1200000, " + "180000 600000))";
        Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

        Parameters parameters = new Parameters();
        List<Parameter> parametersList = parameters.getParameters();
        parametersList.add(new Parameter("writenodata", "false"));
        RawData raster = downloadProcess.execute(
                getLayerId(HETEROGENEOUS_CRS), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                targetCRS, // targetCRS
                targetCRS,
                bboxRoi, // roi
                false, // cropToGeometry
                null, // interpolation
                200, // targetSizeX
                200, // targetSizeY
                null, // bandSelectIndices
                parameters, // Writing params
                true,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );
        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            assertTrue(hasPerfectStraightHorizontalLine(gc.getRenderedImage()));
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a request with a
     * TargetCRS matching one of the underlying CRS, and asking for the best available resolution from matching CRS will
     * result in minimal processing
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSBestResolution() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 3 granules on
        // 3 different UTM zones (32631, 32632, 32633), being exposed as a 4326 Mosaic

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Getting one of the original files being used by this test: green.tif
        // a UTM 32632 granule with a green fill and a couple of white lines
        // [1 horizontal, 1 vertical crossing the first one and 2 oblique tying the vertexes]
        // having a pattern like this (let's call it the bow-tie :D ):
        //   /|
        //  / |
        // ---+---
        //    | /
        //    |/
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        RenderedImage referenceImage;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        try (AutoDisposableGeoTiffReader referenceReader = new AutoDisposableGeoTiffReader(file);
                AutoDisposableGridCoverage2D referenceGc = referenceReader.read()) {
            referenceImage = referenceGc.getRenderedImage();
            // Setting filter to get the granule with resolution
            final PropertyName property = FF.property("location");
            Filter filter = FF.like(property, "green.tif");

            String roiWkt =
                    "POLYGON((160000 600000, 840000 600000, 840000 1200000, 160000 1200000," + " 160000 600000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            RawData raster = downloadProcess.execute(
                    getLayerId(HETEROGENEOUS_CRS), // layerName
                    filter, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
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
                    null,
                    new NullProgressListener() // progressListener
                    );

            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                    AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                    AutoDisposableGridCoverage2D gc = reader.read()) {

                // Compare the downloaded raster with the original tiff.
                // If reprojection to common CRS would have been involved,
                // the above tie-bow pattern would have been distorted, making
                // this comparison fail
                ImageAssert.assertEquals(referenceImage, gc.getRenderedImage(), 5);

                // also make sure the referencing is the same
                assertEquals(referenceGc.getEnvelope2D(), gc.getEnvelope2D());
            }
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a request with a
     * TargetCRS matching the west-most granule CRS, and asking for the best available resolution from matching CRS will
     * result in minimal processing and a bbox matching the native resolution of the data
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSBestResolutionWestMost() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 3 granules on
        // 3 different UTM zones (32631, 32632, 32633), being exposed as a 4326 Mosaic
        // The request hits all and we request data in 32631, the west-most of the granules,
        // making sure the native location and CRS is preserved

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Getting one of the original files being used by this test: green.tif
        // a UTM 32631 granule with a red fill and a white line in the middle
        // xxxxxxx
        // xxxxxxx
        // -------
        // xxxxxxx
        // xxxxxxx
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/red.tif");

        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32631", true);
        MapProjection.SKIP_SANITY_CHECKS = true;
        try (AutoDisposableGeoTiffReader referenceReader = new AutoDisposableGeoTiffReader(file);
                AutoDisposableGridCoverage2D ignored = referenceReader.read()) {
            // tests go out of the stricly sane area for one of the UTMs, could cause 0.006
            // meters of error and that makes the assertions fail..-
            String roiWkt =
                    "POLYGON((150000 550000, 2300000 550000, 2300000 1300000, 160000 1300000," + " 150000 550000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            RawData raster = downloadProcess.execute(
                    getLayerId(HETEROGENEOUS_CRS), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
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
                    null,
                    new NullProgressListener() // progressListener
                    );
            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                    AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                    AutoDisposableGridCoverage2D gc = reader.read()) {

                // check we get the expected referencing and resolution out
                MathTransform2D mt = gc.getGridGeometry().getGridToCRS2D();
                assertThat(mt, CoreMatchers.instanceOf(AffineTransform2D.class));
                AffineTransform2D at = (AffineTransform2D) mt;
                assertEquals(1000, at.getScaleX(), 0);
                assertEquals(-1000, at.getScaleY(), 0);

                // the red one defines left-most location, but the green one lower corner
                // reprojected is just a smidge below 600000, bringing the alignment of
                // output down to 599000
                ReferencedEnvelope gcEnvelope = gc.getEnvelope2D();
                assertEquals(160000, gcEnvelope.getMinimum(0), 0);
                assertEquals(599000, gcEnvelope.getMinimum(1), 0);
            }
        } finally {
            // re-enable checks
            MapProjection.SKIP_SANITY_CHECKS = false;
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a request with a
     * TargetCRS matching one of the underlying CRS of that mosaic but not involving any granule in that CRS will use
     * default approach involving a reprojection.
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSOnDifferentNativeCRS() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 3 granules on
        // 3 different UTM zones (32631, 32632, 32633), being exposed as a 4326 Mosaic
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        RenderedImage referenceImage = null;
        // Note we are asking targetCRS = 32632 but the available granule will be in 32633
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        CoordinateReferenceSystem roiCRS = CRS.decode("EPSG:32633", true);

        // The original sample image has a straight
        // white stripe in the middle of a blue fill.
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/blue.tif");
        try (AutoDisposableGeoTiffReader referenceReader = new AutoDisposableGeoTiffReader(file);
                AutoDisposableGridCoverage2D referenceGc = referenceReader.read()) {

            // Setting filter to get the granule
            final PropertyName property = FF.property("location");
            Filter filter = FF.like(property, "blue.tif");

            String roiWkt =
                    "POLYGON((180000 600000, 820000 600000, 820000 1200000, 180000 1200000," + " 180000 600000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            RawData raster = downloadProcess.execute(
                    getLayerId(HETEROGENEOUS_CRS), // layerName
                    filter, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
                    targetCRS, // targetCRS
                    roiCRS,
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
                    null,
                    new NullProgressListener() // progressListener
                    );

            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                    AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                    AutoDisposableGridCoverage2D gc = reader.read()) {

                AffineTransform gridToWorld =
                        (AffineTransform) gc.getGridGeometry().getGridToCRS();
                double scaleX = XAffineTransform.getScaleX0(gridToWorld);
                double scaleY = XAffineTransform.getScaleY0(gridToWorld);
                // Once the file has been downloaded we can't retrieve the processing chain
                // Let's just check if the resolution isn't the native one
                assertNotEquals(1000, scaleX, 10);
                assertNotEquals(1000, scaleY, 10);

                referenceImage = referenceGc.getRenderedImage();
                assertTrue(hasPerfectStraightHorizontalLine(referenceImage));

                // Let's extract a stripe from the center of the downloaded image
                // A reprojection will spot a not perfectly straight line
                RenderedImage ri = gc.getRenderedImage();
                assertFalse(hasPerfectStraightHorizontalLine(ri));
            }
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a request with a
     * TargetCRS matching one of the underlying CRS of that mosaic should result in no reprojection on granules with
     * that CRS as native, having matching resolution and matching alignment (when the 2 flags minimizeReprojections and
     * bestResolutionOnMatchingCRS are set).
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSMixedCRS() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 2 granules on
        // 2 different EPSG, 31255 and 31256
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);

        // Finally, get the original granule in that target CRS
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs2/31256.tif");

        try (AutoDisposableGeoTiffReader referenceReader = new AutoDisposableGeoTiffReader(file);
                AutoDisposableGridCoverage2D referenceGc = referenceReader.read()) {
            String roiWkt = "POLYGON ((-102583.25 262175.25, -102332.25 262175.25, -102332.25 "
                    + " 262042.25, -102583.25 262042.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            RawData raster = downloadProcess.execute(
                    getLayerId(HETEROGENEOUS_CRS2), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
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
                    null,
                    new NullProgressListener() // progressListener
                    );

            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                    AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                    AutoDisposableGridCoverage2D gc = reader.read()) {

                GridGeometry2D gc2d = gc.getGridGeometry();
                AffineTransform transform = (AffineTransform) gc2d.getGridToCRS();
                GridGeometry2D referenceGc2d = referenceGc.getGridGeometry();
                AffineTransform referenceTransform = (AffineTransform) referenceGc2d.getGridToCRS();

                // Check that even when requesting an area overlapping 2 different CRS we are
                // getting the native resolution
                double resX = XAffineTransform.getScaleX0(referenceTransform);
                double resY = XAffineTransform.getScaleY0(referenceTransform);
                assertEquals(resX, XAffineTransform.getScaleX0(transform), DELTA);
                assertEquals(resY, XAffineTransform.getScaleY0(transform), DELTA);

                // Check proper alignment
                double[] referenceLowerCorner =
                        referenceGc.getEnvelope2D().getLowerCorner().getCoordinate();
                double[] lowerCorner = gc.getEnvelope2D().getLowerCorner().getCoordinate();
                double xPixels = Math.abs(referenceLowerCorner[0] - lowerCorner[0]) / resX;
                double yPixels = Math.abs(referenceLowerCorner[1] - lowerCorner[1]) / resY;
                assertTrue(Math.abs(xPixels - Math.round(xPixels)) < DELTA);
                assertTrue(Math.abs(yPixels - Math.round(yPixels)) < DELTA);
            }
        }
    }

    @Test
    public void testDownloadGranuleHeterogeneousCRSUsingNativeResolutions() throws Exception {
        // This test check that by specifying a resolutionDifferenceTolerance parameter
        // after reprojection we got the native resolution.
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Requesting an area containing a granule in native CRS
        // and a granule in a different CRS
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);

        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs2/31255.tif");
        try (AutoDisposableGeoTiffReader referenceReader = new AutoDisposableGeoTiffReader(file);
                AutoDisposableGridCoverage2D referenceGc = referenceReader.read()) {
            String roiWkt = "POLYGON ((-102583.25 262175.25, -102332.25 262175.25, -102332.25"
                    + " 262042.25, -102583.25 262042.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            RawData raster = downloadProcess.execute(
                    getLayerId(HETEROGENEOUS_CRS2), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
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
                    10d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                    AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                    AutoDisposableGridCoverage2D gc = reader.read()) {

                GridGeometry2D gc2d = gc.getGridGeometry();
                AffineTransform transform = (AffineTransform) gc2d.getGridToCRS();

                // Finally, get the original granule
                GridGeometry2D referenceGc2d = referenceGc.getGridGeometry();
                AffineTransform referenceTransform = (AffineTransform) referenceGc2d.getGridToCRS();

                // Checking that resolutions are equal
                double resX = XAffineTransform.getScaleX0(referenceTransform);
                double resY = XAffineTransform.getScaleY0(referenceTransform);
                assertEquals(resX, XAffineTransform.getScaleX0(transform), 0d);
                assertEquals(resY, XAffineTransform.getScaleY0(transform), 0d);
            }
        }
    }

    @Test
    public void testDownloadGranuleUsingNativeResolutionsWithMinimizeReprojection() throws Exception {
        // This test check that by specifying a resolutionDifferenceTolerance parameter,
        // even if minimize reprojection is enabled and no reprojection occurs since mosaic
        // declared crs and target are same, we got the native granules resolution.
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs2/31255.tif");

        try (AutoDisposableGeoTiffReader referenceReader = new AutoDisposableGeoTiffReader(file);
                AutoDisposableGridCoverage2D referenceGc = referenceReader.read()) {
            String roiWkt = "POLYGON ((-102583.25 262175.25, -102332.25 262175.25, -102332.25"
                    + " 262042.25, -102583.25 262042.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            RawData raster = downloadProcess.execute(
                    getLayerId(HETEROGENEOUS_CRS2), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
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
                    10d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                    AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                    AutoDisposableGridCoverage2D gc = reader.read()) {

                GridGeometry2D gc2d = gc.getGridGeometry();
                AffineTransform transform = (AffineTransform) gc2d.getGridToCRS();

                // Finally, get the original granule
                GridGeometry2D referenceGc2d = referenceGc.getGridGeometry();
                AffineTransform referenceTransform = (AffineTransform) referenceGc2d.getGridToCRS();

                // Checking that resolutions are equal
                double resX = XAffineTransform.getScaleX0(referenceTransform);
                double resY = XAffineTransform.getScaleY0(referenceTransform);
                assertEquals(resX, XAffineTransform.getScaleX0(transform), 0d);
                assertEquals(resY, XAffineTransform.getScaleY0(transform), 0d);
            }
        }
    }

    /** heuristic method do determine if an image has a perfect straight horizontal line in the middle. */
    private boolean hasPerfectStraightHorizontalLine(RenderedImage ri) {
        // The sample image is a pure color (Green or Blue) with a
        // straight white stripe in the middle.
        // Let's get the pixels from the Red band which will be only
        // 0 or 255.
        // If the number of zeros and 255 have mod zero
        // it's straight.
        //
        //
        // ++++++++++++++++++++++++++++++++++++
        // ************************************
        // ************************************
        //
        // A case like this will not:
        //
        // +++++++++++++++++*******************
        // ++++++++*******************+++++++++
        // *************************+++++++++++

        final int stripeLength = ri.getWidth();
        final int minY = ri.getHeight() / 2;
        final int height = 3;
        Raster raster = ri.getData(new Rectangle(0, minY, stripeLength, height));
        int i = 0;
        int minValueCount = 0;
        int maxValueCount = 0;
        int val = 0;
        for (int k = minY; k < minY + height; k++) {
            for (; i < stripeLength; i++) {
                val = raster.getSample(i, k, 0);
                if (val == 0) {
                    minValueCount++;
                } else {
                    maxValueCount++;
                }
            }
        }
        return minValueCount % maxValueCount == 0;
    }

    /**
     * PPIO Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void testZipGeoTiffPPIO() throws Exception {

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();
        ZipArchivePPIO ppio = new ZipArchivePPIO(DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL);

        // ROI as a BBOX
        Envelope env = new Envelope(-125.074006936869, -123.88300771369998, 48.5552612829, 49.03872);
        Polygon roi = JTS.toGeometry(env);

        // Download the data with ROI
        RawData raster = executeVectorDownload(
                downloadProcess, MockData.USA_WORLDIMG, "image/tiff", "image/tiff", "EPSG:4326", roi, true);

        // Final checks on the result
        Assert.assertNotNull(raster);

        // make sure we create files locally so that we don't clog the sytem temp
        final File currentDirectory =
                new File(DownloadProcessTest.class.getResource(".").toURI());
        File tempZipFile = File.createTempFile("zipppiotemp", ".zip", currentDirectory);
        try (AutoCloseableResource resource = new AutoCloseableResource(getResourceManager(), raster)) {
            ppio.encode(resource.getFile(), new FileOutputStream(tempZipFile));
            Assert.assertTrue(tempZipFile.length() > 0);
            final File tempDir = new File(currentDirectory, Long.toString(System.nanoTime()));
            Assert.assertTrue(tempDir.mkdir());
            File tempFile = decode(new FileInputStream(tempZipFile), tempDir);
            Assert.assertNotNull(tempFile);
            IOUtils.delete(tempFile);
        }
    }

    /**
     * Test download estimator for raster data. The estimate must work even when dimension type not present in a saved
     * Coverage. See GEOS-9785
     */
    @Test(expected = Test.None.class)
    public void testDownloadEstimatorReloadsCoverageDimensionsWhenNull() {
        CoverageInfo mycoverage = getCatalog().getCoverageByName(MockData.USA_WORLDIMG.getLocalPart());
        mycoverage.getDimensions().get(0).setDimensionType(null);
        getCatalog().save(mycoverage);
        final WPSResourceManager resourceManager = getResourceManager();
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI as polygon

        // Download the data with ROI. It should throw an exception
        downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                null, // roiCRS
                null, // roi
                true, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );
    }
    /**
     * Test download estimator for raster data. The result should exceed the limits
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorReadLimitsRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        10,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI as polygon
        Polygon roi = (Polygon) new WKTReader2()
                .read("POLYGON (( -127.57473954542964 54.06575021619523,"
                        + " -130.8545966116691 52.00807146727025, -129.50812897394974 49.85372324691927,"
                        + " -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591,"
                        + " -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477,"
                        + " -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926,"
                        + " -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data with ROI. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
                    null, // targetCRS
                    WGS84, // roiCRS
                    roi, // roi
                    true, // cropToGeometry
                    null, // interpolation
                    null, // targetSizeX
                    null, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            assertFalse(true);
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.lang.IllegalArgumentException: Download Limits Exceeded. Unable to"
                            + " proceed!: Download Limits Exceeded. Unable to proceed!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download estimator write limits raster. The result should exceed the limits
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorWriteLimitsRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        10,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI
        Polygon roi = (Polygon) new WKTReader2()
                .read("POLYGON (( -127.57473954542964 54.06575021619523,"
                        + " -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927,"
                        + " -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591,"
                        + " -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477,"
                        + " -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926,"
                        + " -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data with ROI. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
                    null, // targetCRS
                    WGS84, // roiCRS
                    roi, // roi
                    true, // cropToGeometry
                    null, // interpolation
                    null, // targetSizeX
                    null, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            fail("Should have failed with an exception!");
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "org.geotools.process.ProcessException: java.io.IOException: Download"
                            + " Exceeded the maximum HARD allowed size!: java.io.IOException: Download Exceeded the maximum"
                            + " HARD allowed size!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download estimator write limits raster for scaled output. Scaled image should exceed the limits, whereas the
     * original raster should not.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorWriteLimitsScaledRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        921600, // 900KB
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);

        RawData nonScaled = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                WGS84, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, nonScaled);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {
            // ten times the size of the original coverage
            int targetSizeX = (int) (gc.getGridGeometry().getGridRange2D().getWidth() * 10);
            int targetSizeY = (int) (gc.getGridGeometry().getGridRange2D().getHeight() * 10);
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
                    null, // targetCRS
                    WGS84, // roiCRS
                    null, // roi
                    false, // cropToGeometry
                    null, // interpolation
                    targetSizeX, // targetSizeX
                    targetSizeY, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            // exception should have been thrown at this stage
            fail("Should have failed with an exception");
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "org.geotools.process.ProcessException: java.io.IOException: Download"
                            + " Exceeded the maximum HARD allowed size!: java.io.IOException: Download Exceeded the maximum"
                            + " HARD allowed size!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }

        // Test same process for checking write output limits, using selected band indices
        limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        30000, // = 100x100 pixels x 3 bands x 1 byte (8 bits)
                        // per band
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());

        downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);

        try {
            // create a scaled 100x100 raster, with 4 bands
            int targetSizeX = 100;
            int targetSizeY = 100;
            int[] bandIndices = {0, 2, 2, 2};
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
                    null, // targetCRS
                    WGS84, // roiCRS
                    null, // roi
                    false, // cropToGeometry
                    null, // interpolation
                    targetSizeX, // targetSizeX
                    targetSizeY, // targetSizeY
                    bandIndices, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            // exception should have been thrown at this stage
            fail();
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.lang.IllegalArgumentException: Download Limits Exceeded. "
                            + "Unable to proceed!: Download Limits Exceeded. Unable to proceed!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download estimator for raster data. Make sure the estimator works again full raster at native resolution
     * downloads
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorFullNativeRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        10, // small number, but before fix it was not
                        // triggering exception
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());

        // Estimate download full data at native resolution. It should return false
        assertFalse(limits.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                null, // target CRS
                null, // ROI CRS
                null, // ROI
                false, // clip
                null, // targetSizeX
                null, // targetSizeY
                null, // band indices
                new NullProgressListener() // progressListener
                ));
    }

    /**
     * Test download estimator for vectorial data. The result should be exceed the hard output limits
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorHardOutputLimit() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        10,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);
        try {
            // Download the features. It should throw an exception
            executeVectorDownload(
                    downloadProcess, MockData.POLYGONS, "application/zip", "application/zip", "EPSG:32615", roi, false);

            Assert.fail();
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.io.IOException: Download Exceeded the maximum HARD allowed size!: "
                            + "Download Exceeded the maximum HARD allowed size!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download physical limit for raster data. It should throw an exception
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadPhysicalLimitsRaster() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        ProcessListener listener = new ProcessListener(new ExecutionStatus(
                new NameImpl("gs", "DownloadEstimator"), resourceManager.getExecutionId(false), false));
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), getGeoServer());

        // Creates the new process for the download
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI data
        Polygon roi = (Polygon) new WKTReader2()
                .read("POLYGON (( -127.57473954542964 54.06575021619523,"
                        + " -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927,"
                        + " -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591,"
                        + " -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477,"
                        + " -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926,"
                        + " -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    "image/tiff",
                    null, // targetCRS
                    WGS84, // roiCRS
                    roi, // roi
                    true, // cropToGeometry
                    null, // interpolation
                    null, // targetSizeX
                    null, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    listener // progressListener
                    );

        } catch (Exception e) {
            Throwable e1 = listener.exception;
            Assert.assertNotNull(e1);
            Assert.assertEquals(
                    "org.geotools.process.ProcessException: java.io.IOException: Download"
                            + " Exceeded the maximum HARD allowed size!: java.io.IOException: Download Exceeded the maximum"
                            + " HARD allowed size!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download physical limit for vectorial data. It should throw an exception
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadPhysicalLimitsVector() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        ProcessListener listener = new ProcessListener(new ExecutionStatus(
                new NameImpl("gs", "DownloadEstimator"), resourceManager.getExecutionId(false), false));
        // Estimator process for checking limits
        DownloadEstimatorProcess limits = new DownloadEstimatorProcess(
                new StaticDownloadServiceConfiguration(new DownloadServiceConfiguration(
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        DownloadServiceConfiguration.NO_LIMIT,
                        1,
                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                        DownloadServiceConfiguration.NO_LIMIT)),
                getGeoServer());

        // Creates the new process for the download
        DownloadProcess downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);

        try {
            // Download the features. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.POLYGONS), // layerName
                    null, // filter
                    "application/zip", // outputFormat
                    "application/zip",
                    null, // targetCRS
                    CRS.decode("EPSG:32615"), // roiCRS
                    roi, // roi
                    false, // cropToGeometry
                    null, // interpolation
                    null, // targetSizeX
                    null, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    listener // progressListener
                    );

        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.io.IOException: Download Exceeded the maximum HARD allowed size!:"
                            + " Download Exceeded the maximum HARD allowed size!",
                    e.getMessage() + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));

            Throwable le = listener.exception;
            Assert.assertEquals(
                    "java.io.IOException: Download Exceeded the maximum HARD allowed size!:"
                            + " Download Exceeded the maximum HARD allowed size!",
                    le.getMessage()
                            + (le.getCause() != null ? ": " + le.getCause().getMessage() : ""));

            return;
        }

        assertFalse(true);
    }

    /**
     * Test with a wrong output format. It should thrown an exception.
     *
     * @throws Exception the exception
     */
    @Test(expected = Exception.class)
    public void testWrongOutputFormat() throws FactoryException {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        final DefaultProgressListener progressListener = new DefaultProgressListener();

        // Download the features. It should throw an exception.
        downloadProcess.execute(
                getLayerId(MockData.POLYGONS), // layerName
                null, // filter
                "IAmWrong!!!", // outputFormat
                "",
                null, // targetCRS
                CRS.decode("EPSG:32615"), // roiCRS
                roi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                progressListener // progressListener
                );
    }

    /**
     * Test download of raster data using underlying granules resolution. The sample mosaic is composed of:
     *
     * <p>18km_32610.tif with resolution = 17550.948453185396000 meters 9km_32610.tif with resolution =
     * 8712.564801039759900 meters
     */
    @Test
    public void testDownloadGranuleHeterogeneousResolution() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Setting filter to get the granule with resolution
        final PropertyName property = FF.property("resolution");
        Filter filter = FF.greaterOrEqual(property, FF.literal(16000));

        testExpectedResolution(downloadProcess, filter, WGS84, ROI2, resourceManager, 17550.94845318, -17550.94845318);

        // Download native resolution 2
        filter = FF.and(FF.lessOrEqual(property, FF.literal(10000)), FF.greaterOrEqual(property, FF.literal(1000)));

        testExpectedResolution(
                downloadProcess, filter, null, null, resourceManager, 8712.564801039759900, -8712.564801039759900);

        // Download native resolution 3
        filter = FF.lessOrEqual(property, FF.literal(1000));

        // Final checks on the result
        testExpectedResolution(
                downloadProcess, filter, null, null, resourceManager, 7818.453242658203, -10139.712928934865);

        filter = FF.and(FF.lessOrEqual(property, FF.literal(10000)), FF.greaterOrEqual(property, FF.literal(1000)));

        RawData raster = downloadProcess.execute(
                getLayerId(MIXED_RES), // layerName
                filter, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                null, // targetCRS
                CRS.decode("EPSG:32610", true),
                ROI3, // roi
                false, // cropToGeometry
                null, // interpolation
                512, // targetSizeX
                128, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            // check coverage size
            RenderedImage ri = gc.getRenderedImage();
            Assert.assertEquals(512, ri.getWidth());
            Assert.assertEquals(128, ri.getHeight());
        }
    }

    private void testExpectedResolution(
            DownloadProcess downloadProcess,
            Filter filter,
            CoordinateReferenceSystem roiCrs,
            Polygon roi,
            WPSResourceManager resourceManager,
            double expectedX,
            double expectedY)
            throws Exception {

        RawData raster = downloadProcess.execute(
                getLayerId(MIXED_RES), // layerName
                filter, // filter
                "image/tiff", // mimeType
                "image/tiff", // result
                null, // targetCRS
                roiCrs, // roiCRS
                roi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster);
                AutoDisposableGeoTiffReader reader = new AutoDisposableGeoTiffReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {

            Assert.assertEquals(
                    "32610",
                    gc.getCoordinateReferenceSystem()
                            .getIdentifiers()
                            .iterator()
                            .next()
                            .getCode());

            // check coverage size
            MathTransform mt = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
            AffineTransform2D transform2D = (AffineTransform2D) mt;
            double resX = transform2D.getScaleX();
            double resY = transform2D.getScaleY();

            Assert.assertEquals(expectedX, resX, DELTA);
            Assert.assertEquals(expectedY, resY, DELTA);
        }
    }

    /**
     * Test PNG outputFormat
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadPNG() throws Exception {
        testDownloadByOutputFormat("image/png");
    }

    /**
     * Test JPEG outputFormat
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadJPEG() throws Exception {
        testDownloadByOutputFormat("image/jpeg");
    }

    private void testDownloadByOutputFormat(String outputFormat) throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        Polygon roi = (Polygon) new WKTReader2().read("POLYGON ((-128 54, -128 50, -130 50, -130 54, -128 54))");
        roi.setSRID(4326);

        final int requestedSizeX = 128;
        final int requestedSizeY = 128;

        // Download the coverage
        RawData raster = downloadProcess.execute(
                getLayerId(MockData.USA_WORLDIMG), // layerName
                null, // filter
                outputFormat, // outputFormat
                outputFormat,
                null, // targetCRS
                WGS84, // roiCRS
                roi, // roi
                true, // cropToGeometry
                null, // interpolation
                requestedSizeX, // targetSizeX
                requestedSizeY, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (AutoCloseableResource resource = new AutoCloseableResource(resourceManager, raster)) {
            String formatName = MimeType.valueOf(outputFormat).getSubtype().toUpperCase();
            testDownloadedImage(resource.getFile(), formatName, requestedSizeX, requestedSizeY);
        }
    }

    /**
     * The listener interface for receiving process events. The class that is interested in processing a process event implements this interface, and
     * the object created with that class is registered with a component using the component's <code>addProcessListener<code> method. When
     * the process event occurs, that object's appropriate
     * method is invoked.
     *
     * @see ProcessEvent
     */
    static class ProcessListener implements ProgressListener {

        /** The Constant LOGGER. */
        static final Logger LOGGER = Logging.getLogger(ProcessListener.class);

        /** The status. */
        ExecutionStatus status;

        /** The task. */
        InternationalString task;

        /** The description. */
        String description;

        /** The exception. */
        Throwable exception;

        /**
         * Instantiates a new process listener.
         *
         * @param status the status
         */
        public ProcessListener(ExecutionStatus status) {
            this.status = status;
        }

        /**
         * Gets the task.
         *
         * @return the task
         */
        @Override
        public InternationalString getTask() {
            return task;
        }

        /**
         * Sets the task.
         *
         * @param task the new task
         */
        @Override
        public void setTask(InternationalString task) {
            this.task = task;
        }

        /**
         * Gets the description.
         *
         * @return the description
         */
        public String getDescription() {
            return this.description;
        }

        /**
         * Sets the description.
         *
         * @param description the new description
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /** Started. */
        @Override
        public void started() {
            status.setPhase(ProcessState.RUNNING);
        }

        /**
         * Progress.
         *
         * @param percent the percent
         */
        @Override
        public void progress(float percent) {
            status.setProgress(percent);
        }

        /**
         * Gets the progress.
         *
         * @return the progress
         */
        @Override
        public float getProgress() {
            return status.getProgress();
        }

        /** Complete. */
        @Override
        public void complete() {
            // nothing to do
        }

        /** Dispose. */
        @Override
        public void dispose() {
            // nothing to do
        }

        /**
         * Checks if is canceled.
         *
         * @return true, if is canceled
         */
        @Override
        public boolean isCanceled() {
            return status.getPhase() == ProcessState.DISMISSING;
        }

        /**
         * Sets the canceled.
         *
         * @param cancel the new canceled
         */
        @Override
        public void setCanceled(boolean cancel) {
            if (cancel == true) {
                status.setPhase(ProcessState.DISMISSING);
            }
        }

        /**
         * Warning occurred.
         *
         * @param source the source
         * @param location the location
         * @param warning the warning
         */
        @Override
        public void warningOccurred(String source, String location, String warning) {
            LOGGER.log(
                    Level.WARNING,
                    "Got a warning during process execution " + status.getExecutionId() + ": " + warning);
        }

        /**
         * Exception occurred.
         *
         * @param exception the exception
         */
        @Override
        public void exceptionOccurred(Throwable exception) {
            this.exception = exception;
        }
    }

    /**
     * Private method for decoding a Shapefile
     *
     * @param input the input shp
     * @return the object a {@link SimpleFeatureCollection} object related to the shp file.
     * @throws Exception the exception
     */
    private ShapefileDataStore decodeShape(InputStream input) throws Exception {
        // create the temp directory and register it as a temporary resource
        File tempDir = IOUtils.createRandomDirectory(
                IOUtils.createTempDirectory("shpziptemp").getAbsolutePath(), "download-process", "download-services");

        // unzip to the temporary directory
        File shapeFile = null;
        File zipFile = null;

        // extract shp-zip file
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry = null;

            // Cycle on all the entries and copies the input shape in the target directory
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdir();
                } else {

                    if (file.getName().toLowerCase().endsWith(".shp")) {
                        shapeFile = file;
                    } else if (file.getName().toLowerCase().endsWith(".zip")) {
                        zipFile = file;
                    }

                    int count;
                    byte[] data = new byte[4096];
                    // write the files to the disk
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        while ((count = zis.read(data)) != -1) {
                            fos.write(data, 0, count);
                        }
                        fos.flush();
                    }
                }
                zis.closeEntry();
            }
        }

        // Read the shapefile
        if (shapeFile == null) {
            if (zipFile != null) return decodeShape(new FileInputStream(zipFile));
            else {
                FileUtils.deleteDirectory(tempDir);
                throw new IOException("Could not find any file with .shp extension in the zip file");
            }
        } else {
            return new ShapefileDataStore(URLs.fileToUrl(shapeFile));
        }
    }

    /**
     * Private method for decoding a GeoPackage
     *
     * @param input the input zip
     * @return A GeoPackage object if one was found, an exception otherwise
     */
    private GeoPackage decodeGeoPackage(InputStream input) throws Exception {
        // create the temp directory and register it as a temporary resource
        File tempDir = IOUtils.createRandomDirectory(
                IOUtils.createTempDirectory("gpkgziptemp").getAbsolutePath(), "download-process", "download-services");

        // unzip to the temporary directory
        File geopackage = null;
        File zipFile = null;

        // extract shp-zip file
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry = null;

            // Cycle on all the entries and copies the input shape in the target directory
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdir();
                } else {

                    if (file.getName().toLowerCase().endsWith(".gpkg")) {
                        geopackage = file;
                    } else if (file.getName().toLowerCase().endsWith(".zip")) {
                        zipFile = file;
                    }

                    int count;
                    byte[] data = new byte[4096];
                    // write the files to the disk
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        while ((count = zis.read(data)) != -1) {
                            fos.write(data, 0, count);
                        }
                        fos.flush();
                    }
                }
                zis.closeEntry();
            }
        }

        // Read the shapefile
        if (geopackage == null) {
            if (zipFile != null) return decodeGeoPackage(new FileInputStream(zipFile));
            else {
                FileUtils.deleteDirectory(tempDir);
                throw new IOException("Could not find any file with .gpkg extension in the zip file");
            }
        } else {
            return new GeoPackage(geopackage);
        }
    }

    private void testDownloadedImage(File inputFile, String formatName, int sizeX, int sizeY) throws Exception {
        try (FileImageInputStream fis = new FileImageInputStream(inputFile)) {
            ImageReader imageReader = null;
            try {
                imageReader = ImageIO.getImageReaders(fis).next();
                imageReader.setInput(fis);
                assertTrue(formatName.equalsIgnoreCase(imageReader.getFormatName()));
                RenderedImage ri = imageReader.read(0);
                assertNotNull(ri);
                assertEquals(sizeX, ri.getWidth());
                assertEquals(sizeY, ri.getHeight());
            } finally {
                if (imageReader != null) {
                    imageReader.dispose();
                }
            }
        }
    }

    private DownloadProcess createDefaultTestingDownloadProcess() {
        return createDefaultTestingDownloadProcess(getResourceManager());
    }

    private DownloadProcess createDefaultTestingDownloadProcess(WPSResourceManager resourceManager) {
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        return new DownloadProcess(geoserver, limits, resourceManager);
    }

    @Test
    public void testDirectRasterDownloadSimpleSource() throws Exception {
        // simple download, no extra parameters
        RawData raster = executeRasterDownload(getLayerId(MockData.WORLD), "image/tiff", null, null, null);

        CoverageStoreInfo store = getCatalog().getCoverageStoreByName(MockData.WORLD.getLocalPart());
        File input = URLs.urlToFile(new URL(store.getURL()));
        try (FileInputStream is = new FileInputStream(input);
                InputStream os = raster.getInputStream()) {
            assertTrue(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    @Test
    public void testDirectRasterDownloadMosaic() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        Filter filter = FF.like(FF.property("location"), "green.tif");

        RawData raster = executeRasterDownload(getLayerId(HETEROGENEOUS_CRS), "image/tiff", filter, targetCRS, null);

        // got a single file from the source, it's exactly the same
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertTrue(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    @Test
    public void testDirectDownloadCompression() throws Exception {
        checkDownloadCompression("Deflate", true);
    }

    @Test
    public void testDirectDownloadAutoCompression() throws Exception {
        checkDownloadCompression("Auto", true);
    }

    @Test
    public void testDirectDownloadDifferentCompression() throws Exception {
        checkDownloadCompression("JPEG", false);
    }

    private void checkDownloadCompression(String compression, boolean contentEquals)
            throws FactoryException, IOException {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        Filter filter = FF.like(FF.property("location"), "green.tif");

        Parameters writeParams = new Parameters();
        writeParams.getParameters().add(new Parameter("compression", compression));

        RawData raster =
                executeRasterDownload(getLayerId(HETEROGENEOUS_CRS), "image/tiff", filter, targetCRS, writeParams);

        // got a single file from the source, it's exactly the same
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertEquals(contentEquals, org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    @Test
    public void testDirectDownloadTiling() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        Filter filter = FF.like(FF.property("location"), "green.tif");

        // tiling matches source
        Parameters writeParams = new Parameters();
        writeParams.getParameters().add(new Parameter(TILING, "true"));
        writeParams.getParameters().add(new Parameter(TILEWIDTH, "256"));
        writeParams.getParameters().add(new Parameter(TILEHEIGHT, "256"));

        RawData raster =
                executeRasterDownload(getLayerId(HETEROGENEOUS_CRS), "image/tiff", filter, targetCRS, writeParams);

        // got a single file from the source, it's exactly the same
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertTrue(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    @Test
    public void testDirectDownloadTilingMismatch() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        Filter filter = FF.like(FF.property("location"), "green.tif");

        // tiling does not match the source
        Parameters writeParams = new Parameters();
        writeParams.getParameters().add(new Parameter(TILING, "true"));
        writeParams.getParameters().add(new Parameter(TILEWIDTH, "512"));
        writeParams.getParameters().add(new Parameter(TILEHEIGHT, "512"));
        writeParams.getParameters().add(new Parameter(COMPRESSION, "Auto"));

        RawData raster =
                executeRasterDownload(getLayerId(HETEROGENEOUS_CRS), "image/tiff", filter, targetCRS, writeParams);

        // got a single file from the source, but the tiling is different
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertFalse(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }

        // check it has been compressed Deflate with "auto" compression on
        File rasterFile = ((ResourceRawData) raster).getResource().file();
        assertEquals("Deflate", RasterDirectDownloader.getCompression(rasterFile));
    }

    @Test
    public void testDirectDownloadLargeROI() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        Filter filter = FF.like(FF.property("location"), "green.tif");

        // get bounds larger than the file
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        ReferencedEnvelope bounds = getGeoTiffBounds(file);
        bounds.expandBy(bounds.getWidth() * 0.1, bounds.getHeight() * 0.1);
        Polygon roi = JTS.toGeometry(bounds);

        final WPSResourceManager resourceManager = getResourceManager();
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        RawData raster = downloadProcess.execute(
                getLayerId(HETEROGENEOUS_CRS), // layerName
                filter, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                targetCRS, // targetCRS
                targetCRS,
                roi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                true,
                true,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        // got a single file from the source, it's exactly the same
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertTrue(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    @Test
    public void testDirectDownloadCroppingROI() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        Filter filter = FF.like(FF.property("location"), "green.tif");

        // get bounds smaller than the file
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/green.tif");
        ReferencedEnvelope bounds = getGeoTiffBounds(file);
        bounds.expandBy(-bounds.getWidth() * 0.1, -bounds.getHeight() * 0.1);
        Polygon roi = JTS.toGeometry(bounds);

        final WPSResourceManager resourceManager = getResourceManager();
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        RawData raster = downloadProcess.execute(
                getLayerId(HETEROGENEOUS_CRS), // layerName
                filter, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                targetCRS, // targetCRS
                targetCRS,
                roi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                true,
                true,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        // got a single file from the source, but it's different due to ROI
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertFalse(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    /** This image mosaic has NODATA in the source files */
    @Test
    public void testDirectDownloadHeteroNoData() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32634", true);
        Filter filter = FF.like(FF.property("location"), "20170421T100031027Z_T34VCJ.tif");

        RawData raster = executeRasterDownload(getLayerId(HETEROGENEOUS_NODATA), "image/tiff", filter, targetCRS, null);

        // got a single file from the source, it's exactly the same
        final File file =
                new File(this.getTestData().getDataDirectoryRoot(), "hcrs_nodata/20170421T100031027Z_T34VCJ.tif");
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertTrue(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    /** This image mosaic has NODATA (-30000) in the source files and has uniform structure */
    @Test
    public void testDirectDownloadTimeseriesNoData() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
        Filter filter = FF.equals(FF.property("time"), FF.literal("2016-01-01"));

        RawData raster = executeRasterDownload(getLayerId(TIMESERIES), "image/tiff", filter, targetCRS, null);

        // got a single file from the source, it's exactly the same
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "timeseries/sst_20160101.tiff");
        try (FileInputStream is = new FileInputStream(file);
                InputStream os = raster.getInputStream()) {
            assertTrue(org.apache.commons.io.IOUtils.contentEquals(is, os));
        }
    }

    private ReferencedEnvelope getGeoTiffBounds(File file) throws DataSourceException {
        GeoTiffReader reader = new GeoTiffReader(file);
        ReferencedEnvelope bounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
        reader.dispose();
        return bounds;
    }

    /** Convenience method to start a raster download, with a shorter list of parameters */
    private RawData executeRasterDownload(
            String layerId, String format, Filter filter, CoordinateReferenceSystem targetCRS, Parameters writeParams) {
        final WPSResourceManager resourceManager = getResourceManager();
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        return downloadProcess.execute(
                layerId, // layerName
                filter, // filter
                format, // outputFormat
                format,
                targetCRS, // targetCRS
                targetCRS,
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                writeParams, // Writing params
                true,
                true,
                0d,
                null,
                new NullProgressListener() // progressListener
                );
    }
}
