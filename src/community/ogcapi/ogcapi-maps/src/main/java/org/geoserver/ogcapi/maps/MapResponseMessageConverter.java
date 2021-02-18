/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import org.geoserver.ogcapi.MessageConverterResponseAdapter;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wms.WebMap;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpOutputMessage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Adapts all output formats able to encode a WMS {@link org.geoserver.wms.WebMap} to a {@link
 * org.springframework.http.converter.HttpMessageConverter} encoding a {@link
 * org.geoserver.wfs.response.FeatureResponse}. Allows to reuse all existing WMS output formats in
 * the OGC Maps API implementation.
 */
@Component
public class MapResponseMessageConverter extends MessageConverterResponseAdapter<WebMap> {

    static final Logger LOGGER = Logging.getLogger(MapResponseMessageConverter.class);

    public MapResponseMessageConverter() {
        super(WebMap.class, WebMap.class);
    }

    @Override
    protected void writeResponse(
            WebMap value,
            HttpOutputMessage httpOutputMessage,
            Operation operation,
            Response response)
            throws IOException {
        response.write(value, httpOutputMessage.getBody(), operation);
    }

    @Override
    protected Operation getOperation(WebMap result, Request dr) {
        Operation original = dr.getOperation();
        return new Operation(
                original.getId(),
                original.getService(),
                original.getMethod(),
                new Object[] {result.getMapContent().getRequest()});
    }
}
