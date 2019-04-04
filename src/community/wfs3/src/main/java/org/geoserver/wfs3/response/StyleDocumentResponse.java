/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs3.GetStyleRequest;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.springframework.http.HttpStatus;

public class StyleDocumentResponse extends Response {

    private final Catalog catalog;

    public StyleDocumentResponse(Catalog catalog) {
        super(StyleInfo.class, getStyleFormats());
        this.catalog = catalog;
    }

    private static Set<String> getStyleFormats() {
        Set<String> result = new HashSet<>();
        for (StyleHandler handler : Styles.handlers()) {
            for (Version version : handler.getVersions()) {
                result.add(handler.mimeType(version));
                result.add(handler.getName().toLowerCase());
            }
        }

        return result;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetStyleRequest request = (GetStyleRequest) operation.getParameters()[0];
        final StyleInfo style = (StyleInfo) value;
        String requestedFormat = getRequestedFormat(request, style);
        final StyleHandler handler = Styles.handler(requestedFormat);
        return handler.mimeType(style.getFormatVersion());
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GetStyleRequest request = (GetStyleRequest) operation.getParameters()[0];
        StyleInfo style = (StyleInfo) value;
        String requestedFormat = getRequestedFormat(request, style);

        final StyleHandler handler = Styles.handler(requestedFormat);
        if (handler == null) {
            throw new HttpErrorCodeException(
                    HttpStatus.BAD_REQUEST.value(), "Cannot encode style in " + requestedFormat);
        }

        // if no conversion is needed, push out raw style
        if (Objects.equals(handler.getFormat(), style.getFormat())) {
            try (final BufferedReader reader = catalog.getResourcePool().readStyle(style)) {
                OutputStreamWriter writer = new OutputStreamWriter(output);
                IOUtils.copy(reader, writer);
                writer.flush();
            }
        } else {
            // otherwise convert if possible
            final StyledLayerDescriptor sld = style.getSLD();
            if (sld.getName() == null || sld.getName().isEmpty()) {
                sld.setName(style.getName());
            }
            handler.encode(sld, null, true, output);
        }
    }

    public String getRequestedFormat(GetStyleRequest request, StyleInfo style) {
        String requestedFormat = request.getOutputFormat();
        if (requestedFormat == null) {
            requestedFormat = style.getFormat();
        }
        if (requestedFormat == null) {
            requestedFormat = SLDHandler.FORMAT;
        }
        return requestedFormat;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        StyleInfo style = (StyleInfo) value;
        return style.getName() + "." + (style.getFormat() == null ? ".style" : style.getFormat());
    }
}
