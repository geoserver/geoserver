/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.util.IOUtils;
import org.geotools.util.URLs;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class TransactionTest extends ImagesTestSupport {

    protected static QName WATER_TEMP2 =
            new QName(MockData.SF_URI, "watertemp2", MockData.SF_PREFIX);

    @Before
    public void resetLayer() throws Exception {
        // wipe out everything under "mosaic"
        CoverageInfo coverage =
                getCatalog().getResourceByName(WATER_TEMP2.getLocalPart(), CoverageInfo.class);
        if (coverage != null) {
            getCatalog().remove(coverage);
            removeStore(
                    coverage.getStore().getWorkspace().getName(), coverage.getStore().getName());
        }

        // setup the mosaic by hand
        Resource watertemp2 = getDataDirectory().get("watertemp2");
        if (watertemp2.getType() == Resource.Type.DIRECTORY) {
            watertemp2.delete();
        }
        watertemp2.dir();
        try (ZipInputStream zis =
                new ZipInputStream(TransactionTest.class.getResourceAsStream("watertemp2.zip"))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(watertemp2.dir(), zipEntry.getName());
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    IOUtils.copy(zis, fos);
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName(WATER_TEMP2.getPrefix()));
        CoverageStoreInfo store = cb.buildCoverageStore(WATER_TEMP2.getLocalPart());
        store.setURL(URLs.fileToUrl(watertemp2.dir()).toString());
        store.setType("ImageMosaic");
        catalog.add(store);
        cb.setStore(store);
        coverage = cb.buildCoverage("watertemp2");
        catalog.add(coverage);

        // check it's actually there
        assertThat(catalog.getCoverageByName(getLayerId(WATER_TEMP2)), CoreMatchers.notNullValue());
    }

    @Test
    public void testResetState() throws Exception {
        String waterTemp2 = getLayerId(WATER_TEMP2);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/images/collections/"
                                + waterTemp2
                                + "/images?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME),
                        200);

        assertThat(json.read("features.size()"), equalTo(1));
    }

    @Test
    public void testUploadImagePost() throws Exception {
        String waterTemp2 = getLayerId(WATER_TEMP2);
        String fileName = "NCOM_wattemp_100_20081101T0000000_12.tiff";
        byte[] payload = getBytes(fileName);
        MockHttpServletResponse response =
                postAsServletResponse(
                        "ogc/images/collections/" + waterTemp2 + "/images?filename=" + fileName,
                        payload,
                        "image/tiff");
        assertThat(response.getStatus(), equalTo(201));
        assertThat(
                response.getHeader("Location"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp2/images/watertemp2.2"));

        // check it's really there
        DocumentContext json =
                getAsJSONPath(
                        "ogc/images/collections/"
                                + waterTemp2
                                + "/images/watertemp2.2?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME),
                        200);
        assertThat(json.read("type"), equalTo("Feature"));
        assertThat(json.read("id"), equalTo("watertemp2.2"));
        assertThat(json.read("properties.datetime"), equalTo("2008-11-01T00:00:00Z"));
        assertThat(json.read("properties.elevation"), equalTo(100));
        assertThat(
                json.read("assets[0].href"), endsWith("NCOM_wattemp_100_20081101T0000000_12.tiff"));
    }

    @Test
    public void testDeleteImage() throws Exception {
        // add an image
        testUploadImagePost();

        // remove it
        String waterTemp2 = getLayerId(WATER_TEMP2);

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        "ogc/images/collections/" + waterTemp2 + "/images/watertemp2.2");
        assertThat(response.getStatus(), equalTo(200));

        // check it's gone
        response =
                getAsServletResponse(
                        "ogc/images/collections/"
                                + waterTemp2
                                + "/images/watertemp2.2?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME));
        assertThat(response.getStatus(), equalTo(404));
    }

    public byte[] getBytes(String file) throws IOException {
        byte[] payload = null;
        try (InputStream is = TransactionTest.class.getResourceAsStream(file)) {
            payload = IOUtils.toByteArray(is);
        }
        return payload;
    }
}
