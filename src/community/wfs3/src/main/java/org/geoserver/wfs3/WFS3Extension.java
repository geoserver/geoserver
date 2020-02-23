/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.oas.models.OpenAPI;
import org.geoserver.wfs3.response.CollectionDocument;

/** Plugins can implement this interface to declare API extensions to */
public interface WFS3Extension {

    /**
     * Allows to add extensions to the API document
     *
     * @param api The WFS3 API document
     */
    void extendAPI(OpenAPI api);

    /** Extend the collection document */
    void extendCollection(CollectionDocument collection, BaseRequest request);

    /** Here we'll eventually have the method to actually run the extension calls */
}
