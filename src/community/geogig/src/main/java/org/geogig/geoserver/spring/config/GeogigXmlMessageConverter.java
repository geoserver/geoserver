/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Delegate marshalling of xml request bodies to geogig DTO classes to the default spring xml
 * marshaller.
 */
@Component
public class GeogigXmlMessageConverter extends AbstractDelegatingGeogigMessageConverter {

    public GeogigXmlMessageConverter() {
        super(new Jaxb2RootElementHttpMessageConverter());
    }
}
