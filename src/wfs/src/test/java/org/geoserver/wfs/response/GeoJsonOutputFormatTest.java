/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Contains tests related with GeoJSON output format when requested through WFS. */
public final class GeoJsonOutputFormatTest extends WFSTestSupport {

    private static final QName LINESTRING_ZM =
            new QName(MockData.DEFAULT_URI, "lineStringZm", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // create in memory layer containing XYZM lines geometries
        testData.addVectorLayer(
                LINESTRING_ZM,
                Collections.emptyMap(),
                "lineStringZm.properties",
                GeoJsonOutputFormatTest.class,
                getCatalog());
    }

    @Before
    public void beforeTest() {
        // deactivate measures encoding, default behavior
        setMeasuresEncoding(getCatalog(), LINESTRING_ZM.getLocalPart(), false);
    }

    @Test
    public void testMeasuresEncoding() throws Exception {
        // execute the WFS request asking for a GeoJSON output
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?request=GetFeature&typenames=gs:lineStringZm&version=2.0.0"
                                + "&service=wfs&outputFormat=application/json");
        // check that measures where not encoded
        assertThat(response.getContentAsString(), notNullValue());
        assertThat(
                response.getContentAsString(), Matchers.containsString("[[120,50,20],[90,80,35]]"));
        // activate measures encoding
        setMeasuresEncoding(getCatalog(), LINESTRING_ZM.getLocalPart(), true);
        response =
                getAsServletResponse(
                        "wfs?request=GetFeature&typenames=gs:lineStringZm&version=2.0.0"
                                + "&service=wfs&outputFormat=application/json");
        // check that measures where encoded
        assertThat(response.getContentAsString(), notNullValue());
        assertThat(
                response.getContentAsString(),
                Matchers.containsString("[[120,50,20,15],[90,80,35,5]]"));
    }
}
