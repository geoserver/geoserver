/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Checks that a GML 3.2 response declares the same number of ordinates it writes, on measured geometries: the 3.2
 * configuration carries the measures setting on its own path, separate from the 3.1 one.
 */
public final class GML32MeasuresTest extends WFS20TestSupport {

    private static final QName LINESTRING_ZM = new QName(MockData.DEFAULT_URI, "lineStringZm", MockData.DEFAULT_PREFIX);

    private static final QName LINESTRING_M = new QName(MockData.DEFAULT_URI, "lineStringM", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addLayer(testData, LINESTRING_ZM, "lineStringZm.properties");
        addLayer(testData, LINESTRING_M, "lineStringM.properties");
    }

    private void addLayer(SystemTestData testData, QName layer, String properties) throws Exception {
        testData.addVectorLayer(layer, Collections.emptyMap(), properties, GML32MeasuresTest.class, getCatalog());
    }

    @Before
    public void deactivateMeasuresEncoding() {
        setMeasuresEncoding(getCatalog(), LINESTRING_ZM.getLocalPart(), false);
        setMeasuresEncoding(getCatalog(), LINESTRING_M.getLocalPart(), false);
    }

    @Test
    public void testLineXYZMMeasuresNotEncoded() throws Exception {
        assertPosList(LINESTRING_ZM, "50 120 20 80 90 35", 3);
    }

    @Test
    public void testLineXYZMMeasuresEncoded() throws Exception {
        setMeasuresEncoding(getCatalog(), LINESTRING_ZM.getLocalPart(), true);
        assertPosList(LINESTRING_ZM, "50 120 20 15 80 90 35 5", 4);
    }

    /** XYM, measures off: the measure is not a height, so a plain 2D line is written and declared. */
    @Test
    public void testLineXYMMeasuresNotEncoded() throws Exception {
        assertPosList(LINESTRING_M, "50 120 80 90", 2);
    }

    @Test
    public void testLineXYMMeasuresEncoded() throws Exception {
        setMeasuresEncoding(getCatalog(), LINESTRING_M.getLocalPart(), true);
        assertPosList(LINESTRING_M, "50 120 15 80 90 5", 3);
    }

    /** The bug being guarded: srsDimension has to be the ordinates written per coordinate. */
    private void assertPosList(QName layer, String expected, int expectedDimension) throws Exception {
        Document dom = getAsDOM(
                "wfs?request=GetFeature&typenames=gs:%s&version=2.0.0&service=wfs".formatted(layer.getLocalPart()));
        assertXpathEvaluatesTo(expected, "//gml:LineString/gml:posList", dom);
        assertXpathEvaluatesTo(String.valueOf(expectedDimension), "//gml:LineString/@srsDimension", dom);
        assertEquals(
                "declared dimension differs from the ordinates written",
                0,
                expected.split("\\s+").length % expectedDimension);
    }
}
