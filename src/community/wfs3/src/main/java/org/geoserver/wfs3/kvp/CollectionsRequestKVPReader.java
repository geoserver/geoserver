/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import org.geoserver.wfs3.CollectionsRequest;

/** Parses a "collections" request */
public class CollectionsRequestKVPReader extends BaseKvpRequestReader {

    public CollectionsRequestKVPReader() {
        super(CollectionsRequest.class);
    }
}
