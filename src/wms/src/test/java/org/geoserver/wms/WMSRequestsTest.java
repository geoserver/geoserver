/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.net.URL;
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

    @Test
    public void testGetGetMapUrlAllWithDimensions() {
        GetMapRequest request = initGetMapRequest(MockData.TASMANIA_DEM);
        request.getRawKvp().put("time", "2017-04-07T19:56:00.000Z");
        request.getRawKvp().put("elevation", "1013.2");
        request.getRawKvp().put("dim_my_dimension", "010");
        String url = getGetMapUrl(request);
        assertThat(url, containsString("&time=2017-04-07T19:56:00.000Z&"));
        assertThat(url, containsString("&elevation=1013.2&"));
        assertThat(url, containsString("&dim_my_dimension=010&"));
    }

    @Test
    public void testGetGetMapUrlWithDimensions() {
        GetMapRequest request = initGetMapRequest(MockData.TASMANIA_DEM);
        request.getRawKvp().put("time", "2017-04-07T19:56:00.000Z");
        request.getRawKvp().put("elevation", "1013.2");
        request.getRawKvp().put("dim_my_dimension", "010");
        List<String> urls = getGetMapUrls(request);
        assertEquals(1, urls.size());
        assertThat(urls.get(0), containsString("&time=2017-04-07T19:56:00.000Z&"));
        assertThat(urls.get(0), containsString("&elevation=1013.2&"));
        assertThat(urls.get(0), containsString("&dim_my_dimension=010&"));
    }

    @Test
    public void testGetGetMapUrlWithSingleLayer() throws Exception {
        GetMapRequest request = initGetMapRequest(MockData.LAKES);
        request.getRawKvp().put("cql_filter", "fid='123'");
        request.setExceptions("INIMAGE");
        request.setRemoteOwsType("WFS");
        request.setRemoteOwsURL(new URL("https://foo.com/geoserver/wfs"));
        request.setScaleMethod(ScaleComputationMethod.Accurate);
        List<String> urls = getGetMapUrls(request);
        assertEquals(1, urls.size());
        assertThat(urls.get(0), containsString("&cql_filter=fid='123'&"));
        assertThat(urls.get(0), containsString("&remote_ows_type=WFS&"));
        assertThat(urls.get(0), containsString("&remote_ows_url=https://foo.com/geoserver/wfs&"));
        assertThat(urls.get(0), containsString("&scalemethod=Accurate"));
    }

    @Test
    public void testGetGetMapUrlWithMultipleLayers() {
        GetMapRequest request = initGetMapRequest(MockData.LAKES, MockData.TASMANIA_DEM);
        request.getRawKvp().put("cql_filter", "fid='123';INCLUDE");
        request.getRawKvp().put("bgcolor", "0x808080");
        request.setStyleFormat("ysld");
        request.setSldBody("foo");
        List<String> urls = getGetMapUrls(request);
        assertEquals(2, urls.size());
        assertThat(urls.get(0), containsString("&cql_filter=fid='123'&"));
        assertThat(urls.get(0), containsString("&bgcolor=0x808080&"));
        assertThat(urls.get(0), containsString("&style_format=ysld&"));
        assertThat(urls.get(0), containsString("&sld_body=foo"));
        assertThat(urls.get(1), containsString("&cql_filter=INCLUDE&"));
        assertThat(urls.get(1), containsString("&bgcolor=0x808080&"));
        assertThat(urls.get(1), containsString("&style_format=ysld&"));
        assertThat(urls.get(1), containsString("&sld_body=foo"));
    }

    @Test
    public void testGetGetMapUrlWithSingleLayerGroup() throws Exception {
        GetMapRequest request = initGetMapRequest(MockData.LAKES, MockData.FORESTS);
        request.getRawKvp().put("layers", NATURE_GROUP);
        request.getRawKvp().put("cql_filter", "name LIKE 'BLUE%'");
        request.getRawKvp().put("sortby", "name A");
        request.setStartIndex(25);
        request.setMaxFeatures(50);
        request.setStyleVersion("1.1.0");
        request.setSld(new URL("http://localhost/test.sld"));
        request.setValidateSchema(true);
        List<String> urls = getGetMapUrls(request);
        assertEquals(2, urls.size());
        assertThat(urls.get(0), containsString("&cql_filter=name LIKE 'BLUE%'&"));
        assertThat(urls.get(0), containsString("&sortby=name A&"));
        assertThat(urls.get(0), containsString("&startindex=25&"));
        assertThat(urls.get(0), containsString("&maxfeatures=50&"));
        assertThat(urls.get(0), containsString("&style_version=1.1.0&"));
        assertThat(urls.get(0), containsString("&validateschema=true&"));
        assertThat(urls.get(0), containsString("&sld=http://localhost/test.sld"));
        assertThat(urls.get(1), containsString("&cql_filter=name LIKE 'BLUE%'&"));
        assertThat(urls.get(1), containsString("&sortby=name A&"));
        assertThat(urls.get(1), containsString("&startindex=25&"));
        assertThat(urls.get(1), containsString("&maxfeatures=50&"));
        assertThat(urls.get(1), containsString("&style_version=1.1.0&"));
        assertThat(urls.get(1), containsString("&validateschema=true&"));
        assertThat(urls.get(1), containsString("&sld=http://localhost/test.sld"));
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
        request.getRawKvp().put("interpolations", ",,nearest neighbor");
        request.getRawKvp().put("sortby", "(fid)(name D)()");
        List<String> urls = getGetMapUrls(request);
        assertEquals(4, urls.size());
        assertThat(urls.get(0), containsString("&cql_filter=fid='123'&"));
        assertThat(urls.get(0), not(containsString("&interpolations=")));
        assertThat(urls.get(0), containsString("&sortby=fid&"));
        assertThat(urls.get(1), containsString("&cql_filter=name LIKE 'BLUE%'&"));
        assertThat(urls.get(1), not(containsString("&interpolations=")));
        assertThat(urls.get(1), containsString("&sortby=name D&"));
        assertThat(urls.get(2), containsString("&cql_filter=name LIKE 'BLUE%'&"));
        assertThat(urls.get(2), not(containsString("&interpolations=")));
        assertThat(urls.get(2), containsString("&sortby=name D&"));
        assertThat(urls.get(3), containsString("&cql_filter=INCLUDE&"));
        assertThat(urls.get(3), containsString("&interpolations=nearest neighbor&"));
        assertThat(urls.get(3), not(containsString("&sortby=")));
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

    /** Gets the GetMap URL for the full list of requested layers. */
    private static String getGetMapUrl(GetMapRequest request) {
        String url = WMSRequests.getGetMapUrl(request, null, 0, null, null);
        return ResponseUtils.urlDecode(url);
    }

    /** Gets the GetMap URL for each layer in the requested layers list. */
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
