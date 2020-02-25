/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Map;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.platform.resource.LockProvider;

/**
 * Global GeoServer configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface GeoServerInfo extends Info {

    /** Identifier. */
    String getId();

    /**
     * The global settings.
     *
     * <p>Generally client code shoudl not call this method directly, and rather call {@link
     * GeoServer#getSettings()}.
     */
    SettingsInfo getSettings();

    /** Sets the global settings. */
    void setSettings(SettingsInfo settings);

    /** The flag to use request headers for the proxy URL */
    Boolean isUseHeadersProxyURL();

    /** Sets the flag to use request headers for the proxy URL */
    void setUseHeadersProxyURL(Boolean useHeadersProxyURL);

    /** The Java Advanced Imaging configuration. */
    JAIInfo getJAI();

    /** Sets the Java Advanced Imaging configuration. */
    void setJAI(JAIInfo jai);

    /** The Coverage Access configuration. */
    CoverageAccessInfo getCoverageAccess();

    /** Sets the Coverage Access configuration. */
    void setCoverageAccess(CoverageAccessInfo coverageInfo);

    /** Sets the administrator username. */
    String getAdminUsername();

    /** The administrator username. */
    void setAdminUsername(String adminUsername);

    /** The administrator password. */
    String getAdminPassword();

    /** Sets the administrator password. */
    void setAdminPassword(String adminPassword);

    /**
     * Set the XML error handling mode for the server.
     *
     * @see ResourceErrorHandling
     */
    void setResourceErrorHandling(ResourceErrorHandling mode);

    /** Get the XML error handling mode for the server. */
    ResourceErrorHandling getResourceErrorHandling();

    /**
     * The update sequence.
     *
     * <p>This value is used by various ogc services to track changes to a capabilities document.
     */
    long getUpdateSequence();

    /** Sets the update sequence. */
    void setUpdateSequence(long updateSequence);

    /** The size of the cache for feature type objects. */
    int getFeatureTypeCacheSize();

    /** Sets the size of the cache for feature type objects. */
    void setFeatureTypeCacheSize(int featureTypeCacheSize);

    /** Flag determining if access to services should occur only through "virtual services". */
    Boolean isGlobalServices();

    /** Sets the flag forcing access to services only through virtual services. */
    void setGlobalServices(Boolean globalServices);

    /** Sets logging buffer size of incoming XML Post Requests for WFS,WMS,... */
    void setXmlPostRequestLogBufferSize(Integer requestBufferSize);

    /** Gets log buffer size of XML Post Request for WFS,WMS,... */
    Integer getXmlPostRequestLogBufferSize();

    /**
     * If true it enables evaluation of XML entities contained in XML files received in a service
     * (WMS, WFS, ...) request. Default is FALSE. Enabling this feature is a security risk.
     */
    void setXmlExternalEntitiesEnabled(Boolean xmlExternalEntitiesEnabled);

    /**
     * If true it enables evaluation of XML entities contained in XML files received in a service
     * (WMS, WFS, ...) request. Default is FALSE. Enabling this feature is a security risk.
     */
    Boolean isXmlExternalEntitiesEnabled();

    /**
     * Name of lock provider used for resource access.
     *
     * @return name of spring bean to use as lock provider
     */
    public String getLockProviderName();

    /**
     * Sets the name of the {@link LockProvider} to use for resoruce access.
     *
     * <p>The following spring bean names are initially provided with the application:
     *
     * <ul>
     *   <li>nullLockProvider
     *   <li>memoryLockProvider
     *   <li>fileLockProvider
     * </ul>
     *
     * @param lockProviderName Name of lock provider used for resource access.
     */
    public void setLockProviderName(String lockProviderName);

    /**
     * A map of metadata for services.
     *
     * @uml.property name="metadata"
     */
    MetadataMap getMetadata();

    /**
     * Client properties for services.
     *
     * <p>These values are transient, and not persistent.
     */
    Map<Object, Object> getClientProperties();

    /** Disposes the global configuration object. */
    void dispose();

    /** WebUIMode choices */
    public enum WebUIMode {
        /** Let GeoServer determine the best mode. */
        DEFAULT,
        /**
         * Always redirect to persist page state (prevent double submit problem but doesn't support
         * clustering)
         */
        REDIRECT,
        /**
         * Never redirect to persist page state (supports clustering but doesn't prevent double
         * submit problem)
         */
        DO_NOT_REDIRECT
    };

    /**
     * Get the WebUIMode
     *
     * @return the WebUIMode
     */
    public WebUIMode getWebUIMode();

    /** Set the WebUIMode */
    public void setWebUIMode(WebUIMode mode);

    /** Determines if Per-workspace Stores Queries are activated. */
    Boolean isAllowStoredQueriesPerWorkspace();

    /** Sets if Per-workspace Stores Queries are activated. */
    void setAllowStoredQueriesPerWorkspace(Boolean allowStoredQueriesPerWorkspace);
}
