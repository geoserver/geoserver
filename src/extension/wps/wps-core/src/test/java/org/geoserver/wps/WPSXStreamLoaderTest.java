/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.MultiplicityValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geotools.feature.NameImpl;
import org.geotools.process.geometry.GeometryProcessFactory;
import org.geotools.process.raster.RasterProcessFactory;
import org.geotools.util.NumberRange;
import org.geotools.util.URLs;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class WPSXStreamLoaderTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed for this test
    }

    @Test
    public void testCreateFromScratch() throws Exception {
        WPSXStreamLoader loader = GeoServerExtensions.bean(WPSXStreamLoader.class);
        WPSInfo wps = loader.createServiceFromScratch(null);
        assertNotNull(wps);
        assertEquals("WPS", wps.getName());
    }

    @Test
    public void testInit() throws Exception {
        WPSXStreamLoader loader = GeoServerExtensions.bean(WPSXStreamLoader.class);
        WPSInfo wps = new WPSInfoImpl();
        loader.initializeService(wps);
        assertEquals("WPS", wps.getName());
    }

    @Test
    public void testBackFormatXmlComatibility() throws Exception {
        GeoServer gs = createMock(GeoServer.class);
        URL url = Thread.currentThread().getContextClassLoader().getResource("org/geoserver/wps/");
        File file = URLs.urlToFile(url);
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader(file));
        WPSInfo wps = loader.load(gs);
        boolean found1 = false;
        boolean found2 = false;
        for (ProcessGroupInfo pg : wps.getProcessGroups()) {
            if (pg.getFactoryClass()
                    .getName()
                    .equals("org.geoserver.wps.DeprecatedProcessFactory")) {
                assertFalse(pg.isEnabled());
                found1 = true;
            }
            if (pg.getFilteredProcesses() != null) {
                for (Object opi : pg.getFilteredProcesses()) {
                    assertTrue(opi instanceof ProcessInfo);
                }
                if (pg.getFactoryClass()
                        .getName()
                        .equals("org.geoserver.wps.jts.SpringBeanProcessFactory")) {
                    assertTrue(pg.isEnabled());
                    assertEquals(
                            pg.getFilteredProcesses().get(0).getName().toString(),
                            "gs:GeorectifyCoverage");
                    assertEquals(
                            pg.getFilteredProcesses().get(1).getName().toString(),
                            "gs:GetFullCoverage");
                    assertEquals(
                            pg.getFilteredProcesses().get(2).getName().toString(), "gs:Import");
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
        contour.getValidators()
                .put(
                        "levels",
                        new NumberRangeValidator(
                                new NumberRange<Double>(Double.class, -8000d, 8000d)));
        contour.getValidators().put("levels", new MultiplicityValidator(3));
        rasGroup.getFilteredProcesses().add(contour);

        File root = new File("./target");
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader(root));
        WPSInfo wps = loader.createServiceFromScratch(null);
        wps.getProcessGroups().add(geoGroup);
        wps.getProcessGroups().add(rasGroup);

        loader.save(wps, getGeoServer(), Files.asResource(root));

        // check the xml
        String xml = FileUtils.readFileToString(new File(root, "wps.xml"), "UTF-8");
        Document dom = dom(xml);

        // geometry factory
        String baseGeomPath =
                "/wps/processGroups/processGroup[factoryClass='"
                        + GeometryProcessFactory.class.getName()
                        + "']/filteredProcesses/accessInfo";
        XMLAssert.assertXpathExists(baseGeomPath, dom);
        String geoAreaBase = baseGeomPath + "[name='geo:Area']/validators/entry[@key='geom']";
        XMLAssert.assertXpathExists(geoAreaBase, dom);
        XMLAssert.assertXpathEvaluatesTo("10", geoAreaBase + "/maxSizeValidator/maxSizeMB", dom);

        // raster factory
        String baseRasPath =
                "/wps/processGroups/processGroup[factoryClass='"
                        + RasterProcessFactory.class.getName()
                        + "']/filteredProcesses/accessInfo";
        XMLAssert.assertXpathExists(baseRasPath, dom);
        String rasContourBase = baseRasPath + "[name='ras:Contour']";
        XMLAssert.assertXpathExists(rasContourBase, dom);
        XMLAssert.assertXpathEvaluatesTo(
                "3",
                rasContourBase
                        + "/validators/entry[@key='levels']/maxMultiplicityValidator/maxInstances",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "-8000.0",
                rasContourBase + "/validators/entry[@key='levels']/rangeValidator/range/minValue",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "8000.0",
                rasContourBase + "/validators/entry[@key='levels']/rangeValidator/range/maxValue",
                dom);

        // check unmarshalling
        WPSInfo wps2 = loader.load(getGeoServer(), Files.asResource(root));
        assertEquals(wps, wps2);
    }

    Document dom(String xml) throws ParserConfigurationException, SAXException, IOException {
        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

    @Test
    public void testLoadFromXML() throws Exception {
        WPSInfo wpsInfo = loadFromXml("wps-test.xml");
        assertNotNull(wpsInfo);
    }

    @Test
    public void testLoadFromXMLWithUnknownProcessGroups() throws Exception {
        WPSInfo wpsInfo = loadFromXml("wps-test-error.xml");
        assertNotNull(wpsInfo);

        // This comes before the problematic definition
        boolean foundGeometryFactory = false;
        // This is expected to throw an error, but shouldn't be included in the list regardless
        boolean foundMissingProcessFactory = false;
        // This comes after the problematic definition
        boolean foundRasterFactory = false;

        for (ProcessGroupInfo pg : wpsInfo.getProcessGroups()) {
            if (pg.getFactoryClass()
                    .getName()
                    .equals("org.geotools.process.geometry.GeometryProcessFactory")) {
                assertTrue(pg.isEnabled());
                foundGeometryFactory = true;
            }
            if (pg.getFactoryClass().getName().equals("org.geoserver.wps.MissingProcessFactory")) {
                foundMissingProcessFactory = true;
            }
            if (pg.getFactoryClass()
                    .getName()
                    .equals("org.geotools.process.raster.RasterProcessFactory")) {
                assertTrue(pg.isEnabled());
                foundRasterFactory = true;
            }
        }
        assertTrue(foundGeometryFactory);
        assertTrue(foundRasterFactory);
        assertFalse(foundMissingProcessFactory);
    }

    @Test
    public void testLoadFromXMLWithWorkSpace() throws Exception {
        // creating a workspace with same ID was the one in the wps-test-workspace.xml file
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("wps-load-test-workspace-id");
        workspace.setName("wps-load-test-workspace-name");
        workspace.setDefault(false);
        catalog.add(workspace);
        // we parse the wps info that contains the workspace
        WPSInfo wpsInfo = loadFromXml("wps-test-workspace.xml");
        assertNotNull(wpsInfo);
        assertNotNull(wpsInfo.getWorkspace());
        assertTrue(wpsInfo.getWorkspace().getId().equals("wps-load-test-workspace-id"));
        // if the workspace was correctly retrieved from the catalog it should have the name
        // property available
        try {
            assertTrue(wpsInfo.getWorkspace().getName().equals("wps-load-test-workspace-name"));
        } catch (NullPointerException exception) {
            // this is a proxy that only know the workspace id
            fail("NULL proxy");
        }
    }

    /** Helper method tha reads a WPS configuration from a XML file and return that info. */
    private WPSInfo loadFromXml(String resource) throws Exception {
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        WPSXStreamLoader loader = GeoServerExtensions.bean(WPSXStreamLoader.class);
        loader.initXStreamPersister(xp, getGeoServer());
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            return xp.load(is, WPSInfo.class);
        }
    }
}
