/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/** Allows encoding in JPEG or PNG depending on whether the image has transparency, or not */
public class JpegPngMapResponse extends RenderedImageMapResponse {

    public static final String MIME = "image/vnd.jpeg-png";
    public static final String MIME8 = "image/vnd.jpeg-png8";

    private static final String[] OUTPUT_FORMATS = {MIME, MIME8};

    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, false, false, true, null);

    private PNGMapResponse pngResponse;

    private JPEGMapResponse jpegResponse;

    public JpegPngMapResponse(WMS wms, JPEGMapResponse jpegResponse, PNGMapResponse pngResponse) {
        super(OUTPUT_FORMATS, wms);
        this.jpegResponse = jpegResponse;
        this.pngResponse = pngResponse;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        RenderedImageMap map = ((RenderedImageMap) value);
        return JpegOrPngChooser.getFromMap(map).getMime();
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     *
     * @see RasterMapOutputFormat#formatImageOutputStream(RenderedImage, OutputStream)
     */
    @Override
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {
        JpegOrPngChooser chooser = JpegOrPngChooser.getFromMapContent(image, mapContent);
        if (chooser.isJpegPreferred()) {
            jpegResponse.formatImageOutputStream(image, outStream, mapContent);
        } else {
            pngResponse.formatImageOutputStream(image, outStream, mapContent);
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        JpegOrPngChooser chooser = JpegOrPngChooser.getFromMapContent(image, mapContent);
        if (chooser.isJpegPreferred()) {
            return "jpg";
        } else {
            return "png";
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        String fileName = ((WebMap) value).getAttachmentFileName();
        int idx = fileName.lastIndexOf(".");
        if (idx > 0) {
            return fileName.substring(0, idx)
                    + "."
                    + getExtension(
                            ((RenderedImageMap) value).getImage(),
                            ((RenderedImageMap) value).getMapContext());
        } else {
            return fileName;
        }
    }
}
