/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.styling.Style;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for very slow WMS GetMap response times when the requested 
 * bounding box is much smaller than the resolution of the raster data 
 * and advanced projection handling is disabled.
 */
public class TinyRasterBoundingBoxTest extends WMSTestSupport {

    @BeforeClass
    public static void disableAdvancedProjection() {
        System.setProperty("ENABLE_ADVANCED_PROJECTION", "false");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
        testData.addStyle("rainfall", "rainfall.sld", MockData.class, getCatalog());
    }

    @Test
    public void testTinyRasterBoundingBox() throws Exception {
        GetMapRequest request = new GetMapRequest();
        request.setFormat("image/png");
        WMSMapContent map = new WMSMapContent();
        map.setMapWidth(256);
        map.setMapHeight(256);
        map.setTransparent(true);
        map.setRequest(request);

        CoverageInfo coverageInfo = getCatalog().getCoverageByName(
            MockData.TASMANIA_DEM.getNamespaceURI(), MockData.TASMANIA_DEM.getLocalPart());
        GridCoverage2DReader reader = (GridCoverage2DReader) 
            coverageInfo.getGridCoverageReader(null, null);
        Style style = getCatalog().getStyleByName("rainfall").getStyle();
        map.addLayer(new GridReaderLayer(reader, style));
        Envelope env = coverageInfo.boundingBox();
        Coordinate center = env.centre();
        GridEnvelope range = coverageInfo.getGrid().getGridRange();
        double offset = (env.getMaxX() - env.getMinX()) / range.getSpan(0) / 10.0;
        env = new Envelope(center.x + offset, center.x + 2 * offset, 
            center.y + offset, center.y + 2 * offset);
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));

        RenderedImageMapOutputFormat rasterMapProducer = 
            new RenderedImageMapOutputFormat(getWMS());
        RenderedImageMap imageMap = rasterMapProducer.produceMap(map);
        RenderedOp op = (RenderedOp) imageMap.getImage();
        BufferedImage image = op.getAsBufferedImage();
        imageMap.dispose();

        assertNotBlank("testTinyRasterBoundingBox", image);
        assertEquals("Mosaic", op.getOperationName());
        Rectangle imageBounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        ParameterBlock pb = op.getParameterBlock();
        ROI[] rois = (ROI[]) pb.getObjectParameter(2);
        Rectangle roiBounds = rois[0].getBounds();
        assertTrue("Expected " + imageBounds + " to contain " + roiBounds, 
            imageBounds.contains(roiBounds));

        ImageUtilities.disposeImage(op);
    }
}
