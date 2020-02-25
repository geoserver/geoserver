/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ogr;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.ogr.core.Format;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.XmlTestUtil;
import org.geoserver.wfs.response.Ogr2OgrConfigurator;
import org.geoserver.wfs.response.Ogr2OgrTestUtil;
import org.geoserver.wfs.response.OgrConfiguration;
import org.geoserver.wfs.response.OgrFormat;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.util.URLs;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class WPSOgrTest extends WPSTestSupport {

    private XmlTestUtil xml;

    @Before
    public void setUp() throws Exception {
        xml = new XmlTestUtil();
        xml.addNamespace("kml", "http://www.opengis.net/kml/2.2");
        // xml.setShowXML(System.out); // Uncomment to print XML to stdout on failure

        Assume.assumeTrue(Ogr2OgrTestUtil.isOgrAvailable());
    }

    private File loadConfiguration() throws Exception {
        String ogrConfigruationName = "ogr2ogr.xml";
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        XStream xstream = buildXStream();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = URLs.urlToFile(classLoader.getResource(ogrConfigruationName));
        OgrConfiguration ogrConfiguration = (OgrConfiguration) xstream.fromXML(file);
        ogrConfiguration.ogr2ogrLocation = Ogr2OgrTestUtil.getOgr2Ogr();
        ogrConfiguration.gdalData = Ogr2OgrTestUtil.getGdalData();

        File configuration = loader.createFile(ogrConfigruationName);
        xstream.toXML(ogrConfiguration, new FileOutputStream(configuration));

        Ogr2OgrConfigurator configurator = applicationContext.getBean(Ogr2OgrConfigurator.class);
        configurator.loadConfiguration();

        return configuration;
    }

    @Test
    public void testConfigurationLoad() throws Exception {
        File configuration = null;
        try {
            configuration = loadConfiguration();
            Ogr2OgrConfigurator configurator =
                    applicationContext.getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            List<String> formatNames = new ArrayList<>();
            for (Format f : configurator.of.getFormats()) {
                formatNames.add(f.getGeoserverFormat());
            }
            assertTrue(formatNames.contains("OGR-TAB"));
            assertTrue(formatNames.contains("OGR-MIF"));
            assertTrue(formatNames.contains("OGR-CSV"));
            assertTrue(formatNames.contains("OGR-KML"));
        } finally {
            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testDescribeProcess() throws Exception {
        OgrConfiguration.DEFAULT.ogr2ogrLocation = Ogr2OgrTestUtil.getOgr2Ogr();
        OgrConfiguration.DEFAULT.gdalData = Ogr2OgrTestUtil.getGdalData();
        Ogr2OgrConfigurator configurator = applicationContext.getBean(Ogr2OgrConfigurator.class);
        configurator.loadConfiguration();
        Document d =
                getAsDOM(
                        root()
                                + "service=wps&request=describeprocess&identifier=gs:BufferFeatureCollection");
        String base = "/wps:ProcessDescriptions/ProcessDescription/ProcessOutputs";
        for (Format f : OgrConfiguration.DEFAULT.getFormats()) {
            if (f.getMimeType() != null) {
                assertXpathExists(
                        base
                                + "/Output[1]/ComplexOutput/Supported/Format[MimeType='"
                                + f.getMimeType()
                                + "; subtype="
                                + f.getGeoserverFormat()
                                + "']",
                        d);
            }
        }
    }

    @Test
    public void testOGRKMLOutputExecuteRaw() throws Exception {
        File configuration = null;
        try {
            configuration = loadConfiguration();
            Ogr2OgrConfigurator configurator =
                    applicationContext.getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            MockHttpServletResponse r =
                    postAsServletResponse(
                            "wps",
                            getWpsRawXML("application/vnd.google-earth.kml; subtype=OGR-KML"));
            assertEquals("application/vnd.google-earth.kml; subtype=OGR-KML", r.getContentType());
            assertTrue(r.getContentAsString().length() > 0);
        } finally {
            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testOGRKMLOutputExecuteDocument() throws Exception {
        File configuration = null;
        try {
            configuration = loadConfiguration();
            Ogr2OgrConfigurator configurator =
                    applicationContext.getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            Document d =
                    postAsDOM(
                            "wps",
                            getWpsDocumentXML("application/vnd.google-earth.kml; subtype=OGR-KML"));
            assertThat(
                    d,
                    xml.hasOneNode(
                            "//kml:kml/kml:Document/kml:Schema | //kml:kml/kml:Document/kml:Folder/kml:Schema"));
        } finally {
            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testOGRCSVOutputExecuteDocument() throws Exception {
        File configuration = null;
        try {
            configuration = loadConfiguration();
            Ogr2OgrConfigurator configurator =
                    applicationContext.getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            MockHttpServletResponse r =
                    postAsServletResponse("wps", getWpsRawXML("text/csv; subtype=OGR-CSV"));
            assertEquals("text/csv; subtype=OGR-CSV", r.getContentType());
            assertTrue(r.getContentAsString().length() > 0);
            assertTrue(
                    r.getContentAsString().contains("WKT,gml_id,STATE_NAME")
                            || r.getContentAsString().contains("geometry,gml_id,STATE_NAME"));
        } finally {
            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testOGRBinaryOutputExecuteDocument() throws Exception {
        File configuration = null;
        try {
            configuration = loadConfiguration();
            Ogr2OgrConfigurator configurator =
                    applicationContext.getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            MockHttpServletResponse r =
                    postAsServletResponse("wps", getWpsRawXML("application/zip; subtype=OGR-TAB"));
            assertEquals("application/zip; subtype=OGR-TAB", r.getContentType());
            ByteArrayInputStream bis = getBinaryInputStream(r);
            ZipInputStream zis = new ZipInputStream(bis);
            ZipEntry entry = null;
            boolean found = false;
            while ((entry = zis.getNextEntry()) != null) {
                final String name = entry.getName();
                zis.closeEntry();
                if (name.equals("feature.tab")) {
                    found = true;
                    break;
                }
            }
            zis.close();
            assertTrue(found);
        } finally {
            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    private String getWpsRawXML(String ouputMime) throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:BufferFeatureCollection</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/json\"><![CDATA["
                        + readFileIntoString("states-FeatureCollection.json")
                        + "]]></wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>distance</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>10</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\""
                        + ouputMime
                        + "\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        return xml;
    }

    private String getWpsDocumentXML(String ouputMime) throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:BufferFeatureCollection</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/json\"><![CDATA["
                        + readFileIntoString("states-FeatureCollection.json")
                        + "]]></wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>distance</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>10</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument>"
                        + "<wps:Output mimeType=\""
                        + ouputMime
                        + "\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        return xml;
    }

    private static XStream buildXStream() {
        XStream xstream = new SecureXStream();
        xstream.allowTypeHierarchy(OgrConfiguration.class);
        xstream.allowTypeHierarchy(OgrFormat.class);
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);
        xstream.addImplicitCollection(OgrFormat.class, "options", "option", String.class);
        return xstream;
    }
}
