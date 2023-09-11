/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.api.style.Description;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.UserLayer;
import org.geotools.styling.DefaultResourceLocator;

/** Helper class to work with style and their contents */
class StyleWorker {

    private final GeoServerDataDirectory dataDirectory;
    private final EntityResolverProvider resolverProvider;

    public StyleWorker(
            GeoServerDataDirectory dataDirectory, EntityResolverProvider resolverProvider) {
        this.dataDirectory = dataDirectory;
        this.resolverProvider = resolverProvider;
    }

    public StyledLayerDescriptor getSLD(StyleInfo style) throws IOException {
        // grab the sld
        Resource styleResource = dataDirectory.style(style);
        String format = style.getFormat();
        StyleHandler handler = Styles.handler(format);
        File file = styleResource.file();
        DefaultResourceLocator mockLocator = new DefaultResourceLocator();
        return handler.parse(
                file, style.getFormatVersion(), mockLocator, resolverProvider.getEntityResolver());
    }

    public String getStyleBody(StyleInfo style) throws IOException {
        Resource styleResource = dataDirectory.style(style);
        String format = style.getFormat();
        StyleHandler handler = Styles.handler(format);

        try (InputStream in = styleResource.in()) {
            return IOUtils.toString(in, "UTF-8");
        }
    }

    public String getDescription(StyledLayerDescriptor sld) {
        if (StringUtils.isNotBlank(sld.getTitle())) {
            return sld.getTitle();
        } else if (StringUtils.isNotBlank(sld.getAbstract())) {
            return sld.getAbstract();
        }
        Optional<Description> description = getFirstLayerDescription(sld);

        return description
                .map(d -> d.getTitle() != null ? d.getTitle() : d.getAbstract())
                .map(t -> t.toString())
                .orElse(null);
    }

    public String getMimeType(StyleInfo style) {
        Resource styleResource = dataDirectory.style(style);
        String format = style.getFormat();
        StyleHandler handler = Styles.handler(format);
        return handler.mimeType(style.getFormatVersion());
    }

    public String getTitle(StyleInfo style) throws IOException {
        StyledLayerDescriptor sld = getSLD(style);
        if (StringUtils.isNotBlank(sld.getTitle())) {
            return sld.getTitle();
        }
        Optional<Description> description = getFirstLayerDescription(sld);

        return description.map(d -> d.getTitle()).map(t -> t.toString()).orElse(null);
    }

    public String getAbstract(StyleInfo style) throws IOException {
        StyledLayerDescriptor sld = getSLD(style);
        if (StringUtils.isNotBlank(sld.getAbstract())) {
            return sld.getAbstract();
        }
        Optional<Description> description = getFirstLayerDescription(sld);

        return description.map(d -> d.getAbstract()).map(t -> t.toString()).orElse(null);
    }

    private Optional<Description> getFirstLayerDescription(StyledLayerDescriptor sld) {
        return Optional.ofNullable(sld.getStyledLayers())
                .filter(layers -> layers.length > 0)
                .map(layers -> layers[0])
                .map(
                        l ->
                                l instanceof UserLayer
                                        ? ((UserLayer) l).getUserStyles()
                                        : ((NamedLayer) l).getStyles())
                .filter(styles -> styles != null && styles.length > 0)
                .map(styles -> styles[0])
                .map(s -> s.getDescription());
    }
}
