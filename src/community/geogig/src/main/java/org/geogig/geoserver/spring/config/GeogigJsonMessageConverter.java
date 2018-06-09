/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Delegate marshalling of json request bodies to geogig DTO classes to the default spring json
 * marshaller.
 */
@Component
public class GeogigJsonMessageConverter extends AbstractDelegatingGeogigMessageConverter {

    public GeogigJsonMessageConverter() {
        super(new GsonHttpMessageConverter());
    }
}
