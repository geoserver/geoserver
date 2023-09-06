/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.geoserver.util.InternationalStringUtils;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.logging.Logging;

/** Default implementation of {@link ResourceInfo}. */
@SuppressWarnings("serial")
public abstract class ResourceInfoImpl implements ResourceInfo {

    static final Logger LOGGER = Logging.getLogger(ResourceInfoImpl.class);

    protected String id;

    protected String name;

    protected String nativeName;

    protected List<String> alias = new ArrayList<>();

    protected NamespaceInfo namespace;

    protected String title;

    protected String description;

    protected String _abstract;

    protected List<KeywordInfo> keywords = new ArrayList<>();

    protected List<MetadataLinkInfo> metadataLinks = new ArrayList<>();

    protected List<DataLinkInfo> dataLinks = new ArrayList<>();

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

    protected Boolean simpleConversionEnabled = false;

    protected transient Catalog catalog;

    protected GrowableInternationalString internationalTitle;

    protected GrowableInternationalString internationalAbstract;

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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedName() */
    @Override
    public Name getQualifiedName() {
        return new NameImpl(getNamespace().getURI(), getName());
    }

    @Override
    public String getNativeName() {
        return nativeName;
    }

    @Override
    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    /** @see org.geoserver.catalog.ResourceInfo#getQualifiedNativeName() */
    @Override
    public Name getQualifiedNativeName() {
        return new NameImpl(getNamespace().getURI(), getNativeName());
    }

    @Override
    public NamespaceInfo getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(NamespaceInfo namespace) {
        this.namespace = namespace;
    }

    @Override
    public String prefixedName() {
        return getNamespace().getPrefix() + ":" + getName();
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAbstract() {
        return InternationalStringUtils.getOrDefault(_abstract, internationalAbstract);
    }

    @Override
    public void setAbstract(String _abstract) {
        this._abstract = _abstract;
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
    public List<MetadataLinkInfo> getMetadataLinks() {
        return metadataLinks;
    }

    @Override
    public List<DataLinkInfo> getDataLinks() {
        return dataLinks;
    }

    @Override
    public String getSRS() {
        return srs;
    }

    @Override
    public void setSRS(String srs) {
        this.srs = srs;
    }

    @Override
    public ReferencedEnvelope boundingBox() throws Exception {
        CoordinateReferenceSystem declaredCRS = getCRS();
        CoordinateReferenceSystem nativeCRS = getNativeCRS();
        ProjectionPolicy php = getProjectionPolicy();

        ReferencedEnvelope nativeBox = getNativeBoundingBox();
        if (nativeBox == null) {
            // back project from lat lon
            try {
                if (declaredCRS != null) {
                    nativeBox = getLatLonBoundingBox().transform(declaredCRS, true);
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            "Failed to derive native bbox, there is no declared CRS provided");
                    return null;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to derive native bbox from declared one", e);
                return null;
            }
        } else if (nativeBox.getCoordinateReferenceSystem() == null) {
            if (nativeCRS != null) {
                // Ensure provided nativeBox using nativeCRS
                nativeBox = ReferencedEnvelope.create(nativeBox, nativeCRS);
            }
        } else if (nativeCRS != null
                && !CRS.equalsIgnoreMetadata(nativeBox.getCoordinateReferenceSystem(), nativeCRS)) {
            LOGGER.log(Level.FINE, "The native bounding box srs does not match native crs");
        }

        //
        if (php == ProjectionPolicy.REPROJECT_TO_DECLARED
                && (!CRS.equalsIgnoreMetadata(declaredCRS, nativeCRS)
                        || !nativeCRSHasIdentifiers(nativeCRS))) {
            if (nativeBox.getCoordinateReferenceSystem() == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to reproject to declared crs (native bounding box srs and native crs are not defined)");
                return null;
            }
            // transform below makes a copy, in no case the actual field value is returned to the
            // client, this is not a getter, it's a derivative, thus ModificationProxy won't do
            // a copy on its own
            return nativeBox.transform(declaredCRS, true);
        } else if (php == ProjectionPolicy.FORCE_DECLARED) {
            // create below makes a copy, in no case the actual field value is returned to the
            // client, this is not a getter, it's a derivative, thus ModificationProxy won't do
            // a copy on its own
            return ReferencedEnvelope.envelope(nativeBox, declaredCRS);
        } else {
            if (nativeBox == null || nativeBox.getCoordinateReferenceSystem() == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to determine native bounding crs (both native bounding box srs and native crs are not defined)");
                // return null;
            }
            // create below makes a copy, in no case the actual field value is returned to the
            // client, this is not a getter, it's a derivative, thus ModificationProxy won't do
            // a copy on its own
            return ReferencedEnvelope.create(nativeBox);
        }
    }

    private static boolean nativeCRSHasIdentifiers(CoordinateReferenceSystem nativeCRS) {
        return Optional.ofNullable(nativeCRS)
                .map(CoordinateReferenceSystem::getIdentifiers)
                .filter(c -> !c.isEmpty())
                .isPresent();
    }

    @Override
    public ReferencedEnvelope getLatLonBoundingBox() {
        return latLonBoundingBox;
    }

    @Override
    public void setLatLonBoundingBox(ReferencedEnvelope box) {
        this.latLonBoundingBox = box;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** @see ResourceInfo#enabled() */
    @Override
    public boolean enabled() {
        StoreInfo store = getStore();
        boolean storeEnabled = store != null && store.isEnabled();
        return storeEnabled && this.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
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

    @Override
    public StoreInfo getStore() {
        return store;
    }

    @Override
    public void setStore(StoreInfo store) {
        this.store = store;
    }

    @Override
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

    @Override
    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
    }

    @Override
    public CoordinateReferenceSystem getCRS() {
        if (getSRS() == null) {
            return null;
        }

        // TODO: cache this
        try {
            return CRS.decode(getSRS());
        } catch (Exception e) {
            throw new RuntimeException(
                    "This is unexpected, the layer srs seems to be mis-configured", e);
        }
    }

    @Override
    public ReferencedEnvelope getNativeBoundingBox() {
        return nativeBoundingBox;
    }

    @Override
    public void setNativeBoundingBox(ReferencedEnvelope box) {
        this.nativeBoundingBox = box;
    }

    @Override
    public CoordinateReferenceSystem getNativeCRS() {
        return nativeCRS;
    }

    @Override
    public void setNativeCRS(CoordinateReferenceSystem nativeCRS) {
        this.nativeCRS = nativeCRS;
    }

    @Override
    public ProjectionPolicy getProjectionPolicy() {
        return projectionPolicy;
    }

    @Override
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

    @Override
    public boolean isSimpleConversionEnabled() {
        return simpleConversionEnabled == null ? false : simpleConversionEnabled;
    }

    @Override
    public void setSimpleConversionEnabled(boolean simpleConversionEnabled) {
        this.simpleConversionEnabled = simpleConversionEnabled;
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
    public GrowableInternationalString getInternationalAbstract() {
        return this.internationalAbstract;
    }

    @Override
    public void setInternationalAbstract(InternationalString internationalAbstract) {
        this.internationalAbstract = InternationalStringUtils.growable(internationalAbstract);
    }

    @Override
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
        if (getAbstract() == null) {
            if (other.getAbstract() != null) return false;
        } else if (!getAbstract().equals(other.getAbstract())) return false;
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
        if (this.getTitle() == null) {
            if (other.getTitle() != null) return false;
        } else if (!this.getTitle().equals(other.getTitle())) return false;
        return true;
    }
}
