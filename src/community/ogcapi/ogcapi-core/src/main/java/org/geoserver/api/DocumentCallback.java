/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.geoserver.ows.Request;

/** Callback used to decorate documents with cross-links and other additions */
public interface DocumentCallback {

    /**
     * Allows to alter the document being built before it's returned to the client
     *
     * @param document The document about to be returned to the client
     */
    public void apply(Request dr, AbstractDocument document);
}
