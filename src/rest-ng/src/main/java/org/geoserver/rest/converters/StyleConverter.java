package org.geoserver.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
 * Style converters based on the old StyleFormat
 */
public class StyleConverter extends BaseMessageConverter {

    private final List<MediaType> supportedMediaTypes;

    private final Version version;

    private final StyleHandler handler;

    private final EntityResolver entityResolver;

    public StyleConverter(String mimeType, Version version, StyleHandler handler, EntityResolver entityResolver) {
        supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.valueOf(mimeType));
        this.handler = handler;
        this.version = version;
        this.entityResolver = entityResolver;
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return Style.class.equals(clazz) && isSupportedMediaType(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return (Style.class.equals(clazz) || StyleInfo.class.isAssignableFrom(clazz)) &&
                isSupportedMediaType(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        return Styles.style(handler.parse(inputMessage.getBody(), version, null, entityResolver));
    }

    @Override
    public void write(Object object, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        if (object instanceof RestWrapper) {
            object = ((RestWrapper) object).getObject();
        }
        if (object instanceof StyleInfo) {
            StyleInfo style = (StyleInfo) object;
            // optimization, if the requested format is the same as the native format
            // of the style, stream the file directly from the disk, otherwise encode
            // the style in the requested format
            if (handler.getFormat().equalsIgnoreCase(style.getFormat())) {
                copyFromFile(style, outputMessage.getBody());
                return;
            }
        }

        Style style = object instanceof StyleInfo ? ((StyleInfo)object).getStyle() : (Style) object;
        StyledLayerDescriptor sld = Styles.sld(style);
        //todo support pretty print somehow
        handler.encode(sld, version, false, outputMessage.getBody());
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
}
