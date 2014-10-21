/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;

import org.geotools.data.wms.WebMapServer;
import org.opengis.util.ProgressListener;

/**
 * A store backed by a {@link WebMapServer}, allows for WMS cascading
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public interface WMSStoreInfo extends StoreInfo {

    /**
     * Returns the underlying {@link WebMapServer}
     * <p>
     * This method does I/O and is potentially blocking. The <tt>listener</tt> may be used to report
     * the progress of loading the datastore and also to report any errors or warnings that occur.
     * </p>
     * 
     * @param listener
     *            A progress listener, may be <code>null</code>.
     * 
     * @return The datastore.
     * 
     * @throws IOException
     *             Any I/O problems.
     */
    WebMapServer getWebMapServer(ProgressListener listener) throws IOException;

    /**
     * The capabilities url
     */
    String getCapabilitiesURL();

    /**
     * Sets the web map server capabilities url.
     * 
     * @uml.property name="url"
     */
    void setCapabilitiesURL(String url);
    
    String getUsername();
    
    void setUsername(String user);
    
    String getPassword();
    
    void setPassword(String password);
    
    /**
     * @return Upper limit on the number of http connections the store should hold in the pool if
     *         {@link #isUseConnectionPooling()} is {@code true}.
     */
    int getMaxConnections();
    
    void setMaxConnections(int maxConcurrentConnections);
    
    /**
     * @return number of seconds to wait on read before time out, defaults to 60
     */
    int getReadTimeout();

    /**
     * @param timeoutSeconds
     *            seconds to wait before timing out a read request
     */
    void setReadTimeout(int timeoutSeconds);

    /**
     * @return seconds to wait for connect requests before timing out, defaults to 30
     */
    int getConnectTimeout();

    /**
     * @param seconds
     *            to wait for connect requests before timing out
     */
    void setConnectTimeout(int timeoutSeconds);
    
    /**
     * @return {@code true} (default) if the store shall use an http connection managed that pools
     *         connections, {@code false} otherwise.
     * @see #getMaxConnections()
     */
    public boolean isUseConnectionPooling();

    /**
     * @param useHttpConnectionPooling
     *            {@code true} if the store shall use an http connection managed that pools
     *            connections, {@code false} otherwise.
     * @see #setMaxConnections(int)
     */
    public void setUseConnectionPooling(boolean useHttpConnectionPooling);
    
}
