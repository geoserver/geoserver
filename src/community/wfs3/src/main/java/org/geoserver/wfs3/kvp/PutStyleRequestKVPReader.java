/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.wfs3.kvp;

import org.geoserver.wfs3.PutStyleRequest;

/** Parses a "PutStyle" request */
public class PutStyleRequestKVPReader extends BaseKvpRequestReader {

    public PutStyleRequestKVPReader() {
        super(PutStyleRequest.class);
    }
}
