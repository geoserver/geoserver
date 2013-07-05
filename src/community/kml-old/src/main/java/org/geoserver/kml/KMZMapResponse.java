/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;

import org.geoserver.kml.icons.IconRenderer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapResponse;
import org.geoserver.wms.map.PNGMapResponse;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.XMLTransformerMap;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.geotools.xml.transform.TransformerBase;
import org.springframework.util.Assert;

/**
 * Handles a GetMap response in KMZ format.
 * <p>
 * KMZ files are a zipped KML file. The KML file must have an emcompasing <document> or <folder>
 * element. So if you have many different placemarks or ground overlays, they all need to be
 * contained within one <document> element, then zipped up and sent off with the extension "kmz".
 * </p>
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $
 * @author $Author: Brent Owens
 * @author Justin Deoliveira
 * 
 */
public class KMZMapResponse extends AbstractMapResponse {
    
    private WMS wms;

    public static class KMZMap extends XMLTransformerMap {
        public KMZMap(final WMSMapContent mapContent, TransformerBase transformer, String mimeType) {
            super(mapContent, transformer, mapContent, mimeType);
        }
    }

    public KMZMapResponse(WMS wms) {
        super(KMZMap.class, KMZMapOutputFormat.OUTPUT_FORMATS);
        this.wms = wms;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    /**
     * Makes the map and sends it to the zipped output stream The produceMap() method does not
     * create the map in this case. We produce the map here so we can stream directly to the
     * response output stream, and not have to write to disk, then send it to the stream.
     * 
     * @param value
     *            a {@link XMLTransformerMap} as produced by this class'
     *            {@link #produceMap(WMSMapContent)}
     * @param out
     *            OutputStream to stream the map to.
     * 
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        Assert.isInstanceOf(XMLTransformerMap.class, value);

        final XMLTransformerMap map = (XMLTransformerMap) value;
        try {
            final KMLTransformer transformer = (KMLTransformer) map.getTransformer();
            final WMSMapContent mapContent = (WMSMapContent) map.getTransformerSubject();

            // wrap the output stream in a zipped one
            ZipOutputStream zip = new ZipOutputStream(output);

            // first create an entry for the kml
            ZipEntry entry = new ZipEntry("wms.kml");
            zip.putNextEntry(entry);

            try {
                transformer.transform(mapContent, zip);
                zip.closeEntry();
            } catch (TransformerException e) {
                throw (IOException) new IOException().initCause(e);
            }

            final RenderedImageMapOutputFormat pngProducer = new RenderedImageMapOutputFormat(
                    "image/png", wms);
            final PNGMapResponse pngEncoder = new PNGMapResponse(wms);

            ZipEntry images = new ZipEntry("images/");
            zip.putNextEntry(images);
            
            // write the images
            List<Layer> layers = mapContent.layers();
            for (int i = 0; i < layers.size(); i++) {
                Layer mapLayer = layers.get(i);

                // create a context for this single layer
                WMSMapContent subContext = new WMSMapContent();
                subContext.addLayer(mapLayer);
                subContext.setRequest(mapContent.getRequest());
                subContext.setMapHeight(mapContent.getMapHeight());
                subContext.setMapWidth(mapContent.getMapWidth());
                subContext.getViewport().setBounds(mapContent.getRenderingArea());
                subContext.setBgColor(mapContent.getBgColor());
                subContext.setBuffer(mapContent.getBuffer());
                subContext.setContactInformation(mapContent.getContactInformation());
                subContext.setKeywords(mapContent.getKeywords());
                subContext.setAbstract(mapContent.getAbstract());
                subContext.setTransparent(true);

                // render the map
                RenderedImageMap imageMap;
                try {
                    imageMap = pngProducer.produceMap(subContext);
                } finally {
                    subContext.dispose();
                }

                // write it to the zip stream
                entry = new ZipEntry("images/layer_" + i + ".png");
                zip.putNextEntry(entry);
                pngEncoder.write(imageMap, zip, operation);
                zip.closeEntry();
            }
            zip.closeEntry();// close the images/ folder
            
            Map<String, Style> embeddedIcons = transformer.getEmbeddedIcons();
            ZipEntry icons = new ZipEntry("icons/");
            zip.putNextEntry(icons);
            for (Map.Entry<String, Style> namedStyle : embeddedIcons.entrySet()) {
                final String name = namedStyle.getKey();
                final Style style = namedStyle.getValue();
                BufferedImage icon = IconRenderer.renderIcon(style);
                entry = new ZipEntry("icons/" + name + ".png");
                zip.putNextEntry(entry);
                ImageIO.write(icon, "PNG", zip);
            }
            zip.closeEntry();

            zip.finish();
            zip.flush();
        } finally {
            map.dispose();
        }
        
    }

}
