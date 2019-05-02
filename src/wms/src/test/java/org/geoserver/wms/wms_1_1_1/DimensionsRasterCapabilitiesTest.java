/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DimensionsRasterCapabilitiesTest extends WMSDimensionsTestSupport {

    @Test
    public void testNoDimension() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        assertXpathEvaluatesTo("1", "count(//Layer[Name='sf:watertemp'])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Extent)", dom);
    }

    @Test
    public void testDefaultElevationUnits() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, null, null);
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);

        assertXpathEvaluatesTo(DimensionInfo.ELEVATION_UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(
                DimensionInfo.ELEVATION_UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
    }

    @Test
    public void testElevationList() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
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
        assertXpathEvaluatesTo("0.0,100.0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationContinuous() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
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
        assertXpathEvaluatesTo("0.0/100.0/0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationDiscreteNoResolution() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
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
        assertXpathEvaluatesTo("0.0/100.0/100.0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationDiscreteManualResolution() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.DISCRETE_INTERVAL,
                10.0,
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
        assertXpathEvaluatesTo("0.0/100.0/10.0", "//Layer/Extent", dom);
    }

    @Test
    public void testDefaultTimeRangeFixed() throws Exception {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("P1M/PRESENT");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.TIME, defaultValueSetting);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("P1M/PRESENT", "//Layer/Extent/@default", dom);
    }

    @Test
    public void testTimeList() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        //        print(dom);

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
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z", "//Layer/Extent", dom);
    }

    @Test
    public void testTimeNearestMatch() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        setupNearestMatch(WATTEMP, ResourceInfo.TIME, true);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared with nearest match
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("1", "//Layer/Extent/@nearestValue", dom);
    }

    @Test
    public void testTimeContinuous() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
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
                "2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/PT1S", "//Layer/Extent", dom);
    }

    @Test
    public void testTimeResolution() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.DISCRETE_INTERVAL,
                Double.valueOf(1000 * 60 * 60 * 12),
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
                "2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/PT12H", "//Layer/Extent", dom);
    }

    @Test
    public void testTimeContinuousInEarthObservationRootLayer() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                null,
                null);

        LayerInfo rootLayer = getCatalog().getLayerByName("watertemp");
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
                    "2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/PT1S",
                    "//Layer[Name[text() = 'EO Sample']]/Extent",
                    dom);
        } finally {
            getCatalog().remove(eoProduct);
        }
    }

    @Test
    public void testTimeRangeList() throws Exception {
        setupRasterDimension(
                TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent, we should get a single range as times do overlap
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT, "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z/2008-11-04T00:00:00.000Z/PT1S,2008-11-05T00:00:00.000Z/2008-11-07T00:00:00.000Z/PT1S",
                "//Layer/Extent",
                dom);
    }

    @Test
    public void testElevationRangeList() throws Exception {
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.ELEVATION,
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
        // check we have the extent, we should get a single range as times do overlap
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("20.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("20.0/99.0/0,100.0/150.0/0", "//Layer/Extent", dom);
    }

    @Test
    public void testElevationRangeContinousInterval() throws Exception {
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.ELEVATION,
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
        // check we have the extent, we should get a single range as times do overlap
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("20.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("20.0/150.0/0", "//Layer/Extent", dom);
    }
}
