/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonContext;
import java.io.UnsupportedEncodingException;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class CoverageJsonTest extends WCSTestSupport {

    private static final double DELTA = 2E-1;

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        System.setProperty("user.timezone", "UTC");
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
    }

    @Test
    public void getPointSeries() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=watertemp"
                                + "&subset=Lat(41)&subset=Long(1)"
                                + "&subset=elevation(0)"
                                + "&subset=time(\"2008-10-31T00:00:00Z\",\"2008-11-01T00:00:00Z\")"
                                + "&format=application/prs.coverage%2Bjson");

        DocumentContext json = getAsJSONPath(response);
        assertEquals("Coverage", json.read("type"));
        assertEquals("PointSeries", json.read("domain.domainType"));
        assertEquals(2, json.read("domain.axes.t.values.length()", Integer.class), DELTA);
        assertEquals(1, json.read("domain.axes.z.values.length()", Integer.class), DELTA);
        assertEquals(1d, json.read("domain.axes.x.values[0]", Double.class), DELTA);
        assertEquals(41d, json.read("domain.axes.y.values[0]", Double.class), DELTA);

        assertEquals(4, json.read("ranges.WATERTEMP.shape.length()", Integer.class), DELTA);
        assertEquals(2, json.read("ranges.WATERTEMP.shape[0]", Integer.class), DELTA);
        assertEquals(1, json.read("ranges.WATERTEMP.shape[1]", Integer.class), DELTA);
        assertEquals(1, json.read("ranges.WATERTEMP.shape[2]", Integer.class), DELTA);
        assertEquals(1, json.read("ranges.WATERTEMP.shape[3]", Integer.class), DELTA);
    }

    @Test
    public void getGrid() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=watertemp"
                                + "&subset=Lat(41,43)&subset=Long(1,4)"
                                + "&subset=elevation(0)"
                                + "&subset=time(\"2008-10-31T00:00:00Z\")"
                                + "&format=application/prs.coverage%2Bjson");

        DocumentContext json = getAsJSONPath(response);
        assertEquals("Coverage", json.read("type"));
        assertEquals("Grid", json.read("domain.domainType"));
        assertEquals(1, json.read("domain.axes.t.values.length()", Integer.class), DELTA);
        assertEquals(1, json.read("domain.axes.z.values.length()", Integer.class), DELTA);
        assertThat(json.read("domain.axes.x.start"), not(empty()));
        assertThat(json.read("domain.axes.y.start"), not(empty()));
        assertThat(json.read("domain.axes.x.stop"), not(empty()));
        assertThat(json.read("domain.axes.y.stop"), not(empty()));
    }

    protected DocumentContext getAsJSONPath(MockHttpServletResponse response)
            throws UnsupportedEncodingException {
        assertThat(response.getContentType(), containsString("json"));
        JsonContext json = (JsonContext) JsonPath.parse(response.getContentAsString());
        if (!isQuietTests()) {
            print(json(response));
        }
        return json;
    }
}
