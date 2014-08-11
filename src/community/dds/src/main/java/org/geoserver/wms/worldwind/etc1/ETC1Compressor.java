/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind.etc1;

import gov.nasa.worldwind.formats.dds.DXTCompressionAttributes;
import gov.nasa.worldwind.formats.dds.DXTCompressor;
import gov.nasa.worldwind.util.Logging;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;

/**
 * @author nicastel
 */
public class ETC1Compressor implements DXTCompressor
{
    public ETC1Compressor()
    {
    }

    public int getDXTFormat()
    {
        return ETCConstants.D3DFMT_ETC1;
    }

    public int getCompressedSize(java.awt.image.BufferedImage image, DXTCompressionAttributes attributes)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // TODO: comment, provide documentation reference

        int width = Math.max(image.getWidth(), 4);
        int height = Math.max(image.getHeight(), 4);

        return (width * height) / 2;
    }

    public void compressImage(java.awt.image.BufferedImage image, DXTCompressionAttributes attributes,
        java.nio.ByteBuffer buffer)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }        

        int [] band = {0,1,2};
        Raster child = image.getData().createChild(0, 0, image.getWidth(), image.getHeight(), 0, 0, band);
        
        // If it is determined that the image and block have no alpha component, then we compress with DXT1 using a
        // four color palette. Otherwise, we use the three color palette (with the fourth color as transparent black).
        final int kYMask[] = { 0x0, 0xf, 0xff, 0xfff, 0xffff };
	    final int kXMask[] = { 0x0, 0x1111, 0x3333, 0x7777,
	            0xffff };
	    byte [] block = new byte [ETCConstants.DECODED_BLOCK_SIZE];
	    byte [] encoded = new byte [ETCConstants.ENCODED_BLOCK_SIZE];

	    int width = child.getWidth();
	    int height = child.getHeight();
	    
	    int encodedWidth = (width + 3) & ~3;
	    int encodedHeight = (height + 3) & ~3;
	    
	    int pixelSize = 1;	    
	    
	    int stride = pixelSize * width;
	    
	    DataBuffer pIn = child.getDataBuffer();

	    for (int y = 0; y < encodedHeight; y += 4) {
	    	int yEnd = height - y;
	        if (yEnd > 4) {
	            yEnd = 4;
	        }
	        int ymask = kYMask[yEnd];
	        for (int x = 0; x < encodedWidth; x += 4) {
	        	int xEnd = width - x;
	            if (xEnd > 4) {
	                xEnd = 4;
	            }
	            int mask = ymask & kXMask[xEnd];
	            for (int cy = 0; cy < yEnd; cy++) {
	            	int q = (cy * 4) * 3;
	            	int p = pixelSize * x + stride * (y + cy);
	                // (pixelSize == 1 * 32 bit) {
                	for (int cx = 0; cx < xEnd; cx++) {
                        int pixel = pIn.getElem(p);
                        block[q++] = (byte) ((pixel >> 16) & 0xFF);
                        block[q++] = (byte) ((pixel >> 8) & 0xFF);
                        block[q++] = (byte) (pixel & 0xFF);
                        p += pixelSize;
                    }	                
	            }
	            BlockETC1Compressor.encodeBlock(block, mask, encoded);
	            buffer.put(encoded);
	        }
	    }
    }
}
