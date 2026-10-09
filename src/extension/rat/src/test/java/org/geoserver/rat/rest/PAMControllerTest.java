/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rat.RasterAttributeTableTest;
import org.geoserver.rat.web.RasterAttributeTableConfigTest;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class PAMControllerTest extends CatalogRESTTestSupport {

    QName RAT = new QName(MockTestData.CITE_URI, "rat", MockTestData.CITE_PREFIX);
    QName GDAL312 = new QName(MockTestData.CITE_URI, "gdal312", MockTestData.CITE_PREFIX);

    @Before
    public void cleanupStyles() throws Exception {
        CascadeDeleteVisitor deleter = new CascadeDeleteVisitor(getCatalog());
        String[] styles = {"test123", "rat_b0_test", "gdal312_dates"};
        for (String name : styles) {
            StyleInfo style = getCatalog().getStyleByName(name);
            if (style != null) {
                style.accept(deleter);
            }
        }
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Class<RasterAttributeTableConfigTest> clazz = RasterAttributeTableConfigTest.class;
        testData.addRasterLayer(RAT, "rat.tiff", "tiff", null, clazz, getCatalog());
        copySidecar("rat", "rat.tiff.aux.xml");
        testData.addRasterLayer(GDAL312, "gdal312.tiff", "tiff", null, clazz, getCatalog());
        copySidecar("gdal312", "gdal312.tiff.aux.xml");
    }

    /** Copies a sidecar next to the coverage, the readers pick it up from there. */
    private void copySidecar(String store, String name) throws Exception {
        Resource aux = getDataDirectory().get(store, name);
        try (InputStream is = RasterAttributeTableConfigTest.class.getResourceAsStream(name);
                OutputStream os = aux.out()) {
            IOUtils.copy(is, os);
        }
    }

    @Test
    public void testGetPAMDataset() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("rest/workspaces/cite/coveragestores/rat/coverages/rat/pam.xml");
        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), Matchers.startsWith("application/xml"));
        Document doc = dom(getBinaryInputStream(response));
        print(doc);
        // just a quick check that some reasonable output is produced, the mapping is performed
        // via JAXB, correctness of the output should be tested somewhere else
        assertXpathEvaluatesTo("1", "count(/PAMDataset/PAMRasterBand)", doc);
        assertXpathEvaluatesTo("1", "count(/PAMDataset/PAMRasterBand/GDALRasterAttributeTable)", doc);
    }

    @Test
    public void testReloadPAMDataset() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("rest/workspaces/cite/coveragestores/rat/coverages/rat/pam.xml");
        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), Matchers.startsWith("application/xml"));
        Document doc = dom(getBinaryInputStream(response));
        // check the first entry has green color
        assertXpathEvaluatesTo("green", "/PAMDataset/PAMRasterBand/GDALRasterAttributeTable/Row[@index=0]/F[3]", doc);

        // copy over a RAT with a different color
        GeoServerDataDirectory dd = getDataDirectory();
        Resource aux = dd.get("rat", "rat.tiff.aux.xml");
        Class<RasterAttributeTableConfigTest> clazz = RasterAttributeTableConfigTest.class;
        try (InputStream is = clazz.getResourceAsStream("rat.tiff.aux2.xml");
                OutputStream os = aux.out()) {
            IOUtils.copy(is, os);
            os.close();
        }

        // force reload
        response = postAsServletResponse("rest/workspaces/cite/coveragestores/rat/coverages/rat/pam/reload", "");
        assertEquals(200, response.getStatus());

        // grab again and check color
        doc = getAsDOM("rest/workspaces/cite/coveragestores/rat/coverages/rat/pam.xml");
        assertXpathEvaluatesTo("olive", "/PAMDataset/PAMRasterBand/GDALRasterAttributeTable/Row[@index=0]/F[3]", doc);
    }

    @Test
    public void testCreateStyle() throws Exception {
        String createCommand =
                "rest/workspaces/cite/coveragestores/rat/coverages/rat/pam?band=0&styleName=test123&classification=test";
        MockHttpServletResponse response = postAsServletResponse(createCommand, "", null);
        // style creation response
        assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/cite/styles/test123", response.getHeader("Location"));

        // style is actually there, and associated to the layer
        StyleInfo si = getCatalog().getStyleByName("cite:test123");
        assertNotNull(si);
        assertThat(getCatalog().getLayerByName("cite:rat").getStyles(), Matchers.contains(si));
        Style style = si.getStyle();
        RasterSymbolizer rs = (RasterSymbolizer)
                style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        ColorMap cm = rs.getColorMap();
        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        RasterAttributeTableTest.assertRangesNoColor(cm);

        // try again with re-creation (201 is not suitable here)
        response = postAsServletResponse(createCommand, "", null);
        assertEquals(303, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/cite/styles/test123", response.getHeader("Location"));
    }

    @Test
    public void testCreateStyleNoName() throws Exception {
        String createCommand = "rest/workspaces/cite/coveragestores/rat/coverages/rat/pam?band=0&&classification=test";
        MockHttpServletResponse response = postAsServletResponse(createCommand, "", null);
        // style creation response
        assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/cite/styles/rat_b0_test",
                response.getHeader("Location"));
    }

    @Test
    public void testErrorMessages() throws Exception {
        testErrorMessage(
                "rest/workspaces/cite/coveragestores/rat/coverages/rat/pam?band=123&classification=test",
                "Band index 123 out of range for coverage 'cite:rat'");
        testErrorMessage(
                "rest/workspaces/cite/coveragestores/rat/coverages/rat/pam?band=0&classification=foobar",
                "Raster attribute table found, but has no classification field named: 'foobar'");
    }

    @Test
    public void testNotFound() throws Exception {
        testNotFound(
                "rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble/pam?band=0&classification=foobar",
                "No PAMDataset found for coverage: 'wcs:BlueMarble'");
        testNotFound(
                "rest/workspaces/wcs/coveragestores/BlueMarble/coverages/foobar/pam?band=0&classification=foobar",
                "No such coverage: 'wcs:foobar'");
    }

    private void testNotFound(String command, String message) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(command, "", null);
        assertEquals(404, response.getStatus());
        assertEquals(message, response.getContentAsString());
    }

    private void testErrorMessage(String command, String message) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(command, "", null);
        assertEquals(400, response.getStatus());
        assertEquals(message, response.getErrorMessage());
    }

    @Test
    public void testGetGdal312PAMDataset() throws Exception {
        Document doc = getAsDOM("rest/workspaces/cite/coveragestores/gdal312/coverages/gdal312/pam.xml");
        String rat = "/PAMDataset/PAMRasterBand/GDALRasterAttributeTable";
        assertXpathEvaluatesTo("thematic", rat + "/@tableType", doc);
        assertXpathEvaluatesTo("8", "count(" + rat + "/FieldDefn)", doc);
        // the three field types GDAL 3.12 added, kept across the round trip
        assertXpathEvaluatesTo("3", rat + "/FieldDefn[@index=2]/Type", doc);
        assertXpathEvaluatesTo("4", rat + "/FieldDefn[@index=3]/Type", doc);
        assertXpathEvaluatesTo("5", rat + "/FieldDefn[@index=7]/Type", doc);
        assertXpathEvaluatesTo("4", "count(" + rat + "/Row)", doc);
        assertXpathEvaluatesTo("true", rat + "/Row[@index=0]/F[3]", doc);
        assertXpathEvaluatesTo("2024-03-01T00:00:00.000+00:00", rat + "/Row[@index=0]/F[4]", doc);
        assertXpathEvaluatesTo("POLYGON ((0 0,1 0,1 1,0 1,0 0))", rat + "/Row[@index=0]/F[8]", doc);
        // the row where only the survey id is set
        assertXpathEvaluatesTo("", rat + "/Row[@index=3]/F[4]", doc);
    }

    @Test
    public void testCreateGdal312DateStyle() throws Exception {
        String createCommand = "rest/workspaces/cite/coveragestores/gdal312/coverages/gdal312/pam"
                + "?band=0&styleName=gdal312_dates&classification=surveyDateRange.dateStart";
        MockHttpServletResponse response = postAsServletResponse(createCommand, "", null);
        assertEquals(201, response.getStatus());

        StyleInfo si = getCatalog().getStyleByName("cite:gdal312_dates");
        assertNotNull(si);
        RasterSymbolizer rs = (RasterSymbolizer) si.getStyle()
                .featureTypeStyles()
                .get(0)
                .rules()
                .get(0)
                .symbolizers()
                .get(0);
        ColorMap cm = rs.getColorMap();
        assertEquals(ColorMap.TYPE_VALUES, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(4, entries.length);
        RasterAttributeTableTest.assertColorMapEntry(entries[0], "2024-03-01T00:00:00.000+00:00", 54602, "#57208E", 1);
        RasterAttributeTableTest.assertColorMapEntry(entries[3], "", 61004, "#8DCD4E", 1);
    }

    @Test
    public void testGeometryNotAClassification() throws Exception {
        testErrorMessage(
                "rest/workspaces/cite/coveragestores/gdal312/coverages/gdal312/pam?band=0&classification=footprint",
                "Raster attribute table found, but has no classification field named: 'footprint'");
    }
}
