/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RenderedImageMap;

import com.mockrunner.mock.web.MockHttpServletResponse;


/**
 * Some functional tests for animator
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class AnimatorTest extends WMSTestSupport {

    /** default 'format' value */
    public static final String GIF_ANIMATED_FORMAT = "image/gif;subtype=animated";
    

    /**
     * Testing FrameCatalog constructor from a generic WMS request.
     * 
     * @throws Exception
     */
    @org.junit.Test
    public void testFrameCatalog() throws Exception {
    	final WebMapService wms = (WebMapService) applicationContext.getBean("wmsService2");
    	final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" +
        	MockData.BASIC_POLYGONS.getLocalPart();
    	
    	GetMapRequest getMapRequest = createGetMapRequest(new QName(layerName));
    	
    	FrameCatalog catalog = null;
    	try {
    		catalog = new FrameCatalog(getMapRequest, wms, getWMS());
    	} catch (RuntimeException e) {
    		assertEquals("Missing \"animator\" mandatory params \"aparam\" and \"avalues\".", e.getLocalizedMessage());
    	}
    	
    	getMapRequest.getRawKvp().put("aparam", "fake_param");
    	getMapRequest.getRawKvp().put("avalues", "val0,val\\,1,val2\\,\\,,val3");
    	
    	catalog = new FrameCatalog(getMapRequest, wms, getWMS());
    	
    	assertNotNull(catalog);
    	assertEquals("fake_param", catalog.getParameter());
    	assertEquals(4, catalog.getValues().length);
    	assertEquals("val0", catalog.getValues()[0]);
    	assertEquals("val\\,1", catalog.getValues()[1]);
    	assertEquals("val2\\,\\,", catalog.getValues()[2]);
    	assertEquals("val3", catalog.getValues()[3]);
    }
    
    /**
     * Testing FrameVisitor animation frames setup and production.
     * 
     * @throws Exception
     */
    @org.junit.Test
    public void testFrameVisitor() throws Exception {
    	final WebMapService wms = (WebMapService) applicationContext.getBean("wmsService2");
    	final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" +
        	MockData.BASIC_POLYGONS.getLocalPart();
    	
    	GetMapRequest getMapRequest = createGetMapRequest(new QName(layerName));
    	
    	FrameCatalog catalog = null;
    	
    	getMapRequest.getRawKvp().put("aparam", "fake_param");
    	getMapRequest.getRawKvp().put("avalues", "val0,val\\,1,val2\\,\\,,val3");
    	getMapRequest.getRawKvp().put("format", GIF_ANIMATED_FORMAT);
    	getMapRequest.getRawKvp().put("LAYERS", layerName);
    	
    	catalog = new FrameCatalog(getMapRequest, wms, getWMS());
    	
    	assertNotNull(catalog);

    	FrameCatalogVisitor visitor = new FrameCatalogVisitor();
    	catalog.getFrames(visitor);
    	
    	assertEquals(4, visitor.framesNumber);
    	
    	List<RenderedImageMap> frames = visitor.produce(getWMS());
    	
    	assertNotNull(frames);
    	assertEquals(4, frames.size());
    }
    
    /**
     * Produce animated gif through the WMS request.
     */
    @org.junit.Test
    public void testAnimator() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" +
            MockData.BASIC_POLYGONS.getLocalPart();

        String requestURL = "wms/animate?layers=" + layerName + "&aparam=fake_param&avalues=val0,val\\,1,val2\\,\\,,val3";
        
        MockHttpServletResponse resp = getAsServletResponse(requestURL);
        
        assertEquals("image/gif", resp.getContentType());
    }
    
    /**
     * Animate layers
     */
    @org.junit.Test
    public void testAnimatorLayers() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" +
            MockData.BASIC_POLYGONS.getLocalPart();

        String requestURL = "cite/wms/animate?&aparam=layers&avalues=MapNeatline,Buildings,Lakes";
        
        // check we got a gif
        MockHttpServletResponse resp = getAsServletResponse(requestURL);
        assertEquals("image/gif", resp.getContentType());
        
        // check it has three frames
        ByteArrayInputStream bis = getBinaryInputStream(resp);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(iis);
        assertEquals(3, reader.getNumImages(true));
    }
}
