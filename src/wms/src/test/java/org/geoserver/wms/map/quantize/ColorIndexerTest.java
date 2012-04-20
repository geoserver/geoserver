/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.map.quantize;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;

import junit.framework.TestCase;

import org.geotools.image.ImageWorker;
import org.geotools.image.palette.CustomPaletteBuilder;
import org.geotools.image.palette.InverseColorMapRasterOp;

/**
 * Testing custom code for color reduction.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions
 *
 * @source $URL$
 */
public class ColorIndexerTest extends TestCase {
    
    static {
        ColorIndexerDescriptor.register();
    }

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
}
