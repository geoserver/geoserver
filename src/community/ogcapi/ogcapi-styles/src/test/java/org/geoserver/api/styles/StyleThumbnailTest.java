/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geotools.image.test.ImageAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class StyleThumbnailTest extends StylesTestSupport {

    public static final String BUILDINGS_LAKES = "buildingsLakes";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle(
                BUILDINGS_LAKES, "buildingsLakes.sld", StyleThumbnailTest.class, getCatalog());
    }

    @Test
    public void testThumbnailLakes() throws Exception {
        BufferedImage image = getAsImage("ogc/styles/styles/Lakes/thumbnail", "image/png");
        ImageAssert.assertEquals(new File("./src/test/resources/thumbnails/lakes.png"), image, 0);
    }

    @Test
    public void testThumbnailLakesLink() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/styles/Lakes/metadata", 200);
        assertThat(
                readSingle(json, "links[?(@.rel=='preview')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/styles/styles/Lakes/thumbnail?f=image%2Fpng"));
    }

    @Test
    public void testThumbnailBuildingLakes() throws Exception {
        BufferedImage image = getAsImage("ogc/styles/styles/buildingsLakes/thumbnail", "image/png");
        ImageAssert.assertEquals(
                new File("./src/test/resources/thumbnails/buildingsLakes.png"), image, 0);
    }

    @Test
    public void testThumbnailBuildingLakesLink() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/styles/buildingsLakes/metadata", 200);
        assertThat(
                readSingle(json, "links[?(@.rel=='preview')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/styles/styles/buildingsLakes/thumbnail?f=image%2Fpng"));
    }

    @Test
    public void testThumbnailPolygonCommentNoLink() throws Exception {
        // a style with no layers associated
        DocumentContext json =
                getAsJSONPath("ogc/styles/styles/" + POLYGON_COMMENT + "/metadata", 200);
        List items = json.read("[?(@.links.rel=='preview')]");
        assertThat(items.isEmpty(), Matchers.equalTo(true));
    }
}
