/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import org.geoserver.opensearch.eo.QuicklookRequest;

/**
 * Reads a "metadata" request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QuicklookRequestKvpReader extends AbstractProductRequestKvpReader {

    public QuicklookRequestKvpReader() {
        super(QuicklookRequest.class, true);
    }
}
