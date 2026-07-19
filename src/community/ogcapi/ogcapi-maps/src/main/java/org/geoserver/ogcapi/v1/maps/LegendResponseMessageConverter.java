/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.geoserver.ogcapi.MessageConverterResponseAdapter;
import org.geoserver.ogcapi.ResponseMessageConverter;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wms.legendgraphic.LegendGraphic;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Adapts the WMS GetLegendGraphic OWS {@link Response} outputs to an HTTP message converter, so the OGC API - Maps
 * legend resource can reuse every registered legend output format.
 */
@Component
public class LegendResponseMessageConverter extends MessageConverterResponseAdapter<LegendResponse>
        implements ResponseMessageConverter<LegendResponse> {

    public LegendResponseMessageConverter() {
        super(LegendResponse.class, LegendGraphic.class);
    }

    @Override
    protected void writeResponse(
            LegendResponse value, HttpOutputMessage httpOutputMessage, Operation operation, Response response)
            throws IOException {
        response.write(value.legend(), httpOutputMessage.getBody(), operation);
    }

    @Override
    protected Operation getOperation(LegendResponse result, Request dr, MediaType mediaType) {
        Operation original = dr.getOperation();
        return new Operation(
                original.getId(), original.getService(), original.getMethod(), new Object[] {result.request()});
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class<?> valueClass, LegendResponse value) {
        if (!canWrite(valueClass, null)) return Collections.emptyList();
        return getSupportedMediaTypes();
    }

    @Override
    public boolean canWrite(Object value, MediaType mediaType) {
        return value instanceof LegendResponse && getResponse(mediaType).isPresent();
    }
}
