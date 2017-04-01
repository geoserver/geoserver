package org.geoserver.rest.converters;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Base message converter behavior
 */
public abstract class BaseMessageConverter<T> implements HttpMessageConverter<T>, ExtensionPriority {

    protected final Catalog catalog;

    protected final XStreamPersisterFactory xpf;

    protected  final GeoServer geoServer;

    public BaseMessageConverter() {
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }

    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }
    
    /**
     * Checks if the media type provided is "included" by one of the media types declared
     * in {@link #getSupportedMediaTypes()}
     */
    protected boolean isSupportedMediaType(MediaType mediaType) {
        for (MediaType supported : getSupportedMediaTypes()) {
            if(supported.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }
}
