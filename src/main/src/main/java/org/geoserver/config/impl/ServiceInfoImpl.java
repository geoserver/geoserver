/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.util.InternationalStringUtils;
import org.geotools.api.util.InternationalString;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.Version;

public class ServiceInfoImpl implements ServiceInfo {

    protected String id;

    protected WorkspaceInfo workspace;

    protected transient GeoServer geoServer;

    protected boolean enabled = true;

    protected String name;

    protected String title;

    protected GrowableInternationalString internationalTitle;

    protected String maintainer;

    protected String abstrct;

    protected GrowableInternationalString internationalAbstract;

    protected String accessConstraints;

    protected String fees;

    protected List<Version> versions = new ArrayList<>();

    protected List<KeywordInfo> keywords = new ArrayList<>();

    protected List<String> exceptionFormats = new ArrayList<>();

    protected MetadataLinkInfo metadataLink;

    protected boolean citeCompliant;

    protected String onlineResource;

    protected String schemaBaseURL = "http://schemas.opengis.net";

    protected boolean verbose;

    protected String outputStrategy;

    protected MetadataMap metadata = new MetadataMap();

    protected Map<Object, Object> clientProperties = new HashMap<>();

    protected Locale defaultLocale;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Default implementation attempts to determine service type based on class naming convention.
     * Subclasses are encouraged to override.
     *
     * @return service type based on class name, truncating ServiceInfo.
     */
    @Override
    public String getType() {
        String simpleName = getClass().getSimpleName();
        int truncate = simpleName.indexOf("ServiceInfo");
        if (truncate > 0) {
            return simpleName.substring(0, truncate);
        } else {
            // this default, while incorrect, has the greatest chance of
            // success across data directories
            return getName() != null ? getName().toUpperCase() : null;
        }
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
    public GeoServer getGeoServer() {
        return geoServer;
    }

    @Override
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getTitle() {
        return InternationalStringUtils.getOrDefault(title, internationalTitle);
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public GrowableInternationalString getInternationalTitle() {
        return this.internationalTitle;
    }

    @Override
    public void setInternationalTitle(InternationalString internationalTitle) {
        this.internationalTitle = InternationalStringUtils.growable(internationalTitle);
    }

    @Override
    public String getMaintainer() {
        return maintainer;
    }

    @Override
    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    @Override
    public String getAbstract() {
        return InternationalStringUtils.getOrDefault(abstrct, internationalAbstract);
    }

    @Override
    public void setAbstract(String abstrct) {
        this.abstrct = abstrct;
    }

    @Override
    public GrowableInternationalString getInternationalAbstract() {
        return this.internationalAbstract;
    }

    @Override
    public void setInternationalAbstract(InternationalString internationalAbstract) {
        this.internationalAbstract = InternationalStringUtils.growable(internationalAbstract);
    }

    @Override
    public String getAccessConstraints() {
        return accessConstraints;
    }

    @Override
    public void setAccessConstraints(String accessConstraints) {
        this.accessConstraints = accessConstraints;
    }

    @Override
    public String getFees() {
        return fees;
    }

    @Override
    public void setFees(String fees) {
        this.fees = fees;
    }

    @Override
    public List<KeywordInfo> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<KeywordInfo> keywords) {
        this.keywords = keywords;
    }

    @Override
    public List<String> keywordValues() {
        List<String> values = new ArrayList<>();
        if (keywords != null) {
            for (KeywordInfo kw : keywords) {
                values.add(kw.getValue());
            }
        }
        return values;
    }

    @Override
    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    @Override
    public List<String> getExceptionFormats() {
        return exceptionFormats;
    }

    public void setExceptionFormats(List<String> exceptionFormats) {
        this.exceptionFormats = exceptionFormats;
    }

    @Override
    public MetadataLinkInfo getMetadataLink() {
        return metadataLink;
    }

    @Override
    public void setMetadataLink(MetadataLinkInfo metadataLink) {
        this.metadataLink = metadataLink;
    }

    @Override
    public boolean isCiteCompliant() {
        return citeCompliant;
    }

    @Override
    public void setCiteCompliant(boolean citeCompliant) {
        this.citeCompliant = citeCompliant;
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
    public MetadataMap getMetadata() {
        if (metadata == null) {
            metadata = new MetadataMap();
        }
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    @Override
    public Map<Object, Object> getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(Map<Object, Object> clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Override
    public String getOutputStrategy() {
        return outputStrategy;
    }

    @Override
    public void setOutputStrategy(String outputStrategy) {
        this.outputStrategy = outputStrategy;
    }

    @Override
    public String getSchemaBaseURL() {
        return schemaBaseURL;
    }

    @Override
    public void setSchemaBaseURL(String schemaBaseURL) {
        this.schemaBaseURL = schemaBaseURL;
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
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @Override
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ServiceInfo)) {
            return false;
        }

        final ServiceInfo other = (ServiceInfo) obj;
        if (getAbstract() == null) {
            if (other.getAbstract() != null) return false;
        } else if (!getAbstract().equals(other.getAbstract())) return false;
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
        if (getTitle() == null) {
            if (other.getTitle() != null) return false;
        } else if (!getTitle().equals(other.getTitle())) return false;
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
