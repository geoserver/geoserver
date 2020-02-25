/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.StyleWriterConverter;
import org.geotools.util.Version;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class StylesInfoConverter implements HttpMessageConverter<StyleInfo> {

    private final GeoServerDataDirectory dataDirectory;
    private final List<StyleHandler> handlers;
    Map<MediaType, StyleWriterConverter> writers = new HashMap<>();
    List<MediaType> mediaTypes = new ArrayList<>();

    public StylesInfoConverter(
            GeoServerExtensions extensions, GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
        handlers = extensions.extensions(StyleHandler.class);
        for (StyleHandler sh : handlers) {
            for (Version ver : sh.getVersions()) {
                String mimeType = sh.mimeType(ver);
                MediaType mediaType = MediaType.valueOf(mimeType);
                writers.put(mediaType, new StyleWriterConverter(mimeType, ver, sh));
                mediaTypes.add(mediaType);
            }
        }
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return StyleInfo.class.isAssignableFrom(clazz)
                && (mediaType == null || getWriter(mediaType).isPresent());
    }

    private Optional<StyleWriterConverter> getWriter(MediaType mediaType) {
        return writers.entrySet()
                .stream()
                .filter(e -> e.getKey().isCompatibleWith(mediaType))
                .map(e -> e.getValue())
                .findFirst();
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(mediaTypes);
    }

    @Override
    public StyleInfo read(Class<? extends StyleInfo> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(StyleInfo styleInfo, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (contentType == null || contentType.isCompatibleWith(getNativeMediaType(styleInfo))) {
            copyDefinition(styleInfo, outputMessage.getBody());
        } else {
            StyleWriterConverter writer =
                    getWriter(contentType)
                            .orElseThrow(
                                    () ->
                                            new RestException(
                                                    "Cannot write style as " + mediaTypes,
                                                    HttpStatus.UNSUPPORTED_MEDIA_TYPE));
            writer.write(styleInfo, contentType, outputMessage);
        }
    }

    private MediaType getNativeMediaType(StyleInfo styleInfo) {
        String format = styleInfo.getFormat();
        if (format == null) {
            return MediaType.valueOf(SLDHandler.MIMETYPE_10);
        }
        StyleHandler handler =
                handlers.stream()
                        .filter(sh -> styleInfo.getFormat().equals(sh.getFormat()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new RestException(
                                                "Could not find style handler for style "
                                                        + styleInfo.prefixedName(),
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        Version version = styleInfo.getFormatVersion();
        if (version == null) version = handler.getVersions().get(0);
        return MediaType.valueOf(handler.mimeType(version));
    }

    void copyDefinition(StyleInfo style, OutputStream out) throws IOException {
        Resource resource = dataDirectory.style(style);
        try (InputStream in = resource.in()) {
            IOUtils.copy(in, out);
        }
    }
}
