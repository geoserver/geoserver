/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.SettingsInfo;

public class SettingsInfoImpl implements SettingsInfo {

    protected String id;

    protected WorkspaceInfo workspace;

    protected ContactInfo contact = new ContactInfoImpl();

    protected String charset = "UTF-8";

    protected String title;

    protected int numDecimals = 4;

    protected String onlineResource;

    protected String schemaBaseUrl;

    protected String proxyBaseUrl;

    protected boolean verbose = true;

    protected boolean verboseExceptions = false;

    protected MetadataMap metadata = new MetadataMap();

    protected Map<Object, Object> clientProperties = new HashMap<Object, Object>();

    private boolean localWorkspaceIncludesPrefix = false;

    private boolean showCreatedTimeColumnsInAdminList = false;

    private boolean showModifiedTimeColumnsInAdminList = false;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public WorkspaceInfo getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public ContactInfo getContact() {
        return contact;
    }

    @Override
    public void setContact(ContactInfo contact) {
        this.contact = contact;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public int getNumDecimals() {
        return numDecimals;
    }

    @Override
    public void setNumDecimals(int numDecimals) {
        this.numDecimals = numDecimals;
    }

    @Override
    public String getOnlineResource() {
        return onlineResource;
    }

    @Override
    public void setOnlineResource(String onlineResource) {
        this.onlineResource = onlineResource;
    }

    @Override
    public String getProxyBaseUrl() {
        return proxyBaseUrl;
    }

    @Override
    public void setProxyBaseUrl(String proxyBaseUrl) {
        this.proxyBaseUrl = proxyBaseUrl;
    }

    @Override
    public String getSchemaBaseUrl() {
        return schemaBaseUrl;
    }

    @Override
    public void setSchemaBaseUrl(String schemaBaseUrl) {
        this.schemaBaseUrl = schemaBaseUrl;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isVerboseExceptions() {
        return verboseExceptions;
    }

    @Override
    public void setVerboseExceptions(boolean verboseExceptions) {
        this.verboseExceptions = verboseExceptions;
    }

    @Override
    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    @Override
    public Map<Object, Object> getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(Map<Object, Object> properties) {
        this.clientProperties = properties;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((charset == null) ? 0 : charset.hashCode());
        result = prime * result + ((clientProperties == null) ? 0 : clientProperties.hashCode());
        result = prime * result + ((contact == null) ? 0 : contact.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + numDecimals;
        result = prime * result + ((onlineResource == null) ? 0 : onlineResource.hashCode());
        result = prime * result + ((proxyBaseUrl == null) ? 0 : proxyBaseUrl.hashCode());
        result = prime * result + ((schemaBaseUrl == null) ? 0 : schemaBaseUrl.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + (verbose ? 1231 : 1237);
        result = prime * result + (verboseExceptions ? 1231 : 1237);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof SettingsInfo)) {
            return false;
        }
        final SettingsInfo other = (SettingsInfo) obj;
        if (charset == null) {
            if (other.getCharset() != null) return false;
        } else if (!charset.equals(other.getCharset())) return false;
        if (contact == null) {
            if (other.getContact() != null) return false;
        } else if (!contact.equals(other.getContact())) return false;
        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;
        if (numDecimals != other.getNumDecimals()) return false;
        if (onlineResource == null) {
            if (other.getOnlineResource() != null) return false;
        } else if (!onlineResource.equals(other.getOnlineResource())) return false;
        if (proxyBaseUrl == null) {
            if (other.getProxyBaseUrl() != null) return false;
        } else if (!proxyBaseUrl.equals(other.getProxyBaseUrl())) return false;
        if (schemaBaseUrl == null) {
            if (other.getSchemaBaseUrl() != null) return false;
        } else if (!schemaBaseUrl.equals(other.getSchemaBaseUrl())) return false;
        if (title == null) {
            if (other.getTitle() != null) return false;
        } else if (!title.equals(other.getTitle())) return false;
        if (verbose != other.isVerbose()) return false;
        if (verboseExceptions != other.isVerboseExceptions()) return false;

        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(title)
                .append(']')
                .toString();
    }

    @Override
    public boolean isLocalWorkspaceIncludesPrefix() {
        return localWorkspaceIncludesPrefix;
    }

    @Override
    public void setLocalWorkspaceIncludesPrefix(boolean removePrefix) {
        localWorkspaceIncludesPrefix = removePrefix;
    }

    /** @return the showCreatedTimeColumnsInAdminList */
    @Override
    public boolean isShowCreatedTimeColumnsInAdminList() {
        return showCreatedTimeColumnsInAdminList;
    }

    /** @param showCreatedTimeColumnsInAdminList the showCreatedTimeColumnsInAdminList to set */
    @Override
    public void setShowCreatedTimeColumnsInAdminList(boolean showCreatedTimeColumnsInAdminList) {
        this.showCreatedTimeColumnsInAdminList = showCreatedTimeColumnsInAdminList;
    }

    /** @return the showModifiedTimeColumnsInAdminList */
    @Override
    public boolean isShowModifiedTimeColumnsInAdminList() {
        return showModifiedTimeColumnsInAdminList;
    }

    /** @param showModifiedTimeColumnsInAdminList the showModifiedTimeColumnsInAdminList to set */
    @Override
    public void setShowModifiedTimeColumnsInAdminList(boolean showModifiedTimeColumnsInAdminList) {
        this.showModifiedTimeColumnsInAdminList = showModifiedTimeColumnsInAdminList;
    }
}
