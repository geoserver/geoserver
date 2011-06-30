/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.wms.map.XMLTransformerMap;
import org.geotools.xml.transform.TransformerBase;
import org.restlet.data.MediaType;
import org.springframework.util.Assert;

public class LayerKMLDocumentFormat extends StreamDataFormat {

    private static final MediaType MEDIA_TYPE = new MediaType(
            "application/vnd.google-earth.kml+xml", "Keyhole Markup Language");
    static {
        MediaTypes.registerExtension("kml", MEDIA_TYPE);
    }

    public LayerKMLDocumentFormat() {
        super(MEDIA_TYPE);
    }

    /**
     * Unsupported.
     * 
     * @see org.geoserver.rest.format.StreamDataFormat#read(java.io.InputStream)
     */
    @Override
    protected Object read(InputStream in) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes out the sitemap to the given output stream.
     * 
     * @see org.geoserver.rest.format.StreamDataFormat#write(java.lang.Object, java.io.OutputStream)
     */
    @Override
    protected void write(final Object object, OutputStream out) throws IOException {
        // Assert.isTrue((object instanceof LayerInfo) || (object instanceof LayerGroupInfo));
        Assert.isInstanceOf(XMLTransformerMap.class, object);

        XMLTransformerMap map = (XMLTransformerMap) object;
        TransformerBase transformer = map.getTransformer();
        Object transformerSubject = map.getTransformerSubject();
        try {
            transformer.transform(transformerSubject, out);
        } catch (TransformerException e) {
            throw (IOException) new IOException("Error creating KML document: " + e.getMessage())
                    .initCause(e.getCause() == null ? e : e.getCause());
        }

        // final WMSMapContext context = (WMSMapContext) object;
        // final WMS wms = GeoServerExtensions.bean(WMS.class);
        // KMLMetadataDocumentTransformer transformer;
        // transformer = new KMLMetadataDocumentTransformer(wms);
        //
        // try {
        // transformer.transform(context, out);
        // } catch (TransformerException e) {
        // throw new IOException(e.getCause());
        // }
        // // final XMLStreamWriter writer;
        // // try {
        // // XMLOutputFactory factory;
        // // try {
        // // factory = XMLOutputFactory.newInstance();
        // // } catch (FactoryConfigurationError e) {
        // // throw new IOException(e);
        // // }
        // // writer = factory.createXMLStreamWriter(out, "UTF-8");
        // // encode(writer, layerName, title, description, keywords, latLonBoundingBox);
        // // } catch (XMLStreamException e) {
        // // throw new IOException(e);
        // // }
    }

}
