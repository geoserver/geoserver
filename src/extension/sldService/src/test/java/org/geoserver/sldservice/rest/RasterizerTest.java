/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class RasterizerTest extends SLDServiceBaseTest {

    ClassPathXmlApplicationContext appContext;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        appContext =
                new ClassPathXmlApplicationContext(
                        "classpath:**/sldservice-applicationContext.xml");
        appContext.refresh();
        for (BeanFactoryPostProcessor postProcessor : appContext.getBeanFactoryPostProcessors()) {
            applicationContext.addBeanFactoryPostProcessor(postProcessor);
        }
        applicationContext.refresh();

        getTestData().addWorkspace(getTestData().WCS_PREFIX, getTestData().WCS_URI, getCatalog());
        getTestData().addDefaultRasterLayer(getTestData().WORLD, getCatalog());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geoserver.test.GeoServerSystemTestSupport#onTearDown(org.geoserver.data.test.SystemTestData)
     */
    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        appContext.close();
    }

    @Test
    public void testRasterizeWithNoLayer() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH + "/sldservice//" + getServiceUrl() + ".xml";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 404);
    }

    @Test
    public void testRasterizeWithNoParams() throws Exception {
        LayerInfo l = getCatalog().getLayerByName("wcs:World");
        assertEquals("raster", l.getDefaultStyle().getName());
        final String restPath =
                RestBaseController.ROOT_PATH + "/sldservice/wcs:World/" + getServiceUrl() + ".xml";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        assertTrue(baos.toString().indexOf("<sld:ColorMap>") > 0);
        checkColorMap(baos.toString(), 100);
    }

    @Test
    public void testRasterizeFeatureTypeName() throws Exception {
        LayerInfo l = getCatalog().getLayerByName("wcs:World");
        assertEquals("raster", l.getDefaultStyle().getName());
        final String restPath =
                RestBaseController.ROOT_PATH + "/sldservice/wcs:World/" + getServiceUrl() + ".xml";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        assertTrue(
                baos.toString().indexOf("<sld:FeatureTypeName>Feature</sld:FeatureTypeName>") > 0);
        checkColorMap(baos.toString(), 100);
    }

    @Test
    public void testRasterizeOptions() throws Exception {
        LayerInfo l = getCatalog().getLayerByName("wcs:World");
        assertEquals("raster", l.getDefaultStyle().getName());
        Style defaultStyle1 = l.getDefaultStyle().getStyle();
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/wcs:World/"
                        + getServiceUrl()
                        + ".xml?"
                        + "classes=5&min=10.0&max=50.0&digits=1&ramp=custom&startColor=0xFF0000&endColor=0x0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        assertTrue(baos.toString().indexOf("<sld:ColorMap>") > 0);
        ColorMap map = checkColorMap(baos.toString(), 5);
        checkColorEntry(map.getColorMapEntries()[1], "#FF0000", "10.0", "1.0");
        checkColorEntry(map.getColorMapEntries()[5], "#0000FF", "50.0", "1.0");
        Style defaultStyle2 = l.getDefaultStyle().getStyle();
        assertEquals(defaultStyle1, defaultStyle2);
    }

    @Test
    public void testRasterizeOptions2() throws Exception {
        LayerInfo l = getCatalog().getLayerByName("wcs:World");
        assertEquals("raster", l.getDefaultStyle().getName());
        Style defaultStyle1 = l.getDefaultStyle().getStyle();
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/wcs:World/"
                        + getServiceUrl()
                        + ".xml?"
                        + "classes=5&min=10.0&max=50.0&digits=2&ramp=custom&startColor=0xFF0000&endColor=0x0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        assertTrue(baos.toString().indexOf("<sld:ColorMap>") > 0);
        ColorMap map = checkColorMap(baos.toString(), 5);
        checkColorEntry(map.getColorMapEntries()[1], "#FF0000", "10.00", "1.0");
        checkColorEntry(map.getColorMapEntries()[5], "#0000FF", "50.00", "1.0");
        Style defaultStyle2 = l.getDefaultStyle().getStyle();
        assertEquals(defaultStyle1, defaultStyle2);
    }

    private ColorMap checkColorMap(String resultXml, int classes) {
        Rule[] rules = checkSLD(resultXml);
        assertEquals(1, rules.length);
        Rule rule = rules[0];
        assertNotNull(rule.symbolizers());
        assertEquals(1, rule.symbolizers().size());
        assertTrue(rule.symbolizers().get(0) instanceof RasterSymbolizer);
        RasterSymbolizer symbolizer = (RasterSymbolizer) rule.symbolizers().get(0);
        assertNotNull(symbolizer.getColorMap());
        assertEquals(classes + 1, symbolizer.getColorMap().getColorMapEntries().length);
        ColorMapEntry firstEntry = symbolizer.getColorMap().getColorMapEntries()[0];
        checkColorEntry(firstEntry, "#000000", "transparent", "0");
        return symbolizer.getColorMap();
    }

    private void checkColorEntry(
            ColorMapEntry firstEntry, String color, String label, String opacity) {
        assertEquals(color, firstEntry.getColor().toString());
        assertEquals(label, firstEntry.getLabel());
        assertEquals(opacity, firstEntry.getOpacity().toString());
    }

    @Override
    protected String getServiceUrl() {
        return "rasterize";
    }
}
