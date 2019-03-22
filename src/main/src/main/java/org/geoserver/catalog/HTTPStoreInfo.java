/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * A store backed by a remote HTTP service.
 *
 * <p>Most of these methods have been refactored from WMSStoreInfo, now a sub-interface.
 *
 * @author Andrea Aime - OpenGeo
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public interface HTTPStoreInfo extends StoreInfo {

    /** The entrypoint URL, usually a getCapabilities */
    String getCapabilitiesURL();

    /**
     * Sets entrypoint / getCapabilities url.
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
     *     {@link #isUseConnectionPooling()} is {@code true}.
     */
    int getMaxConnections();

    void setMaxConnections(int maxConcurrentConnections);

    /** @return number of seconds to wait on read before time out, defaults to 60 */
    int getReadTimeout();

    /** @param timeoutSeconds seconds to wait before timing out a read request */
    void setReadTimeout(int timeoutSeconds);

    /** @return seconds to wait for connect requests before timing out, defaults to 30 */
    int getConnectTimeout();

    /** @param timeoutSeconds to wait for connect requests before timing out */
    void setConnectTimeout(int timeoutSeconds);

    /**
     * @return {@code true} (default) if the store shall use an http connection managed that pools
     *     connections, {@code false} otherwise.
     * @see #getMaxConnections()
     */
    public boolean isUseConnectionPooling();

    /**
     * @param useHttpConnectionPooling {@code true} if the store shall use an http connection
     *     managed that pools connections, {@code false} otherwise.
     * @see #setMaxConnections(int)
     */
    public void setUseConnectionPooling(boolean useHttpConnectionPooling);
}
