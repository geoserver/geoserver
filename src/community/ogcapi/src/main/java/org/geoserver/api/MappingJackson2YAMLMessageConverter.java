/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import io.swagger.v3.core.util.Yaml;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

/** Message converter encoding a Java bean into YAML using Jackson */
public class MappingJackson2YAMLMessageConverter extends AbstractJackson2HttpMessageConverter {

    public static final MediaType APPLICATION_YAML = MediaType.parseMediaType("application/x-yaml");
    public static final String APPLICATION_YAML_VALUE = APPLICATION_YAML.toString();

    protected MappingJackson2YAMLMessageConverter() {
        super(Yaml.mapper(), APPLICATION_YAML);
    }
}
