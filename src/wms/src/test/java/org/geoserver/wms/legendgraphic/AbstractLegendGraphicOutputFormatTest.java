/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Tets the functioning of the abstract legend producer for raster formats, which relies on
 * Geotools' StyledShapePainter.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class AbstractLegendGraphicOutputFormatTest extends WMSTestSupport {

    private static final Logger LOGGER = Logging
            .getLogger(AbstractLegendGraphicOutputFormatTest.class);

    private BufferedImageLegendGraphicBuilder legendProducer;

    GetLegendGraphic service;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new AbstractLegendGraphicOutputFormatTest());
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        dataDirectory.addCoverage(new QName("http://www.geo-solutions.it", "world", "gs"),
                MockData.class.getResource("world.tiff"), "tiff", "raster");
        dataDirectory.addStyle("rainfall", MockData.class.getResource("rainfall.sld"));
        dataDirectory.addStyle("rainfall_ramp", MockData.class.getResource("rainfall_ramp.sld"));
        dataDirectory.addStyle("rainfall_classes",
                MockData.class.getResource("rainfall_classes.sld"));
    }

    @Override
    public void setUpInternal() throws Exception {
        super.setUpInternal();
        this.legendProducer = new BufferedImageLegendGraphicBuilder() {

            public String getContentType() {
                return "image/png";
            }

        };

        service = new GetLegendGraphic(getWMS());
    }

    @Override
    public void tearDownInternal() throws Exception {
        this.legendProducer = null;
        super.tearDownInternal();
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     */
    public void testUserSpecifiedRule() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        assertNotNull(multipleRulesStyle);

        Rule rule = multipleRulesStyle.getFeatureTypeStyles()[0].getRules()[0];
        LOGGER.info("testing single rule " + rule.getName() + " from style "
                + multipleRulesStyle.getName());

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(multipleRulesStyle);
        req.setRule(rule);
        req.setLegendOptions(new HashMap());

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testUserSpecifiedRule", image, LegendUtils.DEFAULT_BG_COLOR);

        // was created only one rule?
        String errMsg = "expected just one legend of height " + HEIGHT_HINT + ", for the rule "
                + rule.getName();
        int resultLegendCount = image.getHeight() / HEIGHT_HINT;
        assertEquals(errMsg, 1, resultLegendCount);
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     * 
     */
    public void testRainfall() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName("rainfall").getStyle();

        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature;
        feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(multipleRulesStyle);
        req.setLegendOptions(new HashMap());

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

    }

    /**
     * Tests that the legend graphic is still produced when the request's strict parameter is set to
     * false and a layer is not specified
     */
    public void testNoLayerProvidedAndNonStrictRequest() throws Exception {
        Style style = getCatalog().getStyleByName("rainfall").getStyle();
        assertNotNull(style);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setStrict(false);
        req.setLayer(null);
        req.setStyle(style);

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

    }
}
