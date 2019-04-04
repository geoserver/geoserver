/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.icons.IconRenderer;
import org.geoserver.wms.map.AbstractMapResponse;
import org.geoserver.wms.map.PNGMapResponse;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.map.Layer;
import org.geotools.styling.Style;

/**
 * A map response that handles KML documents and writes them out either as KML or as KMZ
 *
 * @author Andrea Aime - GeoSolutions
 */
public class KMLMapResponse extends AbstractMapResponse {

    private WMS wms;
    private KMLEncoder encoder;

    public KMLMapResponse(KMLEncoder encoder, WMS wms) {
        super(KMLMap.class, (Set<String>) null);
        this.wms = wms;
        this.encoder = encoder;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        KMLMap kmlMap = (KMLMap) value;
        try {
            KmlEncodingContext context = kmlMap.getKmlEncodingContext();
            Kml kml = kmlMap.getKml();
            if (context != null && context.isKmz()) {
                encodeAsKmz(kml, context, operation, output);
            } else {
                encoder.encode(kml, output, context);
            }
        } finally {
            kmlMap.dispose();
        }
    }

    private void encodeAsKmz(
            Kml kml, KmlEncodingContext context, Operation operation, OutputStream output)
            throws IOException {
        // wrap the output stream in a zipped one
        ZipOutputStream zip = new ZipOutputStream(output);

        // first create an entry for the kml
        ZipEntry entry = new ZipEntry("wms.kml");
        zip.putNextEntry(entry);
        encoder.encode(kml, zip, context);

        // prepare for the ground overlays
        final RenderedImageMapOutputFormat pngProducer =
                new RenderedImageMapOutputFormat("image/png", wms);
        final PNGMapResponse pngEncoder = new PNGMapResponse(wms);
        ZipEntry images = new ZipEntry("images/");
        zip.putNextEntry(images);
        // write the images
        WMSMapContent mapContent = context.getMapContent();
        for (Entry<String, Layer> goEntry : context.getKmzGroundOverlays().entrySet()) {
            String path = goEntry.getKey();
            Layer mapLayer = goEntry.getValue();

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

                // write it to the zip stream
                entry = new ZipEntry(path);
                zip.putNextEntry(entry);
                pngEncoder.write(imageMap, zip, operation);
                zip.closeEntry();
            } finally {
                subContext.dispose();
            }
        }
        zip.closeEntry(); // close the images/ folder

        // write out the icons
        Map<String, Style> embeddedIcons = context.getIconStyles();
        if (!embeddedIcons.isEmpty()) {
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
        }

        zip.finish();
        zip.flush();
    }
}
