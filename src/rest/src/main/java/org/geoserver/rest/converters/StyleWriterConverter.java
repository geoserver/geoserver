/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Write {@link Style} (or {@link StyleInfo}) using provided mimeType and handler. */
public class StyleWriterConverter extends BaseMessageConverter<Object> {

    private final Version version;

    private final StyleHandler handler;

    public StyleWriterConverter(String mimeType, Version version, StyleHandler handler) {
        super(MediaType.valueOf(mimeType));
        this.handler = handler;
        this.version = version;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RestWrapper.class.isAssignableFrom(clazz)
                || Style.class.isAssignableFrom(clazz)
                || StyleInfo.class.isAssignableFrom(clazz);
    }

    //
    // reading
    //
    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    //
    // writing
    //
    @Override
    public void writeInternal(Object object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        if (object instanceof RestWrapper) {
            object = ((RestWrapper<?>) object).getObject();
        }

        if (object instanceof StyleInfo) {
            StyleInfo style = (StyleInfo) object;
            // optimization, if the requested format is the same as the native format
            // of the style, stream the file directly from the disk, otherwise encode
            // the style in the requested format
            if (handler.getFormat().equalsIgnoreCase(style.getFormat())
                    && (style.getFormatVersion() == null
                            || style.getFormatVersion().equals(version))) {
                copyDefinition(style, outputMessage.getBody());
                return;
            }
        }

        StyledLayerDescriptor sld;
        if (object instanceof StyleInfo) {
            // get the full SLD, might be a multi-layer style (calling getStye only retrieves
            // the first UserStyle instead)
            sld = ((StyleInfo) object).getSLD();
        } else {
            Style style = (Style) object;
            sld = Styles.sld(style);
        }

        // TODO: support pretty print somehow - probably a hint
        handler.encode(sld, version, false, outputMessage.getBody());
    }

    void copyDefinition(StyleInfo style, OutputStream out) throws IOException {
        GeoServerDataDirectory dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        Resource resource = dd.style(style);
        try (InputStream in = resource.in()) {
            IOUtils.copy(in, out);
        }
    }

    @Override
    public String toString() {
        return "StyleWriterConverter [version=" + version + ", handler=" + handler + "]";
    }

    public Version getVersion() {
        return version;
    }

    public StyleHandler getHandler() {
        return handler;
    }
}
