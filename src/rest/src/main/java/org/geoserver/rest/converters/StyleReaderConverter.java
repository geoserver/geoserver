/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.xml.sax.EntityResolver;

/**
 * Read {@link Style} using provided mimeType and handler.
 */
public class StyleReaderConverter extends BaseMessageConverter<Style> {

    private final Version version;

    private final StyleHandler handler;

    private final EntityResolver entityResolver;

    public StyleReaderConverter(String mimeType, Version version, StyleHandler handler,
            EntityResolver entityResolver) {
        super(MediaType.valueOf(mimeType));
        this.handler = handler;
        this.version = version;
        this.entityResolver = entityResolver;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Style.class.isAssignableFrom(clazz);
    }

    //
    // reading
    //
    @Override
    public Style readInternal(Class<? extends Style> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        StyledLayerDescriptor sld = handler.parse(inputMessage.getBody(), version, null,
                entityResolver);
        return Styles.style(sld);
    }

    //
    // writing
    //
    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }
}
