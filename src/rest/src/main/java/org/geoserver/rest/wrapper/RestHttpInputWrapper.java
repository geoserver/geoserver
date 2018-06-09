/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.wrapper;

import java.io.IOException;
import java.io.InputStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

/**
 * Wrapper around {@link HttpInputMessage} used by {@link XStreamMessageConverter} to configure the
 * persister before XStream objects get read.
 */
public class RestHttpInputWrapper implements HttpInputMessage {

    HttpInputMessage message;
    RestBaseController controller;

    public RestHttpInputWrapper(HttpInputMessage message, RestBaseController controller) {
        this.message = message;
        this.controller = controller;
    }

    /**
     * Apply configuration to the XStreamPersister based on the converter
     *
     * @param persister The XStream persister
     * @param xStreamMessageConverter The XStream converter
     */
    public void configurePersister(
            XStreamPersister persister, XStreamMessageConverter xStreamMessageConverter) {
        controller.configurePersister(persister, xStreamMessageConverter);
    }

    @Override
    public InputStream getBody() throws IOException {
        return message.getBody();
    }

    @Override
    public HttpHeaders getHeaders() {
        return message.getHeaders();
    }
}
