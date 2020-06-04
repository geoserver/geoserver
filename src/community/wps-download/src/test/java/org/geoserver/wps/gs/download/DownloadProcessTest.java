/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.junit.Assert.*;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.PrivateTIFFTagSet;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
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
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.ppio.WFSPPIO;
import org.geoserver.wps.ppio.ZipArchivePPIO;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.WKTReader2;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;
import org.springframework.util.MimeType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class tests checks if the DownloadProcess class behaves correctly.
 *
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class DownloadProcessTest extends WPSTestSupport {

    private static final FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;

    private static QName MIXED_RES = new QName(WCS_URI, "mixedres", WCS_PREFIX);
    private static QName HETEROGENEOUS_CRS = new QName(WCS_URI, "hcrs", WCS_PREFIX);
    private static QName HETEROGENEOUS_CRS2 = new QName(WCS_URI, "hcrs2", WCS_PREFIX);
    private static QName SHORT = new QName(WCS_URI, "short", WCS_PREFIX);
    private static QName FLOAT = new QName(WCS_URI, "float", WCS_PREFIX);

    private static Set<String> GTIFF_EXTENSIONS = new HashSet<String>();
    private static Set<String> PNG_EXTENSIONS = new HashSet<String>();
    private static Set<String> JPEG_EXTENSIONS = new HashSet<String>();
    private static Set<String> XML_EXTENSIONS = new HashSet<String>();
    private static Set<String> JSON_EXTENSIONS = new HashSet<String>();
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
                    byte data[] = new byte[4096];
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
            roi =
                    (Polygon)
                            new WKTReader2()
                                    .read(
                                            "POLYGON (( 500116.08576537756 499994.25579707103, 500116.08576537756 500110.1012210889, 500286.2657688021 500110.1012210889, 500286.2657688021 499994.25579707103, 500116.08576537756 499994.25579707103 ))");

            ROI2 =
                    (Polygon)
                            new WKTReader2()
                                    .read(
                                            "POLYGON (( -125 30, -116 30, -116 45, -125 45, -125 30))");

            ROI3 =
                    (Polygon)
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
        testData.addRasterLayer(MIXED_RES, "mixedres.zip", null, getCatalog());
        testData.addRasterLayer(HETEROGENEOUS_CRS, "heterogeneous_crs.zip", null, getCatalog());
        testData.addRasterLayer(HETEROGENEOUS_CRS2, "heterogeneous_crs2.zip", null, getCatalog());
        testData.addRasterLayer(SHORT, "short.zip", null, getCatalog());
        testData.addRasterLayer(FLOAT, "float.zip", null, getCatalog());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add limits properties file
        testData.copyTo(
                DownloadProcessTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/download.properties"),
                "download.properties");
    }

    @Before
    public void clearPolygons() throws IOException {
        revertLayer(MockData.POLYGONS);
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
        File shpeZip =
                downloadProcess.execute(
                        getLayerId(MockData.POLYGONS), // layerName
                        null, // mail
                        "application/zip", // outputFormat
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
                        new NullProgressListener() // progressListener
                        );

        // Final checks on the result
        Assert.assertNotNull(shpeZip);

        ShapefileDataStore store = decodeShape(new FileInputStream(shpeZip));
        SimpleFeatureCollection rawTarget =
                (SimpleFeatureCollection) store.getFeatureSource().getFeatures();
        Assert.assertNotNull(rawTarget);
        Assert.assertEquals(rawSource.size(), rawTarget.size());
        store.dispose();
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
        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON ((0.0008993124415341 0.0006854377923293, 0.0008437876520112 0.0006283489242283, 0.0008566913002806 0.0005341131898971, 0.0009642217025257 0.0005188634237605, 0.0011198475210477 0.000574779232928, 0.0010932581852198 0.0006572843779233, 0.0008993124415341 0.0006854377923293))");

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.BUILDINGS));
        // Download
        File shpeZip = null;
        FileInputStream shapeFis = null;
        ShapefileDataStore store = null;
        try {
            SimpleFeatureCollection rawSource =
                    (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();

            shpeZip =
                    downloadProcess.execute(
                            getLayerId(MockData.BUILDINGS), // layerName
                            CQL.toFilter("ADDRESS = '123 Main Street'"), // filter
                            "application/zip", // outputFormat
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
            shapeFis = new FileInputStream(shpeZip);
            store = decodeShape(shapeFis);
            SimpleFeatureCollection rawTarget =
                    (SimpleFeatureCollection) store.getFeatureSource().getFeatures();
            Assert.assertNotNull(rawTarget);

            Assert.assertEquals(1, rawTarget.size());

            SimpleFeature srcFeature = rawSource.features().next();
            SimpleFeature trgFeature = rawTarget.features().next();

            Assert.assertEquals(
                    srcFeature.getAttribute("ADDRESS"), trgFeature.getAttribute("ADDRESS"));

            // Final checks on the ROI
            Geometry srcGeometry = (Geometry) srcFeature.getDefaultGeometry();
            Geometry trgGeometry = (Geometry) trgFeature.getDefaultGeometry();

            Assert.assertTrue(
                    "Target geometry clipped and included into the source one",
                    srcGeometry.contains(trgGeometry));
        } finally {
            if (store != null) {
                store.dispose();
            }
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
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
        File gml2Zip =
                downloadProcess.execute(
                        getLayerId(MockData.POLYGONS), // layerName
                        null, // filter
                        "application/wfs-collection-1.0", // outputFormat
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
                        new NullProgressListener() // progressListener
                        );

        // Final checks on the result
        Assert.assertNotNull(gml2Zip);

        File[] files = extractFiles(gml2Zip, "XML");

        SimpleFeatureCollection rawTarget =
                (SimpleFeatureCollection) new WFSPPIO.WFS10().decode(new FileInputStream(files[0]));

        Assert.assertNotNull(rawTarget);

        Assert.assertEquals(rawSource.size(), rawTarget.size());

        // Download as GML 3
        File gml3Zip =
                downloadProcess.execute(
                        getLayerId(MockData.POLYGONS), // layerName
                        null, // filter
                        "application/wfs-collection-1.1", // outputFormat
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
                        new NullProgressListener() // progressListener
                        );

        // Final checks on the result
        Assert.assertNotNull(gml3Zip);

        files = extractFiles(gml2Zip, "XML");

        rawTarget =
                (SimpleFeatureCollection) new WFSPPIO.WFS11().decode(new FileInputStream(files[0]));

        Assert.assertNotNull(rawTarget);

        Assert.assertEquals(rawSource.size(), rawTarget.size());
    }

    /** This method is used for extracting only the specified format files from a zipfile archive */
    public static File[] extractFiles(final File zipFile, String format) throws IOException {
        IOUtils.decompress(zipFile, zipFile.getParentFile());
        Set<String> extensions = FORMAT_TO_EXTENSIONS.get(format);

        File[] files =
                zipFile.getParentFile()
                        .listFiles(
                                new FilenameFilter() {
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
        File jsonZip =
                downloadProcess.execute(
                        getLayerId(MockData.POLYGONS), // layerName
                        null, // filter
                        "application/json", // outputFormat
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
                        new NullProgressListener() // progressListener
                        );

        // Final checks on the result
        Assert.assertNotNull(jsonZip);

        File[] files = extractFiles(jsonZip, "JSON");

        SimpleFeatureCollection rawTarget =
                (SimpleFeatureCollection)
                        new FeatureJSON().readFeatureCollection(new FileInputStream(files[0]));

        Assert.assertNotNull(rawTarget);

        Assert.assertEquals(rawSource.size(), rawTarget.size());
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
        Polygon roi =
                (Polygon)
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
                (Polygon)
                        JTS.transform(
                                roi, CRS.findMathTransform(WGS84, CRS.decode("EPSG:900913", true)));
        // Download the coverage as tiff (Not reprojected)
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        // Final checks on the result
        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null, gcResampled = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);

            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.611129008700004, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -123.95304462109999, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    54.0861661371, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);

            // Take a pixel within the ROI
            byte[] result =
                    (byte[])
                            gc.evaluate(
                                    new DirectPosition2D(
                                            new Point2D.Double(firstXRoi, firstYRoi - 1E-4)));
            Assert.assertNotEquals(0, result[0]);
            Assert.assertNotEquals(0, result[1]);
            Assert.assertNotEquals(0, result[2]);

            // Take a pixel outside of the ROI
            result =
                    (byte[])
                            gc.evaluate(
                                    new DirectPosition2D(
                                            new Point2D.Double(firstXRoi - 2, firstYRoi - 0.5)));
            Assert.assertEquals(0, result[0]);
            Assert.assertEquals(0, result[1]);
            Assert.assertEquals(0, result[2]);

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

        // Download the coverage as tiff with clipToROI set to False (Crop on envelope)
        rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
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

        // Final checks on the result
        Assert.assertNotNull(rasterZip);
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);

            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.611129008700004, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -123.95304462109999, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    54.0861661371, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);

            // Take a pixel within the ROI
            byte[] result =
                    (byte[])
                            gc.evaluate(
                                    new DirectPosition2D(
                                            new Point2D.Double(firstXRoi, firstYRoi - 1E-4)));
            Assert.assertNotEquals(0, result[0]);
            Assert.assertNotEquals(0, result[1]);
            Assert.assertNotEquals(0, result[2]);

            // Take a pixel outside of the ROI geometry but within the ROI's envelope
            // (We have set cropToROI = False)
            result =
                    (byte[])
                            gc.evaluate(
                                    new DirectPosition2D(
                                            new Point2D.Double(firstXRoi - 2, firstYRoi - 0.5)));
            Assert.assertNotEquals(0, result[0]);
            Assert.assertNotEquals(0, result[1]);
            Assert.assertNotEquals(0, result[2]);

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
        // Download the coverage as tiff (Reprojected)
        File resampledZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
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

        // Final checks on the result
        Assert.assertNotNull(resampledZip);

        try {
            File[] files = extractFiles(resampledZip, "GTIFF");
            reader = new GeoTiffReader(files[files.length - 1]);
            gcResampled = reader.read(null);

            Assert.assertNotNull(gcResampled);

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

        } finally {

            if (gcResampled != null) {
                CoverageCleanerCallback.disposeCoverage(gcResampled);
            }
            if (reader != null) reader.dispose();

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
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
        Polygon roi =
                (Polygon)
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
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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
        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;

        final Map<String, String> expectedTiffTagValues = new HashMap<String, String>();
        expectedTiffTagValues.put(
                Integer.toString(BaselineTIFFTagSet.TAG_TILE_WIDTH), tileWidthValue);
        expectedTiffTagValues.put(
                Integer.toString(BaselineTIFFTagSet.TAG_TILE_LENGTH), tileHeightValue);
        expectedTiffTagValues.put(
                Integer.toString(BaselineTIFFTagSet.TAG_COMPRESSION), compressionValue);
        expectedTiffTagValues.put(Integer.toString(PrivateTIFFTagSet.TAG_GDAL_NODATA), "0.0");

        int matchingStillRequired = expectedTiffTagValues.size();
        if (!writeNodata) {
            // we keep the map entry for scan but we make sure
            // the matching is missing
            matchingStillRequired--;
        }

        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
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
                                    if (expectedTiffTagValues
                                            .get(nodeValue)
                                            .equals(attrib.getNodeValue())) {
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

        } finally {
            if (reader != null) {
                reader.dispose();
            }

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
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
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        // Final checks on the result
        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

            // check bands
            Assert.assertEquals(2, gc.getNumSampleDimensions());

            // check visible band index for new coverage
            Assert.assertEquals(0, CoverageUtilities.getVisibleBand(gc));

            // check non existing band index
            Assert.assertNotEquals(3, gc.getNumSampleDimensions());

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

    /**
     * Test download of selected bands of raster data, scald and using a ROI area. Result contains
     * only band 1.
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

        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON (( "
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
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        // Final checks on the result
        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

            // check bands
            Assert.assertEquals(1, gc.getNumSampleDimensions());

            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(40, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(40, Math.round(originalGridRange.getHeight()));

            // check envelope
            Assert.assertEquals(
                    -130.88669845369998, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.5552612829, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -124.05382943906582, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    54.00577111704634, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);

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
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        // Final checks on the result
        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(80, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(80, Math.round(originalGridRange.getHeight()));

            // check envelope
            Assert.assertEquals(
                    -130.8866985, gc.getEnvelope().getLowerCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    48.5552613, gc.getEnvelope().getLowerCorner().getOrdinate(1), DELTA);
            Assert.assertEquals(
                    -123.8830077, gc.getEnvelope().getUpperCorner().getOrdinate(0), DELTA);
            Assert.assertEquals(
                    54.1420339, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);
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

        ///////////////////////////////////////
        //      test partial input           //
        ///////////////////////////////////////

        // Download the coverage as tiff
        File largerZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
                        null, // targetCRS
                        WGS84, // roiCRS
                        null, // roi
                        false, // cropToGeometry
                        null, // interpolation
                        160, // targetSizeX
                        null, // targetSizeY not specified, will be calculated based on targetSizeX
                        // and aspect ratio of the original image
                        null, // bandSelectIndices
                        null, // Writing params
                        false,
                        false,
                        0d,
                        null,
                        new NullProgressListener() // progressListener
                        );

        // Final checks on the result
        Assert.assertNotNull(largerZip);
        try {
            final File[] tiffFiles = extractFiles(largerZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(160, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(160, Math.round(originalGridRange.getHeight()));
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

        //////////////////////////////////
        //      test with ROI           //
        //////////////////////////////////

        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON (( -127.57473954542964 54.06575021619523, -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        // Download the coverage as tiff
        File resampledZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        // Final checks on the result
        Assert.assertNotNull(resampledZip);
        try {
            final File[] tiffFiles = extractFiles(resampledZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

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
            Assert.assertEquals(
                    54.0861661371, gc.getEnvelope().getUpperCorner().getOrdinate(1), DELTA);
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

    /**
     * Test download of raster data. The output is scaled to fit exactly the provided size. Check
     * that Datatype won't be modified
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadScaledRasterPreservingDatatype() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Download the coverage as tiff
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(SHORT), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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
        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

            // check coverage size
            Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            Assert.assertEquals(80, Math.round(originalGridRange.getWidth()));
            Assert.assertEquals(80, Math.round(originalGridRange.getHeight()));

            // This JUNIT test only focus on checking that the datatype get preserved
            Assert.assertEquals(
                    DataBuffer.TYPE_SHORT, gc.getRenderedImage().getSampleModel().getDataType());

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

        rasterZip =
                downloadProcess.execute(
                        getLayerId(FLOAT), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        Assert.assertNotNull(rasterZip);
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);

            // This JUNIT test only focus on checking that the datatype get preserved
            Assert.assertEquals(
                    DataBuffer.TYPE_FLOAT, gc.getRenderedImage().getSampleModel().getDataType());

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

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a
     * request with a TargetCRS matching one of the underlying CRS of that mosaic should result in
     * no reprojection on granules with that CRS as native.
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
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        try {
            String roiWkt =
                    "POLYGON((180000 600000, 820000 600000, 820000 1200000, 180000 1200000, 180000 600000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS), // layerName
                            null, // filter
                            "image/tiff", // outputFormat
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

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);
            assertTrue(hasPerfectStraightHorizontalLine(gc.getRenderedImage()));

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

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a
     * request with a TargetCRS matching one of the underlying CRS, and asking for the best
     * available resolution from matching CRS will result in minimal processing
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
        GeoTiffReader referenceReader = null;
        GeoTiffReader reader = null;
        GridCoverage2D referenceGc = null;
        GridCoverage2D gc = null;
        RenderedImage referenceImage = null;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        try {
            referenceReader = new GeoTiffReader(file);
            referenceGc = referenceReader.read(null);
            referenceImage = referenceGc.getRenderedImage();
            // Setting filter to get the granule with resolution
            final PropertyName property = FF.property("location");
            Filter filter = (Filter) FF.like(property, "green.tif");

            String roiWkt =
                    "POLYGON((160000 600000, 840000 600000, 840000 1200000, 160000 1200000, 160000 600000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS), // layerName
                            filter, // filter
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
                            null,
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);

            // Compare the downloaded raster with the original tiff.
            // If reprojection to common CRS would have been involved,
            // the above tie-bow pattern would have been distorted, making
            // this comparison fail
            ImageAssert.assertEquals(referenceImage, gc.getRenderedImage(), 5);

            // also make sure the referencing is the same
            assertEquals(referenceGc.getEnvelope2D(), gc.getEnvelope2D());
        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }
            if (referenceGc != null) {
                CoverageCleanerCallback.disposeCoverage(referenceGc);
            }
            if (referenceReader != null) {
                referenceReader.dispose();
            }

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a
     * request with a TargetCRS matching the west-most granule CRS, and asking for the best
     * available resolution from matching CRS will result in minimal processing and a bbox matching
     * the native resolution of the data
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
        GeoTiffReader referenceReader = null;
        GeoTiffReader reader = null;
        GridCoverage2D referenceGc = null;
        GridCoverage2D gc = null;
        RenderedImage referenceImage = null;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32631", true);
        try {
            // tests go out of the stricly sane area for one of the UTMs, could cause 0.006 meters
            // of error and that makes the assertions fail..-
            MapProjection.SKIP_SANITY_CHECKS = true;

            referenceReader = new GeoTiffReader(file);
            referenceGc = referenceReader.read(null);
            referenceImage = referenceGc.getRenderedImage();

            String roiWkt =
                    "POLYGON((150000 550000, 2300000 550000, 2300000 1300000, 160000 1300000, 150000 550000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS), // layerName
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
                            null,
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);

            // check we get the expected referencing and resolution out
            MathTransform2D mt = gc.getGridGeometry().getGridToCRS2D();
            assertThat(mt, CoreMatchers.instanceOf(AffineTransform2D.class));
            AffineTransform2D at = (AffineTransform2D) mt;
            assertEquals(1000, at.getScaleX(), 0);
            assertEquals(-1000, at.getScaleY(), 0);

            // the red  one defines left-most location, but the green one lower corner reprojected
            // is just a smidge below 600000, bringing the alignment of output down to 599000
            Envelope2D gcEnvelope = gc.getEnvelope2D();
            assertEquals(160000, gcEnvelope.getMinimum(0), 0);
            assertEquals(599000, gcEnvelope.getMinimum(1), 0);
        } finally {
            // re-enable checks
            MapProjection.SKIP_SANITY_CHECKS = false;

            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }
            if (referenceGc != null) {
                CoverageCleanerCallback.disposeCoverage(referenceGc);
            }
            if (referenceReader != null) {
                referenceReader.dispose();
            }

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a
     * request with a TargetCRS matching one of the underlying CRS of that mosaic but not involving
     * any granule in that CRS will use default approach involving a reprojection.
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSOnDifferentNativeCRS() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 3 granules on
        // 3 different UTM zones (32631, 32632, 32633), being exposed as a 4326 Mosaic
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        GeoTiffReader referenceReader = null;
        GeoTiffReader reader = null;
        GridCoverage2D referenceGc = null;
        GridCoverage2D gc = null;
        RenderedImage referenceImage = null;
        // Note we are asking targetCRS = 32632 but the available granule will be in 32633
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true);
        CoordinateReferenceSystem roiCRS = CRS.decode("EPSG:32633", true);
        try {

            // Setting filter to get the granule
            final PropertyName property = FF.property("location");
            Filter filter = (Filter) FF.like(property, "blue.tif");

            String roiWkt =
                    "POLYGON((180000 600000, 820000 600000, 820000 1200000, 180000 1200000, 180000 600000))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            File rasterZip =
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS), // layerName
                            filter, // filter
                            "image/tiff", // outputFormat
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

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            Assert.assertNotNull(gc);
            AffineTransform gridToWorld = (AffineTransform) gc.getGridGeometry().getGridToCRS();
            double scaleX = XAffineTransform.getScaleX0(gridToWorld);
            double scaleY = XAffineTransform.getScaleY0(gridToWorld);
            // Once the file has been downloaded we can't retrieve the processing chain
            // Let's just check if the resolution isn't the native one
            assertNotEquals(1000, scaleX, 10);
            assertNotEquals(1000, scaleY, 10);

            // Finally, the original sample image has a straight
            // white stripe in the middle of a blue fill.
            final File file = new File(this.getTestData().getDataDirectoryRoot(), "hcrs/blue.tif");
            referenceReader = new GeoTiffReader(file);
            referenceGc = referenceReader.read(null);
            referenceImage = referenceGc.getRenderedImage();
            assertTrue(hasPerfectStraightHorizontalLine(referenceImage));

            // Let's extract a stripe from the center of the downloaded image
            // A reprojection will spot a not perfectly straight line
            RenderedImage ri = gc.getRenderedImage();
            assertFalse(hasPerfectStraightHorizontalLine(ri));

        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }
            if (referenceGc != null) {
                CoverageCleanerCallback.disposeCoverage(referenceGc);
            }
            if (referenceReader != null) {
                referenceReader.dispose();
            }

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    /**
     * Test download of raster data. The source is an ImageMosaic with Heterogeneous CRS. Sending a
     * request with a TargetCRS matching one of the underlying CRS of that mosaic should result in
     * no reprojection on granules with that CRS as native, having matching resolution and matching
     * alignment (when the 2 flags minimizeReprojections and bestResolutionOnMatchingCRS are set).
     */
    @Test
    public void testDownloadGranuleHeterogeneousCRSMixedCRS() throws Exception {
        // This test uses an Heterogeneous ImageMosaic made by 2 granules on
        // 2 different EPSG, 31255 and 31256
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS
        GeoTiffReader referenceReader = null;
        GeoTiffReader reader = null;
        GridCoverage2D referenceGc = null;
        GridCoverage2D gc = null;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
        try {
            String roiWkt =
                    "POLYGON ((-102583.25 262175.25, -102332.25 262175.25, -102332.25 262042.25, -102583.25 262042.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

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
                            null,
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            GridGeometry2D gc2d = gc.getGridGeometry();
            AffineTransform transform = (AffineTransform) gc2d.getGridToCRS();

            // Finally, get the original granule in that target CRS
            final File file =
                    new File(this.getTestData().getDataDirectoryRoot(), "hcrs2/31256.tif");
            referenceReader = new GeoTiffReader(file);
            referenceGc = referenceReader.read(null);
            GridGeometry2D referenceGc2d = referenceGc.getGridGeometry();
            AffineTransform referenceTransform = (AffineTransform) referenceGc2d.getGridToCRS();

            // Check that even when requesting an area overlapping 2 different CRS we are getting
            // the native resolution
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

        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }
            if (referenceGc != null) {
                CoverageCleanerCallback.disposeCoverage(referenceGc);
            }
            if (referenceReader != null) {
                referenceReader.dispose();
            }
            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    @Test
    public void testDownloadGranuleHeterogeneousCRSUsingNativeResolutions() throws Exception {
        // This test check that by specifying a resolutionDifferenceTolerance parameter
        // after reprojection we got the native resolution.
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS
        GeoTiffReader referenceReader = null;
        GeoTiffReader reader = null;
        GridCoverage2D referenceGc = null;
        GridCoverage2D gc = null;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
        try {
            String roiWkt =
                    "POLYGON ((-102583.25 262175.25, -102332.25 262175.25, -102332.25 262042.25, -102583.25 262042.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

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
                            false,
                            false,
                            10d,
                            null,
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            GridGeometry2D gc2d = gc.getGridGeometry();
            AffineTransform transform = (AffineTransform) gc2d.getGridToCRS();

            // Finally, get the original granule
            final File file =
                    new File(this.getTestData().getDataDirectoryRoot(), "hcrs2/31255.tif");
            referenceReader = new GeoTiffReader(file);
            referenceGc = referenceReader.read(null);
            GridGeometry2D referenceGc2d = referenceGc.getGridGeometry();
            AffineTransform referenceTransform = (AffineTransform) referenceGc2d.getGridToCRS();

            // Checking that resolutions are equal
            double resX = XAffineTransform.getScaleX0(referenceTransform);
            double resY = XAffineTransform.getScaleY0(referenceTransform);
            assertEquals(resX, XAffineTransform.getScaleX0(transform), 0d);
            assertEquals(resY, XAffineTransform.getScaleY0(transform), 0d);

        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }
            if (referenceGc != null) {
                CoverageCleanerCallback.disposeCoverage(referenceGc);
            }
            if (referenceReader != null) {
                referenceReader.dispose();
            }
            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    @Test
    public void testDownloadGranuleUsingNativeResolutionsWithMinimizeReprojection()
            throws Exception {
        // This test check that by specifying a resolutionDifferenceTolerance parameter,
        // even if minimize reprojection is enabled and no reprojection occurs since mosaic
        // declared crs and target are same, we got the native granules resolution.
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS
        GeoTiffReader referenceReader = null;
        GeoTiffReader reader = null;
        GridCoverage2D referenceGc = null;
        GridCoverage2D gc = null;
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
        try {
            String roiWkt =
                    "POLYGON ((-102583.25 262175.25, -102332.25 262175.25, -102332.25 262042.25, -102583.25 262042.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);

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
                            10d,
                            null,
                            new NullProgressListener() // progressListener
                            );

            Assert.assertNotNull(rasterZip);
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            GridGeometry2D gc2d = gc.getGridGeometry();
            AffineTransform transform = (AffineTransform) gc2d.getGridToCRS();

            // Finally, get the original granule
            final File file =
                    new File(this.getTestData().getDataDirectoryRoot(), "hcrs2/31255.tif");
            referenceReader = new GeoTiffReader(file);
            referenceGc = referenceReader.read(null);
            GridGeometry2D referenceGc2d = referenceGc.getGridGeometry();
            AffineTransform referenceTransform = (AffineTransform) referenceGc2d.getGridToCRS();

            // Checking that resolutions are equal
            double resX = XAffineTransform.getScaleX0(referenceTransform);
            double resY = XAffineTransform.getScaleY0(referenceTransform);
            assertEquals(resX, XAffineTransform.getScaleX0(transform), 0d);
            assertEquals(resY, XAffineTransform.getScaleY0(transform), 0d);

        } finally {
            if (gc != null) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }
            if (reader != null) {
                reader.dispose();
            }
            if (referenceGc != null) {
                CoverageCleanerCallback.disposeCoverage(referenceGc);
            }
            if (referenceReader != null) {
                referenceReader.dispose();
            }
            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    /**
     * heuristic method do determine if an image has a perfect straight horizontal line in the
     * middle.
     */
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
        ZipArchivePPIO ppio =
                new ZipArchivePPIO(DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL);

        // ROI as a BBOX
        Envelope env =
                new Envelope(-125.074006936869, -123.88300771369998, 48.5552612829, 49.03872);
        Polygon roi = JTS.toGeometry(env);

        // Download the data with ROI
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
                        null, // targetCRS
                        CRS.decode("EPSG:4326"), // roiCRS
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
        Assert.assertNotNull(rasterZip);

        // make sure we create files locally so that we don't clog the sytem temp
        final File currentDirectory = new File(DownloadProcessTest.class.getResource(".").toURI());
        File tempZipFile = File.createTempFile("zipppiotemp", ".zip", currentDirectory);
        ppio.encode(rasterZip, new FileOutputStream(tempZipFile));

        Assert.assertTrue(tempZipFile.length() > 0);

        final File tempDir = new File(currentDirectory, Long.toString(System.nanoTime()));
        Assert.assertTrue(tempDir.mkdir());
        File tempFile = decode(new FileInputStream(tempZipFile), tempDir);
        Assert.assertNotNull(tempFile);
        IOUtils.delete(tempFile);
    }

    /**
     * Test download estimator for raster data. The result should exceed the limits
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorReadLimitsRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        10,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI as polygon
        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON (( -127.57473954542964 54.06575021619523, -130.8545966116691 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data with ROI. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
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

            Assert.assertFalse(true);
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.lang.IllegalArgumentException: Download Limits Exceeded. Unable to proceed!: Download Limits Exceeded. Unable to proceed!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
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
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        10,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI
        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON (( -127.57473954542964 54.06575021619523, -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data with ROI. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
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

            Assert.assertFalse(true);
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "org.geotools.process.ProcessException: java.io.IOException: Download Exceeded the maximum HARD allowed size!: java.io.IOException: Download Exceeded the maximum HARD allowed size!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download estimator write limits raster for scaled output. Scaled image should exceed the
     * limits, whereas the original raster should not.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorWriteLimitsScaledRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        921600, // 900KB
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);

        File nonScaled =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        "image/tiff", // outputFormat
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

        Assert.assertNotNull(nonScaled);

        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(nonScaled, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);

            // ten times the size of the original coverage
            int targetSizeX = (int) (gc.getGridGeometry().getGridRange2D().getWidth() * 10);
            int targetSizeY = (int) (gc.getGridGeometry().getGridRange2D().getHeight() * 10);
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
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
            Assert.assertFalse(true);
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "org.geotools.process.ProcessException: java.io.IOException: Download Exceeded the maximum HARD allowed size!: java.io.IOException: Download Exceeded the maximum HARD allowed size!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
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

        // Test same process for checking write output limits, using selected band indices
        limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        30000, // = 100x100 pixels x 3 bands x 1 byte (8 bits) per
                                        // band
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        downloadProcess = new DownloadProcess(getGeoServer(), limits, resourceManager);

        try {
            // create a scaled 100x100 raster, with 4 bands
            int targetSizeX = 100;
            int targetSizeY = 100;
            int[] bandIndices = new int[] {0, 2, 2, 2};
            File scaled =
                    downloadProcess.execute(
                            getLayerId(MockData.USA_WORLDIMG), // layerName
                            null, // filter
                            "image/tiff", // outputFormat
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
            Assert.assertFalse(true);
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.lang.IllegalArgumentException: Download Limits Exceeded. "
                            + "Unable to proceed!: Download Limits Exceeded. Unable to proceed!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    /**
     * Test download estimator for raster data. The result should exceed the integer limits
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorIntegerMaxValueLimitRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        (long) 1E12, // huge number, way above integer limits
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI as polygon
        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON (( -127.57473954542964 54.06575021619523, -130.8545966116691 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data with ROI. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
                    null, // targetCRS
                    WGS84, // roiCRS
                    roi, // roi
                    false, // cropToGeometry
                    null, // interpolation
                    100000, // targetSizeX
                    60000, // targetSizeY
                    null, // bandSelectIndices
                    null, // Writing params
                    false,
                    false,
                    0d,
                    null,
                    new NullProgressListener() // progressListener
                    );

            Assert.fail();
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.lang.IllegalArgumentException: Download Limits Exceeded. Unable to proceed!",
                    e.getMessage());
        }
    }

    /**
     * Test download estimator for raster data. Make sure the estimator works again full raster at
     * native resolution downloads
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorFullNativeRaster() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        (long) 10, // small number, but before fix it was not
                                        // triggering exception
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        // Estimate download full data at native resolution. It should return false
        assertFalse(
                limits.execute(
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
     * Test download estimator for vectorial data. The result should be exceed the hard output
     * limits
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadEstimatorHardOutputLimit() throws Exception {
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        10,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);
        try {
            // Download the features. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.POLYGONS), // layerName
                    null, // filter
                    "application/zip", // outputFormat
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
                    new NullProgressListener() // progressListener
                    );

            Assert.assertFalse(true);
        } catch (ProcessException e) {
            Assert.assertEquals(
                    "java.io.IOException: Download Exceeded the maximum HARD allowed size!: Download Exceeded the maximum HARD allowed size!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
        }
    }

    private WPSResourceManager getResourceManager() {
        return GeoServerExtensions.bean(WPSResourceManager.class);
    }

    /**
     * Test download physical limit for raster data. It should throw an exception
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadPhysicalLimitsRaster() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        ProcessListener listener =
                new ProcessListener(
                        new ExecutionStatus(
                                new NameImpl("gs", "DownloadEstimator"),
                                resourceManager.getExecutionId(false),
                                false));
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(), getGeoServer());

        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);
        // ROI data
        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read(
                                        "POLYGON (( -127.57473954542964 54.06575021619523, -130.88669845369998 52.00807146727025, -129.50812897394974 49.85372324691927, -130.5300633861675 49.20465679591609, -129.25955033314003 48.60392508062591, -128.00975216684665 50.986137055052474, -125.8623089087404 48.63154492960477, -123.984159178178 50.68231871628503, -126.91186316993704 52.15307567440926, -125.3444367403868 53.54787804784162, -127.57473954542964 54.06575021619523 ))");
        roi.setSRID(4326);

        try {
            // Download the data. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.USA_WORLDIMG), // layerName
                    null, // filter
                    "image/tiff", // outputFormat
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
                    "org.geotools.process.ProcessException: java.io.IOException: Download Exceeded the maximum HARD allowed size!: java.io.IOException: Download Exceeded the maximum HARD allowed size!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
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
        ProcessListener listener =
                new ProcessListener(
                        new ExecutionStatus(
                                new NameImpl("gs", "DownloadEstimator"),
                                resourceManager.getExecutionId(false),
                                false));
        // Estimator process for checking limits
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(
                        new StaticDownloadServiceConfiguration(
                                new DownloadServiceConfiguration(
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        DownloadServiceConfiguration.NO_LIMIT,
                                        1,
                                        DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL,
                                        DownloadServiceConfiguration.NO_LIMIT)),
                        getGeoServer());

        // Creates the new process for the download
        DownloadProcess downloadProcess =
                new DownloadProcess(getGeoServer(), limits, resourceManager);

        try {
            // Download the features. It should throw an exception
            downloadProcess.execute(
                    getLayerId(MockData.POLYGONS), // layerName
                    null, // filter
                    "application/zip", // outputFormat
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
                    "java.io.IOException: Download Exceeded the maximum HARD allowed size!: Download Exceeded the maximum HARD allowed size!",
                    e.getMessage()
                            + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));

            Throwable le = listener.exception;
            Assert.assertEquals(
                    "java.io.IOException: Download Exceeded the maximum HARD allowed size!: Download Exceeded the maximum HARD allowed size!",
                    le.getMessage()
                            + (le.getCause() != null ? ": " + le.getCause().getMessage() : ""));

            return;
        }

        Assert.assertFalse(true);
    }

    /**
     * Test with a wrong output format. It should thrown an exception.
     *
     * @throws Exception the exception
     */
    @Test
    public void testWrongOutputFormat() throws Exception {
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess();

        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        SimpleFeatureCollection rawSource =
                (SimpleFeatureCollection) ti.getFeatureSource(null, null).getFeatures();

        final DefaultProgressListener progressListener = new DefaultProgressListener();
        try {
            // Download the features. It should throw an exception.
            downloadProcess.execute(
                    getLayerId(MockData.POLYGONS), // layerName
                    null, // filter
                    "IAmWrong!!!", // outputFormat
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

            Assert.assertTrue("We did not get an exception", false);
        } catch (Exception e) {
            Assert.assertTrue("Everything as expected", true);
        }
    }

    /**
     * Test download of raster data using underlying granules resolution. The sample mosaic is
     * composed of:
     *
     * <p>18km_32610.tif with resolution = 17550.948453185396000 meters 9km_32610.tif with
     * resolution = 8712.564801039759900 meters
     */
    @Test
    public void testDownloadGranuleHeterogeneousResolution() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);

        // Setting filter to get the granule with resolution
        final PropertyName property = FF.property("resolution");
        Filter filter = (Filter) FF.greaterOrEqual(property, FF.literal(16000));

        testExpectedResolution(
                downloadProcess,
                filter,
                WGS84,
                ROI2,
                resourceManager,
                17550.94845318,
                -17550.94845318);

        // Download native resolution 2
        filter =
                FF.and(
                        FF.lessOrEqual(property, FF.literal(10000)),
                        FF.greaterOrEqual(property, FF.literal(1000)));

        testExpectedResolution(
                downloadProcess,
                filter,
                null,
                null,
                resourceManager,
                8712.564801039759900,
                -8712.564801039759900);

        // Download native resolution 3
        filter = (Filter) FF.lessOrEqual(property, FF.literal(1000));

        // Final checks on the result
        testExpectedResolution(
                downloadProcess,
                filter,
                null,
                null,
                resourceManager,
                7818.453242658203,
                -10139.712928934865);

        filter =
                FF.and(
                        FF.lessOrEqual(property, FF.literal(10000)),
                        FF.greaterOrEqual(property, FF.literal(1000)));

        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MIXED_RES), // layerName
                        filter, // filter
                        "image/tiff", // outputFormat
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

        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);
            // check coverage size
            RenderedImage ri = gc.getRenderedImage();
            Assert.assertEquals(512, ri.getWidth());
            Assert.assertEquals(128, ri.getHeight());

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

    private void testExpectedResolution(
            DownloadProcess downloadProcess,
            Filter filter,
            CoordinateReferenceSystem roiCrs,
            Polygon roi,
            WPSResourceManager resourceManager,
            double expectedX,
            double expectedY)
            throws IOException {

        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MIXED_RES), // layerName
                        filter, // filter
                        "image/tiff", // outputFormat
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

        Assert.assertNotNull(rasterZip);
        GeoTiffReader reader = null;
        GridCoverage2D gc = null;
        try {
            final File[] tiffFiles = extractFiles(rasterZip, "GTIFF");
            Assert.assertNotNull(tiffFiles);
            Assert.assertTrue(tiffFiles.length > 0);
            reader = new GeoTiffReader(tiffFiles[0]);
            gc = reader.read(null);

            Assert.assertNotNull(gc);
            Assert.assertEquals(
                    "32610",
                    gc.getCoordinateReferenceSystem().getIdentifiers().iterator().next().getCode());

            // check coverage size
            MathTransform mt = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
            AffineTransform2D transform2D = (AffineTransform2D) mt;
            double resX = transform2D.getScaleX();
            double resY = transform2D.getScaleY();

            Assert.assertEquals(expectedX, resX, DELTA);
            Assert.assertEquals(expectedY, resY, DELTA);

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

        Polygon roi =
                (Polygon)
                        new WKTReader2()
                                .read("POLYGON ((-128 54, -128 50, -130 50, -130 54, -128 54))");
        roi.setSRID(4326);

        final int requestedSizeX = 128;
        final int requestedSizeY = 128;

        // Download the coverage
        File rasterZip =
                downloadProcess.execute(
                        getLayerId(MockData.USA_WORLDIMG), // layerName
                        null, // filter
                        outputFormat, // outputFormat
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

        // Final checks on the result
        Assert.assertNotNull(rasterZip);

        try {
            String formatName = MimeType.valueOf(outputFormat).getSubtype().toUpperCase();
            final File[] files = extractFiles(rasterZip, formatName);
            Assert.assertNotNull(files);
            Assert.assertTrue(files.length > 0);
            testDownloadedImage(files[0], formatName, requestedSizeX, requestedSizeY);
        } finally {

            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
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
        public InternationalString getTask() {
            return task;
        }

        /**
         * Sets the task.
         *
         * @param task the new task
         */
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
        public void started() {
            status.setPhase(ProcessState.RUNNING);
        }

        /**
         * Progress.
         *
         * @param percent the percent
         */
        public void progress(float percent) {
            status.setProgress(percent);
        }

        /**
         * Gets the progress.
         *
         * @return the progress
         */
        public float getProgress() {
            return status.getProgress();
        }

        /** Complete. */
        public void complete() {
            // nothing to do
        }

        /** Dispose. */
        public void dispose() {
            // nothing to do
        }

        /**
         * Checks if is canceled.
         *
         * @return true, if is canceled
         */
        public boolean isCanceled() {
            return status.getPhase() == ProcessState.DISMISSING;
        }

        /**
         * Sets the canceled.
         *
         * @param cancel the new canceled
         */
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
        public void warningOccurred(String source, String location, String warning) {
            LOGGER.log(
                    Level.WARNING,
                    "Got a warning during process execution "
                            + status.getExecutionId()
                            + ": "
                            + warning);
        }

        /**
         * Exception occurred.
         *
         * @param exception the exception
         */
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
        File tempDir =
                IOUtils.createRandomDirectory(
                        IOUtils.createTempDirectory("shpziptemp").getAbsolutePath(),
                        "download-process",
                        "download-services");

        // unzip to the temporary directory
        ZipInputStream zis = null;
        File shapeFile = null;
        File zipFile = null;

        // extract shp-zip file
        try {
            zis = new ZipInputStream(input);
            ZipEntry entry = null;

            // Cycle on all the entries and copies the input shape in the target directory
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
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
                    byte data[] = new byte[4096];
                    // write the files to the disk
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                        while ((count = zis.read(data)) != -1) {
                            fos.write(data, 0, count);
                        }
                        fos.flush();
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                }
                zis.closeEntry();
            }
        } finally {
            if (zis != null) {
                zis.close();
            }
        }

        // Read the shapefile
        if (shapeFile == null) {
            if (zipFile != null) return decodeShape(new FileInputStream(zipFile));
            else {
                FileUtils.deleteDirectory(tempDir);
                throw new IOException(
                        "Could not find any file with .shp extension in the zip file");
            }
        } else {
            return new ShapefileDataStore(URLs.fileToUrl(shapeFile));
        }
    }

    private void testDownloadedImage(File inputFile, String formatName, int sizeX, int sizeY)
            throws Exception {
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

    private DownloadProcess createDefaultTestingDownloadProcess(
            WPSResourceManager resourceManager) {
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        return new DownloadProcess(geoserver, limits, resourceManager);
    }
}
