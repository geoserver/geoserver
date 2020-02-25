/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import static org.geoserver.api.changeset.ChangesetIndexProvider.INITIAL_STATE;
import static org.geoserver.ows.util.ResponseUtils.urlEncode;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.layer.TileLayer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ChangesetTest extends OGCApiTestSupport {

    static final double EPS = 1e-3;
    public static final String S2_STORE = "s2";
    static final QName S2 = new QName(MockTestData.SF_URI, S2_STORE, MockTestData.SF_PREFIX);
    static final QName S2_SCALES =
            new QName(MockTestData.SF_URI, "s2Scales", MockTestData.SF_PREFIX);
    public static final String RASTER_SCALES_STYLE = "raster_scales";
    public static final String S2_LAYER = "sf:s2";
    public static final String S2_SCALES_LAYER = "sf:s2Scales";
    public static final String RASTER_STYLE = "raster";
    private File s2TestData;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no canned test data
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle(
                RASTER_SCALES_STYLE, "raster_scales.sld", ChangesetTest.class, getCatalog());
    }

    @Before
    public void setupBaseMosaic() throws Exception {
        File s2Directory = getDataDirectory().get(S2_STORE).dir();

        // clean up if the store is there
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getStoreByName(S2_STORE, CoverageStoreInfo.class);
        if (store != null) {
            new CascadeDeleteVisitor(catalog).visit(store);
            FileUtils.deleteDirectory(s2Directory);
        }

        // prepare a mosaic with just one tile
        s2Directory.mkdir();
        this.s2TestData = new File("src/test/resources/org/geoserver/api/changeset/hetero_s2");
        FileUtils.copyFileToDirectory(new File(s2TestData, "g1.tif"), s2Directory);
        FileUtils.copyFileToDirectory(new File(s2TestData, "indexer.properties"), s2Directory);
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName(MockData.SF_PREFIX));
        CoverageStoreInfo newStore = cb.buildCoverageStore(S2_STORE);
        newStore.setURL(URLs.fileToUrl(s2Directory).toExternalForm());
        newStore.setType(new ImageMosaicFormat().getName());
        catalog.add(newStore);
        cb.setStore(newStore);
        CoverageInfo ci = cb.buildCoverage();
        catalog.add(ci);
        LayerInfo layer = cb.buildLayer(ci);
        catalog.add(layer);

        // configure tile caching for it
        GWC gwc = GeoServerExtensions.bean(GWC.class);
        TileLayer tileLayer = gwc.getTileLayer(catalog.getLayerByName(getLayerId(S2)));
        XMLGridSubset editableWgs84 = new XMLGridSubset(tileLayer.removeGridSubset("EPSG:4326"));
        editableWgs84.setZoomStart(0);
        editableWgs84.setZoomStop(11);
        tileLayer.addGridSubset(editableWgs84.getGridSubSet(gwc.getGridSetBroker()));
        XMLGridSubset editableWebMercator =
                new XMLGridSubset(tileLayer.removeGridSubset("EPSG:900913"));
        editableWebMercator.setZoomStart(0);
        editableWebMercator.setZoomStop(11);
        tileLayer.addGridSubset(editableWebMercator.getGridSubSet(gwc.getGridSetBroker()));
        gwc.save(tileLayer);

        // setup a variant with scale range limits in the style
        CoverageInfo ciScales = cb.buildCoverage();
        ciScales.setName("s2Scales");
        catalog.add(ciScales);
        LayerInfo layerScales = cb.buildLayer(ciScales);
        layerScales.setDefaultStyle(catalog.getStyleByName(RASTER_SCALES_STYLE));
        catalog.add(layerScales);
    }

    @Test
    public void testGetSummarySingle4326() throws Exception {
        // upload single image
        uploadImage("g2.tif", S2);

        DocumentContext doc = getChangesAsJSONPath(S2_LAYER, RASTER_STYLE, "EPSG:4326", null);

        // the doc contains the requested checkpoint
        assertThat(doc.read("checkpoint"), equalTo(INITIAL_STATE));
        assertThat(doc.read("summaryOfChangedItems[0].priority"), equalTo("medium"));
        // area modified is small, for the zoom levels available
        assertThat(doc.read("summaryOfChangedItems[0].count"), equalTo(18));
        // single modified extent
        assertThat(doc.read("extentOfChangedItems.size()"), equalTo(1));
        assertThat(
                doc.read("extentOfChangedItems[0].crs"),
                equalTo("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        assertThat(doc.read("extentOfChangedItems[0].bbox[0]"), closeTo(11.683611, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[1]"), closeTo(47.63776, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[2]"), closeTo(11.861294, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[3]"), closeTo(47.754253, EPS));
    }

    @Test
    public void testGetSummarySingle4326Scales() throws Exception {
        // upload single image
        uploadImage("g2.tif", S2_SCALES);

        DocumentContext doc =
                getChangesAsJSONPath(S2_SCALES_LAYER, RASTER_SCALES_STYLE, "EPSG:4326", null);

        // the doc contains the requested checkpoint
        assertThat(doc.read("checkpoint"), equalTo(INITIAL_STATE));
        assertThat(doc.read("summaryOfChangedItems[0].priority"), equalTo("medium"));
        // area modified is small, and the style further reduces the number of zoom levels caches to
        // two
        assertThat(doc.read("summaryOfChangedItems[0].count"), equalTo(8));
        // single modified extent
        assertThat(doc.read("extentOfChangedItems.size()"), equalTo(1));
        assertThat(
                doc.read("extentOfChangedItems[0].crs"),
                equalTo("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        assertThat(doc.read("extentOfChangedItems[0].bbox[0]"), closeTo(11.683611, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[1]"), closeTo(47.63776, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[2]"), closeTo(11.861294, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[3]"), closeTo(47.754253, EPS));
    }

    @Test
    public void testGetSummarySingle3857() throws Exception {
        // upload single image
        uploadImage("g2.tif", S2);

        DocumentContext doc = getChangesAsJSONPath(S2_LAYER, RASTER_STYLE, "EPSG:900913", null);
        checkSummarySingle3857(doc, 16);
    }

    public void checkSummarySingle3857(DocumentContext doc, int expectedChanges) {
        // the doc contains the requested checkpoint
        assertThat(doc.read("checkpoint"), equalTo(INITIAL_STATE));
        assertThat(doc.read("summaryOfChangedItems[0].priority"), equalTo("medium"));
        // area modified is small, for the zoom levels available
        assertThat(doc.read("summaryOfChangedItems[0].count"), equalTo(expectedChanges));
        // single modified extent
        assertThat(doc.read("extentOfChangedItems.size()"), equalTo(1));
        assertThat(
                doc.read("extentOfChangedItems[0].crs"), equalTo("urn:ogc:def:crs:EPSG::900913"));
        assertThat(doc.read("extentOfChangedItems[0].bbox[0]"), closeTo(1300613, 1));
        assertThat(doc.read("extentOfChangedItems[0].bbox[1]"), closeTo(6046801, 1));
        assertThat(doc.read("extentOfChangedItems[0].bbox[2]"), closeTo(1320393, 1));
        assertThat(doc.read("extentOfChangedItems[0].bbox[3]"), closeTo(6066068, 1));
    }

    @Test
    public void testGetPackageSingle3857() throws Exception {
        // upload single image
        uploadImage("g2.tif", S2);

        Map<String, byte[]> contents = getChangesAsZip(S2_LAYER, "EPSG:900913", null, RASTER_STYLE);
        System.out.println(contents.keySet());

        assertEquals(17, contents.size());
        assertThat(contents.keySet(), hasItem("changeset.json"));
        assertThat(contents.keySet(), hasItem("EPSG:900913/EPSG:900913:0/0/0.png"));

        // get the changeset
        JsonContext json =
                (JsonContext)
                        JsonPath.parse(new ByteArrayInputStream(contents.get("changeset.json")));
        checkSummarySingle3857(json, 16);

        // checking a few paths hand tested with a WMTS client, verifies y axis inversion from
        // internal vision
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/713/1090.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/714/1090.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/713/1091.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/714/1091.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:10/357/545.png"));

        // check that all files are actually PNGs, can be read, are not empty
        int countPNGs = 0;
        for (Map.Entry<String, byte[]> file : contents.entrySet()) {
            if (file.getKey().endsWith(".png")) {
                countPNGs++;
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getValue()));
                assertNotNull(image);
            }
        }

        // check a single tile contents
        BufferedImage image =
                ImageIO.read(
                        new ByteArrayInputStream(
                                contents.get("EPSG:900913/EPSG:900913:10" + "/357/545.png")));
        ImageAssert.assertEquals(
                new File("src/test/resources/org/geoserver/api/changeset/10_357_545.png"),
                image,
                100);
    }

    @Test
    public void testGetPackageSingle3857Scales() throws Exception {
        // upload single image
        uploadImage("g2.tif", S2_SCALES);

        Map<String, byte[]> contents =
                getChangesAsZip(S2_SCALES_LAYER, "EPSG:900913", null, RASTER_SCALES_STYLE);
        System.out.println(contents.keySet());

        assertEquals(14, contents.size());
        assertThat(contents.keySet(), hasItem("changeset.json"));
        // scale limitations, zoom level 0 is not there
        assertThat(contents.keySet(), not(hasItem("EPSG:900913/EPSG:900913:0/0/0.png")));

        // get the changeset
        JsonContext json =
                (JsonContext)
                        JsonPath.parse(new ByteArrayInputStream(contents.get("changeset.json")));
        // this layer has scale limitations and covers zoom level 11 and 12, thus different number
        // of changes
        checkSummarySingle3857(json, 13);

        // checking a few paths hand tested with a WMTS client, verifies y axis inversion from
        // internal vision
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/713/1090.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/714/1090.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/713/1091.png"));
        assertThat(contents, hasKey("EPSG:900913/EPSG:900913:11/714/1091.png"));

        // check that all files are actually PNGs, can be read, are not empty
        int countPNGs = 0;
        for (Map.Entry<String, byte[]> file : contents.entrySet()) {
            if (file.getKey().endsWith(".png")) {
                countPNGs++;
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getValue()));
                assertNotNull(image);
            }
        }
    }

    @Test
    public void testGetSummaryTwo4326() throws Exception {
        // upload two images (toghether they cover the same bbox as g2)
        uploadImage("g3.tif", S2);
        uploadImage("g4.tif", S2);

        DocumentContext doc = getChangesAsJSONPath(S2_LAYER, RASTER_STYLE, "EPSG:4326", null);

        // the doc contains the requested checkpoint
        assertThat(doc.read("checkpoint"), equalTo(INITIAL_STATE));
        assertThat(doc.read("summaryOfChangedItems[0].priority"), equalTo("medium"));
        // area modified is small, for the zoom levels available
        assertThat(doc.read("summaryOfChangedItems[0].count"), equalTo(18));
        // single modified extent
        assertThat(doc.read("extentOfChangedItems.size()"), equalTo(2));
        assertThat(
                doc.read("extentOfChangedItems[0].crs"),
                equalTo("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        assertThat(doc.read("extentOfChangedItems[0].bbox[0]"), closeTo(11.683482, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[1]"), closeTo(47.637856, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[2]"), closeTo(11.861166, EPS));
        assertThat(doc.read("extentOfChangedItems[0].bbox[3]"), closeTo(47.754345, EPS));
        assertThat(
                doc.read("extentOfChangedItems[1].crs"),
                equalTo("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        assertThat(doc.read("extentOfChangedItems[1].bbox[0]"), closeTo(11.683616, EPS));
        assertThat(doc.read("extentOfChangedItems[1].bbox[1]"), closeTo(47.63785, EPS));
        assertThat(doc.read("extentOfChangedItems[1].bbox[2]"), closeTo(11.8612995, EPS));
        assertThat(doc.read("extentOfChangedItems[1].bbox[3]"), closeTo(47.75434, EPS));
    }

    private void uploadImage(String fileName, QName layerName) throws Exception {
        String layerId = getLayerId(layerName);
        byte[] payload = getBytes(fileName);
        MockHttpServletResponse response =
                postAsServletResponse(
                        "ogc/images/collections/" + layerId + "/images?filename=" + fileName,
                        payload,
                        "image/tiff");
        assertThat(response.getStatus(), equalTo(201));
        // layer is stored in the S2 table of the mosaic index, even if we use two different
        // published layer names for it
        assertThat(
                response.getHeader("Location"),
                startsWith(
                        "http://localhost:8080/geoserver/ogc/images/collections/"
                                + urlEncode(layerId)
                                + "/images/s2."));

        // check it's really there
        DocumentContext json =
                getAsJSONPath(
                        response.getHeader("Location")
                                .substring("http://localhost:8080/geoserver/".length()),
                        200);
        assertThat(json.read("type"), equalTo("Feature"));
        assertThat(json.read("id"), startsWith("s2."));
        // in case of no date, the unix epoch is used
        assertThat(json.read("properties.datetime"), equalTo("1970-01-01T00:00:00Z"));
        assertThat(json.read("assets[0].href"), endsWith(fileName));
    }

    public byte[] getBytes(String file) throws IOException {
        byte[] payload = null;
        try (InputStream is = new FileInputStream(new File(s2TestData, file))) {
            payload = IOUtils.toByteArray(is);
        }
        return payload;
    }

    protected DocumentContext getChangesAsJSONPath(
            String collectionId, String styleName, String tileMatrixId, String extraParameters)
            throws Exception {
        // build the request and check the checkpoint
        String url =
                "ogc/tiles/collections/"
                        + collectionId
                        + "/map/"
                        + styleName
                        + "/tiles/"
                        + tileMatrixId
                        + "?f="
                        + urlEncode(ChangesetTilesService.CHANGESET_MIME);
        if (extraParameters != null) {
            url += "&" + extraParameters;
        }

        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        assertEquals(ChangesetTilesService.CHANGESET_MIME, response.getContentType());
        String lastCheckpoint = getLatestCheckpoint(collectionId);
        assertThat(response.getHeader(CheckpointCallback.X_CHECKPOINT), equalTo(lastCheckpoint));
        return getAsJSONPath(response);
    }

    protected Map<String, byte[]> getChangesAsZip(
            String collectionId, String tileMatrixId, String extraParameters, String styleName)
            throws Exception {
        // build the request and check the checkpoint
        String url =
                "ogc/tiles/collections/"
                        + collectionId
                        + "/map/"
                        + styleName
                        + "/tiles/"
                        + tileMatrixId
                        + "?f="
                        + urlEncode(ChangesetTilesService.ZIP_MIME);
        if (extraParameters != null) {
            url += "&" + extraParameters;
        }

        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        assertEquals(ChangesetTilesService.ZIP_MIME, response.getContentType());
        String lastCheckpoint = getLatestCheckpoint(collectionId);
        assertThat(response.getHeader(CheckpointCallback.X_CHECKPOINT), equalTo(lastCheckpoint));

        Map<String, byte[]> contents = new LinkedHashMap<>();
        try (ZipInputStream zis =
                new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] bytes = IOUtils.toByteArray(zis);
                contents.put(entry.getName(), bytes);
            }
        }

        return contents;
    }

    private String getLatestCheckpoint(String collectionId) throws IOException {
        ChangesetIndexProvider provider = GeoServerExtensions.bean(ChangesetIndexProvider.class);
        return provider.getLatestCheckpoint(getCatalog().getCoverageByName(collectionId));
    }
}
