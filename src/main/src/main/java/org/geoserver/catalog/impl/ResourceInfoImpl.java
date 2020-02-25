/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Default implementation of {@link ResourceInfo}. */
@SuppressWarnings("serial")
public abstract class ResourceInfoImpl implements ResourceInfo {

    static final Logger LOGGER = Logging.getLogger(ResourceInfoImpl.class);

    protected String id;

    protected String name;

    protected String nativeName;

    protected List<String> alias = new ArrayList<String>();

    protected NamespaceInfo namespace;

    protected String title;

    protected String description;

    protected String _abstract;

    protected List<KeywordInfo> keywords = new ArrayList<KeywordInfo>();

    protected List<MetadataLinkInfo> metadataLinks = new ArrayList<MetadataLinkInfo>();

    protected List<DataLinkInfo> dataLinks = new ArrayList<DataLinkInfo>();

    protected CoordinateReferenceSystem nativeCRS;

    protected String srs;

    protected ReferencedEnvelope nativeBoundingBox;

    protected ReferencedEnvelope latLonBoundingBox;

    protected ProjectionPolicy projectionPolicy;

    protected boolean enabled;

    protected Boolean advertised;

    protected MetadataMap metadata = new MetadataMap();

    protected StoreInfo store;

    protected boolean serviceConfiguration = false;

    protected List<String> disabledServices = new ArrayList<>();

    protected transient Catalog catalog;

    protected ResourceInfoImpl() {}

    protected ResourceInfoImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    protected ResourceInfoImpl(Catalog catalog, String id) {
        this(catalog);
        setId(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedName() */
    public Name getQualifiedName() {
        return new NameImpl(getNamespace().getURI(), getName());
    }

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedNativeName() */
    public Name getQualifiedNativeName() {
        return new NameImpl(getNamespace().getURI(), getNativeName());
    }

    public NamespaceInfo getNamespace() {
        return namespace;
    }

    public void setNamespace(NamespaceInfo namespace) {
        this.namespace = namespace;
    }

    public String prefixedName() {
        return getNamespace().getPrefix() + ":" + getName();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAbstract() {
        return _abstract;
    }

    public void setAbstract(String _abstract) {
        this._abstract = _abstract;
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

    public List<MetadataLinkInfo> getMetadataLinks() {
        return metadataLinks;
    }

    public List<DataLinkInfo> getDataLinks() {
        return dataLinks;
    }

    public String getSRS() {
        return srs;
    }

    public void setSRS(String srs) {
        this.srs = srs;
    }

    public ReferencedEnvelope boundingBox() throws Exception {
        CoordinateReferenceSystem declaredCRS = getCRS();
        CoordinateReferenceSystem nativeCRS = getNativeCRS();
        ProjectionPolicy php = getProjectionPolicy();

        ReferencedEnvelope nativeBox = this.nativeBoundingBox;
        if (nativeBox == null) {
            // back project from lat lon
            try {
                nativeBox = getLatLonBoundingBox().transform(declaredCRS, true);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to derive native bbox from declared one", e);
                return null;
            }
        }

        ReferencedEnvelope result;
        if (!CRS.equalsIgnoreMetadata(declaredCRS, nativeCRS)
                && php == ProjectionPolicy.REPROJECT_TO_DECLARED) {
            result = nativeBox.transform(declaredCRS, true);
        } else if (php == ProjectionPolicy.FORCE_DECLARED) {
            result = ReferencedEnvelope.create((Envelope) nativeBox, declaredCRS);
        } else {
            result = nativeBox;
        }

        // make sure that in no case the actual field value is returned to the client, this
        // is not a getter, it's a derivative, thus ModificationProxy won't do a copy on its own
        return ReferencedEnvelope.create(result);
    }

    public ReferencedEnvelope getLatLonBoundingBox() {
        return latLonBoundingBox;
    }

    public void setLatLonBoundingBox(ReferencedEnvelope box) {
        this.latLonBoundingBox = box;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** @see ResourceInfo#enabled() */
    public boolean enabled() {
        StoreInfo store = getStore();
        boolean storeEnabled = store != null && store.isEnabled();
        return storeEnabled && this.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metaData) {
        this.metadata = metaData;
    }

    public void setMetadataLinks(List<MetadataLinkInfo> metaDataLinks) {
        this.metadataLinks = metaDataLinks;
    }

    public void setDataLinks(List<DataLinkInfo> dataLinks) {
        this.dataLinks = dataLinks;
    }

    public StoreInfo getStore() {
        return store;
    }

    public void setStore(StoreInfo store) {
        this.store = store;
    }

    public <T extends Object> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        // subclasses should override
        return null;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(name)
                .append(']')
                .toString();
    }

    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
    }

    public CoordinateReferenceSystem getCRS() {
        if (getSRS() == null) {
            return null;
        }

        // TODO: cache this
        try {
            return CRS.decode(getSRS());
        } catch (Exception e) {
            throw new RuntimeException(
                    "This is unexpected, the layer seems to be mis-configured", e);
        }
    }

    public ReferencedEnvelope getNativeBoundingBox() {
        return nativeBoundingBox;
    }

    public void setNativeBoundingBox(ReferencedEnvelope box) {
        this.nativeBoundingBox = box;
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return nativeCRS;
    }

    public void setNativeCRS(CoordinateReferenceSystem nativeCRS) {
        this.nativeCRS = nativeCRS;
    }

    public ProjectionPolicy getProjectionPolicy() {
        return projectionPolicy;
    }

    public void setProjectionPolicy(ProjectionPolicy projectionPolicy) {
        this.projectionPolicy = projectionPolicy;
    }

    @Override
    public boolean isAdvertised() {
        if (this.advertised != null) {
            return advertised;
        }

        // check the metadata map for backwards compatibility with 2.1.x series
        MetadataMap md = getMetadata();
        if (md == null) {
            return true;
        }
        Boolean metadataAdvertised = md.get(LayerInfoImpl.KEY_ADVERTISED, Boolean.class);
        if (metadataAdvertised == null) {
            metadataAdvertised = true;
        }
        return metadataAdvertised;
    }

    @Override
    public void setAdvertised(boolean advertised) {
        this.advertised = advertised;
    }

    @Override
    public boolean isServiceConfiguration() {
        return serviceConfiguration;
    }

    @Override
    public void setServiceConfiguration(boolean serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    @Override
    public List<String> getDisabledServices() {
        return disabledServices;
    }

    @Override
    public void setDisabledServices(List<String> disabledServices) {
        this.disabledServices = disabledServices;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_abstract == null) ? 0 : _abstract.hashCode());
        result = prime * result + ((alias == null) ? 0 : alias.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
        result = prime * result + ((latLonBoundingBox == null) ? 0 : latLonBoundingBox.hashCode());
        result = prime * result + ((metadataLinks == null) ? 0 : metadataLinks.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((nativeBoundingBox == null) ? 0 : nativeBoundingBox.hashCode());
        result = prime * result + ((nativeCRS == null) ? 0 : nativeCRS.hashCode());
        result = prime * result + ((nativeName == null) ? 0 : nativeName.hashCode());
        result = prime * result + ((projectionPolicy == null) ? 0 : projectionPolicy.hashCode());
        result = prime * result + ((srs == null) ? 0 : srs.hashCode());
        result = prime * result + ((store == null) ? 0 : store.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ResourceInfo)) return false;

        final ResourceInfo other = (ResourceInfo) obj;
        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;
        if (_abstract == null) {
            if (other.getAbstract() != null) return false;
        } else if (!_abstract.equals(other.getAbstract())) return false;
        if (alias == null) {
            if (other.getAlias() != null) return false;
        } else if (!alias.equals(other.getAlias())) return false;
        if (description == null) {
            if (other.getDescription() != null) return false;
        } else if (!description.equals(other.getDescription())) return false;
        if (enabled != other.isEnabled()) return false;
        if (keywords == null) {
            if (other.getKeywords() != null) return false;
        } else if (!keywords.equals(other.getKeywords())) return false;
        if (latLonBoundingBox == null) {
            if (other.getLatLonBoundingBox() != null) return false;
        } else if (!latLonBoundingBox.equals(other.getLatLonBoundingBox())) return false;
        if (metadataLinks == null) {
            if (other.getMetadataLinks() != null) return false;
        } else if (!metadataLinks.equals(other.getMetadataLinks())) return false;
        if (name == null) {
            if (other.getName() != null) return false;
        } else if (!name.equals(other.getName())) return false;
        if (namespace == null) {
            if (other.getNamespace() != null) return false;
        } else if (!namespace.equals(other.getNamespace())) return false;
        if (nativeBoundingBox == null) {
            if (other.getNativeBoundingBox() != null) return false;
        } else if (!nativeBoundingBox.equals(other.getNativeBoundingBox())) return false;
        if (nativeCRS == null) {
            if (other.getNativeCRS() != null) return false;
        } else if (!CRS.equalsIgnoreMetadata(nativeCRS, other.getNativeCRS())) return false;
        if (nativeName == null) {
            if (other.getNativeName() != null) return false;
        } else if (!nativeName.equals(other.getNativeName())) return false;
        if (projectionPolicy == null) {
            if (other.getProjectionPolicy() != null) return false;
        } else if (!projectionPolicy.equals(other.getProjectionPolicy())) return false;
        if (srs == null) {
            if (other.getSRS() != null) return false;
        } else if (!srs.equals(other.getSRS())) return false;
        if (store == null) {
            if (other.getStore() != null) return false;
        } else if (!store.equals(other.getStore())) return false;
        if (title == null) {
            if (other.getTitle() != null) return false;
        } else if (!title.equals(other.getTitle())) return false;
        return true;
    }
}
