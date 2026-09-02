/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Checks that the per layer measures setting reaches the GML2 output, as it already does for GML3. */
public final class GML2MeasuresTest extends WFSTestSupport {

    private static final QName LINESTRING_ZM = new QName(MockData.DEFAULT_URI, "lineStringZm", MockData.DEFAULT_PREFIX);

    private static final QName LINESTRING_M = new QName(MockData.DEFAULT_URI, "lineStringM", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // in memory layer holding a single XYZM line
        testData.addVectorLayer(
                LINESTRING_ZM, Collections.emptyMap(), "lineStringZm.properties", GML2MeasuresTest.class, getCatalog());
        // and one holding a single XYM line, where the measure is not a height
        testData.addVectorLayer(
                LINESTRING_M, Collections.emptyMap(), "lineStringM.properties", GML2MeasuresTest.class, getCatalog());
    }

    @Before
    public void deactivateMeasuresEncoding() {
        setMeasuresEncoding(getCatalog(), LINESTRING_ZM.getLocalPart(), false);
        setMeasuresEncoding(getCatalog(), LINESTRING_M.getLocalPart(), false);
    }

    @Test
    public void testMeasuresNotEncoded() throws Exception {
        assertThat(getGml2(), containsString("120,50,20 90,80,35"));
    }

    @Test
    public void testMeasuresEncoded() throws Exception {
        setMeasuresEncoding(getCatalog(), LINESTRING_ZM.getLocalPart(), true);
        assertThat(getGml2(), containsString("120,50,20,15 90,80,35,5"));
    }

    /** XYM, measures off: the measure is not a height, so nothing of it may reach the third slot. */
    @Test
    public void testXYMMeasuresNotEncoded() throws Exception {
        assertThat(getGml2(LINESTRING_M), containsString("120,50 90,80"));
    }

    @Test
    public void testXYMMeasuresEncoded() throws Exception {
        setMeasuresEncoding(getCatalog(), LINESTRING_M.getLocalPart(), true);
        assertThat(getGml2(LINESTRING_M), containsString("120,50,15 90,80,5"));
    }

    private String getGml2() throws Exception {
        return getGml2(LINESTRING_ZM);
    }

    private String getGml2(QName layer) throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wfs?request=GetFeature&typename=gs:%s".formatted(layer.getLocalPart())
                        + "&version=1.0.0&service=wfs&outputFormat=GML2");
        return response.getContentAsString();
    }
}
