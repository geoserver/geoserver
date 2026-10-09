/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.MessageConverterResponseAdapter;
import org.geoserver.ogcapi.ResponseMessageConverter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wms.WebMap;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Adapts all output formats able to encode a WMS {@link org.geoserver.wms.WebMap} to a
 * {@link org.springframework.http.converter.HttpMessageConverter} . Allows to reuse all existing WMS output formats in
 * the OGC Maps API implementation.
 */
@Component
public class MapResponseMessageConverter extends MessageConverterResponseAdapter<WebMap>
        implements ResponseMessageConverter<WebMap> {

    static final Logger LOGGER = Logging.getLogger(MapResponseMessageConverter.class);

    public MapResponseMessageConverter() {
        super(WebMap.class, WebMap.class);
    }

    @Override
    protected void writeResponse(
            WebMap value, HttpOutputMessage httpOutputMessage, Operation operation, Response response)
            throws IOException {
        response.write(value, httpOutputMessage.getBody(), operation);
    }

    @Override
    protected Operation getOperation(WebMap result, Request dr, MediaType mediaType) {
        Operation original = dr.getOperation();
        return new Operation(original.getId(), original.getService(), original.getMethod(), new Object[] {
            result.getMapContent().getRequest()
        });
    }

    /**
     * The response that will encode this map. Several WMS responses share a media type while binding to different
     * {@link WebMap} subclasses (e.g. the two SVG renderers), so a lookup by media type alone can pick a response that
     * might not handle the map it is given.
     */
    @Override
    public Optional<Response> getResponse(MediaType mediaType) {
        Request dr = Dispatcher.REQUEST.get();
        if (!(APIRequestInfo.get().getResult() instanceof WebMap map)) return super.getResponse(mediaType);
        Operation operation = getOperation(map, dr, mediaType);
        return responses.stream()
                .filter(r -> r.getBinding().isInstance(map))
                .filter(r -> r.canHandle(operation))
                .filter(r -> getMediaTypeStream(r).anyMatch(mediaType::isCompatibleWith))
                .findFirst();
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class<?> valueClass, WebMap value) {
        if (!canWrite(valueClass, null)) return Collections.emptyList();
        List<MediaType> result = new ArrayList<>(getSupportedMediaTypes());
        // allows supporting RawMap
        if (value.getMimeType() != null) {
            result.add(MediaType.parseMediaType(value.getMimeType()));
        }
        return result;
    }

    @Override
    public boolean canWrite(Object value, MediaType mediaType) {
        if (!(value instanceof WebMap)) return false;
        WebMap map = (WebMap) value;
        return getResponse(mediaType).isPresent()
                && (map.getMimeType() == null
                        || MediaType.parseMediaType(map.getMimeType()).isCompatibleWith(mediaType));
    }
}
