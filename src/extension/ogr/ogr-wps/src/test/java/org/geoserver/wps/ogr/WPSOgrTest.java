/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ogr;

import static junit.framework.Assert.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.response.Ogr2OgrConfigurator;
import org.geoserver.wfs.response.Ogr2OgrTestUtil;
import org.geoserver.wfs.response.OgrConfiguration;
import org.geoserver.wfs.response.OgrFormat;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.thoughtworks.xstream.XStream;

public class WPSOgrTest extends WPSTestSupport {

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(Ogr2OgrTestUtil.isOgrAvailable());
    }

    private File loadConfiguration() throws Exception {
        String ogrConfigruationName = "ogr2ogr.xml";
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        XStream xstream = buildXStream();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(ogrConfigruationName).getFile());
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
            Ogr2OgrConfigurator configurator = applicationContext
                    .getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            List<String> formatNames = new ArrayList<>();
            for (OgrFormat f : configurator.of.getFormats()) {
                formatNames.add(f.formatName);
            }
            assertTrue(formatNames.contains("OGR-TAB"));
            assertTrue(formatNames.contains("OGR-MIF"));
            assertTrue(formatNames.contains("OGR-CSV"));
            assertTrue(formatNames.contains("OGR-KML"));
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
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
        Document d = getAsDOM(root()
                + "service=wps&request=describeprocess&identifier=gs:BufferFeatureCollection");
        String base = "/wps:ProcessDescriptions/ProcessDescription/ProcessOutputs";
        for (OgrFormat f : OgrConfiguration.DEFAULT.formats) {
            if (f.mimeType != null) {
                assertXpathExists(base + "/Output[1]/ComplexOutput/Supported/Format[MimeType='"
                        + f.mimeType + "; subtype=" + f.formatName + "']", d);
            }
        }
    }

    @Test
    public void testOGRKMLOutputExecuteRaw() throws Exception {
        File configuration = null;
        try {
            configuration = loadConfiguration();
            Ogr2OgrConfigurator configurator = applicationContext
                    .getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            MockHttpServletResponse r = postAsServletResponse("wps",
                    getWpsRawXML("application/vnd.google-earth.kml; subtype=OGR-KML"));
            assertEquals("application/vnd.google-earth.kml; subtype=OGR-KML", r.getContentType());
            assertTrue(r.getOutputStreamContent().length() > 0);

        } catch (IOException e) {
            System.err.println(e.getStackTrace());
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
            Ogr2OgrConfigurator configurator = applicationContext
                    .getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            Document d = postAsDOM("wps",
                    getWpsDocumentXML("application/vnd.google-earth.kml; subtype=OGR-KML"));
            Map<String, String> m = new HashMap<String, String>();
            m.put("kml", "http://www.opengis.net/kml/2.2");
            org.custommonkey.xmlunit.NamespaceContext ctx = new SimpleNamespaceContext(m);
            XpathEngine engine = XMLUnit.newXpathEngine();
            engine.setNamespaceContext(ctx);
            assertEquals(1, engine.getMatchingNodes("//kml:kml/kml:Document/kml:Schema", d)
                    .getLength());
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
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
            Ogr2OgrConfigurator configurator = applicationContext
                    .getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            MockHttpServletResponse r = postAsServletResponse("wps",
                    getWpsRawXML("text/csv; subtype=OGR-CSV"));
            assertEquals("text/csv; subtype=OGR-CSV", r.getContentType());
            assertTrue(r.getOutputStreamContent().length() > 0);
            assertTrue(r.getOutputStreamContent().contains("WKT,gml_id,STATE_NAME"));
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
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
            Ogr2OgrConfigurator configurator = applicationContext
                    .getBean(Ogr2OgrConfigurator.class);
            configurator.loadConfiguration();
            MockHttpServletResponse r = postAsServletResponse("wps",
                    getWpsRawXML("application/zip; subtype=OGR-TAB"));
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
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        } finally {
            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    private String getWpsRawXML(String ouputMime) throws Exception {
        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
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
                + "</wps:RawDataOutput>" + "</wps:ResponseForm>" + "</wps:Execute>";
        return xml;
    }

    private String getWpsDocumentXML(String ouputMime) throws Exception {
        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
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
                + "</wps:ResponseDocument>" + "</wps:ResponseForm>" + "</wps:Execute>";
        return xml;
    }

    private static XStream buildXStream() {
        XStream xstream = new XStream();
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);
        xstream.addImplicitCollection(OgrFormat.class, "options", "option", String.class);
        return xstream;
    }


}
