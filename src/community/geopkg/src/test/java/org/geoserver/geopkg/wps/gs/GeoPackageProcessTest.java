/* (c) 2014-2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geopkg.wps.GeoPkgAnnotationReference;
import org.geoserver.geopkg.wps.GeoPkgSemanticAnnotation;
import org.geoserver.geopkg.wps.GeoPkgStyle;
import org.geoserver.geopkg.wps.GeoPkgStyleSheet;
import org.geoserver.geopkg.wps.GeoPkgSymbol;
import org.geoserver.geopkg.wps.GeoPkgSymbolImage;
import org.geoserver.geopkg.wps.PortrayalExtension;
import org.geoserver.geopkg.wps.SemanticAnnotationsExtension;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.geopkg.TileReader;
import org.geotools.util.URLs;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GeoPackageProcessTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle("burg", "burg.sld", this.getClass(), catalog);
        try (InputStream is = this.getClass().getResourceAsStream("burg02.svg")) {
            testData.copyTo(is, "styles/burg02.svg");
        }

        Catalog catalog = getCatalog();
        StyleInfo burgStyle = catalog.getStyleByName("burg");
        LayerInfo lakesLayer = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakesLayer.getStyles().add(burgStyle);
        catalog.save(lakesLayer);
    }

    @Before
    public void disableXXEDetection() {
        // running tests in the IDE having also GeoTools loaded otherwise fails
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        global.setXmlExternalEntitiesEnabled(true);
        gs.save(global);
    }

    @Test
    public void testGeoPackageProcess() throws Exception {
        String urlPath = string(post("wps", getXml())).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "test.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));
        assertNotNull(file);
        assertEquals("test.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<FeatureEntry> features = gpkg.features();
        assertEquals(2, features.size());

        FeatureEntry fe = features.get(0);
        assertEquals("Fifteen", fe.getTableName());
        assertEquals("fifteen description", fe.getDescription());
        assertEquals("f15", fe.getIdentifier());
        assertEquals(32615, fe.getSrid().intValue());
        assertEquals(500000, fe.getBounds().getMinX(), 0.0001);
        assertEquals(500000, fe.getBounds().getMinY(), 0.0001);
        assertEquals(500100, fe.getBounds().getMaxX(), 0.0001);
        assertEquals(500100, fe.getBounds().getMaxY(), 0.0001);

        assertFalse(gpkg.hasSpatialIndex(fe));
        assertTrue(gpkg.hasSpatialIndex(features.get(1)));

        SimpleFeatureReader fr = gpkg.reader(fe, null, null);
        assertEquals(1, fr.getFeatureType().getAttributeCount());
        assertEquals(
                "pointProperty",
                fr.getFeatureType().getAttributeDescriptors().get(0).getLocalName());
        assertTrue(fr.hasNext());
        fr.next();
        fr.close();

        // style for fifteen was not exported
        PortrayalExtension portrayal = gpkg.getExtension(PortrayalExtension.class);
        SemanticAnnotationsExtension annotations =
                gpkg.getExtension(SemanticAnnotationsExtension.class);
        assertEquals(
                3, portrayal.getStyles().size()); // the default style is associated to all layers
        GeoPkgStyle fifteenStyle = portrayal.getStyle("Fifteen");
        assertNull(fifteenStyle);

        fe = features.get(1);
        assertEquals("Lakes", fe.getTableName());
        assertEquals("lakes description", fe.getDescription());
        assertEquals("lakes1", fe.getIdentifier());

        fr = gpkg.reader(fe, null, null);
        assertTrue(fr.hasNext());
        fr.next();
        fr.close();

        // check the style
        GeoPkgStyle lakesStyle = portrayal.getStyle("Lakes");
        assertNotNull(lakesStyle);
        assertEquals("http://localhost:8080/geoserver/styles/Lakes.sld", lakesStyle.getUri());
        List<GeoPkgStyleSheet> lakesStylesheets = portrayal.getStylesheets(lakesStyle);
        assertEquals(1, lakesStylesheets.size());
        GeoPkgStyleSheet lakesStylesheet = lakesStylesheets.get(0);
        assertEquals("application/vnd.ogc.sld+xml", lakesStylesheet.getFormat());
        String expectedFill =
                "                        <CssParameter name=\"fill\">\n"
                        + "                            <ogc:Literal>#4040C0</ogc:Literal>\n"
                        + "                        </CssParameter>\n";
        assertThat(lakesStylesheet.getStylesheet(), CoreMatchers.containsString(expectedFill));

        // the symbology
        GeoPkgSymbol burg = portrayal.getSymbol("burg02.svg");
        assertNotNull(burg);
        assertEquals("symbols://burg/0", burg.getUri());
        List<GeoPkgSymbolImage> images = portrayal.getImages(burg);
        assertEquals(1, images.size());
        GeoPkgSymbolImage image = images.get(0);
        assertEquals("image/svg+xml", image.getFormat());
        assertEquals("symbols://burg/0", image.getUri());
        String svg = IOUtils.toString(image.getContent(), "UTF-8");
        assertThat(
                svg,
                containsString(
                        "<line fill=\"none\" stroke=\"#000000\" stroke-width=\"1\" x1=\"10\" y1=\"10\" x2=\"10\" y2=\"0\" />"));

        // the association with the layer
        List<GeoPkgSemanticAnnotation> lakesAnnotations =
                annotations.getAnnotationsByURI("http://localhost:8080/geoserver/styles/Lakes.sld");
        GeoPkgSemanticAnnotation lakesAnnotation = lakesAnnotations.get(0);
        assertNotNull(lakesAnnotation);
        assertEquals(PortrayalExtension.SA_TYPE_STYLE, lakesAnnotation.getType());
        assertEquals("Lakes", lakesAnnotation.getTitle());
        List<GeoPkgAnnotationReference> references =
                annotations.getReferencesForAnnotation(lakesAnnotation);
        assertEquals(2, references.size());
        GeoPkgAnnotationReference lakesReference1 = references.get(0);
        assertEquals("Lakes", lakesReference1.getTableName());
        assertNull(lakesReference1.getKeyColumnName());
        assertNull(lakesReference1.getKeyValue());
        assertEquals(lakesAnnotation, lakesReference1.getAnnotation());
        GeoPkgAnnotationReference lakesReference2 = references.get(1);
        assertEquals("gpkgext_styles", lakesReference2.getTableName());
        assertEquals("id", lakesReference2.getKeyColumnName());
        assertEquals(lakesStyle.getId(), (long) lakesReference2.getKeyValue());
        assertEquals(lakesAnnotation, lakesReference2.getAnnotation());

        GeoPkgStyle defaultStyle = portrayal.getStyle("Default");
        assertNotNull(defaultStyle);
        assertEquals("http://localhost:8080/geoserver/styles/Default.sld", defaultStyle.getUri());

        List<TileEntry> tiles = gpkg.tiles();
        assertEquals(2, tiles.size());

        TileEntry te = tiles.get(0);
        assertEquals("world_lakes", te.getTableName());
        assertEquals("world and lakes overlay", te.getDescription());
        assertEquals("wl1", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.17578125, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.087890625, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.17578125, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.087890625, te.getBounds().getMaxY(), 0.0001);

        List<TileMatrix> matrices = te.getTileMatricies();
        assertEquals(1, matrices.size());
        TileMatrix matrix = matrices.get(0);
        assertEquals(10, matrix.getZoomLevel().intValue());
        assertEquals(256, matrix.getTileWidth().intValue());
        assertEquals(256, matrix.getTileHeight().intValue());
        assertEquals(2048, matrix.getMatrixWidth().intValue());
        assertEquals(1024, matrix.getMatrixHeight().intValue());

        TileReader tr = gpkg.reader(te, null, null, null, null, null, null);
        assertTrue(tr.hasNext());
        assertEquals(10, tr.next().getZoom().intValue());
        tr.close();

        te = tiles.get(1);
        assertEquals("world_lakes2", te.getTableName());
        assertEquals("world and lakes overlay 2", te.getDescription());
        assertEquals("wl2", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.17578125, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.087890625, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.17578125, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.087890625, te.getBounds().getMaxY(), 0.0001);

        gpkg.close();
    }

    @Test
    public void testGeoPackageProcessWithRemove() throws Exception {
        File path = getDataDirectory().findOrCreateDataRoot();
        String urlPath = string(post("wps", getXml2(path, true))).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "test.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));

        assertNotNull(file);
        assertEquals("test.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<TileEntry> tiles = gpkg.tiles();
        assertEquals(1, tiles.size());

        TileEntry te = tiles.get(0);
        assertEquals("world_lakes", te.getTableName());
        assertEquals("world and lakes overlay", te.getDescription());
        assertEquals("wl1", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.17578125, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.087890625, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.17578125, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.087890625, te.getBounds().getMaxY(), 0.0001);

        List<TileMatrix> matrices = te.getTileMatricies();
        assertEquals(1, matrices.size());
        TileMatrix matrix = matrices.get(0);
        assertEquals(10, matrix.getZoomLevel().intValue());
        assertEquals(256, matrix.getTileWidth().intValue());
        assertEquals(256, matrix.getTileHeight().intValue());
        assertEquals(2048, matrix.getMatrixWidth().intValue());
        assertEquals(1024, matrix.getMatrixHeight().intValue());

        TileReader tr = gpkg.reader(te, null, null, null, null, null, null);
        assertTrue(tr.hasNext());
        assertEquals(10, tr.next().getZoom().intValue());
        tr.close();

        gpkg.close();
    }

    @Test
    public void testGeoPackageProcessWithPath() throws Exception {
        File path = getDataDirectory().findOrCreateDataRoot();

        String urlPath = string(post("wps", getXml2(path, false))).trim();
        File file = new File(path, "test.gpkg");
        assertNotNull(file);
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<TileEntry> tiles = gpkg.tiles();
        assertEquals(1, tiles.size());

        TileEntry te = tiles.get(0);
        assertEquals("world_lakes", te.getTableName());
        assertEquals("world and lakes overlay", te.getDescription());
        assertEquals("wl1", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.17578125, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.087890625, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.17578125, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.087890625, te.getBounds().getMaxY(), 0.0001);

        List<TileMatrix> matrices = te.getTileMatricies();
        assertEquals(1, matrices.size());
        TileMatrix matrix = matrices.get(0);
        assertEquals(10, matrix.getZoomLevel().intValue());
        assertEquals(256, matrix.getTileWidth().intValue());
        assertEquals(256, matrix.getTileHeight().intValue());
        assertEquals(2048, matrix.getMatrixWidth().intValue());
        assertEquals(1024, matrix.getMatrixHeight().intValue());

        TileReader tr = gpkg.reader(te, null, null, null, null, null, null);
        assertTrue(tr.hasNext());
        assertEquals(10, tr.next().getZoom().intValue());
        tr.close();

        gpkg.close();
    }

    @Test
    public void testGeoPackageProcessValidationError() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                        + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                        + "  <wps:DataInputs>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>contents</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:ComplexData mimeType=\"text/xml; subtype=geoserver/geopackage\"><![CDATA["
                        + "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                        + "  <features name=\"lakes\" identifier=\"lakes1\">"
                        + "    <description>lakes description</description>"
                        + "    <featuretype>cite:Lakes</featuretype>"
                        + "    <indexed>HELLO WORLD</indexed>"
                        + "   </features>"
                        + "</geopackage>"
                        + "]]></wps:ComplexData>"
                        + "      </wps:Data>"
                        + "    </wps:Input>"
                        + "  </wps:DataInputs>"
                        + "  <wps:ResponseForm>"
                        + "    <wps:RawDataOutput>"
                        + "      <ows:Identifier>geopackage</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";
        Document d = postAsDOM("wps", xml);
        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());
        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessFailed", d);
        String message =
                XMLUnit.newXpathEngine()
                        .evaluate(
                                "//wps:ExecuteResponse/wps:Status/wps:ProcessFailed"
                                        + "/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()",
                                d);
        assertThat(message, containsString("org.xml.sax.SAXParseException"));
        assertThat(message, containsString("HELLO WORLD"));
    }

    @Test
    public void testGeoPackageProcessValidationXXE() throws Exception {
        // for this one test we want the check on
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        global.setXmlExternalEntitiesEnabled(false);
        gs.save(global);

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                        + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                        + "  <wps:DataInputs>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>contents</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:ComplexData mimeType=\"text/xml; subtype=geoserver/geopackage\"><![CDATA["
                        + "<!DOCTYPE indexed ["
                        + "<!ELEMENT indexed ANY >"
                        + "<!ENTITY xxe SYSTEM \"file:///this/file/does/not/exist\" >]>"
                        + "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                        + "  <features name=\"lakes\" identifier=\"lakes1\">"
                        + "    <description>lakes description</description>"
                        + "    <featuretype>cite:Lakes</featuretype>"
                        + "    <indexed>&xxe;</indexed>"
                        + "   </features>"
                        + "</geopackage>"
                        + "]]></wps:ComplexData>"
                        + "      </wps:Data>"
                        + "    </wps:Input>"
                        + "  </wps:DataInputs>"
                        + "  <wps:ResponseForm>"
                        + "    <wps:RawDataOutput>"
                        + "      <ows:Identifier>geopackage</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";
        Document d = postAsDOM("wps", xml);
        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());
        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessFailed", d);
        String message =
                XMLUnit.newXpathEngine()
                        .evaluate(
                                "//wps:ExecuteResponse/wps:Status/wps:ProcessFailed"
                                        + "/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()",
                                d);
        assertThat(message, containsString("Entity resolution disallowed"));
    }

    public String getXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>contents</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:ComplexData mimeType=\"text/xml; subtype=geoserver/geopackage\"><![CDATA["
                + "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <features name=\"fifteen\" identifier=\"f15\">"
                + "    <description>fifteen description</description>"
                + "    <srs>EPSG:32615</srs>"
                + "    <bbox>"
                + "      <minx>500000</minx>"
                + "      <maxx>500100</maxx>"
                + "      <miny>500000</miny>"
                + "      <maxy>500100</maxy>"
                + "    </bbox>"
                + "    <featuretype>cdf:Fifteen</featuretype>"
                + "    <propertynames>pointProperty</propertynames>"
                + "  </features>"
                + "  <features name=\"lakes\" identifier=\"lakes1\">"
                + "    <description>lakes description</description>"
                + "    <featuretype>cite:Lakes</featuretype>"
                + " <filter xmlns:fes=\"http://www.opengis.net/fes/2.0\">"
                + " <fes:PropertyIsEqualTo>"
                + " <fes:ValueReference>NAME</fes:ValueReference>"
                + " <fes:Literal>Blue Lake</fes:Literal>"
                + " </fes:PropertyIsEqualTo>"
                + " </filter>"
                + "    <indexed>true</indexed>"
                + "    <styles>true</styles>"
                + "   </features>"
                + "  <tiles name=\"world_lakes\" identifier=\"wl1\">"
                + "    <description>world and lakes overlay</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-0.17578125</minx>"
                + "      <maxx>0.17578125</maxx>"
                + "      <miny>-0.087890625</miny>"
                + "      <maxy>0.087890625</maxy>"
                + "    </bbox>"
                + "    <layers>wcs:World,cite:Lakes</layers>"
                + "    <styles></styles>"
                + "    <format>png</format>"
                + "    <bgcolor>aaaaaa</bgcolor>"
                + "    <transparent>true</transparent>"
                + "    <coverage>"
                + "      <minZoom>10</minZoom>"
                + "      <maxZoom>11</maxZoom>"
                + "    </coverage>"
                + "    <gridset>"
                + "      <grids>"
                + "        <grid>"
                + "          <zoomlevel>10</zoomlevel>"
                + "          <tilewidth>256</tilewidth>"
                + "          <tileheight>256</tileheight>"
                + "          <matrixwidth>2048</matrixwidth>"
                + "          <matrixheight>1024</matrixheight>"
                + "          <pixelxsize>0.00068</pixelxsize>"
                + "          <pixelysize>0.00068</pixelysize>"
                + "        </grid> "
                + "      </grids>"
                + "    </gridset>"
                + "  </tiles>"
                + "  <tiles name=\"world_lakes2\" identifier=\"wl2\">"
                + "    <description>world and lakes overlay 2</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-0.17578125</minx>"
                + "      <maxx>0.17578125</maxx>"
                + "      <miny>-0.087890625</miny>"
                + "      <maxy>0.087890625</maxy>"
                + "    </bbox>"
                + "    <layers>wcs:World,cite:Lakes</layers>"
                + "    <styles></styles>"
                + "    <format>png</format>"
                + "    <bgcolor>aaaaaa</bgcolor>"
                + "    <transparent>true</transparent>"
                + "    <coverage>"
                + "      <minZoom>10</minZoom>"
                + "      <maxZoom>11</maxZoom>"
                + "    </coverage>"
                + "  </tiles>"
                + "</geopackage>"
                + "]]></wps:ComplexData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "  <wps:ResponseForm>"
                + "    <wps:RawDataOutput>"
                + "      <ows:Identifier>geopackage</ows:Identifier>"
                + "    </wps:RawDataOutput>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }

    public String getXml2(File temp, Boolean remove) {
        String path = "";
        String removal = "";

        if (temp != null) {
            path = " path=\"" + URLs.fileToUrl(temp) + "\"";
        }

        if (remove != null) {
            removal = " remove=\"" + remove + "\"";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>contents</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:ComplexData mimeType=\"text/xml; subtype=geoserver/geopackage\"><![CDATA["
                + "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\""
                + path
                + removal
                + ">"
                + "  <tiles name=\"world_lakes\" identifier=\"wl1\">"
                + "    <description>world and lakes overlay</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-0.17578125</minx>"
                + "      <maxx>0.17578125</maxx>"
                + "      <miny>-0.087890625</miny>"
                + "      <maxy>0.087890625</maxy>"
                + "    </bbox>"
                + "    <layers>wcs:World,cite:Lakes</layers>"
                + "    <styles></styles>"
                + "    <format>png</format>"
                + "    <bgcolor>aaaaaa</bgcolor>"
                + "    <transparent>true</transparent>"
                + "    <coverage>"
                + "      <minZoom>10</minZoom>"
                + "      <maxZoom>11</maxZoom>"
                + "    </coverage>"
                + "    <gridset>"
                + "      <grids>"
                + "        <grid>"
                + "          <zoomlevel>10</zoomlevel>"
                + "          <tilewidth>256</tilewidth>"
                + "          <tileheight>256</tileheight>"
                + "          <matrixwidth>2048</matrixwidth>"
                + "          <matrixheight>1024</matrixheight>"
                + "          <pixelxsize>0.00068</pixelxsize>"
                + "          <pixelysize>0.00068</pixelysize>"
                + "        </grid> "
                + "      </grids>"
                + "    </gridset>"
                + "  </tiles>"
                + "</geopackage>"
                + "]]></wps:ComplexData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "  <wps:ResponseForm>"
                + "    <wps:RawDataOutput>"
                + "      <ows:Identifier>geopackage</ows:Identifier>"
                + "    </wps:RawDataOutput>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }

    @Test
    public void testGeoPackageProcessTilesNoFormat() throws Exception {
        String urlPath = string(post("wps", getXmlTilesNoFormat())).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "test.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));
        assertNotNull(file);
        assertEquals("test.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<TileEntry> tiles = gpkg.tiles();
        assertEquals(1, tiles.size());

        TileEntry te = tiles.get(0);
        assertEquals("world_lakes", te.getTableName());
        assertEquals("world and lakes overlay", te.getDescription());
        assertEquals("wl1", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.17578125, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.087890625, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.17578125, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.087890625, te.getBounds().getMaxY(), 0.0001);

        TileReader tr = gpkg.reader(te, null, null, null, null, null, null);
        assertTrue(tr.hasNext());
        assertEquals(10, tr.next().getZoom().intValue());
        tr.close();

        te = tiles.get(0);
        assertEquals("world_lakes", te.getTableName());
        assertEquals("world and lakes overlay", te.getDescription());
        assertEquals("wl1", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.17578125, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.087890625, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.17578125, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.087890625, te.getBounds().getMaxY(), 0.0001);

        gpkg.close();
    }

    private String getXmlTilesNoFormat() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>contents</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:ComplexData mimeType=\"text/xml; subtype=geoserver/geopackage\"><![CDATA["
                + "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <tiles name=\"world_lakes\" identifier=\"wl1\">"
                + "    <description>world and lakes overlay</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-0.17578125</minx>"
                + "      <maxx>0.17578125</maxx>"
                + "      <miny>-0.087890625</miny>"
                + "      <maxy>0.087890625</maxy>"
                + "    </bbox>"
                + "    <layers>wcs:World,cite:Lakes</layers>"
                + "    <styles></styles>"
                + "    <bgcolor>aaaaaa</bgcolor>"
                + "    <transparent>true</transparent>"
                + "    <coverage>"
                + "      <minZoom>10</minZoom>"
                + "      <maxZoom>11</maxZoom>"
                + "    </coverage>"
                + "    <gridset>"
                + "      <grids>"
                + "        <grid>"
                + "          <zoomlevel>10</zoomlevel>"
                + "          <tilewidth>256</tilewidth>"
                + "          <tileheight>256</tileheight>"
                + "          <matrixwidth>2048</matrixwidth>"
                + "          <matrixheight>1024</matrixheight>"
                + "          <pixelxsize>0.00068</pixelxsize>"
                + "          <pixelysize>0.00068</pixelysize>"
                + "        </grid> "
                + "      </grids>"
                + "    </gridset>"
                + "  </tiles>"
                + "</geopackage>"
                + "]]></wps:ComplexData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "  <wps:ResponseForm>"
                + "    <wps:RawDataOutput>"
                + "      <ows:Identifier>geopackage</ows:Identifier>"
                + "    </wps:RawDataOutput>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }
}
