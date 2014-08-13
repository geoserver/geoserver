/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.kml.KMLEncoder;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.format.StreamDataFormat;
import org.restlet.data.MediaType;
import org.springframework.util.Assert;

public class LayerKMLDocumentFormat extends StreamDataFormat {

    private static final MediaType MEDIA_TYPE = new MediaType(
            "application/vnd.google-earth.kml+xml", "Keyhole Markup Language");
    static {
        MediaTypes.registerExtension("kml", MEDIA_TYPE);
    }

    private KMLEncoder encoder;

    public LayerKMLDocumentFormat(KMLEncoder encoder) {
        super(MEDIA_TYPE);
        this.encoder = encoder;
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
        Assert.isInstanceOf(KmlEncodingBundle.class, object);
        
        KmlEncodingBundle bundle = (KmlEncodingBundle) object;
        encoder.encode(bundle.kml, out, bundle.context);
    }

}
