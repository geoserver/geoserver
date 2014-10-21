/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.SLDParser;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;

public class StyleFormat extends StreamDataFormat {

    StyleHandler handler;
    Version version;
    boolean prettyPrint;
    Request request;
    
    public StyleFormat(String mimeType, Version version, boolean prettyPrint, StyleHandler handler, Request request) {
        super(new MediaType(mimeType));
        this.version = version;
        this.prettyPrint = prettyPrint;
        this.handler = handler;
        this.request = request;
    }

    public StyleHandler getHandler() {
        return handler;
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        if (object instanceof StyleInfo) {
            StyleInfo style = (StyleInfo) object;
            // optimization, if the requested format is the same as the native format
            // of the style, stream the file directly from the disk, otherwise encode
            // the style in the requested format
            if (handler.getFormat().equalsIgnoreCase(style.getFormat())) {
                copyFromFile(style, out);
                return;
            }
        }

        Style style = object instanceof StyleInfo ? ((StyleInfo)object).getStyle() : (Style) object;
        StyledLayerDescriptor sld = Styles.sld(style);
        handler.encode(sld, version, prettyPrint, out);
    }

    void copyFromFile(StyleInfo style, OutputStream out) throws IOException {
        GeoServerDataDirectory dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        Resource resource = dd.style(style);
        InputStream in = resource.in();
        try {
            IOUtils.copy(in, out);
        }
        finally {
            in.close();
        }
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        if (isRawUpload(request)) {
            return in;
        }
        return Styles.style(handler.parse(in, version, null, null));
    }

    boolean isRawUpload(Request request) {
        Form q = request.getResourceRef().getQueryAsForm();
        String raw = q.getFirstValue("raw");
        return raw != null && Boolean.TRUE.equals(Converters.convert(raw, Boolean.class));
    }
}
