/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;
import org.springframework.http.MediaType;

public class CollectionTest extends CoveragesTestSupport {

    private static final double EPS = 1e-6d;

    @Test
    public void testCollectionJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1/collections/rs:DEM", 200);

        checkDEMCoverage(json, "rs:DEM");
    }

    private void checkDEMCoverage(DocumentContext json, String expectedId) {
        assertEquals(expectedId, json.read("$.id", String.class));
        assertEquals(TAZDEM_TITLE, json.read("$.title", String.class));
        assertEquals(145, json.read("$.extent.spatial.bbox[0][0]", Double.class), EPS);
        assertEquals(-43, json.read("$.extent.spatial.bbox[0][1]", Double.class), EPS);
        assertEquals(146, json.read("$.extent.spatial.bbox[0][2]", Double.class), EPS);
        assertEquals(-41, json.read("$.extent.spatial.bbox[0][3]", Double.class), EPS);
        assertEquals(
                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                json.read("$.extent.spatial.crs", String.class));

        // check we have the expected number of links and they all use the right "rel"
        Collection<MediaType> formats = getCoverageFormats();
        assertThat(formats.size(), lessThanOrEqualTo(json.read("links.length()", Integer.class)));
        for (MediaType format : formats) {
            // check rel.
            List items = json.read("links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals(CollectionDocument.REL_COVERAGE, item.get("rel"));
        }
        // image/geotiff is available
        readSingle(json, "links[?(@.type=='image/geotiff')]");

        // check the CRS list, this coverage has a customized list
        List<String> crs = json.read("crs");
        assertThat(
                crs,
                hasItems(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/4326",
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/EPSG/0/3003"));
    }

    private List<MediaType> getCoverageFormats() {
        return GeoServerExtensions.bean(APIDispatcher.class, applicationContext)
                .getProducibleMediaTypes(CoveragesResponse.class, false);
    }

    @Test
    public void testCollectionVirtualWorkspace() throws Exception {
        DocumentContext json = getAsJSONPath("rs/ogc/coverages/v1/collections/DEM", 200);
        checkDEMCoverage(json, "DEM");

        // check workspace qualified link
        assertEquals(
                "http://localhost:8080/geoserver/rs/ogc/coverages/v1/collections/DEM/coverage?f=image%2Fgeotiff",
                readSingle(json, "$.links[?(@.type == 'image/geotiff')].href"));
    }

    @Test
    public void testCollectionYaml() throws Exception {
        String yaml = getAsString("ogc/coverages/v1/collections/rs:DEM?f=application/x-yaml");
        checkDEMCoverage(convertYamlToJsonPath(yaml), "rs:DEM");
    }

    @Test
    public void testCollectionHTML() throws Exception {
        org.jsoup.nodes.Document document =
                getAsJSoup("ogc/coverages/v1/collections/rs:DEM?f=html");

        String tazDemName = "rs:DEM";
        String tazDemHtmlId = tazDemName.replace(":", "__");

        // check title and description
        assertEquals(TAZDEM_TITLE, document.select("#" + tazDemHtmlId + "_title").text());
        assertEquals(
                TAZDEM_DESCRIPTION, document.select("#" + tazDemHtmlId + "_description").text());

        // check coverage links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/coverages/v1/collections/rs:DEM"
                        + "/coverage?f=image%2Fgeotiff",
                document.select("#html_" + tazDemHtmlId + "_link").attr("href"));

        // check temporal and spatial extent (time should not be there)
        assertEquals(
                "Geographic extents: 145, -43, 146, -41.",
                document.select("#" + tazDemHtmlId + "_spatial").text());
        assertEquals("", document.select("#" + tazDemHtmlId + "_temporal").text());
    }

    @Test
    public void testTemporalCollectionHTML() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, DimensionPresentation.LIST, null, null, null);
        org.jsoup.nodes.Document document =
                getAsJSoup("ogc/coverages/v1/collections/sf:timeseries?f=html");

        String id = getLayerId(TIMESERIES).replace(":", "__");

        // check temporal and spatial extent
        assertEquals(
                "Geographic extents: 0.237, 40.562, 14.593, 44.558.",
                document.select("#" + id + "_spatial").text());
        assertEquals(
                "Temporal extent: 2014-01-01T00:00:00Z/2019-01-01T00:00:00Z",
                document.select("#" + id + "_temporal").text());
    }
}
