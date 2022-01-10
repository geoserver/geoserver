/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

import gov.nasa.worldwind.formats.dds.DDSConverter;
import gov.nasa.worldwind.util.Logging;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapResponse;

public class DDSMapResponse extends RenderedImageMapResponse {

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/dds";

    /** convenient singleton Set to expose the output format this producer supports */
    private static final Set SUPPORTED_FORMATS = Collections.singleton(MIME_TYPE);

    public DDSMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
    }

    public void formatImageOutputStream(
            RenderedImage img, OutputStream os, WMSMapContent mapContent)
            throws ServiceException, IOException {
        BufferedImage bimg = convertRenderedImage(img);
        ByteBuffer bb = DDSConverter.convertToDxt3(bimg);
        saveBuffer(bb, os);
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
        }

        int numBytesRead = 0;
        // WWIO.saveBuffer(buffer, new File("C:\\testdds\\image.dds"));
        os.write(buffer.array());
        return true;
    }

    /**
     * Covert RenderedImage to BufferedImage with correct colour model (lifted from
     * http://www.jguru.com/faq/view.jsp?EID=114602)
     */
    protected BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage bimg = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return bimg;
    }

    /** DXT3 does support transparency, unless alpha is pre-multiplied */
    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        // FIXME Become more capable
        return new MapProducerCapabilities(false, false, false, true, null);
    }
}
