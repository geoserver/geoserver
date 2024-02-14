/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport.setupBasicOpenSearch;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import net.minidev.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.hamcrest.Matchers;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class STACTestSupport extends OGCApiTestSupport {
    protected static final String STAC_TITLE = "STAC server title";

    /**
     * The EPS value to use for floating point comparisons. Matches precision of the expected values
     */
    protected static final double EPS = 1e-4;

    static TimeZone currentTimeZone;
    static Locale currentLocale;

    @BeforeClass
    public static void setupGMT() {
        currentLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        currentTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @AfterClass
    public static void resetTimeZone() {
        TimeZone.setDefault(currentTimeZone);
        Locale.setDefault(currentLocale);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no need for test data
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer gs = getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.setTitle(STAC_TITLE);
        service.getGlobalQueryables()
                .addAll(Arrays.asList("id", "geometry", "collection", "eo:cloud_cover"));
        gs.save(service);

        setupBasicOpenSearch(testData, getCatalog(), gs, false);

        // add the custom product class
        service.getProductClasses().add(JDBCOpenSearchAccessTest.GS_PRODUCT);
        gs.save(service);
    }

    @BeforeClass
    public static void checkOnLine() {
        GeoServerOpenSearchTestSupport.checkOnLine();
    }

    /**
     * Returns the {@link OpenSearchAccess} backing the OpenSearch/STAC services
     *
     * @return
     * @throws IOException
     */
    public OpenSearchAccess getOpenSearchAccess() throws IOException {
        OpenSearchAccessProvider provider =
                GeoServerExtensions.bean(OpenSearchAccessProvider.class);
        return provider.getOpenSearchAccess();
    }

    /**
     * Returns the {@link STACService}
     *
     * @return
     */
    public STACService getStacService() {
        return GeoServerExtensions.bean(STACService.class);
    }

    protected void assertTextContains(Elements elements, String selector, String text) {
        assertThat(elements.select(selector).text(), containsString(text));
    }

    /**
     * Copies the given template from the classpath to the data directory. Lookup is relative to the
     * test class.
     */
    protected void copyTemplate(String template) throws IOException {
        copyTemplate(template, "templates/ogc/stac/v1/");
    }

    /**
     * Copies the given template from the classpath to the data directory. Lookup is relative to the
     * test class.
     */
    protected void copyTemplate(String template, String targetPath) throws IOException {
        copyTemplate(template, "templates/ogc/stac/v1/", template);
    }

    /**
     * Copies the given template from the classpath to the data directory. Lookup is relative to the
     * test class. The target path is for when you want to prioritize a template in a specific
     * folder over an identically named template in a different folder.
     */
    protected void copyTemplate(String template, String targetPath, String targetFileName)
            throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();
        Resource target = dd.get(targetPath, targetFileName);
        if (getClass().getResource(template) == null)
            throw new IllegalArgumentException(
                    "Could not find " + template + " relative to " + getClass());
        try (InputStream is = getClass().getResourceAsStream(template);
                OutputStream os = target.out()) {
            IOUtils.copy(is, os);
        }
    }

    protected void copyTemplate(String template, InputStream is) throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();
        Resource target = dd.get("templates/ogc/stac/v1/", template);
        try (OutputStream os = target.out()) {
            IOUtils.copy(is, os);
        }
    }

    protected void checkLandsat8_02(DocumentContext l8_02) {
        // ... instrument related
        assertEquals("LANDSAT_8", l8_02.read("properties.platform"));
        List<String> instruments = l8_02.read("properties.instruments");
        assertThat(instruments, Matchers.containsInAnyOrder("OLI", "TIRS"));
        assertEquals("landsat8", l8_02.read("properties.constellation"));
        // creation and modification
        assertEquals("2017-02-26T10:24:58.000+00:00", l8_02.read("properties.created"));
        assertEquals("2017-02-28T10:24:58.000+00:00", l8_02.read("properties.updated"));

        // check bits unique to the LS8 template
        assertEquals(Integer.valueOf(30), l8_02.read("properties.gsd"));
        assertEquals("pre-collection", l8_02.read("properties['landsat:tier']"));

        // check one assets bit
        DocumentContext mtl = readContext(l8_02, "assets.MTL");
        String mtlLink =
                "https://landsat-pds.s3.us-west-2.amazonaws.com/c1/L8/218/077/LC08_L1TP_218077_20210511_20210511_01_T1/LC08_L1TP_218077_20210511_20210511_01_T1_MTL.txt";
        assertEquals(mtlLink, mtl.read("href"));
        assertEquals("text/plain", mtl.read("type"));

        // check extraction of full json objects using the jsonPointer
        DocumentContext mtlInLinks = readSingleContext(l8_02, "links[?(@.type == 'text/plain')]");
        assertEquals(mtlLink, mtlInLinks.read("href"));
        assertEquals("MTL Metadata", mtlInLinks.read("title"));

        // and of a single value
        DocumentContext customMtl = readSingleContext(l8_02, "links[?(@.rel == 'custom-mtl')]");
        assertEquals("MTL Metadata", customMtl.read("title"));
        assertEquals("text/csv", customMtl.read("type"));
    }

    /**
     * Checks the JSON representation of
     * S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04 using the common items
     * template
     *
     * @param s2Sample
     */
    protected void checkSentinel2Sample(DocumentContext s2Sample) {
        // ... geometry
        assertEquals("Polygon", s2Sample.read("geometry.type"));
        JSONArray coordinates = s2Sample.read("geometry.coordinates");
        JSONArray shell = (JSONArray) coordinates.get(0);
        assertPoint(-117.9694, 33.3476, (JSONArray) shell.get(0));
        assertPoint(-117.9806, 34.3377, (JSONArray) shell.get(1));
        assertPoint(-119.1738, 34.3224, (JSONArray) shell.get(2));
        assertPoint(-119.1489, 33.3328, (JSONArray) shell.get(3));
        assertPoint(-117.9694, 33.3476, (JSONArray) shell.get(4));
        // ... bbox
        assertEquals(-119.17378, s2Sample.read("bbox[0]", Double.class), EPS);
        assertEquals(33.332767, s2Sample.read("bbox[1]", Double.class), EPS);
        assertEquals(-117.969376, s2Sample.read("bbox[2]", Double.class), EPS);
        assertEquals(34.337738, s2Sample.read("bbox[3]", Double.class), EPS);
        // ... time range (single value)
        assertEquals("2017-03-08T18:54:21.026+00:00", s2Sample.read("properties.datetime"));
        // ... instrument related
        assertEquals("sentinel-2a", s2Sample.read("properties.platform"));
        assertEquals("sentinel2", s2Sample.read("properties.constellation"));
        List<String> instruments = s2Sample.read("properties.instruments");
        assertThat(instruments, Matchers.containsInAnyOrder("MSI"));
        // ... eo
        assertEquals(Integer.valueOf(7), s2Sample.read("properties.eo:cloud_cover", Integer.class));
        // ... links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2",
                readSingle(s2Sample, "links[?(@.rel == 'collection')].href"));
        assertEquals(
                "application/json", readSingle(s2Sample, "links[?(@.rel == 'collection')].type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1",
                readSingle(s2Sample, "links[?(@.rel == 'root')].href"));
        assertEquals("application/json", readSingle(s2Sample, "links[?(@.rel == 'root')].type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                readSingle(s2Sample, "links[?(@.rel == 'self')].href"));
        assertEquals(
                "application/geo+json", readSingle(s2Sample, "links[?(@.rel == 'self')].type"));
    }

    /** Checks the array contains the given coordinates in <code>x, y</code> order. */
    protected void assertPoint(double x, double y, JSONArray coordinate) {
        assertEquals(x, (Double) coordinate.get(0), EPS);
        assertEquals(y, (Double) coordinate.get(1), EPS);
    }
}
