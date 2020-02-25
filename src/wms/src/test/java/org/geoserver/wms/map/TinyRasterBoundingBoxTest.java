/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.map.GridReaderLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridEnvelope;

/**
 * Unit test for very slow WMS GetMap response times when the requested bounding box is much smaller
 * than the resolution of the raster data and advanced projection handling is disabled.
 */
public class TinyRasterBoundingBoxTest extends WMSTestSupport {

    @BeforeClass
    public static void disableAdvancedProjection() {
        System.setProperty("ENABLE_ADVANCED_PROJECTION", "false");
    }

    private WMSMapContent map;

    private BufferedImage image;

    private RenderedOp op;

    @Before
    public void setUp() {
        GetMapRequest request = new GetMapRequest();
        request.setFormat("image/png");
        this.map = new WMSMapContent();
        this.map.setMapWidth(256);
        this.map.setMapHeight(256);
        this.map.setTransparent(true);
        this.map.setRequest(request);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
        testData.addStyle("rainfall", "rainfall.sld", MockData.class, getCatalog());
    }

    @After
    public void tearDown() {
        ImageUtilities.disposeImage(this.op);
        this.map.dispose();
        this.map = null;
        this.image = null;
        this.op = null;
    }

    @Test
    public void testTinyRasterBboxContained() throws Exception {
        CoverageInfo coverageInfo = addRasterToMap(MockData.TASMANIA_DEM);
        Envelope env = coverageInfo.boundingBox();
        Coordinate center = env.centre();
        GridEnvelope range = coverageInfo.getGrid().getGridRange();
        double offset = (env.getMaxX() - env.getMinX()) / range.getSpan(0) / 10.0;
        Rectangle imageBounds =
                produceMap(
                        center.x + offset,
                        center.x + 2 * offset,
                        center.y + offset,
                        center.y + 2 * offset);
        assertNotBlank("testTinyRasterBboxContained", this.image);
        assertEquals("Mosaic", this.op.getOperationName());
        Rectangle roiBounds = getRoiBounds();
        assertTrue(
                "Expected " + imageBounds + " to contain " + roiBounds,
                imageBounds.contains(roiBounds));
    }

    @Test
    public void testTinyRasterBboxIntersection() throws Exception {
        CoverageInfo coverageInfo = addRasterToMap(MockData.TASMANIA_DEM);
        Envelope env = coverageInfo.boundingBox();
        GridEnvelope range = coverageInfo.getGrid().getGridRange();
        double offset = (env.getMaxX() - env.getMinX()) / range.getSpan(0) / 20.0;
        Rectangle imageBounds =
                produceMap(
                        env.getMinX() - offset,
                        env.getMinX() + offset,
                        env.getMaxY() - offset,
                        env.getMaxY() + offset);
        assertNotBlank("testTinyRasterBboxIntersection", this.image);
        assertEquals("Mosaic", this.op.getOperationName());
        Rectangle roiBounds = getRoiBounds();
        assertTrue(
                "Expected " + imageBounds + " to contain " + roiBounds,
                imageBounds.contains(roiBounds));
    }

    @Test
    public void testTinyRasterBboxNoIntersection() throws Exception {
        CoverageInfo coverageInfo = addRasterToMap(MockData.TASMANIA_DEM);
        Envelope env = coverageInfo.boundingBox();
        GridEnvelope range = coverageInfo.getGrid().getGridRange();
        double offset = (env.getMaxX() - env.getMinX()) / range.getSpan(0) / 10.0;
        Rectangle imageBounds =
                produceMap(
                        env.getMaxX() + offset,
                        env.getMaxX() + 2 * offset,
                        env.getMinY() - 2 * offset,
                        env.getMinY() - offset);
        assertNotBlank("testTinyRasterBboxNoIntersection", this.image);
        assertEquals("Mosaic", this.op.getOperationName());
        Rectangle roiBounds = getRoiBounds();
        assertTrue(
                "Expected " + imageBounds + " to contain " + roiBounds,
                imageBounds.contains(roiBounds));
    }

    private CoverageInfo addRasterToMap(QName typeName) throws Exception {
        CoverageInfo coverageInfo =
                getCatalog().getCoverageByName(typeName.getNamespaceURI(), typeName.getLocalPart());
        GridCoverage2DReader reader =
                (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        Style style = getCatalog().getStyleByName("rainfall").getStyle();
        this.map.addLayer(new GridReaderLayer(reader, style));
        return coverageInfo;
    }

    private Rectangle getRoiBounds() {
        ParameterBlock pb = this.op.getParameterBlock();
        ROI[] rois = (ROI[]) pb.getObjectParameter(2);
        return rois[0].getBounds();
    }

    private Rectangle produceMap(double minX, double maxX, double minY, double maxY) {
        this.map
                .getViewport()
                .setBounds(
                        new ReferencedEnvelope(minX, maxX, minY, maxY, DefaultGeographicCRS.WGS84));
        RenderedImageMapOutputFormat rasterMapProducer = new RenderedImageMapOutputFormat(getWMS());
        RenderedImageMap imageMap = rasterMapProducer.produceMap(this.map);
        this.op = (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        this.image = this.op.getAsBufferedImage();
        imageMap.dispose();
        return new Rectangle(0, 0, this.image.getWidth(), this.image.getHeight());
    }
}
