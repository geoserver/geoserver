/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Map;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Service and organizational settings object.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface SettingsInfo extends Info {

    /**
     * The workspace the settings are specific to.
     *
     * <p>Will be null for global settings: {@link GeoServerInfo#getSettings()}
     *
     * @return A workspace, or <code>null</code>.
     */
    WorkspaceInfo getWorkspace();

    /** Sets the workspace the settings are specific to. */
    void setWorkspace(WorkspaceInfo workspace);

    /** The title of the settings instance. */
    String getTitle();

    /** Sets the title of the settings instance. */
    void setTitle(String title);

    /** The contact information. */
    ContactInfo getContact();

    /** Sets the contact information. */
    void setContact(ContactInfo contactInfo);

    /** The default character set. */
    String getCharset();

    /** Sets the default character set. */
    void setCharset(String charset);

    /** A cap on the number of decimals to use when encoding floating point numbers. */
    int getNumDecimals();

    /** Sets the cap on the number of decimals to use when encoding floating point numbers. */
    void setNumDecimals(int numDecimals);

    /**
     * Provider web site (used for default contact information, or service provider information if
     * user has not filled in contact details.
     */
    String getOnlineResource();

    /**
     * Provider web site (used for default contact information, or service provider information if
     * user has not filled in contact details.
     *
     * @param onlineResource Provider website
     */
    void setOnlineResource(String onlineResource);

    /**
     * The url of a proxy in front of the GeoServer instance.
     *
     * <p>This value is used when a reference back to the GeoServer instance must be made in a
     * response.
     */
    String getProxyBaseUrl();

    /** Sets The url of a proxy in front of the GeoServer instance. */
    void setProxyBaseUrl(String proxyBaseUrl);

    /** The base url to use when including a reference to an xml schema document in a response. */
    String getSchemaBaseUrl();

    /**
     * Sets the base url to use when including a reference to an xml schema document in a response.
     */
    void setSchemaBaseUrl(String schemaBaseUrl);

    /**
     * Sets indent level for XML output, causing output to be more verbose.
     *
     * <p>Then set to false GeoServer will also take step so to strip out some formating and produce
     * more condensed output.
     */
    boolean isVerbose();

    /**
     * Sets indent level for XML output, causing output to be more verbose.
     *
     * <p>Then set to false GeoServer will also take step so to strip out some formating and produce
     * more condensed output.
     */
    void setVerbose(boolean verbose);

    /**
     * Verbosity flag for exceptions.
     *
     * <p>When set GeoServer will include full stack traces for exceptions.
     */
    boolean isVerboseExceptions();

    /** Sets verbosity flag for exceptions. */
    void setVerboseExceptions(boolean verboseExceptions);

    /** A map of metadata for services. */
    MetadataMap getMetadata();

    /**
     * Client properties for services.
     *
     * <p>These values are transient, and not persistent.
     */
    Map<Object, Object> getClientProperties();

    /** If true local workspace should keep the namespace prefixes in getCapabilities etc... */
    boolean isLocalWorkspaceIncludesPrefix();

    /**
     * Set whether or not a local workspace should keep namespace prefixes in the getCapabilities
     * etc...
     *
     * @param includePrefix if true then the prefixes will be kept, default behaviour is to remove
     *     it.
     */
    void setLocalWorkspaceIncludesPrefix(boolean includePrefix);

    public boolean isShowCreatedTimeColumnsInAdminList();

    public void setShowCreatedTimeColumnsInAdminList(boolean showCreatedTimeColumnsInAdminList);

    public boolean isShowModifiedTimeColumnsInAdminList();

    public void setShowModifiedTimeColumnsInAdminList(boolean showModifiedTimeColumnsInAdminList);
}
