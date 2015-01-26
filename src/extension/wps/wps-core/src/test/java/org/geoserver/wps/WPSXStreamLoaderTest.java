package org.geoserver.wps;

import static org.easymock.classextension.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.MultiplicityValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geotools.feature.NameImpl;
import org.geotools.process.geometry.GeometryProcessFactory;
import org.geotools.process.raster.RasterProcessFactory;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class WPSXStreamLoaderTest {

    @Test
    public void testCreateFromScratch() throws Exception {
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader());
        WPSInfo wps = loader.createServiceFromScratch(null);
        assertNotNull(wps);
        assertEquals("WPS", wps.getName());
    }

    @Test
    public void testInit() throws Exception {
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader());
        WPSInfo wps = new WPSInfoImpl();
        loader.initializeService(wps);
        assertEquals("WPS", wps.getName());
    }

    @Test
    public void testBackFormatXmlComatibility() throws Exception {
        GeoServer gs = createMock(GeoServer.class);
        URL url = Thread.currentThread().getContextClassLoader().getResource("org/geoserver/wps/");
        File file;
        try {
            file = new File(url.toURI());
        }
        catch (URISyntaxException e) {
            file = new File(url.getPath());
        }

        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader(file));
        WPSInfo wps = loader.load(gs);
        boolean found1 = false;
        boolean found2 = false;
        for (ProcessGroupInfo pg : wps.getProcessGroups()) {
            if (pg.getFactoryClass().getName().equals("org.geoserver.wps.DeprecatedProcessFactory")) {
                assertFalse(pg.isEnabled());
                found1 = true;
            }
            if (pg.getFilteredProcesses() != null) {
                for (Object opi : pg.getFilteredProcesses()) {
                    assertTrue(opi instanceof ProcessInfo);
                }
                if (pg.getFactoryClass().getName()
                        .equals("org.geoserver.wps.jts.SpringBeanProcessFactory")) {
                    assertTrue(pg.isEnabled());
                    assertEquals(pg.getFilteredProcesses().get(0).getName().toString(),
                            "gs:GeorectifyCoverage");
                    assertEquals(pg.getFilteredProcesses().get(1).getName().toString(),
                            "gs:GetFullCoverage");
                    assertEquals(pg.getFilteredProcesses().get(2).getName().toString(), "gs:Import");
                    found2 = true;
                }
            }
        }
        assertTrue(found1);
        assertTrue(found2);
    }

    @Test
    public void testPersistValidators() throws Exception {
        ProcessGroupInfo geoGroup = new ProcessGroupInfoImpl();
        geoGroup.setFactoryClass(GeometryProcessFactory.class);
        ProcessInfo area = new ProcessInfoImpl();
        area.setEnabled(true);
        area.setName(new NameImpl("geo", "Area"));
        area.getValidators().put("geom", new MaxSizeValidator(10));
        geoGroup.getFilteredProcesses().add(area);

        ProcessGroupInfo rasGroup = new ProcessGroupInfoImpl();
        rasGroup.setFactoryClass(RasterProcessFactory.class);
        ProcessInfo contour = new ProcessInfoImpl();
        contour.setEnabled(true);
        contour.setName(new NameImpl("ras", "Contour"));
        contour.getValidators().put("levels",
                new NumberRangeValidator(new NumberRange<Double>(Double.class, -8000d, 8000d)));
        contour.getValidators().put("levels", new MultiplicityValidator(3));
        rasGroup.getFilteredProcesses().add(contour);

        File root = new File("./target");
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader(root));
        WPSInfo wps = loader.createServiceFromScratch(null);
        wps.getProcessGroups().add(geoGroup);
        wps.getProcessGroups().add(rasGroup);

        loader.save(wps, null, root);

        // check the xml
        String xml = FileUtils.readFileToString(new File(root, "wps.xml"));
        System.out.println(xml);
        Document dom = dom(xml);

        // geometry factory
        String baseGeomPath = "/wps/processGroups/processGroup[factoryClass='"
                + GeometryProcessFactory.class.getName() + "']/filteredProcesses/accessInfo";
        XMLAssert.assertXpathExists(baseGeomPath, dom);
        String geoAreaBase = baseGeomPath + "[name='geo:Area']/validators/entry[@key='geom']";
        XMLAssert.assertXpathExists(geoAreaBase, dom);
        XMLAssert.assertXpathEvaluatesTo("10", geoAreaBase + "/maxSizeValidator/maxSizeMB", dom);

        // raster factory
        String baseRasPath = "/wps/processGroups/processGroup[factoryClass='"
                + RasterProcessFactory.class.getName() + "']/filteredProcesses/accessInfo";
        XMLAssert.assertXpathExists(baseRasPath, dom);
        String rasContourBase = baseRasPath + "[name='ras:Contour']";
        XMLAssert.assertXpathExists(rasContourBase, dom);
        XMLAssert.assertXpathEvaluatesTo("3", rasContourBase
                + "/validators/entry[@key='levels']/maxMultiplicityValidator/maxInstances", dom);
        XMLAssert.assertXpathEvaluatesTo("-8000.0", rasContourBase
                + "/validators/entry[@key='levels']/rangeValidator/range/minValue", dom);
        XMLAssert.assertXpathEvaluatesTo("8000.0", rasContourBase
                + "/validators/entry[@key='levels']/rangeValidator/range/maxValue", dom);

        // check unmarshalling
        WPSInfo wps2 = loader.load(null, root);
        assertEquals(wps, wps2);
    }

    Document dom(String xml) throws ParserConfigurationException, SAXException, IOException {
        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

}
