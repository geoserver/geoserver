/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class LayerGroupInfoImpl implements LayerGroupInfo {

    protected String id;
    protected String name;
    protected Mode mode = Mode.SINGLE;
    protected Boolean queryDisabled;

    /** This property in 2.2.x series is stored under the metadata map with key 'title'. */
    protected String title;

    /** This property in 2.2.x series is stored under the metadata map with key 'abstract'. */
    protected String abstractTxt;

    protected Boolean enabled;

    protected Boolean advertised;

    protected WorkspaceInfo workspace;
    protected String path;
    protected LayerInfo rootLayer;
    protected StyleInfo rootLayerStyle;

    /**
     * This property is here for compatibility purpose, in 2.3.x series it has been replaced by
     * 'publishables'
     */
    protected List<LayerInfo> layers = new ArrayList<LayerInfo>();

    protected List<PublishedInfo> publishables = new ArrayList<PublishedInfo>();
    protected List<StyleInfo> styles = new ArrayList<StyleInfo>();
    protected List<MetadataLinkInfo> metadataLinks = new ArrayList<MetadataLinkInfo>();

    protected ReferencedEnvelope bounds;

    protected MetadataMap metadata = new MetadataMap();

    protected AttributionInfo attribution;

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "authorityURLs", and a not transient in the 2.2.x series.
     *
     * @since 2.1.3
     */
    protected List<AuthorityURLInfo> authorityURLs = new ArrayList<AuthorityURLInfo>(2);

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "identifiers", and a not transient in the 2.2.x series.
     *
     * @since 2.1.3
     */
    protected List<LayerIdentifierInfo> identifiers = new ArrayList<LayerIdentifierInfo>(2);

    private List<KeywordInfo> keywords = new ArrayList<>();

    protected Date dateCreated;

    protected Date dateModified;

    @Override
    public List<KeywordInfo> getKeywords() {
        return keywords;
    }

    /**
     * Set the keywords of this layer group. The provided keywords will override any existing
     * keywords no merge will be done.
     *
     * @param keywords new keywords of this layer group
     */
    public void setKeywords(List<KeywordInfo> keywords) {
        this.keywords = keywords == null ? new ArrayList<>() : keywords;
    }

    public LayerGroupInfoImpl() {
        mode = Mode.SINGLE;
        publishables = new ArrayList<PublishedInfo>();
        styles = new ArrayList<StyleInfo>();
        metadata = new MetadataMap();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean isEnabled() {
        if (this.enabled != null) return this.enabled;
        else return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isQueryDisabled() {
        return queryDisabled != null ? queryDisabled.booleanValue() : false;
    }

    @Override
    public void setQueryDisabled(boolean queryDisabled) {
        this.queryDisabled = queryDisabled ? Boolean.TRUE : null;
    }

    @Override
    public String getTitle() {
        if (title == null && metadata != null) {
            title = metadata.get("title", String.class);
        }
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean isAdvertised() {
        if (this.advertised != null) {
            return advertised;
        } else {
            return true;
        }
    }

    @Override
    public void setAdvertised(boolean advertised) {
        this.advertised = advertised;
    }

    @Override
    public String getAbstract() {
        if (abstractTxt == null && metadata != null) {
            abstractTxt = metadata.get("title", String.class);
        }
        return abstractTxt;
    }

    @Override
    public void setAbstract(String abstractTxt) {
        this.abstractTxt = abstractTxt;
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
    public String prefixedName() {
        return workspace != null ? workspace.getName() + ":" + name : name;
    }

    @Override
    public LayerInfo getRootLayer() {
        return rootLayer;
    }

    @Override
    public void setRootLayer(LayerInfo rootLayer) {
        this.rootLayer = rootLayer;
    }

    @Override
    public StyleInfo getRootLayerStyle() {
        return rootLayerStyle;
    }

    @Override
    public void setRootLayerStyle(StyleInfo style) {
        this.rootLayerStyle = style;
    }

    @Override
    public List<PublishedInfo> getLayers() {
        return publishables;
    }

    public void setLayers(List<PublishedInfo> publishables) {
        this.publishables = publishables;
    }

    /**
     * Used after deserialization. It converts 'layers' property content, used until 2.3.x, to
     * 'publishables' property content.
     */
    public void convertLegacyLayers() {
        if (layers != null && publishables == null) {
            publishables = new ArrayList<PublishedInfo>();
            for (LayerInfo layer : layers) {
                publishables.add(layer);
            }
            layers = null;
        }
    }

    @Override
    public List<StyleInfo> getStyles() {
        return styles;
    }

    public void setStyles(List<StyleInfo> styles) {
        this.styles = styles;
    }

    @Override
    public List<LayerInfo> layers() {
        LayerGroupHelper helper = new LayerGroupHelper(this);
        return helper.allLayersForRendering();
    }

    @Override
    public List<StyleInfo> styles() {
        LayerGroupHelper helper = new LayerGroupHelper(this);
        return helper.allStylesForRendering();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(ReferencedEnvelope bounds) {
        this.bounds = bounds;
    }

    @Override
    public MetadataMap getMetadata() {
        checkMetadataNotNull();
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        return LayerGroupInfo.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return LayerGroupInfo.equals(this, obj);
    }

    @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return authorityURLs;
    }

    public void setAuthorityURLs(List<AuthorityURLInfo> authorities) {
        this.authorityURLs = authorities;
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<LayerIdentifierInfo> identifiers) {
        this.identifiers = identifiers;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(name)
                .append(']')
                .toString();
    }

    public String getPrefixedName() {
        return prefixedName();
    }

    @Override
    public PublishedType getType() {
        return PublishedType.GROUP;
    }

    @Override
    public AttributionInfo getAttribution() {
        return attribution;
    }

    @Override
    public void setAttribution(AttributionInfo attribution) {
        this.attribution = attribution;
    }

    @Override
    public List<MetadataLinkInfo> getMetadataLinks() {
        return metadataLinks;
    }

    public void setMetadataLinks(List<MetadataLinkInfo> metadataLinks) {
        this.metadataLinks = metadataLinks;
    }

    @Override
    public Date getDateModified() {
        return this.dateModified;
    }

    @Override
    public Date getDateCreated() {
        return this.dateCreated;
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    private void checkMetadataNotNull() {
        if (metadata == null) metadata = new MetadataMap();
    }
}
