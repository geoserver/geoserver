/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

import gov.nasa.worldwind.formats.dds.DDSCompressor;
import gov.nasa.worldwind.formats.dds.DDSConstants;
import gov.nasa.worldwind.formats.dds.DXTCompressionAttributes;
import gov.nasa.worldwind.util.Logging;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.worldwind.etc1.ETC1DDSCompressor;
import org.geoserver.wms.worldwind.etc1.ETCConstants;

public class DDSMapResponse extends RenderedImageMapResponse {

	/** the default MIME type this map producer supports (=DXT3) */
	static final String MIME_TYPE_DEFAULT = "image/dds";
	
	/** the more specificied MIME type this map producer supports */
	static final String MIME_TYPE_DXT1 = "image/dds; format=DXT1";
	static final String MIME_TYPE_DXT3 = "image/dds; format=DXT3";
	static final String MIME_TYPE_ETC1 = "image/dds; format=ETC1";
	
	

	/**
	 * convenient array to expose the output formats this producer
	 * supports
	 */
	private static final String [] SUPPORTED_FORMATS = {MIME_TYPE_DEFAULT, MIME_TYPE_DXT1, MIME_TYPE_DXT3, MIME_TYPE_ETC1};
	
	public DDSMapResponse(WMS wms) {
		super(SUPPORTED_FORMATS, wms);		
	}

	public void formatImageOutputStream(RenderedImage img, OutputStream os, 
	        WMSMapContent mapContent)
			throws ServiceException, IOException {	
        BufferedImage bimg = (BufferedImage) img;
        DXTCompressionAttributes attributes = DDSCompressor.getDefaultCompressionAttributes();

        if (mapContent.getRequest().getFormat().equals(MIME_TYPE_ETC1)) {
              attributes.setDXTFormat(ETCConstants.D3DFMT_ETC1);
        } else if (mapContent.getRequest().getFormat().equals(MIME_TYPE_DXT1)) {
        	attributes.setDXTFormat(DDSConstants.D3DFMT_DXT1);
        } else if (mapContent.getRequest().getFormat().equals(MIME_TYPE_DXT3)) {
        	attributes.setDXTFormat(DDSConstants.D3DFMT_DXT3);
        }

        ETC1DDSCompressor compressor = new ETC1DDSCompressor();

        attributes.setBuildMipmaps(true);

        ByteBuffer buffer = compressor.compressImage(bimg, attributes);

        saveBuffer(buffer, os);
	}
	
	public static boolean saveBuffer(ByteBuffer buffer, OutputStream os) throws IOException {
        if (buffer == null) {
            String message = "nullValue.BufferNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (os == null) {
            String message = "nullValue.FileIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        } else {
            BufferedOutputStream bos = new BufferedOutputStream(os);
            
            byte[] arrayBytes = new byte[buffer.remaining()];
            int counter = 0;
            // Extract array of bytes from buffer because the array method is not available for MappedByteBuffer
            while (buffer.hasRemaining()) {
                arrayBytes[counter] = buffer.get();
                counter++;
            }

            bos.write(arrayBytes);

            return true;
        }
    }

	/**
	 * DXT3 does support transparency, unless alpha is pre-multiplied
	 * ETC1 does not support transparency at all
	 */
	@Override
	public MapProducerCapabilities getCapabilities(String outputFormat) {
		if (outputFormat.equals(MIME_TYPE_DEFAULT) || outputFormat.equals(MIME_TYPE_DXT3)) {
			return new MapProducerCapabilities(false, false, false, true, null);
		} else {
			return new MapProducerCapabilities(false, false, false, false, null);
		}
		
	}

}
