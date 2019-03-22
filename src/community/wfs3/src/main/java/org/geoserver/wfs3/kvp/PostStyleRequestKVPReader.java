/*
 * (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import org.geoserver.wfs3.PostStyleRequest;

/** Parses a "PostStyle" request */
public class PostStyleRequestKVPReader extends BaseKvpRequestReader {

    public PostStyleRequestKVPReader() {
        super(PostStyleRequest.class);
    }
}
