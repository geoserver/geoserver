/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Resource;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ImageTest extends ImagesTestSupport {

    @Test
    public void testWaterTempImages() throws Exception {
        String waterTemp = getLayerId(WATER_TEMP);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/images/collections/"
                                + waterTemp
                                + "/images?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME),
                        200);

        // get the feature id by the properties, as the harvesting order might change
        // with the file system
        String featureId =
                readSingle(
                        json,
                        "features[?(@.properties.datetime == '2008-11-01T00:00:00Z' && @.properties.elevation == 100)].id");
        assertThat(featureId, startsWith("watertemp."));
        // check properties (location is gone, time torned to "datetime")
        String basePath = "features[?(@.id == '" + featureId + "')]";
        checkWaterTemp(json, basePath);

        // also check the links (will fail if the link is not available
        assertThat(
                json.read("links[?(@.rel=='item')].href"),
                Matchers.hasItems(
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.1?f=application%2Fstac%2Bjson",
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.2?f=application%2Fstac%2Bjson",
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.3?f=application%2Fstac%2Bjson",
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.4?f=application%2Fstac%2Bjson"));
    }

    public void checkWaterTemp(DocumentContext json, String basePath) {
        assertThat(readSingle(json, basePath + ".properties.size()"), equalTo(2));
        // check mandatory STAC item properties
        assertThat(readSingle(json, basePath + ".stac_version"), equalTo("0.8.0"));
        assertThat(readSingle(json, basePath + ".collection"), equalTo("sf:watertemp"));
        assertThat(
                readSingle(json, basePath + ".bbox"),
                Matchers.hasItems(
                        closeTo(0.23722069, 1e-6),
                        closeTo(40.56208, 1e-6),
                        closeTo(14.592757, 1e-6),
                        closeTo(44.558083, 1e-6)));

        // check links (mandatory in STAC)
        assertThat(
                readSingle(json, basePath + ".links[?(@.rel=='self')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.1?f=application%2Fstac%2Bjson"));
        assertThat(
                readSingle(
                        json,
                        basePath + ".links[?(@.rel=='alternate' && @.type =='text/html')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.1?f=text%2Fhtml"));
        // check the assets (also mandatory in STAC)
        assertThat(
                readSingle(json, basePath + ".assets[?(@.type=='image/tiff')].title"),
                equalTo("NCOM_wattemp_100_20081101T0000000_12.tiff"));
        assertThat(
                readSingle(json, basePath + ".assets[?(@.type=='image/tiff')].href"),
                allOf(
                        startsWith(
                                "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images/watertemp.1/assets/"),
                        endsWith("-NCOM_wattemp_100_20081101T0000000_12.tiff")));
    }

    @Test
    public void testImageNotFound() throws Exception {
        String waterTemp = getLayerId(WATER_TEMP);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/images/collections/"
                                + waterTemp
                                + "/images/"
                                + "floopaloo"
                                + "?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME));
        assertThat(response.getStatus(), equalTo(404));
    }

    @Test
    public void testWaterTempSingleImage() throws Exception {
        // get the feature id we want to query, as the harvesting order might change with the file
        // system
        String waterTemp = getLayerId(WATER_TEMP);
        DocumentContext collectionJson =
                getAsJSONPath(
                        "ogc/images/collections/"
                                + waterTemp
                                + "/images?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME),
                        200);
        String featureId =
                readSingle(
                        collectionJson,
                        "features[?(@.properties.datetime == '2008-11-01T00:00:00Z' && @.properties.elevation == 100)].id");
        assertThat(featureId, startsWith("watertemp."));

        // get the single feature now by service call
        // system
        DocumentContext json =
                getAsJSONPath(
                        "ogc/images/collections/"
                                + waterTemp
                                + "/images/"
                                + featureId
                                + "?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME),
                        200);
    }

    @Test
    public void testWaterTempGetAsset() throws Exception {
        // get the feature id we want to query, as the harvesting order might change with the file
        // system
        String waterTemp = getLayerId(WATER_TEMP);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/images/collections/"
                                + waterTemp
                                + "/images?f="
                                + ResponseUtils.urlEncode(STACItemFeaturesResponse.MIME),
                        200);
        String featureId =
                readSingle(
                        json,
                        "features[?(@.properties.datetime == '2008-11-01T00:00:00Z' && @.properties.elevation == 100)].id");
        assertThat(featureId, startsWith("watertemp."));

        String basePath = "features[?(@.id == '" + featureId + "')]";
        assertThat(
                readSingle(json, basePath + ".assets[?(@.type=='image/tiff')].title"),
                equalTo("NCOM_wattemp_100_20081101T0000000_12.tiff"));
        String assetHRef = readSingle(json, basePath + ".assets[?(@.type=='image/tiff')].href");
        String testAssetHRef = assetHRef.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(testAssetHRef);
        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getContentType(), equalTo("image/tiff"));
        // go get the original file
        Resource mosaicDirectory = getDataDirectory().get("watertemp");
        Resource referenceFile = mosaicDirectory.get("NCOM_wattemp_100_20081101T0000000_12.tiff");
        try (InputStream referenceIs = referenceFile.in()) {
            IOUtils.contentEquals(
                    referenceIs, new ByteArrayInputStream(response.getContentAsByteArray()));
        }
    }
}
