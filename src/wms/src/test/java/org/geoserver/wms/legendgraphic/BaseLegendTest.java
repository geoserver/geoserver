/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.legendgraphic;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;

public class BaseLegendTest extends WMSTestSupport {

    protected static final Logger LOGGER =
            Logging.getLogger(AbstractLegendGraphicOutputFormatTest.class);

    protected BufferedImageLegendGraphicBuilder legendProducer;

    protected GetLegendGraphic service;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addRasterLayer(
                new QName("http://www.geo-solutions.it", "world", "gs"),
                "world.tiff",
                "tiff",
                new HashMap(),
                MockData.class,
                catalog);
        testData.addStyle("rainfall", MockData.class, catalog);
        testData.addStyle("rainfall_ramp", MockData.class, catalog);
        testData.addStyle("rainfall_classes", MockData.class, catalog);
        testData.addStyle("rainfall_classes_nolabels", MockData.class, catalog);
        // add raster layer for rendering transform test
        testData.addRasterLayer(
                new QName("http://www.opengis.net/wcs/1.1.1", "DEM", "wcs"),
                "tazdem.tiff",
                "tiff",
                new HashMap(),
                MockData.class,
                catalog);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(
                Font.createFont(
                        Font.TRUETYPE_FONT, WMSTestSupport.class.getResourceAsStream("Vera.ttf")));
    }

    @Before
    public void setLegendProducer() throws Exception {
        this.legendProducer =
                new BufferedImageLegendGraphicBuilder() {
                    public String getContentType() {
                        return "image/png";
                    }
                };

        service = new GetLegendGraphic(getWMS());
    }

    @After
    public void resetLegendProducer() throws Exception {
        this.legendProducer = null;
    }

    protected int getTitleHeight(GetLegendGraphicRequest req) {
        final BufferedImage image =
                ImageUtils.createImage(
                        req.getWidth(),
                        req.getHeight(),
                        (IndexColorModel) null,
                        req.isTransparent());
        return getRenderedLabel(image, "TESTTITLE", req).getHeight();
    }

    private BufferedImage getRenderedLabel(
            BufferedImage image, String label, GetLegendGraphicRequest request) {
        Font labelFont = LegendUtils.getLabelFont(request);
        boolean useAA = LegendUtils.isFontAntiAliasing(request);

        final Graphics2D graphics = image.createGraphics();
        graphics.setFont(labelFont);
        if (useAA) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        return LegendUtils.renderLabel(label, graphics, request);
    }
}
