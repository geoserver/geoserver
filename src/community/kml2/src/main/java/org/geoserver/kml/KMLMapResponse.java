/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.geoserver.kml.decorator.KmlEncodingContext;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapResponse;
import org.geoserver.wms.map.PNGMapResponse;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.map.Layer;

import de.micromata.opengis.kml.v_2_2_0.Kml;

public class KMLMapResponse extends AbstractMapResponse {

    private WMS wms;

    public KMLMapResponse(WMS wms) {
        super(KMLMap.class, (Set<String>) null);
        this.wms = wms;
    }
    
    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        KMLMap kmlMap = (KMLMap) value;
        try {
            KmlEncodingContext context = kmlMap.getKmlEncodingContext();
            Kml kml = kmlMap.getKml();
            if(context != null && context.isKmz()) {
                encodeAsKmz(kml, context, operation, output);
            } else {
                encodeAsKml(kml, output);
            }
        } finally {
            kmlMap.dispose();
        }
    }

    private void encodeAsKmz(Kml kml, KmlEncodingContext context, Operation operation, OutputStream output) throws IOException {
        // wrap the output stream in a zipped one
        ZipOutputStream zip = new ZipOutputStream(output);

        // first create an entry for the kml
        ZipEntry entry = new ZipEntry("wms.kml");
        zip.putNextEntry(entry);
        encodeAsKml(kml, zip);

        // prepare for the ground overlays
        final RenderedImageMapOutputFormat pngProducer = new RenderedImageMapOutputFormat(
                "image/png", wms);
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
            } finally {
                subContext.dispose();
            }

            // write it to the zip stream
            entry = new ZipEntry(path);
            zip.putNextEntry(entry);
            pngEncoder.write(imageMap, zip, operation);
            zip.closeEntry();
        }
        zip.closeEntry();// close the images/ folder

        zip.finish();
        zip.flush();
        
    }

    private void encodeAsKml(Kml kml, OutputStream output) {
        try {
            createMarshaller().marshal(kml, output);
        } catch (JAXBException e) {
            throw new ServiceException(e);
        } 
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller m = JAXBContext.newInstance((Kml.class)).createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // hmm... this one is nasty, without the reference implementation the prefixes
        // are going to be a bit ugly. Not a big deal, to solve look at
        // http://cglib.sourceforge.net/xref/samples/Beans.html
        // try {
        // Class.forName("com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper");
        // m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new JKD6PrefixMapper());
        // } catch(Exception e) {
        //
        // }

        return m;
    }

}
