/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.data.test.CiteTestData.STREAMS;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSPartialMapException;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.filter.IllegalFilterException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;

public class RenderedImageMapOutputFormatTest extends WMSTestSupport {

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(RenderedImageMapOutputFormatTest.class.getPackage().getName());

    private RenderedImageMapOutputFormat rasterMapProducer;

    private String mapFormat = "image/gif";

    @Before
    public void setRasterMapProducer() throws Exception {
        Logging.getLogger("org.geotools.rendering").setLevel(Level.OFF);
        this.rasterMapProducer = getProducerInstance();
    }

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new DummyRasterMapProducer(getWMS());
    }

    @After
    public void unsetRasterMapProducer() throws Exception {
        this.rasterMapProducer = null;
    }

    public String getMapFormat() {
        return this.mapFormat;
    }

    @Test
    public void testSimpleGetMapQuery() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs = catalog.getFeatureTypeByName(MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart()).getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("testSimpleGetMapQuery", image);
    }
    
    @Test
    public void testTimeoutOption() throws Exception {
        Catalog catalog = getCatalog();
        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        
        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        
        //Build up a complex map so that we can reasonably guarantee a 1 ms timout
        FeatureSource fs = catalog.getFeatureTypeByName(MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart()).getFeatureSource(null, null);
        Envelope env = fs.getBounds();
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.BRIDGES.getPrefix(),
                MockData.BRIDGES.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.DIVIDED_ROUTES.getPrefix(),
                MockData.DIVIDED_ROUTES.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.FORESTS.getPrefix(),
                MockData.FORESTS.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.LAKES.getPrefix(),
                MockData.LAKES.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.MAP_NEATLINE.getPrefix(),
                MockData.MAP_NEATLINE.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.NAMED_PLACES.getPrefix(),
                MockData.NAMED_PLACES.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.PONDS.getPrefix(),
                MockData.PONDS.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.ROAD_SEGMENTS.getPrefix(),
                MockData.ROAD_SEGMENTS.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        fs = catalog.getFeatureTypeByName(MockData.STREAMS.getPrefix(),
                MockData.STREAMS.getLocalPart()).getFeatureSource(null, null);
        env.expandToInclude(fs.getBounds());
        map.addLayer(new FeatureLayer(fs, basicStyle));
        
        LOGGER.info("about to create map ctx for "+map.layers().size()+" layers with bounds " + env);
        
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(1000);
        map.setMapHeight(1000);
        map.setRequest(request);
        
        request.setFormat(getMapFormat());
        Map formatOptions = new HashMap();
        //1 ms timeout
        formatOptions.put("timeout", 1);
        request.setFormatOptions(formatOptions);
        try {
            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            fail("Timeout was not reached");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().startsWith("This request used more time than allowed"));
        }
        
        //Test partial image exception format
        Map rawKvp = new HashMap();
        rawKvp.put("EXCEPTIONS", "PARTIALMAP");
        request.setRawKvp(rawKvp);
        
        try {
            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            fail("Timeout was not reached");
        } catch (ServiceException e) {
            assertTrue(e instanceof WMSPartialMapException);
            assertTrue(e.getCause().getMessage().startsWith("This request used more time than allowed"));
            RenderedImageMap partialMap = (RenderedImageMap) ((WMSPartialMapException)e).getMap();
            assertNotNull(partialMap);
            assertNotNull(partialMap.getImage());
        }
    }

    @Test
    public void testDefaultStyle() throws Exception {
        List<org.geoserver.catalog.FeatureTypeInfo> typeInfos = getCatalog().getFeatureTypes();

        for (org.geoserver.catalog.FeatureTypeInfo info : typeInfos) {
            if (info.getQualifiedName().getNamespaceURI().equals(MockData.CITE_URI)
                    && info.getFeatureType().getGeometryDescriptor() != null)
                testDefaultStyle(info.getFeatureSource(null, null));
        }
    }

    @Test 
    public void testBlueLake() throws IOException, IllegalFilterException, Exception {
        final Catalog catalog = getCatalog();
        org.geoserver.catalog.FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(
                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        double shift = env.getWidth() / 6;

        env = new Envelope(env.getMinX() - shift, env.getMaxX() + shift, env.getMinY() - shift,
                env.getMaxY() + shift);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);

        addToMap(map, MockData.FORESTS);
        addToMap(map, MockData.LAKES);
        addToMap(map, MockData.STREAMS);
        addToMap(map, MockData.NAMED_PLACES);
        addToMap(map, MockData.ROAD_SEGMENTS);
        addToMap(map, MockData.PONDS);
        addToMap(map, MockData.BUILDINGS);
        addToMap(map, MockData.DIVIDED_ROUTES);
        addToMap(map, MockData.BRIDGES);
        addToMap(map, MockData.MAP_NEATLINE);

        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("testBlueLake", image);
    }
    
    
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
    }

    
    @Test 
    public void testInterpolations() throws IOException, IllegalFilterException, Exception {
        final Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(MockData.TASMANIA_DEM.getNamespaceURI(),
                MockData.TASMANIA_DEM.getLocalPart());
        
        Envelope env = coverageInfo.boundingBox();
        double shift = env.getWidth() / 6;

        env = new Envelope(env.getMinX() - shift, env.getMaxX() + shift, env.getMinY() - shift,
                env.getMaxY() + shift);

        GetMapRequest request = new GetMapRequest();
        WMSMapContent map = new WMSMapContent();
        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);
        addRasterToMap(map, MockData.TASMANIA_DEM);
        
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        request.setInterpolations(Arrays.asList(Interpolation
                .getInstance(Interpolation.INTERP_NEAREST)));
        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op = (RenderedOp)imageMap.getImage();
        BufferedImage imageNearest = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsNearest", imageNearest);
        
        request = new GetMapRequest();
        map = new WMSMapContent();
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);
        addRasterToMap(map, MockData.TASMANIA_DEM);
        
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        request.setInterpolations(Arrays.asList(Interpolation
                .getInstance(Interpolation.INTERP_BICUBIC)));
        request.setFormat(getMapFormat());

        imageMap = this.rasterMapProducer.produceMap(map);
        op = (RenderedOp)imageMap.getImage();
        BufferedImage imageBicubic = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsBicubic", imageBicubic);
        // test some sample pixels to check rendering is different using different interpolations
        assertNotEquals(getPixelColor(imageNearest, 200, 200).getRGB(),
                getPixelColor(imageBicubic, 200, 200).getRGB());
        assertNotEquals(getPixelColor(imageNearest, 300, 300).getRGB(),
                getPixelColor(imageBicubic, 300, 300).getRGB());
    }

    private void addRasterToMap(final WMSMapContent map, final QName typeName) throws IOException, FactoryRegistryException, TransformException, SchemaException {
        final CoverageInfo coverageInfo = getCatalog().getCoverageByName(typeName.getNamespaceURI(),
                typeName.getLocalPart());


        List<LayerInfo> layers = getCatalog().getLayers(coverageInfo);
        StyleInfo defaultStyle = layers.get(0).getDefaultStyle();
        Style style = defaultStyle.getStyle();
        
        SimpleFeatureCollection fc = FeatureUtilities.wrapGridCoverageReader(
                (GridCoverage2DReader)coverageInfo.getGridCoverageReader(null, null), new GeneralParameterValue[] {});
        map.addLayer(new FeatureLayer(fc, style));
    }
    
    private void addToMap(final WMSMapContent map, final QName typeName) throws IOException {
        final FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                typeName.getNamespaceURI(), typeName.getLocalPart());

        List<LayerInfo> layers = getCatalog().getLayers(ftInfo);
        StyleInfo defaultStyle = layers.get(0).getDefaultStyle();
        Style style = defaultStyle.getStyle();

        map.addLayer(new FeatureLayer(ftInfo.getFeatureSource(null, null), style));
    }

    private void testDefaultStyle(FeatureSource fSource) throws Exception {
        Catalog catalog = getCatalog();
        Style style = catalog.getStyleByName("Default").getStyle();

        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(),
                MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        env.expandToInclude(fSource.getBounds());

        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());

        double shift = env.getWidth() / 6;

        env = new Envelope(env.getMinX() - shift, env.getMaxX() + shift, env.getMinY() - shift,
                env.getMaxY() + shift);

        WMSMapContent map = new WMSMapContent();
        GetMapRequest request = new GetMapRequest();
        map.setRequest(request);
        map.addLayer(new FeatureLayer(fSource, style));
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(false);

        // this.rasterMapProducer.setOutputFormat(getMapFormat());
        // this.rasterMapProducer.setMapContext(map);
        // this.rasterMapProducer.produceMap();
        request.setFormat(getMapFormat());

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);

        RenderedImage image = imageMap.getImage();
        imageMap.dispose();
        assertNotNull(image);
        String typeName = fSource.getSchema().getName().getLocalPart();
        assertNotBlank("testDefaultStyle " + typeName, (BufferedImage) image);
    }

    /**
     * Checks {@link RenderedImageMapOutputFormat} makes good use of {@link RenderExceptionStrategy}
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testRenderingErrorsHandling() throws Exception {

        // the ones that are ignorable by the renderer
        assertNotNull(forceRenderingError(new TransformException("fake transform exception")));
        assertNotNull(forceRenderingError(new NoninvertibleTransformException(
                "fake non invertible exception")));
        assertNotNull(forceRenderingError(new IllegalAttributeException(
                "non illegal attribute exception")));
        assertNotNull(forceRenderingError(new FactoryException("fake factory exception")));

        // any other one should make the map producer fail
        try {
            forceRenderingError(new RuntimeException("fake runtime exception"));
            fail("Expected WMSException");
        } catch (ServiceException e) {
            assertTrue(true);
        }

        try {
            forceRenderingError(new IOException("fake IO exception"));
            fail("Expected WMSException");
        } catch (ServiceException e) {
            assertTrue(true);
        }

        try {
            forceRenderingError(new IllegalArgumentException("fake IAE exception"));
            fail("Expected WMSException");
        } catch (ServiceException e) {
            assertTrue(true);
        }
    }

    /**
     * Sets up a rendering loop and throws {@code renderExceptionToThrow} wrapped to a
     * RuntimeException when the renderer tries to get a Feature to render.
     * <p>
     * If the rendering succeeded returns the image, which is going to be a blank one but means the
     * renderer didn't complain about the exception caught. Otherwise throws back the exception
     * thrown by {@link RenderedImageMapOutputFormat#produceMap()}
     * </p>
     */
    @SuppressWarnings("unchecked")
    private RenderedImage forceRenderingError(final Exception renderExceptionToThrow)
            throws Exception {

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.setMapWidth(100);
        map.setMapHeight(100);
        map.setRequest(request);
        final ReferencedEnvelope bounds = new ReferencedEnvelope(-180, 180, -90, 90,
                DefaultGeographicCRS.WGS84);
        map.getViewport().setBounds(bounds);

        final FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(STREAMS.getNamespaceURI(),
                STREAMS.getLocalPart());

        final SimpleFeatureSource featureSource = (SimpleFeatureSource) ftInfo.getFeatureSource(
                null, null);

        DecoratingFeatureSource source;
        // This source should make the renderer fail when asking for the features
        source = new DecoratingFeatureSource(featureSource) {
            @Override
            public SimpleFeatureCollection getFeatures(Query query) throws IOException {
                throw new RuntimeException(renderExceptionToThrow);
                // return delegate.getFeatures(query);
            }
        };

        StyleInfo someStyle = getCatalog().getStyleByName("line");
        map.addLayer(new FeatureLayer(source, someStyle.getStyle()));
        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();

        return image;
    }

    /**
     * This dummy producer adds no functionality to DefaultRasterMapOutputFormat, just implements a
     * void formatImageOutputStream to have a concrete class over which test that
     * DefaultRasterMapOutputFormat correctly generates the BufferedImage.
     * 
     * @author Gabriel Roldan
     * @version $Id: DefaultRasterMapOutputFormatTest.java 6797 2007-05-16 10:23:50Z aaime $
     */
    private static class DummyRasterMapProducer extends RenderedImageMapOutputFormat {

        public DummyRasterMapProducer(WMS wms) {
            super("image/gif", new String[] { "image/gif" }, wms);
        }
    }

}
