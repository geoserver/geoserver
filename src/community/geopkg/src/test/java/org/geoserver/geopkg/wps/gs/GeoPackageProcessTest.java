/* (c) 2014-2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.geopkg.TileReader;
import org.geotools.util.URLs;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GeoPackageProcessTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
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

        fe = features.get(1);
        assertEquals("Lakes", fe.getTableName());
        assertEquals("lakes description", fe.getDescription());
        assertEquals("lakes1", fe.getIdentifier());

        fr = gpkg.reader(fe, null, null);
        assertTrue(fr.hasNext());
        fr.next();
        fr.close();

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
