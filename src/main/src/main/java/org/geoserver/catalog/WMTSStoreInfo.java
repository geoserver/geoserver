/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.ows.wmts.WebMapTileServer;
import org.opengis.util.ProgressListener;

/**
 * A store backed by a {@link WebMapTileServer}, allows for WMTS cascading
 *
 * @author ian
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public interface WMTSStoreInfo extends HTTPStoreInfo {

    /**
     * Returns the underlying {@link WebMapTileServer}.
     *
     * <p>This method does I/O and is potentially blocking. The <tt>listener</tt> may be used to
     * report the progress of loading the datastore and also to report any errors or warnings that
     * occur.
     *
     * @param listener A progress listener, may be <code>null</code>.
     * @return The datastore.
     * @throws IOException Any I/O problems.
     */
    WebMapTileServer getWebMapTileServer(ProgressListener listener) throws IOException;

    String getHeaderName();

    void setHeaderName(String headerName);

    String getHeaderValue();

    void setHeaderValue(String headerValue);
}
