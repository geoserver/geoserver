/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import java.net.URL;
import org.w3c.dom.Document;

/**
 * Import the metadata from a geonetwork server.
 *
 * @author Timothy De Bock
 */
public interface RemoteDocumentReader {

    Document readDocument(URL url) throws IOException;
}
