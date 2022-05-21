/* (c) 2014-2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgExtension;
import org.geotools.geopkg.GeoPkgMetadata;
import org.geotools.geopkg.GeoPkgMetadataExtension;
import org.geotools.geopkg.GeoPkgMetadataReference;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.geopkg.TileReader;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GeoPackageProcessTest extends WPSTestSupport {

    private static final int MAX_ASYNCH_WAIT = 10000;

    @Override
    protected String getLogConfiguration() {
        // needed for a test on logging capabilities
        GeoServerResourceLoader loader =
                new GeoServerResourceLoader(testData.getDataDirectoryRoot());

        Resource resource = loader.get("logs/GPKG_LOGGING.properties");
        if (resource.getType() == Resource.Type.UNDEFINED) {
            try {
                loader.copyFromClassPath(
                        "/GPKG_LOGGING.properties", "logs/GPKG_LOGGING.properties");
            } catch (IOException e) {
                LOGGER.fine("Unable to configure with GPKG_LOGGING");
                return "TEST_LOGGING";
            }
        }
        return "GPKG_LOGGING";
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        // so that we can work from a fake file system xml
        MetadataLinkInfoImpl.addProtocol("file");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle("burg", "burg.sld", this.getClass(), catalog);
        try (InputStream is = this.getClass().getResourceAsStream("burg02.svg")) {
            testData.copyTo(is, "styles/burg02.svg");
        }
        testData.addStyle("fillenv", "fillenv.sld", this.getClass(), catalog);

        Catalog catalog = getCatalog();
        StyleInfo burgStyle = catalog.getStyleByName("burg");
        String lakesName = getLayerId(MockData.LAKES);
        LayerInfo lakesLayer = catalog.getLayerByName(lakesName);
        lakesLayer.getStyles().add(burgStyle);
        catalog.save(lakesLayer);

        String fifteenName = getLayerId(MockData.FIFTEEN);
        LayerInfo fifteenLayer = catalog.getLayerByName(fifteenName);
        StyleInfo polygonStyle = catalog.getStyleByName("polygon");
        fifteenLayer.getStyles().add(polygonStyle);
        catalog.save(fifteenLayer);

        // add some shared metadata link
        MetadataLinkInfo link = getCatalog().getFactory().createMetadataLink();
        link.setMetadataType("gsFakeMetadata");
        link.setAbout("http://www.geoserver.org");
        link.setType("text/xml");
        link.setContent(getClass().getResource("fakeMetadata.xml").toExternalForm());

        FeatureTypeInfo fifteen = catalog.getFeatureTypeByName(fifteenName);
        fifteen.getMetadataLinks().add(link);
        catalog.save(fifteen);

        FeatureTypeInfo lakes = catalog.getFeatureTypeByName(lakesName);
        lakes.getMetadataLinks().add(link);
        catalog.save(lakes);

        // Create layer groups for the stylesheets OWS Context generation
        // First one using the default styles
        CatalogBuilder cb = new CatalogBuilder(catalog);
        LayerGroupInfo lg1 = catalog.getFactory().createLayerGroup();
        lg1.setName("fifteenLakes");
        lg1.getLayers().add(fifteenLayer);
        lg1.getStyles().add(null);
        lg1.getLayers().add(lakesLayer);
        lg1.getStyles().add(null);
        cb.calculateLayerGroupBounds(lg1);
        catalog.add(lg1);
        // Second one using other styles, and a different layer order
        LayerGroupInfo lg2 = catalog.getFactory().createLayerGroup();
        lg2.setName("LakeFifteen");
        lg2.getLayers().add(lakesLayer);
        lg2.getStyles().add(burgStyle);
        lg2.getLayers().add(fifteenLayer);
        lg2.getStyles().add(polygonStyle);
        cb.calculateLayerGroupBounds(lg2);
        catalog.add(lg2);
        // A group with a parametric style
        StyleInfo fillenv = catalog.getStyleByName("fillenv");
        LayerInfo forestsLayer = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        LayerGroupInfo groupenv = catalog.getFactory().createLayerGroup();
        groupenv.setName("groupenv");
        groupenv.getLayers().add(forestsLayer);
        groupenv.getStyles().add(polygonStyle);
        groupenv.getLayers().add(lakesLayer);
        groupenv.getStyles().add(fillenv);
        cb.calculateLayerGroupBounds(groupenv);
        catalog.add(groupenv);
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
        String urlPath = string(post("wps", getXml(getXMLInnerRequest()))).trim();
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
        // these are 15 superimposed points. A previous result included the geometry
        // of the polygon, which is not extracted at all in the WPS request
        assertEquals(500050, fe.getBounds().getMinX(), 0.0001);
        assertEquals(500050, fe.getBounds().getMinY(), 0.0001);
        assertEquals(500050, fe.getBounds().getMaxX(), 0.0001);
        assertEquals(500050, fe.getBounds().getMaxY(), 0.0001);

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
        assertEquals(4, portrayal.getStyles().size()); // default style plus one alternative each
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

        // check the metadata
        GeoPkgMetadataExtension metadataExt = gpkg.getExtension(GeoPkgMetadataExtension.class);
        List<GeoPkgMetadata> metadatas = metadataExt.getMetadatas();
        assertEquals(6, metadatas.size());
        // metadatas have no significant identifiers, use the insertion order knowledge to assert
        // them
        for (GeoPkgMetadata metadata : metadatas) {
            switch (metadata.getId().intValue()) {
                case 1:
                    assertRequestContext(metadataExt, annotations, metadata);
                    break;
                case 2:
                    assertLinkedMetadata(metadataExt, metadata);
                    break;
                case 3:
                    assertFeatureMetadata(
                            metadataExt,
                            annotations,
                            metadata,
                            "Fifteen",
                            "fifteen",
                            "cdf%3AFifteen");
                    break;
                case 4:
                    assertFeatureMetadata(
                            metadataExt, annotations, metadata, "Lakes", "lakes", "cite%3ALakes");
                    break;
                case 5:
                case 6:
                    assertStylesheetsMetadata(metadataExt, annotations, metadata);
                    break;
            }
        }

        gpkg.close();
    }

    private void assertStylesheetsMetadata(
            GeoPkgMetadataExtension metadataExt,
            SemanticAnnotationsExtension annotations,
            GeoPkgMetadata metadata)
            throws SQLException {
        JSONObject json = assertGeoJSONMetadata(metadata, GeoPkgMetadata.Scope.Undefined);
        // print(json);

        // root
        assertEquals("FeatureCollection", json.get("type"));
        JSONObject rootProperties = json.getJSONObject("properties");
        assertEquals("en", rootProperties.getString("lang"));
        assertEquals("GeoServer", rootProperties.getString("generator"));
        String title = rootProperties.getString("title");
        if ("LakeFifteen".equals(title)) {
            assertLakeFifteenStylesheet(json);
        } else if ("fifteenLakes".equals(title)) {
            assertFifteenLakesStylesheet(json);
        } else {
            fail("Unexpected title: " + title);
        }

        // check the reference
        List<GeoPkgMetadataReference> references = metadataExt.getReferences(metadata);
        assertEquals(1, references.size());
        GeoPkgMetadataReference reference = references.get(0);
        assertEquals(GeoPkgMetadataReference.Scope.GeoPackage, reference.getScope());
        assertNull(reference.getTable());
        assertNull(reference.getColumn());
        assertNull(reference.getRowId());
        assertNull(reference.getMetadataParent());

        // check the semantic annotations
        List<GeoPkgSemanticAnnotation> sas =
                annotations.getAnnotationsByURI(OWSContextWriter.STYLESHEET_SA_URI);
        boolean found = false;
        String expectedTitle = "OGC OWS Context GeoJSON for " + title;
        for (GeoPkgSemanticAnnotation sa : sas) {
            if (expectedTitle.equals(sa.getTitle())) {
                found = true;
                assertEquals(OWSContextWriter.STYLESHEET_SA_TYPE, sa.getType());
                List<GeoPkgAnnotationReference> ars = annotations.getReferencesForAnnotation(sa);
                assertThat(
                        ars,
                        hasItem(
                                allOf(
                                        hasProperty("tableName", equalTo("gpkg_metadata")),
                                        hasProperty("keyColumnName", equalTo("id")),
                                        hasProperty("keyValue", equalTo(metadata.getId())))));
            }
        }
        if (!found) {
            fail("Could not find a semantic annotation titled: " + expectedTitle);
        }
    }

    private GeoPkgSemanticAnnotation assertProvenanceAnnotation(
            SemanticAnnotationsExtension annotations) throws SQLException {
        List<GeoPkgSemanticAnnotation> provenances =
                annotations.getAnnotationsByURI(OWSContextWriter.PROVENANCE_SA_URI);
        assertEquals(1, provenances.size());
        GeoPkgSemanticAnnotation provenance = provenances.get(0);
        assertEquals("Dataset provenance", provenance.getTitle());
        assertEquals(OWSContextWriter.PROVENANCE_SA_TYPE, provenance.getType());
        return provenance;
    }

    private void assertLakeFifteenStylesheet(JSONObject json) {
        // features
        JSONArray features = json.getJSONArray("features");
        assertEquals(2, features.size());

        // first feature, fifteen (OWS context lists layers top to bottom, opposite of WMS)
        JSONObject f0 = features.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/cdf:Fifteen",
                f0.getString("id"));
        JSONObject f0p = f0.getJSONObject("properties");
        assertEquals("Fifteen", f0p.getString("title"));
        JSONArray offerings0 = f0p.getJSONArray("offerings");
        JSONObject offering0 = offerings0.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/1.2/opt/features",
                offering0.getString("code"));
        JSONObject operation0 = offering0.getJSONArray("operations").getJSONObject(0);
        assertEquals("GPKG", operation0.getString("code"));
        assertEquals("test.gpkg", operation0.getString("href"));
        assertEquals(
                "SELECT * FROM fifteen;", operation0.getJSONObject("request").getString("content"));
        JSONObject style0 = offering0.getJSONArray("styles").getJSONObject(0);
        assertEquals("polygon", style0.getString("name"));
        assertEquals("Grey Polygon", style0.getString("title"));
        assertEquals(
                "A sample style that just prints out a grey interior with a black outline",
                style0.getString("abstract"));
        assertEquals(
                "SELECT stylesheet, format FROM gpkgext_stylesheets WHERE style_id = (SELECT id FROM gpkgext_styles where style = 'polygon');",
                style0.getJSONObject("content").getString("content"));

        // second feature, lakes
        JSONObject f1 = features.getJSONObject(1);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/cite:Lakes", f1.getString("id"));
        JSONObject f1p = f1.getJSONObject("properties");
        assertEquals("Lakes", f1p.getString("title"));
        JSONArray offerings1 = f1p.getJSONArray("offerings");
        JSONObject offering1 = offerings1.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/1.2/opt/features",
                offering1.getString("code"));
        JSONObject operation1 = offering1.getJSONArray("operations").getJSONObject(0);
        assertEquals("GPKG", operation1.getString("code"));
        assertEquals("test.gpkg", operation1.getString("href"));
        assertEquals(
                "SELECT * FROM lakes;", operation1.getJSONObject("request").getString("content"));
        JSONObject style1 = offering1.getJSONArray("styles").getJSONObject(0);
        assertEquals("burg", style1.getString("name"));
        assertEquals("A small red flag", style1.getString("title"));
        assertEquals(
                "A sample of how to use an SVG based symbolizer", style1.getString("abstract"));
        assertEquals(
                "SELECT stylesheet, format FROM gpkgext_stylesheets WHERE style_id = (SELECT id FROM gpkgext_styles where style = 'burg');",
                style1.getJSONObject("content").getString("content"));
    }

    private void assertFifteenLakesStylesheet(JSONObject json) {
        // features
        JSONArray features = json.getJSONArray("features");
        assertEquals(2, features.size());

        // first feature, lakes (OWS context lists layers top to bottom, opposite of WMS)
        JSONObject f0 = features.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/cite:Lakes", f0.getString("id"));
        JSONObject f0p = f0.getJSONObject("properties");
        assertEquals("Lakes", f0p.getString("title"));
        JSONArray offerings0 = f0p.getJSONArray("offerings");
        JSONObject offering0 = offerings0.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/1.2/opt/features",
                offering0.getString("code"));
        JSONObject operation0 = offering0.getJSONArray("operations").getJSONObject(0);
        assertEquals("GPKG", operation0.getString("code"));
        assertEquals("test.gpkg", operation0.getString("href"));
        assertEquals(
                "SELECT * FROM lakes;", operation0.getJSONObject("request").getString("content"));
        JSONObject style0 = offering0.getJSONArray("styles").getJSONObject(0);
        assertEquals("Lakes", style0.getString("name"));
        assertEquals("Default Styler", style0.getString("title"));
        assertNull(style0.get("abstract"));
        assertEquals(
                "SELECT stylesheet, format FROM gpkgext_stylesheets WHERE style_id = (SELECT id FROM gpkgext_styles where style = 'Lakes');",
                style0.getJSONObject("content").getString("content"));

        // second feature, fifteen
        JSONObject f1 = features.getJSONObject(1);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/cdf:Fifteen",
                f1.getString("id"));
        JSONObject f1p = f1.getJSONObject("properties");
        assertEquals("Fifteen", f1p.getString("title"));
        JSONArray offerings1 = f1p.getJSONArray("offerings");
        JSONObject offering1 = offerings1.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/spec/owc-json/1.0/req/gpkg/1.2/opt/features",
                offering1.getString("code"));
        JSONObject operation1 = offering1.getJSONArray("operations").getJSONObject(0);
        assertEquals("GPKG", operation1.getString("code"));
        assertEquals("test.gpkg", operation1.getString("href"));
        assertEquals(
                "SELECT * FROM fifteen;", operation1.getJSONObject("request").getString("content"));
        JSONObject style1 = offering1.getJSONArray("styles").getJSONObject(0);
        assertEquals("Default", style1.getString("name"));
        assertEquals("Default Styler", style1.getString("title"));
        assertNull(style1.get("abstract"));
        assertEquals(
                "SELECT stylesheet, format FROM gpkgext_stylesheets WHERE style_id = (SELECT id FROM gpkgext_styles where style = 'Default');",
                style1.getJSONObject("content").getString("content"));
    }

    private JSONObject assertGeoJSONMetadata(GeoPkgMetadata metadata, GeoPkgMetadata.Scope scope) {
        assertEquals(scope, metadata.getScope());
        assertEquals("application/geo+json", metadata.getMimeType());
        assertEquals(
                "https://portal.opengeospatial.org/files/?artifact_id=68826",
                metadata.getStandardURI());
        return (JSONObject) JSONSerializer.toJSON(metadata.getMetadata());
    }

    private void assertRequestContext(
            GeoPkgMetadataExtension metadataExt,
            SemanticAnnotationsExtension annotations,
            GeoPkgMetadata metadata)
            throws Exception {
        JSONObject json = assertGeoJSONMetadata(metadata, GeoPkgMetadata.Scope.Undefined);

        // root
        assertEquals("FeatureCollection", json.get("type"));
        JSONObject rootProperties = json.getJSONObject("properties");
        assertEquals("en", rootProperties.getString("lang"));
        assertEquals("GeoServer", rootProperties.getString("generator"));
        // author
        JSONObject author = rootProperties.getJSONArray("authors").getJSONObject(0);
        assertEquals("Andrea Aime", author.getString("name"));
        assertEquals("andrea@geoserver.org", author.getString("email"));
        // features
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertEquals("Feature", feature.getString("type"));
        JSONObject featureProperties = feature.getJSONObject("properties");
        // offerings
        JSONArray offerings = featureProperties.getJSONArray("offerings");
        // ... first offering
        JSONObject offering = offerings.getJSONObject(0);
        assertEquals(
                "code",
                "http://www.opengis.net/spec/owc-geojson/1.0/req/wps",
                offering.getString("code"));
        JSONArray operations = offering.getJSONArray("operations");
        // caps
        JSONObject caps = operations.getJSONObject(0);
        assertEquals("GetCapabilities", caps.getString("code"));
        assertEquals("GET", caps.getString("method"));
        assertEquals("application/xml", caps.getString("type"));
        assertEquals(
                "http://localhost:8080/geoserver/wps?service=WPS&version=1.0&request=GetCapabilities",
                caps.getString("href"));
        // describe
        JSONObject describe = operations.getJSONObject(1);
        assertEquals("DescribeProcess", describe.getString("code"));
        assertEquals("GET", describe.getString("method"));
        assertEquals("application/xml", describe.getString("type"));
        assertEquals(
                "http://localhost:8080/geoserver/wps?service=WPS&version=1.0&request=DescribeProcess&identifier=gs%3AGeoPackageProcess",
                describe.getString("href"));
        // execute
        JSONObject execute = operations.getJSONObject(2);
        assertEquals("Execute", execute.getString("code"));
        assertEquals("POST", execute.getString("method"));
        assertEquals("application/xml", execute.getString("type"));
        assertEquals("http://localhost:8080/geoserver/wps", execute.getString("href"));
        JSONObject requestBody = execute.getJSONObject("request");
        assertEquals("application/xml", requestBody.getString("type"));
        String xml = requestBody.getString("content");
        Document dom = dom(new ByteArrayInputStream(xml.getBytes()));
        // check the request has been properly encoded
        XpathEngine engine = XMLUnit.newXpathEngine();
        String actualXML =
                engine.evaluate(
                        "/wps:Execute/wps:DataInputs/wps:Input/wps:Data/wps:ComplexData", dom);
        assertEquals(getXMLInnerRequest(), actualXML);

        // check the reference
        List<GeoPkgMetadataReference> references = metadataExt.getReferences(metadata);
        assertEquals(1, references.size());
        GeoPkgMetadataReference reference = references.get(0);
        assertEquals(GeoPkgMetadataReference.Scope.GeoPackage, reference.getScope());
        assertNull(reference.getTable());
        assertNull(reference.getColumn());
        assertNull(reference.getRowId());
        assertNull(reference.getMetadataParent());

        // check the semantic annotations
        GeoPkgSemanticAnnotation provenance = assertProvenanceAnnotation(annotations);
        List<GeoPkgAnnotationReference> ars = annotations.getReferencesForAnnotation(provenance);
        assertThat(
                ars,
                hasItem(
                        allOf(
                                hasProperty("tableName", equalTo("gpkg_metadata")),
                                hasProperty("keyColumnName", equalTo("id")),
                                hasProperty("keyValue", equalTo(metadata.getId())))));
    }

    private void assertFeatureMetadata(
            GeoPkgMetadataExtension metadataExt,
            SemanticAnnotationsExtension annotations,
            GeoPkgMetadata metadata,
            String layerName,
            String tableName,
            String prefixedName)
            throws Exception {
        JSONObject json = assertGeoJSONMetadata(metadata, GeoPkgMetadata.Scope.Dataset);

        // root
        assertEquals("Feature", json.get("type"));
        JSONObject rootProperties = json.getJSONObject("properties");
        assertEquals(layerName, rootProperties.getString("title"));
        assertEquals("abstract about " + layerName, rootProperties.getString("abstract"));

        JSONObject featureProperties = json.getJSONObject("properties");
        // offerings
        JSONArray offerings = featureProperties.getJSONArray("offerings");
        // ... first offering
        JSONObject offering = offerings.getJSONObject(0);
        assertEquals(
                "code",
                "http://www.opengis.net/spec/owc-geojson/1.0/req/wfs",
                offering.getString("code"));
        JSONArray operations = offering.getJSONArray("operations");
        // caps
        JSONObject caps = operations.getJSONObject(0);
        assertEquals("GetCapabilities", caps.getString("code"));
        assertEquals("GET", caps.getString("method"));
        assertEquals("application/xml", caps.getString("type"));
        assertEquals(
                "http://localhost:8080/geoserver/wfs?service=WFS&version=2.0&request=GetCapabilities",
                caps.getString("href"));
        // getFeature
        JSONObject getFeatures = operations.getJSONObject(1);
        assertEquals("GetFeature", getFeatures.getString("code"));
        assertEquals("GET", getFeatures.getString("method"));
        assertEquals("application/gml+xml", getFeatures.getString("type"));
        assertEquals(
                "http://localhost:8080/geoserver/wfs?service=WFS&version=2.0&request=GetFeature&typeNames="
                        + prefixedName,
                getFeatures.getString("href"));

        // check the reference
        List<GeoPkgMetadataReference> references = metadataExt.getReferences(metadata);
        assertEquals(1, references.size());
        GeoPkgMetadataReference reference = references.get(0);
        assertEquals(GeoPkgMetadataReference.Scope.Table, reference.getScope());
        assertEquals(tableName, reference.getTable());
        assertNull(reference.getColumn());
        assertNull(reference.getRowId());
        assertNotNull(reference.getMetadataParent());
        assertRequestContext(metadataExt, annotations, reference.getMetadataParent());

        // check the semantic annotations, still referenceing the provenance
        GeoPkgSemanticAnnotation provenance = assertProvenanceAnnotation(annotations);
        List<GeoPkgAnnotationReference> ars = annotations.getReferencesForAnnotation(provenance);
        assertThat(
                ars,
                hasItem(
                        allOf(
                                hasProperty("tableName", equalTo("gpkg_metadata")),
                                hasProperty("keyColumnName", equalTo("id")),
                                hasProperty("keyValue", equalTo(metadata.getId())))));
    }

    private void assertLinkedMetadata(GeoPkgMetadataExtension metadataExt, GeoPkgMetadata metadata)
            throws SQLException {
        assertEquals(GeoPkgMetadata.Scope.Dataset, metadata.getScope());
        assertEquals("http://www.geoserver.org", metadata.getStandardURI());
        assertEquals("text/xml", metadata.getMimeType());
        assertThat(metadata.getMetadata(), containsString("<fake>true</fake>"));

        // and the metadata references
        List<GeoPkgMetadataReference> metadataReferences = metadataExt.getReferences(metadata);
        assertEquals(2, metadataReferences.size());
        Map<String, GeoPkgMetadataReference> referencesMap =
                metadataReferences.stream().collect(Collectors.toMap(r -> r.getTable(), r -> r));
        assertNotNull(referencesMap.get("Lakes"));
        assertNotNull(referencesMap.get("Fifteen"));
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

    @Test
    public void testGeoPackageVectorOverviews() throws Exception {
        String urlPath = string(post("wps", getXml(getXMLInnerRequestOverviews()))).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "test.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));
        assertNotNull(file);
        assertEquals("test.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<FeatureEntry> features = gpkg.features();
        assertEquals(1, features.size());

        FeatureEntry fe = features.get(0);
        String primaryTableName = "RoadSegments";
        assertEquals(primaryTableName, fe.getTableName());

        // getting direct access to the underlying packages, showing all tables, paints a different
        // picture
        JDBCDataStore store = GeoPackageProcess.getStoreFromPackage(gpkg, false);
        // get base level geometry, check srid
        ContentFeatureSource fs = store.getFeatureSource("RoadSegments");
        SimpleFeature mainStreet =
                DataUtilities.first(fs.getFeatures(ECQL.toFilter("NAME = 'Main Street'")));
        MultiLineString baseGeometry = (MultiLineString) mainStreet.getDefaultGeometry();
        assertEquals(4326, baseGeometry.getSRID());
        // first overview, all features (no filter) and
        ContentFeatureSource fs_ov1 = store.getFeatureSource("roads_ov1");
        assertEquals(5, fs_ov1.getCount(Query.ALL));
        SimpleFeature mainStreetOv1 =
                DataUtilities.first(fs_ov1.getFeatures(ECQL.toFilter("NAME = 'Main Street'")));
        MultiLineString ov1Geometry = (MultiLineString) mainStreetOv1.getDefaultGeometry();
        assertEquals(4326, ov1Geometry.getSRID());
        assertEquals(3, ov1Geometry.getNumPoints());
        assertEquals(new Coordinate(0.0002, 0.0007), ov1Geometry.getCoordinates()[0]);
        assertEquals(new Coordinate(0.0014, 0.001), ov1Geometry.getCoordinates()[1]);
        assertEquals(new Coordinate(0.0042, 0.0018), ov1Geometry.getCoordinates()[2]);
        // second overview, just a single feature, and more simplified
        ContentFeatureSource fs_ov2 = store.getFeatureSource("roads_ov2");
        assertEquals(1, fs_ov2.getCount(Query.ALL));
        SimpleFeature mainStreetOv2 =
                DataUtilities.first(fs_ov2.getFeatures(ECQL.toFilter("NAME = 'Main Street'")));
        MultiLineString ov2Geometry = (MultiLineString) mainStreetOv2.getDefaultGeometry();
        assertEquals(4326, ov2Geometry.getSRID());
        assertEquals(2, ov2Geometry.getNumPoints());
        assertEquals(new Coordinate(0.0002, 0.0007), ov2Geometry.getCoordinates()[0]);
        assertEquals(new Coordinate(0.0042, 0.0018), ov2Geometry.getCoordinates()[1]);

        // check the overview tables have been registered among the extensions
        GeneralizedTablesExtension extension = gpkg.getExtension(GeneralizedTablesExtension.class);
        List<GeoPkgExtension.Association> associations = extension.getAssociations();
        assertThat(associations, hasItem(hasProperty("tableName", equalTo("roads_ov1"))));
        assertThat(associations, hasItem(hasProperty("tableName", equalTo("roads_ov2"))));
        assertThat(associations, hasItem(hasProperty("tableName", equalTo("gpkgext_generalized"))));

        // get the definitions, they are ordered by generalization distance
        List<GeneralizedTable> generalizedTables = extension.getGeneralizedTables(primaryTableName);
        assertEquals(2, generalizedTables.size());
        // first generalized table
        GeneralizedTable ov1 = generalizedTables.get(0);
        assertEquals(primaryTableName, ov1.getPrimaryTable());
        assertEquals("roads_ov1", ov1.getGeneralizedTable());
        assertEquals(0.00001, ov1.getDistance(), 0d);
        // second generalized table
        GeneralizedTable ov2 = generalizedTables.get(1);
        assertEquals(primaryTableName, ov2.getPrimaryTable());
        assertEquals("roads_ov2", ov2.getGeneralizedTable());
        assertEquals(0.0001, ov2.getDistance(), 0d);
    }

    @Test
    public void testGeoPackageSortNameFID() throws Exception {
        String urlPath = string(post("wps", getXml(getXMLInnerRequestSortNameFID()))).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "test.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));
        assertNotNull(file);
        assertEquals("test.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<FeatureEntry> featureEntries = gpkg.features();
        assertEquals(1, featureEntries.size());

        JDBCDataStore store = GeoPackageProcess.getStoreFromPackage(gpkg, true);
        ContentFeatureCollection fc = store.getFeatureSource("RoadSegments").getFeatures();
        SimpleFeature[] features = fc.toArray(new SimpleFeature[fc.size()]);
        assertEquals("106", features[0].getAttribute("FID")); // dirt road
        assertEquals("105", features[1].getAttribute("FID")); // main street
        assertEquals("104", features[2].getAttribute("FID")); // route 5, fid desc
        assertEquals("103", features[3].getAttribute("FID")); // route 5, fid desc
        assertEquals("102", features[4].getAttribute("FID")); // route 5, fid desc
    }

    @Test
    public void testGeoPackageSortGeoHash() throws Exception {
        String urlPath = string(post("wps", getXml(getXMLInnerRequestSortGeoHash()))).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "test.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));
        assertNotNull(file);
        assertEquals("test.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);

        List<FeatureEntry> featureEntries = gpkg.features();
        assertEquals(1, featureEntries.size());

        JDBCDataStore store = GeoPackageProcess.getStoreFromPackage(gpkg, true);
        ContentFeatureCollection fc = store.getFeatureSource("RoadSegments").getFeatures();
        SimpleFeature[] features = fc.toArray(new SimpleFeature[fc.size()]);
        assertEquals("106", features[0].getAttribute("FID")); // 7zzzzzycx6un (vertical line)
        assertEquals("102", features[1].getAttribute("FID")); // ebpbpbn
        assertEquals("105", features[2].getAttribute("FID")); // s000001, Main Street
        assertEquals("103", features[3].getAttribute("FID")); // s000001, Route 5
        assertEquals("104", features[4].getAttribute("FID")); // s000006
    }

    @Test
    public void testGeoPackageParametersGroups() throws Exception {
        String xml = getXml(getXMLInnerRequestParametersGroups());
        String urlPath = string(post("wps", xml)).trim();
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
        assertEquals("groupenv", te.getTableName());
        assertEquals("A group, and a style with env params", te.getDescription());
        assertEquals("ge", te.getIdentifier());
        assertEquals(4326, te.getSrid().intValue());
        assertEquals(-0.0014, te.getBounds().getMinX(), 0.0001);
        assertEquals(-0.0024, te.getBounds().getMinY(), 0.0001);
        assertEquals(0.004, te.getBounds().getMaxX(), 0.0001);
        assertEquals(0.0016, te.getBounds().getMaxY(), 0.0001);

        List<TileMatrix> matrices = te.getTileMatricies();
        assertEquals(1, matrices.size());
        TileMatrix matrix = matrices.get(0);
        assertEquals(16, matrix.getZoomLevel().intValue());
        assertEquals(256, matrix.getTileWidth().intValue());
        assertEquals(256, matrix.getTileHeight().intValue());
        assertEquals(131072, matrix.getMatrixWidth().intValue());
        assertEquals(65536, matrix.getMatrixHeight().intValue());

        TileReader tr = gpkg.reader(te, null, null, null, null, null, null);
        assertTrue(tr.hasNext());
        Tile candidate = null;
        while (tr.hasNext()) {
            Tile tile = tr.next();
            assertEquals(16, tile.getZoom().intValue());
            if (tile.getRow() == 32768 && tile.getColumn() == 65536) {
                candidate = tile;
            }
        }
        tr.close();
        gpkg.close();

        // this tile is in the middle of the dataset, has red fill due to the env variable
        assertNotNull(candidate);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(candidate.getData()));
        assertEquals(Color.RED, new Color(image.getRGB(128, 128))); // lake with env
        assertEquals(new Color(170, 170, 170), new Color(image.getRGB(128, 192))); // forest
    }

    @Test
    public void testProcessCancellation() throws Exception {
        assertProcessTimeout(getXMLInnerRequestTimeout());
    }

    @Test
    public void testProcessCancellationWithGridset() throws Exception {
        assertProcessTimeout(getXMLInnerRequestTimeoutGridset());
    }

    private void assertProcessTimeout(String innerXML) throws Exception {
        // grab logger
        AtomicBoolean foundExecution = new AtomicBoolean(false);
        AtomicBoolean foundCancel = new AtomicBoolean(false);
        AppenderSkeleton appender =
                new AppenderSkeleton() {
                    @Override
                    public void close() {}

                    @Override
                    public boolean requiresLayout() {
                        return false;
                    }

                    @Override
                    protected void append(LoggingEvent event) {
                        String message = event.getRenderedMessage();

                        if (message != null) {
                            if (message.contains("Creating tile entry")) foundExecution.set(true);
                            else if (message.contains("request has been canceled"))
                                foundCancel.set(true);
                        }
                    }
                };
        // relevant logging happens on the get tiled map logger
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("org.geoserver.tiles");
        logger.setLevel(Level.DEBUG);
        logger.removeAllAppenders();
        logger.addAppender(appender);

        // start
        String xml = getAsynchXml(innerXML);
        Document dom = postAsDOM("wps", xml);

        // grab execution id
        String statusLocation = getStatusLocation(dom);
        Map<String, Object> kvp = KvpUtils.parseQueryString(statusLocation);
        String executionId = (String) kvp.get("executionId");

        // wait for execution to kick in
        waitForCondition(foundExecution);
        assertTrue(foundExecution.get());

        // dismiss
        getAsDOM("wps?service=WPS&version=1.0.0&request=Dismiss&executionId=" + executionId);

        // wait for cancellation
        waitForCondition(foundCancel);

        // make sure it actually got cancelled
        assertTrue(foundCancel.get());
    }

    private void waitForCondition(AtomicBoolean condition) {
        long start = System.currentTimeMillis();
        while (!condition.get() && (System.currentTimeMillis() - start) < MAX_ASYNCH_WAIT)
            ; // nothing to do
    }

    private String getStatusLocation(Document dom) throws XpathException {
        String fullStatusLocation = getFullStatusLocation(dom);
        String statusLocation = fullStatusLocation.substring(fullStatusLocation.indexOf('?') - 3);
        return statusLocation;
    }

    private String getFullStatusLocation(Document dom) throws XpathException {
        assertXpathExists("//wps:ProcessAccepted", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        return xpath.evaluate("//wps:ExecuteResponse/@statusLocation", dom);
    }

    public String getXml(String xmlInnerRequest) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3"
                + ".org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis"
                + ".net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis"
                + ".net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www"
                + ".opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>contents</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:ComplexData mimeType=\"text/xml; "
                + "subtype=geoserver/geopackage\"><![CDATA["
                + xmlInnerRequest
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

    public String getAsynchXml(String xmlInnerRequest) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3"
                + ".org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis"
                + ".net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis"
                + ".net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www"
                + ".opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>contents</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:ComplexData mimeType=\"text/xml; "
                + "subtype=geoserver/geopackage\"><![CDATA["
                + xmlInnerRequest
                + "]]></wps:ComplexData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "  <wps:ResponseForm>"
                + "    <wps:ResponseDocument storeExecuteResponse='true' status='true'>"
                + "      <wps:Output>"
                + "        <ows:Identifier>geopackage</ows:Identifier>"
                + "      </wps:Output>"
                + "    </wps:ResponseDocument>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }

    private String getXMLInnerRequest() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
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
                + "    <metadata>true</metadata>"
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
                + "</geopackage>";
    }

    private String getXMLInnerRequestParametersGroups() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <tiles name=\"groupenv\" identifier=\"ge\">"
                + "    <description>A group, and a style with env params</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-0.0014</minx>"
                + "      <maxx>0.004</maxx>"
                + "      <miny>-0.0024</miny>"
                + "      <maxy>0.0016</maxy>"
                + "    </bbox>"
                + "    <layers>groupenv</layers>"
                + "    <styles></styles>"
                + "    <format>png</format>"
                + "    <bgcolor>222222</bgcolor>"
                + "    <transparent>false</transparent>"
                + "    <coverage>"
                + "      <minZoom>16</minZoom>"
                + "      <maxZoom>17</maxZoom>"
                + "    </coverage>"
                + "    <parameters>"
                + "      <parameter name=\"env\">fill:#FF0000</parameter>"
                + "    </parameters>"
                + "  </tiles>"
                + "</geopackage>";
    }

    private String getXMLInnerRequestTimeout() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <tiles name=\"groupenv\" identifier=\"ge\">"
                + "    <description>A group, and a style with env params</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-0.0014</minx>"
                + "      <maxx>0.004</maxx>"
                + "      <miny>-0.0024</miny>"
                + "      <maxy>0.0016</maxy>"
                + "    </bbox>"
                + "    <layers>groupenv</layers>"
                + "    <styles></styles>"
                + "    <format>png</format>"
                + "    <bgcolor>222222</bgcolor>"
                + "    <transparent>false</transparent>"
                + "    <coverage>"
                + "      <minZoom>16</minZoom>"
                // too many levels to complete
                + "      <maxZoom>30</maxZoom>"
                + "    </coverage>"
                + "  </tiles>"
                + "</geopackage>";
    }

    private String getXMLInnerRequestTimeoutGridset() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
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
                + "      <maxZoom>30</maxZoom>"
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
                + "</geopackage>";
    }

    private String getXMLInnerRequestOverviews() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <features name=\"roads\" identifier=\"roads\">"
                + "    <featuretype>cite:RoadSegments</featuretype>"
                + "    <overviews>"
                + "      <overview>"
                + "         <name>roads_ov1</name>"
                + "         <distance>0.00001</distance>"
                + "      </overview>"
                + "      <overview>"
                + "         <name>roads_ov2</name>"
                + "         <distance>0.0001</distance>"
                + "         <filter xmlns:fes=\"http://www.opengis.net/fes/2.0\">"
                + "            <fes:PropertyIsEqualTo>"
                + "            <fes:ValueReference>NAME</fes:ValueReference>"
                + "            <fes:Literal>Main Street</fes:Literal>"
                + "            </fes:PropertyIsEqualTo>"
                + "         </filter>"
                + "      </overview>"
                + "    </overviews>"
                + "  </features>"
                + "</geopackage>";
    }

    private String getXMLInnerRequestSortNameFID() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <features name=\"roads\" identifier=\"roads\">"
                + "    <featuretype>cite:RoadSegments</featuretype>"
                + "    <sort xmlns:fes=\"http://www.opengis.net/fes/2.0\">"
                + "      <fes:SortProperty>"
                + "         <fes:ValueReference>NAME</fes:ValueReference>"
                + "      </fes:SortProperty>"
                + "      <fes:SortProperty>"
                + "         <fes:ValueReference>FID</fes:ValueReference>"
                + "         <fes:SortOrder>DESC</fes:SortOrder>"
                + "      </fes:SortProperty>"
                + "    </sort>"
                + "  </features>"
                + "</geopackage>";
    }

    private String getXMLInnerRequestSortGeoHash() {
        return "<geopackage name=\"test\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <features name=\"roads\" identifier=\"roads\">"
                + "    <featuretype>cite:RoadSegments</featuretype>"
                + "    <sort xmlns:fes=\"http://www.opengis.net/fes/2.0\">"
                + "      <fes:SortProperty>"
                + "         <fes:ValueReference>the_geom</fes:ValueReference>"
                + "      </fes:SortProperty>"
                + "      <fes:SortProperty>"
                + "         <fes:ValueReference>NAME</fes:ValueReference>"
                + "      </fes:SortProperty>"
                + "    </sort>"
                + "  </features>"
                + "</geopackage>";
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

    @Test
    public void testScaleDenominatorDistanceProjected() throws FactoryException {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:32632", true);
        double originalDistance = 100;
        double scale = GeoPackageProcess.distanceToScale(crs, originalDistance);
        assertEquals(357142, scale, 1d);
        double distance = GeoPackageProcess.scaleToDistance(crs, scale);
        assertEquals(originalDistance, distance, 0.1d);
    }

    @Test
    public void testScaleDenominatorDistanceGeographic() throws FactoryException {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", true);
        double originalDistance = 0.001; // in degrees
        double scale = GeoPackageProcess.distanceToScale(crs, originalDistance);
        assertEquals(397569, scale, 1d);
        double distance = GeoPackageProcess.scaleToDistance(crs, scale);
        assertEquals(originalDistance, distance, 1e-6);
    }
}
