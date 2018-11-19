/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;

public class WMSRequestsTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetGetMapUrlWithDimensions() throws Exception {
        GetMapRequest request = createGetMapRequest(MockData.TASMANIA_DEM);
        KvpMap rawKvp = new KvpMap(request.getRawKvp());
        rawKvp.put("layers", request.getLayers().get(0).getName());
        rawKvp.put("time", "2017-04-07T19:56:00.000Z");
        rawKvp.put("elevation", "1013.2");
        rawKvp.put("dim_my_dimension", "010");
        request.setRawKvp(rawKvp);
        request.setFormat(DefaultWebMapService.FORMAT);
        DefaultWebMapService.autoSetBoundsAndSize(request);
        String url =
                WMSRequests.getGetMapUrl(
                        request, request.getLayers().get(0).getName(), 0, null, null, null);
        url = URLDecoder.decode(url, "UTF-8");
        assertTrue(
                "Missing time in GetMap URL: " + url,
                url.contains("&time=2017-04-07T19:56:00.000Z"));
        assertTrue("Missing elevation in GetMap URL: " + url, url.contains("&elevation=1013.2"));
        assertTrue(
                "Missing custom dimension in GetMap URL: " + url,
                url.contains("&dim_my_dimension=010"));
    }

    @Test
    public void testGetGetMapUrlWithSingleLayer() {
        GetMapRequest request = initGetMapRequest(MockData.LAKES);
        request.getRawKvp().put("cql_filter", "fid='123'");
        List<String> urls = getGetMapUrls(request);
        assertEquals(1, urls.size());
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(0),
                urls.get(0).contains("&cql_filter=fid='123'&"));
    }

    @Test
    public void testGetGetMapUrlWithMultipleLayers() {
        GetMapRequest request = initGetMapRequest(MockData.LAKES, MockData.TASMANIA_DEM);
        request.getRawKvp().put("cql_filter", "fid='123';INCLUDE");
        List<String> urls = getGetMapUrls(request);
        assertEquals(2, urls.size());
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(0),
                urls.get(0).contains("&cql_filter=fid='123'&"));
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(1),
                urls.get(1).contains("&cql_filter=INCLUDE&"));
    }

    @Test
    public void testGetGetMapUrlWithSingleLayerGroup() {
        GetMapRequest request = initGetMapRequest(MockData.LAKES, MockData.FORESTS);
        request.getRawKvp().put("layers", NATURE_GROUP);
        request.getRawKvp().put("cql_filter", "name LIKE 'BLUE%'");
        List<String> urls = getGetMapUrls(request);
        assertEquals(2, urls.size());
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(0),
                urls.get(0).contains("&cql_filter=name LIKE 'BLUE%'&"));
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(1),
                urls.get(1).contains("&cql_filter=name LIKE 'BLUE%'&"));
    }

    @Test
    public void testGetGetMapUrlWithLayerGroupAndLayers() {
        GetMapRequest request =
                initGetMapRequest(
                        MockData.LAKES, MockData.LAKES, MockData.FORESTS, MockData.TASMANIA_DEM);
        request.getRawKvp()
                .put(
                        "layers",
                        request.getLayers().get(0).getName()
                                + ','
                                + NATURE_GROUP
                                + ','
                                + request.getLayers().get(3).getName());
        request.getRawKvp().put("cql_filter", "fid='123';name LIKE 'BLUE%';INCLUDE");
        List<String> urls = getGetMapUrls(request);
        assertEquals(4, urls.size());
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(0),
                urls.get(0).contains("&cql_filter=fid='123'&"));
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(1),
                urls.get(1).contains("&cql_filter=name LIKE 'BLUE%'&"));
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(2),
                urls.get(2).contains("&cql_filter=name LIKE 'BLUE%'&"));
        assertTrue(
                "Incorrect cql_filter in GetMap URL: " + urls.get(3),
                urls.get(3).contains("&cql_filter=INCLUDE&"));
    }

    @SuppressWarnings("unchecked")
    private GetMapRequest initGetMapRequest(QName... names) {
        GetMapRequest request = createGetMapRequest(names);
        request.setRawKvp(new KvpMap(request.getRawKvp()));
        String layers =
                request.getLayers()
                        .stream()
                        .map(MapLayerInfo::getName)
                        .collect(Collectors.joining(","));
        request.getRawKvp().put("layers", layers);
        request.setFormat(DefaultWebMapService.FORMAT);
        DefaultWebMapService.autoSetBoundsAndSize(request);
        return request;
    }

    private static List<String> getGetMapUrls(GetMapRequest request) {
        List<String> urls = new ArrayList<>(request.getLayers().size());
        for (int i = 0; i < request.getLayers().size(); i++) {
            String name = request.getLayers().get(i).getName();
            String url = WMSRequests.getGetMapUrl(request, name, i, null, null, null);
            urls.add(ResponseUtils.urlDecode(url));
        }
        return urls;
    }
}
