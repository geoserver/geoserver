/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DimensionsVectorCapabilitiesTest extends WMSDimensionsTestSupport {

    @Test
    public void testNoDimension() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        assertXpathEvaluatesTo("1", "count(//Layer[Name='sf:TimeElevation'])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Extent)", dom);
    }

    @Test
    public void testDefaultElevationUnits() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, null, null, null);
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);

        assertXpathEvaluatesTo(DimensionInfo.ELEVATION_UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(
                DimensionInfo.ELEVATION_UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
    }

    @Test
    public void testEmptyDataSet() throws Exception {
        for (DimensionPresentation p : DimensionPresentation.values()) {
            setupVectorDimension(
                    V_TIME_ELEVATION_EMPTY.getLocalPart(),
                    ResourceInfo.TIME,
                    "time",
                    p,
                    null,
                    null,
                    null);
            checkEmptyTimeDimensionAndExtent();
        }

        // clear time metadata
        FeatureTypeInfo info =
                getCatalog().getFeatureTypeByName(V_TIME_ELEVATION_EMPTY.getLocalPart());
        info.getMetadata().remove(ResourceInfo.TIME);
        getCatalog().save(info);

        for (DimensionPresentation p : DimensionPresentation.values()) {
            setupVectorDimension(
                    V_TIME_ELEVATION_EMPTY.getLocalPart(),
                    ResourceInfo.ELEVATION,
                    "elevation",
                    p,
                    null,
                    UNITS,
                    UNIT_SYMBOL);
            checkEmptyElevationDimensionAndExtent();
        }
    }

    void checkEmptyElevationDimensionAndExtent() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        Element e = dom.getDocumentElement();

        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        // check dimension info exists
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0", "//Layer/Extent/@default", dom);
        // and that it is empty
        assertXpathEvaluatesTo("", "//Layer/Extent", dom);
    }

    void checkEmptyTimeDimensionAndExtent() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        Element e = dom.getDocumentElement();

        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        // check dimension info exists
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT, "//Layer/Extent/@default", dom);
        // and that it is empty
        assertXpathEvaluatesTo("", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationList() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0,1.0,2.0,3.0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationContinuous() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationDiscreteNoResolution() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.DISCRETE_INTERVAL,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/1.0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationDiscrerteManualResolution() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.DISCRETE_INTERVAL,
                2.0,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/2.0", "//Layer/Extent", dom);
    }

    @Test
    public void testTimeList() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0", "//Layer/Extent/@nearestValue", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT, "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo(
                "2011-05-01T00:00:00.000Z,2011-05-02T00:00:00.000Z,2011-05-03T00:00:00.000Z,2011-05-04T00:00:00.000Z",
                "//Layer/Extent",
                dom);
    }

    @Test
    public void testTimeNearestMatch() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("1", "//Layer/Extent/@nearestValue", dom);
    }

    @Test
    public void testTimeContinuous() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                null,
                null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT, "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo(
                "2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/PT1S", "//Layer/Extent", dom);
    }

    @Test
    public void testTimeResolution() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.DISCRETE_INTERVAL,
                Double.valueOf(1000 * 60 * 60 * 24),
                null,
                null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT, "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo(
                "2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/P1D", "//Layer/Extent", dom);
    }

    @Test
    public void testTimeContinuousInEarthObservationRootLayer() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                null,
                null);

        LayerInfo rootLayer = getCatalog().getLayerByName("TimeElevation");
        LayerGroupInfo eoProduct = new LayerGroupInfoImpl();
        eoProduct.setName("EO Sample");
        eoProduct.setMode(LayerGroupInfo.Mode.EO);
        eoProduct.setRootLayer(rootLayer);
        eoProduct.setRootLayerStyle(rootLayer.getDefaultStyle());

        CatalogBuilder catBuilder = new CatalogBuilder(getCatalog());
        catBuilder.calculateLayerGroupBounds(eoProduct);

        eoProduct.getLayers().add(rootLayer);
        eoProduct.getStyles().add(null);
        getCatalog().add(eoProduct);
        try {
            Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
            // print(dom);

            // check dimension has been declared
            assertXpathEvaluatesTo(
                    "1", "count(//Layer[Name[text() = 'EO Sample']]/Dimension)", dom);
            assertXpathEvaluatesTo(
                    "time", "//Layer[Name[text() = 'EO Sample']]/Dimension/@name", dom);
            assertXpathEvaluatesTo(
                    "ISO8601", "//Layer[Name[text() = 'EO Sample']]/Dimension/@units", dom);
            // check we have the extent
            assertXpathEvaluatesTo("1", "count(//Layer[Name[text() = 'EO Sample']]/Extent)", dom);
            assertXpathEvaluatesTo("time", "//Layer[Name[text() = 'EO Sample']]/Extent/@name", dom);
            assertXpathEvaluatesTo(
                    DimensionDefaultValueSetting.TIME_CURRENT,
                    "//Layer[Name[text() = 'EO Sample']]/Extent/@default",
                    dom);
            assertXpathEvaluatesTo(
                    "2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/PT1S",
                    "//Layer[Name[text() = 'EO Sample']]/Extent",
                    dom);
        } finally {
            getCatalog().remove(eoProduct);
        }
    }

    @Test
    public void testDefaultTimeRangeFixed() throws Exception {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("P1M/PRESENT");
        setupResourceDimensionDefaultValue(
                V_TIME_ELEVATION, ResourceInfo.TIME, defaultValueSetting);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("P1M/PRESENT", "//Layer/Extent/@default", dom);
    }

    @Test
    public void testDefaultElevationRangeFixed() throws Exception {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("-100/0");
        setupResourceDimensionDefaultValue(
                V_TIME_ELEVATION, ResourceInfo.ELEVATION, defaultValueSetting);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("-100/0", "//Layer/Extent/@default", dom);
    }

    @Test
    public void testCustomDimension() throws Exception {
        setupVectorDimension(
                "dim_custom", "elevation", DimensionPresentation.LIST, null, null, null);
        final Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        assertXpathEvaluatesTo("custom", "//Layer[Name='sf:TimeElevation']/Dimension/@name", dom);
        assertXpathEvaluatesTo("custom", "//Layer[Name='sf:TimeElevation']/Extent/@name", dom);
        assertXpathEvaluatesTo(
                "0.0,1.0,2.0,3.0", "//Layer[Name='sf:TimeElevation']/Extent/text()", dom);
    }

    @Test
    public void testCustomContinuousDate() throws Exception {
        setupVectorDimension(
                "dim_custom",
                "time",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.0"), false);
        assertXpathEvaluatesTo(
                "2011-05-01T00:00:00Z", "//Layer[Name='sf:TimeElevation']/Extent/@default", dom);
    }
}
