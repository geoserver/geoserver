/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.QName;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.resource.Resources;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ShapeZipTest extends WFSTestSupport {

    private static final QName ALL_TYPES =
            new QName(SystemTestData.CITE_URI, "AllTypes", SystemTestData.CITE_PREFIX);

    private static final QName ALL_DOTS =
            new QName(SystemTestData.CITE_URI, "All.Types.Dots", SystemTestData.CITE_PREFIX);

    private static final QName GEOMMID =
            new QName(SystemTestData.CITE_URI, "geommid", SystemTestData.CITE_PREFIX);

    private static final QName LONGNAMES =
            new QName(SystemTestData.CITE_URI, "longnames", SystemTestData.CITE_PREFIX);

    private static final QName NULLGEOM =
            new QName(SystemTestData.CITE_URI, "nullgeom", SystemTestData.CITE_PREFIX);

    private static final QName DOTS =
            new QName(SystemTestData.CITE_URI, "dots.in.name", SystemTestData.CITE_PREFIX);

    private Operation op;

    private GetFeatureType gft;

    @Before
    public void init() throws Exception {
        gft = WfsFactory.eINSTANCE.createGetFeatureType();
        op = new Operation("GetFeature", getServiceDescriptor10(), null, new Object[] {gft});
    }

    @Before
    public void cleanupTemplates() throws Exception {
        WorkspaceInfo ws =
                getCatalog().getWorkspaceByName(SystemTestData.BASIC_POLYGONS.getPrefix());
        File wsDir = Resources.directory(getDataDirectory().get(ws));
        new File(wsDir, "shapezip.ftl").delete();
        setupESRIFormatByDefault(getGeoServer(), false);
    }

    @Before
    public void resetServiceConfiguration() throws Exception {
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setProxyBaseUrl(null);
        getGeoServer().save(gs);
    }

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {

        Map params = new HashMap();
        params.put(SystemTestData.LayerProperty.SRS, 4326);
        dataDirectory.addVectorLayer(ALL_TYPES, params, ShapeZipTest.class, getCatalog());
        dataDirectory.addVectorLayer(ALL_DOTS, params, ShapeZipTest.class, getCatalog());
        dataDirectory.addVectorLayer(GEOMMID, params, ShapeZipTest.class, getCatalog());
        dataDirectory.addVectorLayer(NULLGEOM, params, ShapeZipTest.class, getCatalog());
        dataDirectory.addVectorLayer(DOTS, params, ShapeZipTest.class, getCatalog());
        dataDirectory.addVectorLayer(LONGNAMES, params, ShapeZipTest.class, getCatalog());
    }

    @Test
    public void testNoNativeProjection() throws Exception {
        byte[] zip = writeOut(getFeatureSource(SystemTestData.BASIC_POLYGONS).getFeatures());

        checkShapefileIntegrity(new String[] {"BasicPolygons"}, new ByteArrayInputStream(zip));
    }

    @Test
    public void testCharset() throws Exception {
        FeatureSource<? extends FeatureType, ? extends Feature> fs;
        fs = getFeatureSource(SystemTestData.BASIC_POLYGONS);
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(
                        GeoServerExtensions.bean(GeoServer.class),
                        (Catalog) GeoServerExtensions.bean("catalog"),
                        (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());

        // add the charset
        Map options = new HashMap();
        options.put("CHARSET", Charset.forName("ISO-8859-15"));
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);

        checkShapefileIntegrity(
                new String[] {"BasicPolygons"}, new ByteArrayInputStream(bos.toByteArray()));
        assertEquals("ISO-8859-15", getCharset(new ByteArrayInputStream(bos.toByteArray())));
    }

    @Test
    public void testRequestUrlNoProxy() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=1.0.0"
                                + "&request=GetFeature&typeName="
                                + getLayerId(SystemTestData.BASIC_POLYGONS)
                                + "&outputFormat=SHAPE-ZIP");
        assertEquals("application/zip", response.getContentType());
        checkShapefileIntegrity(new String[] {"BasicPolygons"}, getBinaryInputStream(response));
        assertEquals(
                "http://localhost:8080/geoserver/wfs?service=WFS&version=1.0.0&request=GetFeature&typeName=cite:BasicPolygons&outputFormat=SHAPE-ZIP",
                getRequest(getBinaryInputStream(response)));
    }

    @Test
    public void testRequestUrlWithProxyBase() throws Exception {
        // setup a proxy base url
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setProxyBaseUrl("https://www.geoserver.org/geoserver");
        getGeoServer().save(gs);

        // check it has been honored
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=1.0.0"
                                + "&request=GetFeature&typeName="
                                + getLayerId(SystemTestData.BASIC_POLYGONS)
                                + "&outputFormat=SHAPE-ZIP");
        assertEquals("application/zip", response.getContentType());
        checkShapefileIntegrity(new String[] {"BasicPolygons"}, getBinaryInputStream(response));
        assertEquals(
                "https://www.geoserver.org/geoserver/wfs?service=WFS&version=1.0.0&request=GetFeature&typeName=cite:BasicPolygons&outputFormat=SHAPE-ZIP",
                getRequest(getBinaryInputStream(response)));
    }

    @Test
    public void testMultiType() throws Exception {
        byte[] zip = writeOut(getFeatureSource(ALL_TYPES).getFeatures());

        final String[] expectedTypes =
                new String[] {"AllTypesPoint", "AllTypesMPoint", "AllTypesPolygon", "AllTypesLine"};
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    @Test
    public void testSplitSize() throws Exception {
        ShapeZipOutputFormat of =
                (ShapeZipOutputFormat) applicationContext.getBean("shapezipOutputFormat");
        byte[] zip =
                writeOut(getFeatureSource(SystemTestData.BASIC_POLYGONS).getFeatures(), 500, 500);
        String shapefileName = SystemTestData.BASIC_POLYGONS.getLocalPart();
        final String[] expectedTypes =
                new String[] {shapefileName, shapefileName + "1", shapefileName + "2"};
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
    }

    @Test
    public void testMultiTypeDots() throws Exception {
        byte[] zip = writeOut(getFeatureSource(ALL_DOTS).getFeatures());

        final String[] expectedTypes =
                new String[] {
                    "All_Types_DotsPoint",
                    "All_Types_DotsMPoint",
                    "All_Types_DotsPolygon",
                    "All_Types_DotsLine"
                };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    @Test
    public void testGeometryInTheMiddle() throws Exception {
        byte[] zip = writeOut(getFeatureSource(GEOMMID).getFeatures());

        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    @Test
    public void testNullGeometries() throws Exception {
        byte[] zip = writeOut(getFeatureSource(NULLGEOM).getFeatures());

        final String[] expectedTypes = new String[] {"nullgeom"};
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
    }

    @Test
    public void testLongNames() throws Exception {
        byte[] zip = writeOut(getFeatureSource(LONGNAMES).getFeatures());

        // check the result is not empty
        SimpleFeatureType schema = checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));

        // check the schema is the expected one
        checkLongNamesSchema(schema);

        // run it again, we had a bug in which the remapped names changed at each run
        zip = writeOut(getFeatureSource(LONGNAMES).getFeatures());
        schema = checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
        checkLongNamesSchema(schema);
    }

    void checkLongNamesSchema(SimpleFeatureType schema) {
        assertEquals(4, schema.getAttributeCount());
        assertEquals("the_geom", schema.getDescriptor(0).getName().getLocalPart());
        assertEquals(MultiPolygon.class, schema.getDescriptor(0).getType().getBinding());
        assertEquals("FID", schema.getDescriptor(1).getName().getLocalPart());
        assertEquals("VERYLONGNA", schema.getDescriptor(2).getName().getLocalPart());
        assertEquals("VERYLONGN0", schema.getDescriptor(3).getName().getLocalPart());
    }

    @Test
    public void testDots() throws Exception {
        byte[] zip = writeOut(getFeatureSource(DOTS).getFeatures());

        final String[] expectedTypes = new String[] {"dots_in_name"};
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    @Test
    public void testEmptyResult() throws Exception {
        byte[] zip =
                writeOut(
                        getFeatureSource(SystemTestData.BASIC_POLYGONS)
                                .getFeatures(Filter.EXCLUDE));

        checkShapefileIntegrity(new String[] {"BasicPolygons"}, new ByteArrayInputStream(zip));
    }

    @Test
    public void testEmptyResultMultiGeom() throws Exception {
        byte[] zip = writeOut(getFeatureSource(ALL_DOTS).getFeatures(Filter.EXCLUDE));

        boolean foundReadme = false;
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            foundReadme |= entry.getName().equals("README.TXT");
        }
        assertTrue("Did not find readme file", foundReadme);
    }

    @Test
    public void testTemplateSingleType() throws Exception {
        // copy the new template to the data dir
        WorkspaceInfo ws =
                getCatalog().getWorkspaceByName(SystemTestData.BASIC_POLYGONS.getPrefix());
        Resources.copy(
                getClass().getResourceAsStream("shapeziptest.ftl"),
                getDataDirectory().get(ws),
                "shapezip.ftl");

        // setup the request params
        SimpleFeatureCollection fc =
                getFeatureSource(SystemTestData.BASIC_POLYGONS).getFeatures(Filter.INCLUDE);
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(
                        GeoServerExtensions.bean(GeoServer.class),
                        (Catalog) GeoServerExtensions.bean("catalog"),
                        (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fc);

        // get the file name
        assertEquals("shapezip_BasicPolygons.zip", zip.getAttachmentFileName(fct, op));

        // check the contents
        zip.write(fct, bos, op);
        byte[] zipBytes = bos.toByteArray();
        checkShapefileIntegrity(
                new String[] {"theshape_BasicPolygons"}, new ByteArrayInputStream(zipBytes));
    }

    @Test
    public void testTemplateMultiType() throws Exception {
        // copy the new template to the data dir
        WorkspaceInfo ws =
                getCatalog().getWorkspaceByName(SystemTestData.BASIC_POLYGONS.getPrefix());
        Resources.copy(
                getClass().getResourceAsStream("shapeziptest.ftl"),
                getDataDirectory().get(ws),
                "shapezip.ftl");

        // setup the request params
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(
                        GeoServerExtensions.bean(GeoServer.class),
                        (Catalog) GeoServerExtensions.bean("catalog"),
                        (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature()
                .add(getFeatureSource(SystemTestData.BASIC_POLYGONS).getFeatures(Filter.INCLUDE));
        fct.getFeature().add(getFeatureSource(SystemTestData.BRIDGES).getFeatures(Filter.INCLUDE));

        // get the file name
        assertEquals("shapezip_BasicPolygons.zip", zip.getAttachmentFileName(fct, op));

        // check the contents
        zip.write(fct, bos, op);
        byte[] zipBytes = bos.toByteArray();
        checkShapefileIntegrity(
                new String[] {"theshape_BasicPolygons", "theshape_Bridges"},
                new ByteArrayInputStream(zipBytes));
    }

    @Test
    public void testTemplateMultiGeomType() throws Exception {
        // copy the new template to the data dir
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(ALL_DOTS.getPrefix());
        Resources.copy(
                getClass().getResourceAsStream("shapeziptest.ftl"),
                getDataDirectory().get(ws),
                "shapezip.ftl");

        // setup the request params
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(
                        GeoServerExtensions.bean(GeoServer.class),
                        (Catalog) GeoServerExtensions.bean("catalog"),
                        (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(getFeatureSource(ALL_DOTS).getFeatures(Filter.INCLUDE));

        // get the file name
        assertEquals("shapezip_All_Types_Dots.zip", zip.getAttachmentFileName(fct, op));

        // check the contents
        zip.write(fct, bos, op);
        byte[] zipBytes = bos.toByteArray();
        checkShapefileIntegrity(
                new String[] {
                    "theshape_All_Types_DotsPoint",
                    "theshape_All_Types_DotsMPoint",
                    "theshape_All_Types_DotsPolygon",
                    "theshape_All_Types_DotsLine"
                },
                new ByteArrayInputStream(zipBytes));
    }

    @Test
    public void testTemplatePOSTRequest10() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "outputFormat=\"shape-zip\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals("application/zip", response.getContentType());
    }

    @Test
    public void testOutputZipFileNameSpecifiedInFormatOptions() throws Exception {
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(getGeoServer(), getCatalog(), getResourceLoader());

        FeatureCollectionResponse mockResult =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        mockResult.getFeature().add(getFeatureSource(ALL_DOTS).getFeatures(Filter.INCLUDE));

        GetFeatureType mockRequest = WfsFactory.eINSTANCE.createGetFeatureType();

        Operation mockOperation =
                new Operation(
                        "GetFeature", getServiceDescriptor10(), null, new Object[] {mockRequest});

        assertEquals("All_Types_Dots.zip", zip.getAttachmentFileName(mockResult, mockOperation));

        mockRequest.getFormatOptions().put("FILENAME", "REQUEST_SUPPLIED_FILENAME.zip");

        assertEquals(
                "REQUEST_SUPPLIED_FILENAME.zip",
                zip.getAttachmentFileName(mockResult, mockOperation));
    }

    @Test
    public void testTemplatePOSTRequest11() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<GetFeature xmlns=\"http://www.opengis.net/wfs\" xmlns:DigitalGlobe=\"http://www.digitalglobe.com\"\n"
                        + "    xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "    xmlns:gml=\"http://www.opengis.net/gml\" service=\"WFS\" version=\"1.1.0\"\n"
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "    outputFormat=\"shape-zip\" maxFeatures=\"100\" handle=\"\">\n"
                        + "    <Query typeName=\"cdf:Other\" srsName=\"urn:ogc:def:crs:EPSG::4326\">"
                        + "</Query> "
                        + "</GetFeature>";

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals("application/zip", response.getContentType());
    }

    @Test
    public void testESRIFormat() throws Exception {
        setupESRIPropertyFile();
        FeatureSource<? extends FeatureType, ? extends Feature> fs;
        fs = getFeatureSource(SystemTestData.BASIC_POLYGONS);
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(getGeoServer(), getCatalog(), getResourceLoader());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());

        // add the charset
        Map options = new HashMap();
        options.put("PRJFILEFORMAT", "ESRI");
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);

        byte[] byteArrayZip = bos.toByteArray();
        checkShapefileIntegrity(
                new String[] {"BasicPolygons"}, new ByteArrayInputStream(byteArrayZip));

        checkFileContent(
                "BasicPolygons.prj",
                new ByteArrayInputStream(byteArrayZip),
                get4326_ESRI_WKTContent());
    }

    @Test
    public void testESRIFormatMultiType() throws Exception {
        setupESRIPropertyFile();
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(getGeoServer(), getCatalog(), getResourceLoader());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(getFeatureSource(ALL_TYPES).getFeatures());
        Map options = new HashMap();
        options.put("PRJFILEFORMAT", "ESRI");
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);
        byte[] byteArrayZip = bos.toByteArray();

        final String[] expectedTypes =
                new String[] {"AllTypesPoint", "AllTypesMPoint", "AllTypesPolygon", "AllTypesLine"};
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(byteArrayZip));

        for (String fileName : expectedTypes) {
            checkFileContent(
                    fileName + ".prj",
                    new ByteArrayInputStream(byteArrayZip),
                    get4326_ESRI_WKTContent());
        }
    }

    @Test
    public void testESRIFormatFromDefaultValue() throws Exception {
        setupESRIPropertyFile();

        final GeoServer geoServer = getGeoServer();
        setupESRIFormatByDefault(geoServer, true);

        final FeatureSource fs = getFeatureSource(SystemTestData.BASIC_POLYGONS);
        final Catalog catalog = getCatalog();
        final GeoServerResourceLoader resourceLoader = getResourceLoader();

        ShapeZipOutputFormat zip = new ShapeZipOutputFormat(geoServer, catalog, resourceLoader);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());

        // add the charset
        Map options = new HashMap();
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);

        byte[] byteArrayZip = bos.toByteArray();
        checkShapefileIntegrity(
                new String[] {"BasicPolygons"}, new ByteArrayInputStream(byteArrayZip));

        checkFileContent(
                "BasicPolygons.prj",
                new ByteArrayInputStream(byteArrayZip),
                get4326_ESRI_WKTContent());
    }

    /**
     * Saves the feature source contents into a zipped shapefile, returns the output as a byte array
     */
    byte[] writeOut(FeatureCollection fc, long maxShpSize, long maxDbfSize) throws IOException {
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(
                        GeoServerExtensions.bean(GeoServer.class),
                        (Catalog) GeoServerExtensions.bean("catalog"),
                        (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
        zip.setMaxDbfSize(maxDbfSize);
        zip.setMaxShpSize(maxShpSize);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fc);
        zip.write(fct, bos, op);
        return bos.toByteArray();
    }

    /**
     * Saves the feature source contents into a zipped shapefile, returns the output as a byte array
     */
    byte[] writeOut(FeatureCollection fc) throws IOException {
        ShapeZipOutputFormat zip =
                new ShapeZipOutputFormat(
                        GeoServerExtensions.bean(GeoServer.class),
                        (Catalog) GeoServerExtensions.bean("catalog"),
                        (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fc);
        zip.write(fct, bos, op);
        return bos.toByteArray();
    }

    private File createTempFolder(String prefix) throws IOException {
        File temp = File.createTempFile(prefix, null);

        temp.delete();
        temp.mkdir();
        return temp;
    }

    private void copyStream(InputStream inStream, OutputStream outStream) throws IOException {
        int count = 0;
        byte[] buf = new byte[8192];
        while ((count = inStream.read(buf, 0, 8192)) != -1) outStream.write(buf, 0, count);
    }

    private SimpleFeatureType checkFieldsAreNotEmpty(InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;

        File tempFolder = createTempFolder("shp_");
        String shapeFileName = "";
        while ((entry = zis.getNextEntry()) != null) {
            final String name = entry.getName();
            String outName = tempFolder.getAbsolutePath() + File.separatorChar + name;
            // store .shp file name
            if (name.toLowerCase().endsWith("shp")) shapeFileName = outName;
            // copy each file to temp folder

            FileOutputStream outFile = new FileOutputStream(outName);
            copyStream(zis, outFile);
            outFile.close();
            zis.closeEntry();
        }
        zis.close();

        // create a datastore reading the uncompressed shapefile
        File shapeFile = new File(shapeFileName);
        ShapefileDataStore ds = new ShapefileDataStore(URLs.fileToUrl(shapeFile));
        SimpleFeatureSource fs = ds.getFeatureSource();
        SimpleFeatureCollection fc = fs.getFeatures();
        SimpleFeatureType schema = fc.getSchema();

        SimpleFeatureIterator iter = fc.features();
        try {
            // check that every field has a not null or "empty" value
            while (iter.hasNext()) {
                SimpleFeature f = iter.next();
                for (Object attrValue : f.getAttributes()) {
                    assertNotNull(attrValue);
                    if (Geometry.class.isAssignableFrom(attrValue.getClass()))
                        assertFalse("Empty geometry", ((Geometry) attrValue).isEmpty());
                    else
                        assertFalse(
                                "Empty value for attribute",
                                attrValue.toString().trim().equals(""));
                }
            }
        } finally {
            iter.close();
            ds.dispose();
            FileUtils.deleteQuietly(tempFolder);
        }

        return schema;
    }

    /**
     * Writes out an {@code esri.properties} file to {@code <data_dir>/user_projections/} with the
     * single entry: {@code 4326=<esri version of 4326 WKT>}
     */
    private void setupESRIPropertyFile() throws IOException {
        String esri_properties = "4326=" + get4326_ESRI_WKTContent();
        InputStream input = new ByteArrayInputStream(esri_properties.getBytes());
        File directory = getResourceLoader().findOrCreateDirectory("user_projections");
        File file = new File(directory, "esri.properties");
        if (file.exists()) {
            file.delete();
        }
        org.geoserver.util.IOUtils.copy(input, file);
    }

    private void setupESRIFormatByDefault(GeoServer geoServer, Boolean value) throws IOException {
        WFSInfo wfsInfo = geoServer.getService(WFSInfo.class);
        MetadataMap metadata = wfsInfo.getMetadata();
        metadata.put(ShapeZipOutputFormat.SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI, value);
        geoServer.save(wfsInfo);
    }

    private void checkShapefileIntegrity(String[] typeNames, final InputStream in)
            throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;

        final String[] extensions = new String[] {".shp", ".shx", ".dbf", ".prj", ".cst"};
        Set names = new HashSet();
        for (String name : typeNames) {
            for (String extension : extensions) {
                names.add(name + extension);
            }
        }
        Set<String> found = new HashSet<>();
        while ((entry = zis.getNextEntry()) != null) {
            final String name = entry.getName();
            found.add(name);
            if (name.toLowerCase().endsWith(".txt")) {
                // not part of the shapefile, it's the request dump
                continue;
            }
            assertTrue("Unexpected " + name, names.contains(name));
            names.remove(name);
            zis.closeEntry();
        }
        assertTrue(
                "Could not find all expected files, missing ones are: "
                        + names
                        + "\nFound in zip are: "
                        + found,
                names.isEmpty());
        zis.close();
    }

    /**
     * Asserts the contents for the file named {@code fileName} contained in the zip file given by
     * the {@code zippedIn} matched the {@code expectedContent}
     */
    private void checkFileContent(
            final String fileName, final InputStream zippedIn, final String expectedContent)
            throws IOException {

        ZipInputStream zis = new ZipInputStream(zippedIn);
        ZipEntry entry = null;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    final String name = entry.getName();
                    if (name.toLowerCase().endsWith(fileName.toLowerCase())) {
                        String unzippedFileContents = IOUtils.toString(zis, "UTF-8");
                        assertEquals(expectedContent, unzippedFileContents);
                        return;
                    }
                } finally {
                    zis.closeEntry();
                }
            }
        } finally {
            zis.close();
        }
        fail(fileName + " was not found in the provided stream");
    }

    private String getCharset(final InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;
        byte[] bytes = new byte[1024];
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().endsWith(".cst")) {
                zis.read(bytes);
            }
        }
        zis.close();

        if (bytes == null) return null;
        else return new String(bytes).trim();
    }

    private String getRequest(final InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;
        byte[] bytes = new byte[1024];
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().endsWith(".txt")) {
                zis.read(bytes);
            }
        }
        zis.close();

        if (bytes == null) return null;
        else return new String(bytes).trim();
    }

    /** */
    private String get4326_ESRI_WKTContent() {
        return "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\","
                + "SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],"
                + "PRIMEM[\"Greenwich\",0.0],"
                + "UNIT[\"Degree\",0.0174532925199433]]";
    }

    /** Test for Point ZM support on GetFeature shapefile output */
    @Test
    public void testPointZMShp() throws Exception {
        // create a feature collection of POINT ZM (4D)
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType("pointmz", "name:String,geom:Point:4326");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType);
        fb.add("point1");
        fb.add(gf.createPoint(new CoordinateXYZM(1, 2, 3, 4)));
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        features.add(fb.buildFeature("1"));
        SimpleFeatureCollection featureCollection = DataUtilities.collection(features);
        // write the zip shapefile bytes
        byte[] zipBytes = writeOut(featureCollection);
        // get shp file bytes only
        byte[] resultBytes = getShpOnlyBytes(zipBytes);
        // get expected byte array
        InputStream resource =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("org/geoserver/wfs/response/pointZm.shp");
        byte[] expectedBytes = IOUtils.toByteArray(resource);
        resource.close();
        // compare generated bytes
        assertTrue(Arrays.equals(resultBytes, expectedBytes));
    }

    /** Test for MultiPoint ZM support on GetFeature shapefile output */
    @Test
    public void testMultiPointZMShp() throws Exception {
        // create a feature collection of POINT ZM (4D)
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType("multipointmz", "name:String,geom:MultiPoint:4326");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType);
        fb.add("points1");
        fb.add(
                gf.createMultiPoint(
                        new Point[] {
                            gf.createPoint(new CoordinateXYZM(1, 2, 3, 4)),
                            gf.createPoint(new CoordinateXYZM(5, 6, 7, 8))
                        }));
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        features.add(fb.buildFeature("1"));
        SimpleFeatureCollection featureCollection = DataUtilities.collection(features);
        // write the zip shapefile bytes
        byte[] zipBytes = writeOut(featureCollection);
        // get shp file bytes only
        byte[] resultBytes = getShpOnlyBytes(zipBytes);
        // get expected byte array
        InputStream in =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("org/geoserver/wfs/response/multiPointZm.shp");
        byte[] expectedBytes = IOUtils.toByteArray(in);
        in.close();
        // compare generated bytes
        assertTrue(Arrays.equals(resultBytes, expectedBytes));
    }

    /** Test for MultiLineString ZM support on GetFeature shapefile output */
    @Test
    public void testMultiLineStringZMShp() throws Exception {
        // create a feature collection of MULTILINESTRING ZM (4D)
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType("linestringmz", "name:String,geom:MultiLineString:4326");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType);
        fb.add("line1");
        fb.add(
                gf.createMultiLineString(
                        new LineString[] {
                            gf.createLineString(
                                    new CoordinateXYZM[] {
                                        new CoordinateXYZM(1, 2, 3, 4),
                                        new CoordinateXYZM(5, 6, 7, 8)
                                    })
                        }));
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        features.add(fb.buildFeature("1"));
        SimpleFeatureCollection featureCollection = DataUtilities.collection(features);
        // write the zip shapefile bytes
        byte[] zipBytes = writeOut(featureCollection);
        // get shp file bytes only
        byte[] resultBytes = getShpOnlyBytes(zipBytes);
        // get expected byte array
        InputStream is =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("org/geoserver/wfs/response/lineStringZm.shp");
        byte[] expectedBytes = IOUtils.toByteArray(is);
        is.close();
        // compare generated bytes
        assertTrue(Arrays.equals(resultBytes, expectedBytes));
    }

    /** Test for MultiPolygon ZM support on GetFeature shapefile output */
    @Test
    public void testMultiPolygonZMShp() throws Exception {
        // create a feature collection of MULTIPOLYGON ZM (4D)
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType("polygonmz", "name:String,geom:MultiPolygon:4326");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType);
        fb.add("polygon1");
        fb.add(
                gf.createMultiPolygon(
                        new Polygon[] {
                            gf.createPolygon(
                                    new CoordinateXYZM[] {
                                        new CoordinateXYZM(0, 0, 3, 1),
                                        new CoordinateXYZM(1, 1, 7, 2),
                                        new CoordinateXYZM(1, 0, 7, 3),
                                        new CoordinateXYZM(0, 0, 3, 1)
                                    })
                        }));
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        features.add(fb.buildFeature("1"));
        SimpleFeatureCollection featureCollection = DataUtilities.collection(features);
        // write the zip shapefile bytes
        byte[] zipBytes = writeOut(featureCollection);
        // get shp file bytes only
        byte[] resultBytes = getShpOnlyBytes(zipBytes);
        // get expected byte array
        InputStream in =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("org/geoserver/wfs/response/polygonZm.shp");
        byte[] expectedBytes = IOUtils.toByteArray(in);
        // compare generated bytes
        assertTrue(Arrays.equals(resultBytes, expectedBytes));
    }

    /**
     * Extracts bytes only for the shp file from zip
     *
     * @param zip zip byte array
     * @return shp file byte array
     */
    private byte[] getShpOnlyBytes(byte[] zipBytes) throws IOException {
        byte[] resultBytes = new byte[] {};
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            final String name = entry.getName();
            if (name.toLowerCase().endsWith(".shp")) {
                // this is the shp file, get bytes
                resultBytes = IOUtils.toByteArray(zis);
            }
        }
        zis.close();
        return resultBytes;
    }
}
