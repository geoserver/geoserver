/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the default content type for a given API controller method. To be used when the expectd
 * default in not {@link org.springframework.http.MediaType#APPLICATION_JSON_VALUE}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefaultContentType {

    /**
     * The default content type to use in place of {@link
     * org.springframework.http.MediaType#APPLICATION_JSON_VALUE}.
     */
    String value();
}
