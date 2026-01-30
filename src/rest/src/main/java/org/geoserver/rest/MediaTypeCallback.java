/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Map;
import org.springframework.http.MediaType;

/** Extension point to register extra media types in Spring MVC */
public interface MediaTypeCallback {

    void configure(Map<String, MediaType> mediaTypes);
}
