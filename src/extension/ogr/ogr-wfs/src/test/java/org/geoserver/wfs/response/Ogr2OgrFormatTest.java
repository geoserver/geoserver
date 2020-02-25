/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.data.DataStore;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.Version;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Ogr2OgrFormatTest {

    DataStore dataStore;

    Ogr2OgrOutputFormat ogr;

    Operation op;

    FeatureCollectionType fct;

    GetFeatureType gft;

    @Before
    public void setUp() throws Exception {
        // check if we can run the tests
        Assume.assumeTrue(Ogr2OgrTestUtil.isOgrAvailable());

        // the data source we'll use for the tests
        dataStore = new PropertyDataStore(new File("./src/test/java/org/geoserver/wfs/response"));

        // the output format (and let's add a few output formats to play with
        ogr = new Ogr2OgrOutputFormat(new GeoServerImpl(), new OGRWrapperFactory());
        ogr.addFormat(
                new OgrFormat("KML", "OGR-KML", ".kml", true, "application/vnd.google-earth.kml"));
        ogr.addFormat(
                new OgrFormat(
                        "KML", "OGR-KML-ZIP", ".kml", false, "application/vnd.google-earth.kml"));
        ogr.addFormat(new OgrFormat("CSV", "OGR-CSV", ".csv", true, "text/csv"));
        ogr.addFormat(new OgrFormat("SHP", "OGR-SHP", ".shp", false, null));
        ogr.addFormat(
                new OgrFormat(
                        "MapInfo File", "OGR-MIF", ".mif", false, null, "-dsco", "FORMAT=MIF"));

        ogr.setExecutable(Ogr2OgrTestUtil.getOgr2Ogr());
        ogr.setEnvironment(Collections.singletonMap("GDAL_DATA", Ogr2OgrTestUtil.getGdalData()));

        // the EMF objects used to talk with the output format
        gft = WfsFactory.eINSTANCE.createGetFeatureType();
        fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        op =
                new Operation(
                        "GetFeature",
                        new Service("WFS", null, new Version("1.0.0"), Arrays.asList("GetFeature")),
                        null,
                        new Object[] {gft});
    }

    @Test
    public void testCanHandle() {
        gft.setOutputFormat("OGR-KML");
        assertTrue(ogr.canHandle(op));
        gft.setOutputFormat("OGR-CSV");
        assertTrue(ogr.canHandle(op));
        gft.setOutputFormat("RANDOM_FORMAT");
        assertTrue(ogr.canHandle(op));
    }

    @Test
    public void testContentTypeZip() {
        gft.setOutputFormat("OGR-SHP");
        assertEquals("application/zip", ogr.getMimeType(null, op));
    }

    @Test
    public void testContentTypeKml() {
        gft.setOutputFormat("OGR-KML");
        assertEquals("application/vnd.google-earth.kml", ogr.getMimeType(null, op));
    }

    @Test
    public void testSimpleKML() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("Buildings").getFeatures();
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-KML");
        ogr.write(fct, bos, op);

        // parse the kml to check it's really xml...
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testZippedKML() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("Buildings").getFeatures();
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-KML-ZIP");
        ogr.write(fct, bos, op);

        // unzip the result
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Document dom = null;
        ZipEntry entry = zis.getNextEntry();
        assertEquals("Buildings.kml", entry.getName());
        dom = dom(zis);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testEmptyKML() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("Buildings").getFeatures(Filter.EXCLUDE);
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-KML");
        ogr.write(fct, bos, op);

        // parse the kml to check it's really xml...
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(0, dom.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testSimpleCSV() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("Buildings").getFeatures();
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-CSV");
        ogr.write(fct, bos, op);

        // read back
        String csv = read(new ByteArrayInputStream(bos.toByteArray()));

        // couple simple checks
        String[] lines = csv.split("\n");
        // headers and the two lines
        assertEquals(3, lines.length);
        assertTrue(csv.contains("123 Main Street"));
    }

    @Test
    public void testSimpleMIF() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("Buildings").getFeatures();
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-MIF");
        ogr.write(fct, bos, op);

        // read back
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bos.toByteArray()));

        // we should get two files at least, a .mif and a .mid
        Set<String> fileNames = new HashSet<String>();
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            fileNames.add(entry.getName());
        }
        assertTrue(fileNames.contains("Buildings.mif"));
        assertTrue(fileNames.contains("Buildings.mid"));
    }

    @Test
    public void testGeometrylessCSV() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("Geometryless").getFeatures();
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-CSV");
        ogr.write(fct, bos, op);

        // read back
        String csv = read(new ByteArrayInputStream(bos.toByteArray()));

        // couple simple checks
        String[] lines = csv.split("\n");
        // headers and the feature lines
        assertEquals(4, lines.length);
        // let's see if one of the expected lines is there
        assertTrue(csv.contains("Alessia"));
    }

    @Test
    public void testAllTypesKML() throws Exception {
        // prepare input
        FeatureCollection fc = dataStore.getFeatureSource("AllTypes").getFeatures();
        fct.getFeature().add(fc);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gft.setOutputFormat("OGR-KML");
        ogr.write(fct, bos, op);

        // read back
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(6, dom.getElementsByTagName("Placemark").getLength());
    }

    /** Utility method to print out a dom. */
    protected void print(Document dom) throws Exception {
        TransformerFactory txFactory = TransformerFactory.newInstance();
        try {
            txFactory.setAttribute(
                    "{http://xml.apache.org/xalan}indent-number", Integer.valueOf(2));
        } catch (Exception e) {
            // some
        }

        Transformer tx = txFactory.newTransformer();
        tx.setOutputProperty(OutputKeys.METHOD, "xml");
        tx.setOutputProperty(OutputKeys.INDENT, "yes");

        tx.transform(
                new DOMSource(dom), new StreamResult(new OutputStreamWriter(System.out, "utf-8")));
    }

    protected String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Parses a stream into a dom.
     *
     * @param skipDTD If true, will skip loading and validating against the associated DTD
     */
    protected Document dom(InputStream input)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(input);
    }
}
