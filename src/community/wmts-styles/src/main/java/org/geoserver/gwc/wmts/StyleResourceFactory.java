/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.styling.StyledLayerDescriptor;
import org.geowebcache.service.HttpErrorCodeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class StyleResourceFactory implements ResourceFactory {

    Catalog catalog;

    public StyleResourceFactory(Catalog catalog) {
        this.catalog = catalog;
    }

    final Pattern stylePattern =
            Pattern.compile(".*/service/wmts/reststyles/layers/([^/]+)/styles/([^/]+)");

    @Override
    public Resource getResourceFor(HttpServletRequest request) {
        final Matcher matcher = stylePattern.matcher(request.getPathInfo());
        if (matcher.matches()) {
            return new StyleResource(matcher.group(1), matcher.group(2), request);
        }

        return null;
    }

    class StyleResource implements Resource {

        private final String styleName;
        private final String layerName;
        private final PublishedInfo published;
        private final String format;
        private final HttpServletRequest request;

        public StyleResource(String layerName, String styleName, HttpServletRequest request) {
            this.styleName = styleName;
            this.layerName = layerName;
            this.format = request.getParameter("f");
            this.request = request;

            // validate and get layer name
            this.published = getPublishedInfo(layerName, styleName);
        }

        PublishedInfo getPublishedInfo(String layerName, String styleName) {
            PublishedInfo thePublished = null;
            if (layerName != null) {
                LayerInfo layer = catalog.getLayerByName(layerName);
                if (layer != null) {
                    thePublished = layer;
                } else {
                    LayerGroupInfo group = catalog.getLayerGroupByName(layerName);
                    if (group != null) {
                        if (group.getLayers().size() == 1
                                && group.getLayers().get(0) == null
                                && group.getStyles().size() == 1
                                && group.getStyles().get(0) != null) {
                            if (styleName.equals(group.getStyles().get(0).getName())) {
                                thePublished = group;
                            } else {
                                throw new HttpErrorCodeException(
                                        BAD_REQUEST.value(),
                                        "Style groups can only handle their definition style, not any other");
                            }
                        } else {
                            throw new HttpErrorCodeException(
                                    BAD_REQUEST.value(),
                                    "The group is not a style group, cannot handle styles for it");
                        }
                    }
                }

                if (thePublished == null) {
                    throw new HttpErrorCodeException(
                            NOT_FOUND.value(), "Could not find layer " + layerName);
                }
            }
            return thePublished;
        }

        @Override
        public void get(HttpServletResponse response) throws IOException {
            final StyleInfo style = getStyleInfo(true);

            final String requestedFormat = getFormat(style);
            final StyleHandler handler = Styles.handler(requestedFormat);
            if (handler == null) {
                throw new HttpErrorCodeException(
                        BAD_REQUEST.value(), "Cannot encode style in " + requestedFormat);
            }

            // if no conversion is needed, push out raw style
            if (Objects.equals(handler.getFormat(), style.getFormat())) {
                try (final BufferedReader reader = catalog.getResourcePool().readStyle(style)) {
                    response.setContentType(handler.mimeType(style.getFormatVersion()));
                    response.setHeader(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=" + styleName + "." + style.getFormat());
                    OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
                    IOUtils.copy(reader, writer);
                    writer.flush();
                }
            } else {
                // otherwise convert if possible
                final StyledLayerDescriptor sld = style.getSLD();
                if (sld.getName() == null || sld.getName().isEmpty()) {
                    sld.setName(style.getName());
                }
                response.setContentType(handler.mimeType(handler.getVersions().get(0)));
                response.setHeader(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=" + styleName + "." + handler.getFormat());
                handler.encode(sld, null, true, response.getOutputStream());
            }
        }

        private String getFormat(StyleInfo style) {
            String requestedFormat = format;
            if (requestedFormat == null) {
                requestedFormat = style.getFormat();
            }
            if (requestedFormat == null) {
                requestedFormat = SLDHandler.FORMAT;
            }
            return requestedFormat;
        }

        @Override
        public void put(HttpServletResponse response) throws IOException {
            final String contentType = request.getContentType();
            final StyleHandler handler = Styles.handler(contentType);
            if (handler == null) {
                throw new org.geoserver.ows.HttpErrorCodeException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                        "Cannot handle a style of type " + contentType);
            }
            final String styleBody = IOUtils.toString(request.getReader());

            final WorkspaceInfo wsInfo = LocalWorkspace.get();
            StyleInfo sinfo = catalog.getStyleByName(wsInfo, styleName);
            boolean newStyle = sinfo == null;
            if (newStyle) {
                sinfo = catalog.getFactory().createStyle();
                sinfo.setName(styleName);
                sinfo.setFilename(styleName + "." + handler.getFileExtension());
            }

            sinfo.setFormat(handler.getFormat());
            sinfo.setFormatVersion(handler.versionForMimeType(contentType));
            if (wsInfo != null) {
                sinfo.setWorkspace(wsInfo);
            }

            try {
                catalog.getResourcePool()
                        .writeStyle(sinfo, new ByteArrayInputStream(styleBody.getBytes()));
            } catch (Exception e) {
                throw new org.geoserver.ows.HttpErrorCodeException(
                        INTERNAL_SERVER_ERROR.value(), "Error writing style");
            }

            if (newStyle) {
                catalog.add(sinfo);
            } else {
                catalog.save(sinfo);
            }

            if (published instanceof LayerInfo) {
                LayerInfo layer = (LayerInfo) published;
                if (!layer.getStyles()
                        .stream()
                        .anyMatch(s -> s != null && s.getName().equalsIgnoreCase(styleName))) {
                    layer.getStyles().add(sinfo);
                    catalog.save(layer);
                }
            }

            response.setStatus(NO_CONTENT.value());
        }

        @Override
        public void delete(HttpServletResponse response) throws IOException {
            WorkspaceInfo ws = LocalWorkspace.get();
            StyleInfo sinfo = catalog.getStyleByName(ws, styleName);
            if (sinfo == null) {
                throw new HttpErrorCodeException(
                        NOT_FOUND.value(), "Could not find style with id: " + styleName);
            }

            if (published != null) {
                if (published instanceof LayerInfo) {
                    LayerInfo layer = (LayerInfo) published;
                    if (sinfo.equals(layer.getDefaultStyle())) {
                        StyleInfo newDefault =
                                new CatalogBuilder(catalog).getDefaultStyle(layer.getResource());
                        layer.setDefaultStyle(newDefault);
                    } else {
                        if (!(layer.getStyles().remove(sinfo))) {
                            throw new HttpErrorCodeException(
                                    NOT_FOUND.value(),
                                    "Style with id: "
                                            + styleName
                                            + " is not associated to layer "
                                            + layerName);
                        }
                    }
                    catalog.save(layer);
                } else {
                    throw new HttpErrorCodeException(
                            BAD_REQUEST.value(), "Cannot delete style defining a style group");
                }
            }

            catalog.remove(sinfo);

            response.setStatus(NO_CONTENT.value());
        }

        StyleInfo getStyleInfo(boolean throwIfNotFound) {
            StyleInfo style;
            if (published != null) {
                style = getStyleFromPublished(published);
            } else {
                style = catalog.getStyleByName(styleName);
            }
            if (style == null && throwIfNotFound) {
                String message = "Style " + styleName + " could not be found";
                if (layerName != null) {
                    message += " in layer " + layerName;
                }
                throw new HttpErrorCodeException(NOT_FOUND.value(), message);
            }

            return style;
        }

        private StyleInfo getStyleFromPublished(PublishedInfo published) {
            StyleInfo style = null;
            if (published instanceof LayerInfo) {
                LayerInfo layer = (LayerInfo) published;
                if (layer.getDefaultStyle() != null
                        && layer.getDefaultStyle().getName().equals(styleName)) {
                    style = layer.getDefaultStyle();
                } else {
                    Predicate<StyleInfo> styleFilter =
                            s -> s != null && s.getName().equals(styleName);
                    Optional<StyleInfo> first =
                            layer.getStyles().stream().filter(styleFilter).findFirst();
                    if (first.isPresent()) {
                        style = first.get();
                    }
                }
            } else if (published instanceof LayerGroupInfo) {
                // can only handle simple style groups now
                LayerGroupInfo group = (LayerGroupInfo) published;
                return group.getStyles().get(0);
            }
            return style;
        }
    }
}
