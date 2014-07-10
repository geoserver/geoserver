/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.SLDParser;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.restlet.data.MediaType;

public class StyleFormat extends StreamDataFormat {

    StyleHandler handler;
    Version version;
    boolean prettyPrint;
    
    public StyleFormat(String mimeType, Version version, boolean prettyPrint, StyleHandler handler) {
        super(new MediaType(mimeType));
        this.version = version;
        this.prettyPrint = prettyPrint;
        this.handler = handler;
    }

    public StyleHandler getHandler() {
        return handler;
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        Style style = (Style) object;
        StyledLayerDescriptor sld = Styles.sld(style);

        handler.encode(sld, version, prettyPrint, out);
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        return Styles.style(handler.parse(in, version, null, null));
    }
}
