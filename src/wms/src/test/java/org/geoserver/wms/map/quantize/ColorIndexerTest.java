/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 *           (c) 2006-2008 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;

import javax.media.jai.TiledImage;

import org.geotools.image.ImageWorker;
import org.junit.Test;

/**
 * Testing custom code for color reduction.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions
 *
 * @source $URL$
 */
public class ColorIndexerTest {
    
    static {
        ColorIndexerDescriptor.register();
    }

    @Test
    public void test2BandsBug() {
        // build a transparent image
        BufferedImage image = new BufferedImage(256,256,BufferedImage.TYPE_BYTE_GRAY);
        image=new ImageWorker(image).addBand(image, true).getBufferedImage();
        
        
        // create a palette out of it
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        
        // png encoder go mad if they get a one element palette, we need at least two
        assertEquals(2, icm.getMapSize());
    }

    private RenderedImage quantize(RenderedImage image) {
        Quantizer q = new Quantizer(256);
        ColorIndexer indexer = q.buildColorIndexer(image);
        RenderedImage indexed = ColorIndexerDescriptor.create(image, indexer, null);
        return indexed;
    }
    
    @Test
    public void testOneColorBug() {
        // build a transparent image
        BufferedImage image = new BufferedImage(256, 256,
                BufferedImage.TYPE_4BYTE_ABGR);
        
        // create a palette out of it
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        
        // png encoder go mad if they get a one element palette, we need at least two
        assertEquals(2, icm.getMapSize());
    }
	
    @Test
    public void testGrayColor() {
		BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = image.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 20, 20);
		g.setColor(new Color(20, 20, 20)); // A dark gray
		g.fillRect(20, 20, 20, 20);
		g.setColor(new Color(200, 200, 200)); // A light gray
		g.fillRect(0, 20, 20, 20);
		g.dispose();
        RenderedImage indexed =  quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        assertEquals(4, icm.getMapSize()); //Black background, white fill, light gray fill, dark gray fill = 4 colors
		
	}
	
    @Test
    public void testTranslatedImage() throws Exception {
	    BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        TiledImage image = new TiledImage(0, 0, 256, 256, 1, 1, bi.getSampleModel().createCompatibleSampleModel(256, 256), bi.getColorModel());
        Graphics g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 20, 20);
        g.setColor(new Color(20, 20, 20)); // A dark gray
        g.fillRect(20, 20, 20, 20);
        g.setColor(new Color(200, 200, 200)); // A light gray
        g.fillRect(0, 20, 20, 20);
        g.dispose();
        RenderedImage indexed =  quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        assertEquals(4, icm.getMapSize()); //Black background, white fill, light gray fill, dark gray fill = 4 colors
    }
	
    
    @Test
    public void testFourColor() {
        // build a transparent image
        BufferedImage image = new BufferedImage(256, 256,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.RED);
        g.fillRect(10, 0, 10, 10);
        g.setColor(Color.BLUE);
        g.fillRect(20, 0, 10, 10);
        g.setColor(Color.GREEN);
        g.fillRect(30, 0, 10, 10);
        g.dispose();
        
        //
        // create a palette out of it
        //
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        
        // make sure we have 4 colors + transparent one
        assertEquals(5, icm.getMapSize());
    }
    
    
    public void testTranslatedImageTileGrid() {
    	BufferedImage image_ = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image_.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(236, 236, 20, 20);
        g.setColor(new Color(80, 80, 80)); // A dark gray
        g.fillRect(216, 216, 20, 20);
        g.setColor(new Color(200, 200, 200)); // A light gray
        g.fillRect(216, 236, 20, 20);
        g.dispose();
        
        
        TiledImage image=new TiledImage(
                0, 
                0, 
                256, 
                256, 
                128, 
                128, 
                image_.getColorModel().createCompatibleSampleModel(256, 256), 
                image_.getColorModel());
        image.set(image_);
        
        
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        assertEquals(4, icm.getMapSize()); //Black background, white fill, light gray fill, dark gray fill = 4 colors
        
        // check image not black
        ImageWorker iw = new ImageWorker(indexed).forceComponentColorModel().intensity();
        double[] mins = iw.getMinimums();
        double[] maxs = iw.getMaximums();
        boolean result=true;
        for(int i=0;i<mins.length;i++){
        	result=mins[i]==maxs[i]?false:result;
        }
        assertTrue(result);
    }
}
