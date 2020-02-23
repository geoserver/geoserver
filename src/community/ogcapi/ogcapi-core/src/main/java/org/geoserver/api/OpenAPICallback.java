/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import io.swagger.v3.oas.models.OpenAPI;
import org.geoserver.ows.Request;

/**
 * Callback used to decorate {@link io.swagger.v3.oas.models.OpenAPI} documents with extra bits
 * added by extensions
 */
public interface OpenAPICallback {

    /**
     * Allows to alter the OpenAPI being built before it's returned to the client
     *
     * @param api The OpenAPI about to be returned to the client
     */
    public void apply(Request dr, OpenAPI api);
}
