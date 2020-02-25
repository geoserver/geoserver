/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;

public class GeoServerInfoImpl implements GeoServerInfo {

    protected String id;

    protected SettingsInfo settings = new SettingsInfoImpl();

    protected JAIInfo jai = new JAIInfoImpl();

    protected CoverageAccessInfo coverageAccess = new CoverageAccessInfoImpl();

    protected MetadataMap metadata = new MetadataMap();

    protected Map<Object, Object> clientProperties = new HashMap<Object, Object>();

    protected long updateSequence;

    protected String adminUsername;
    protected String adminPassword;

    protected int featureTypeCacheSize;

    protected Boolean globalServices = true;

    protected Boolean useHeadersProxyURL = false;

    protected transient GeoServer geoServer;

    protected Integer xmlPostRequestLogBufferSize = 1024;

    protected Boolean xmlExternalEntitiesEnabled = Boolean.FALSE;

    protected String lockProviderName;

    protected WebUIMode webUIMode = WebUIMode.DEFAULT;

    protected Boolean allowStoredQueriesPerWorkspace = true;

    // deprecated members, kept around to maintain xstream persistence backward compatability
    protected ContactInfo contact;
    protected String charset;
    protected String title;
    protected Integer numDecimals;
    protected String onlineResource;
    protected String schemaBaseUrl;
    protected String proxyBaseUrl;
    protected Boolean verbose;
    protected Boolean verboseExceptions;

    private ResourceErrorHandling resourceErrorHandling;

    public GeoServerInfoImpl(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public GeoServerInfoImpl() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public SettingsInfo getSettings() {
        return settings;
    }

    @Override
    public void setSettings(SettingsInfo settings) {
        this.settings = settings;
    }

    public void setContact(ContactInfo contactInfo) {
        getSettings().setContact(contactInfo);
    }

    public ContactInfo getContact() {
        return getSettings().getContact();
    }

    public JAIInfo getJAI() {
        return jai;
    }

    public void setJAI(JAIInfo jai) {
        this.jai = jai;
    }

    public CoverageAccessInfo getCoverageAccess() {
        return coverageAccess;
    }

    public void setCoverageAccess(CoverageAccessInfo coverageAccess) {
        this.coverageAccess = coverageAccess;
    }

    public void setTitle(String title) {
        getSettings().setTitle(title);
    }

    public String getTitle() {
        return getSettings().getTitle();
    }

    public String getCharset() {
        return getSettings().getCharset();
    }

    public void setCharset(String charset) {
        getSettings().setCharset(charset);
    }

    public int getNumDecimals() {
        return getSettings().getNumDecimals();
    }

    public void setNumDecimals(int numDecimals) {
        getSettings().setNumDecimals(numDecimals);
    }

    public String getOnlineResource() {
        return getSettings().getOnlineResource();
    }

    public void setOnlineResource(String onlineResource) {
        getSettings().setOnlineResource(onlineResource);
    }

    public String getProxyBaseUrl() {
        return getSettings().getProxyBaseUrl();
    }

    public void setProxyBaseUrl(String proxyBaseUrl) {
        getSettings().setProxyBaseUrl(proxyBaseUrl);
    }

    public String getSchemaBaseUrl() {
        return getSettings().getSchemaBaseUrl();
    }

    public void setSchemaBaseUrl(String schemaBaseUrl) {
        getSettings().setSchemaBaseUrl(schemaBaseUrl);
    }

    public boolean isVerbose() {
        return getSettings().isVerbose();
    }

    public void setVerbose(boolean verbose) {
        getSettings().setVerbose(verbose);
    }

    public boolean isVerboseExceptions() {
        return getSettings().isVerboseExceptions();
    }

    public void setVerboseExceptions(boolean verboseExceptions) {
        getSettings().setVerboseExceptions(verboseExceptions);
    }

    public long getUpdateSequence() {
        return updateSequence;
    }

    public void setUpdateSequence(long updateSequence) {
        this.updateSequence = updateSequence;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public int getFeatureTypeCacheSize() {
        return featureTypeCacheSize;
    }

    public void setFeatureTypeCacheSize(int featureTypeCacheSize) {
        this.featureTypeCacheSize = featureTypeCacheSize;
    }

    public Boolean isGlobalServices() {
        return globalServices;
    }

    public void setGlobalServices(Boolean forceVirtualServices) {
        this.globalServices = forceVirtualServices;
    }

    public Boolean isUseHeadersProxyURL() {
        return useHeadersProxyURL == null ? false : useHeadersProxyURL;
    }

    public void setUseHeadersProxyURL(Boolean useHeadersProxyURL) {
        this.useHeadersProxyURL = useHeadersProxyURL;
    }

    public void setXmlPostRequestLogBufferSize(Integer bufferSize) {
        this.xmlPostRequestLogBufferSize = bufferSize;
    }

    public Integer getXmlPostRequestLogBufferSize() {
        return this.xmlPostRequestLogBufferSize;
    }

    /**
     * If true it enables evaluation of XML entities contained in XML files received in a service
     * (WMS, WFS, ...) request. Default is FALSE. Enabling this feature is a security risk.
     */
    public void setXmlExternalEntitiesEnabled(Boolean xmlExternalEntitiesEnabled) {
        this.xmlExternalEntitiesEnabled = xmlExternalEntitiesEnabled;
    }

    /**
     * If true it enables evaluation of XML entities contained in XML files received in a service
     * (WMS, WFS, ...) request. Default is FALSE. Enabling this feature is a security risk.
     */
    public Boolean isXmlExternalEntitiesEnabled() {
        return this.xmlExternalEntitiesEnabled;
    }

    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    public Map<Object, Object> getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(Map<Object, Object> properties) {
        this.clientProperties = properties;
    }

    public String getLockProviderName() {
        return this.lockProviderName;
    }

    public void setLockProviderName(String lockProviderName) {
        this.lockProviderName = lockProviderName;
    }

    public void dispose() {
        if (coverageAccess != null) {
            coverageAccess.dispose();
        }
    }

    public Boolean isAllowStoredQueriesPerWorkspace() {
        return allowStoredQueriesPerWorkspace == null
                ? Boolean.TRUE
                : allowStoredQueriesPerWorkspace;
    }

    public void setAllowStoredQueriesPerWorkspace(Boolean allowStoredQueriesPerWorkspace) {
        this.allowStoredQueriesPerWorkspace = allowStoredQueriesPerWorkspace;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((settings == null) ? 0 : settings.hashCode());
        result = prime * result + ((adminPassword == null) ? 0 : adminPassword.hashCode());
        result = prime * result + ((adminUsername == null) ? 0 : adminUsername.hashCode());
        result = prime * result + ((clientProperties == null) ? 0 : clientProperties.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + Long.valueOf(updateSequence).hashCode();
        result = prime * result + (globalServices ? 1231 : 1237);
        result = prime * result + xmlPostRequestLogBufferSize;
        result =
                prime * result
                        + ((resourceErrorHandling == null) ? 0 : resourceErrorHandling.hashCode());
        result = prime * result + ((lockProviderName == null) ? 0 : lockProviderName.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof GeoServerInfo)) {
            return false;
        }
        final GeoServerInfo other = (GeoServerInfo) obj;
        if (adminPassword == null) {
            if (other.getAdminPassword() != null) return false;
        } else if (!adminPassword.equals(other.getAdminPassword())) return false;
        if (adminUsername == null) {
            if (other.getAdminUsername() != null) return false;
        } else if (!adminUsername.equals(other.getAdminUsername())) return false;
        if (settings == null) {
            if (other.getSettings() != null) return false;
        } else if (!settings.equals(other.getSettings())) return false;
        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;
        if (updateSequence != other.getUpdateSequence()) return false;

        if (!Objects.equals(globalServices, other.isGlobalServices())) return false;
        if (xmlPostRequestLogBufferSize == null) {
            if (other.getXmlPostRequestLogBufferSize() != null) {
                return false;
            }
        } else if (!xmlPostRequestLogBufferSize.equals(other.getXmlPostRequestLogBufferSize())) {
            return false;
        }

        if (getResourceErrorHandling() == null) {
            if (other.getResourceErrorHandling() != null) return false;
        } else {
            if (!getResourceErrorHandling().equals(other.getResourceErrorHandling())) return false;
        }

        if (lockProviderName == null) {
            if (other.getLockProviderName() != null) return false;
        } else {
            if (!lockProviderName.equals(other.getLockProviderName())) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(getTitle())
                .append(']')
                .toString();
    }

    /*
     * XStream specific method, needed to initialize members that are added over time and to cleanly
     * handle deprecated members.
     */
    public Object readResolve() {
        if (this.globalServices == null) {
            this.globalServices = true;
        }
        if (this.xmlPostRequestLogBufferSize == null) {
            this.xmlPostRequestLogBufferSize = 1024;
        }
        if (this.settings == null) {
            this.settings = new SettingsInfoImpl();
        }

        // handle deprecated members, forward values onto the setter methods
        if (contact != null) {
            setContact(contact);
            contact = null;
        }
        if (charset != null) {
            setCharset(charset);
            charset = null;
        }
        if (title != null) {
            setTitle(title);
            title = null;
        }
        if (numDecimals != null) {
            setNumDecimals(numDecimals);
            numDecimals = null;
        }
        if (onlineResource != null) {
            setOnlineResource(onlineResource);
            onlineResource = null;
        }
        if (schemaBaseUrl != null) {
            setSchemaBaseUrl(schemaBaseUrl);
            schemaBaseUrl = null;
        }
        if (proxyBaseUrl != null) {
            setProxyBaseUrl(proxyBaseUrl);
            proxyBaseUrl = null;
        }
        if (verbose != null) {
            setVerbose(verbose);
            verbose = null;
        }
        if (verboseExceptions != null) {
            setVerboseExceptions(verboseExceptions);
            verboseExceptions = null;
        }

        return this;
    }

    public void setResourceErrorHandling(ResourceErrorHandling mode) {
        this.resourceErrorHandling = mode;
    }

    public ResourceErrorHandling getResourceErrorHandling() {
        if (this.resourceErrorHandling == null) {
            return ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS;
        }

        return resourceErrorHandling;
    }

    @Override
    public WebUIMode getWebUIMode() {
        return webUIMode;
    }

    @Override
    public void setWebUIMode(WebUIMode webUIMode) {
        this.webUIMode = webUIMode;
    }
}
