/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;

public class ServiceInfoImpl implements ServiceInfo {

    protected String id;

    protected WorkspaceInfo workspace;

    protected transient GeoServer geoServer;

    protected boolean enabled = true;

    protected String name;

    protected String title;

    protected String maintainer;

    protected String abstrct;

    protected String accessConstraints;

    protected String fees;

    protected List versions = new ArrayList();

    protected List<KeywordInfo> keywords = new ArrayList();

    protected List exceptionFormats = new ArrayList();

    protected MetadataLinkInfo metadataLink;

    protected boolean citeCompliant;

    protected String onlineResource;

    protected String schemaBaseURL = "http://schemas.opengis.net";

    protected boolean verbose;

    protected String outputStrategy;

    protected MetadataMap metadata = new MetadataMap();

    protected Map clientProperties = new HashMap();

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

    public GeoServer getGeoServer() {
        return geoServer;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    public String getAbstract() {
        return abstrct;
    }

    public void setAbstract(String abstrct) {
        this.abstrct = abstrct;
    }

    public String getAccessConstraints() {
        return accessConstraints;
    }

    public void setAccessConstraints(String accessConstraints) {
        this.accessConstraints = accessConstraints;
    }

    public String getFees() {
        return fees;
    }

    public void setFees(String fees) {
        this.fees = fees;
    }

    public List<KeywordInfo> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<KeywordInfo> keywords) {
        this.keywords = keywords;
    }

    public List<String> keywordValues() {
        List<String> values = new ArrayList<String>();
        if (keywords != null) {
            for (KeywordInfo kw : keywords) {
                values.add(kw.getValue());
            }
        }
        return values;
    }

    public List getVersions() {
        return versions;
    }

    public void setVersions(List versions) {
        this.versions = versions;
    }

    public List getExceptionFormats() {
        return exceptionFormats;
    }

    public void setExceptionFormats(List exceptionFormats) {
        this.exceptionFormats = exceptionFormats;
    }

    public MetadataLinkInfo getMetadataLink() {
        return metadataLink;
    }

    public void setMetadataLink(MetadataLinkInfo metadataLink) {
        this.metadataLink = metadataLink;
    }

    public boolean isCiteCompliant() {
        return citeCompliant;
    }

    public void setCiteCompliant(boolean citeCompliant) {
        this.citeCompliant = citeCompliant;
    }

    public String getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(String onlineResource) {
        this.onlineResource = onlineResource;
    }

    public MetadataMap getMetadata() {
        if (metadata == null) {
            metadata = new MetadataMap();
        }
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    public Map getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(Map clientProperties) {
        this.clientProperties = clientProperties;
    }

    public String getOutputStrategy() {
        return outputStrategy;
    }

    public void setOutputStrategy(String outputStrategy) {
        this.outputStrategy = outputStrategy;
    }

    public String getSchemaBaseURL() {
        return schemaBaseURL;
    }

    public void setSchemaBaseURL(String schemaBaseURL) {
        this.schemaBaseURL = schemaBaseURL;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((abstrct == null) ? 0 : abstrct.hashCode());
        result = prime * result + ((accessConstraints == null) ? 0 : accessConstraints.hashCode());
        result = prime * result + (citeCompliant ? 1231 : 1237);
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((exceptionFormats == null) ? 0 : exceptionFormats.hashCode());
        result = prime * result + ((fees == null) ? 0 : fees.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
        result = prime * result + ((maintainer == null) ? 0 : maintainer.hashCode());
        result = prime * result + ((metadataLink == null) ? 0 : metadataLink.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((onlineResource == null) ? 0 : onlineResource.hashCode());
        result = prime * result + ((outputStrategy == null) ? 0 : outputStrategy.hashCode());
        result = prime * result + ((schemaBaseURL == null) ? 0 : schemaBaseURL.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + (verbose ? 1231 : 1237);
        result = prime * result + ((versions == null) ? 0 : versions.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ServiceInfo)) {
            return false;
        }

        final ServiceInfo other = (ServiceInfo) obj;
        if (abstrct == null) {
            if (other.getAbstract() != null) return false;
        } else if (!abstrct.equals(other.getAbstract())) return false;
        if (accessConstraints == null) {
            if (other.getAccessConstraints() != null) return false;
        } else if (!accessConstraints.equals(other.getAccessConstraints())) return false;
        if (citeCompliant != other.isCiteCompliant()) return false;
        if (enabled != other.isEnabled()) return false;
        if (exceptionFormats == null) {
            if (other.getExceptionFormats() != null) return false;
        } else if (!exceptionFormats.equals(other.getExceptionFormats())) return false;
        if (fees == null) {
            if (other.getFees() != null) return false;
        } else if (!fees.equals(other.getFees())) return false;
        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;
        if (keywords == null) {
            if (other.getKeywords() != null) return false;
        } else if (!keywords.equals(other.getKeywords())) return false;
        if (maintainer == null) {
            if (other.getMaintainer() != null) return false;
        } else if (!maintainer.equals(other.getMaintainer())) return false;
        if (metadataLink == null) {
            if (other.getMetadataLink() != null) return false;
        } else if (!metadataLink.equals(other.getMetadataLink())) return false;
        if (name == null) {
            if (other.getName() != null) return false;
        } else if (!name.equals(other.getName())) return false;
        if (onlineResource == null) {
            if (other.getOnlineResource() != null) return false;
        } else if (!onlineResource.equals(other.getOnlineResource())) return false;
        if (outputStrategy == null) {
            if (other.getOutputStrategy() != null) return false;
        } else if (!outputStrategy.equals(other.getOutputStrategy())) return false;
        if (schemaBaseURL == null) {
            if (other.getSchemaBaseURL() != null) return false;
        } else if (!schemaBaseURL.equals(other.getSchemaBaseURL())) return false;
        if (title == null) {
            if (other.getTitle() != null) return false;
        } else if (!title.equals(other.getTitle())) return false;
        if (verbose != other.isVerbose()) return false;
        if (versions == null) {
            if (other.getVersions() != null) return false;
        } else if (!versions.equals(other.getVersions())) return false;
        if (workspace == null) {
            if (other.getWorkspace() != null) return false;
        } else if (!workspace.equals(other.getWorkspace())) return false;
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(name)
                .append(']')
                .toString();
    }
}
