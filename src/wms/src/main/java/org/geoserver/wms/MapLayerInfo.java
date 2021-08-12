/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.InternationalContentHelper;
import org.geoserver.util.GeoServerDefaultLocale;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.Style;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A convenience class that hides some of the differences between the various types of layers
 *
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 * @author Gabriel Roldan
 */
public final class MapLayerInfo {

    private static final Logger LOGGER = Logging.getLogger(MapLayerInfo.class);

    public static int TYPE_VECTOR = PublishedType.VECTOR.getCode();

    public static int TYPE_RASTER = PublishedType.RASTER.getCode();

    public static int TYPE_REMOTE_VECTOR = PublishedType.REMOTE.getCode();

    public static int TYPE_WMS = PublishedType.WMS.getCode();

    public static int TYPE_WMTS = PublishedType.WMTS.getCode();

    /** The feature source for the remote WFS layer (see REMOVE_OWS_TYPE/URL in the SLD spec) */
    private final SimpleFeatureSource remoteFeatureSource;

    /** @uml.property name="type" multiplicity="(0 1)" */
    private final int type;

    /** @uml.property name="name" multiplicity="(0 1)" */
    private String name;

    /** @uml.property name="label" multiplicity="(0 1)" */
    private final String label;

    /** @uml.property name="description" multiplicity="(0 1)" */
    private final String description;

    private final LayerInfo layerInfo;

    private Style style;

    /** The extra constraints that can be set when an external SLD is used */
    private FeatureTypeConstraint[] layerFeatureConstraints;

    private MetadataMap layerGroupMetadata;

    public MapLayerInfo(SimpleFeatureSource remoteSource) {
        this(remoteSource, new MetadataMap());
    }

    /**
     * @param remoteSource
     * @param layerGroupMetadata override layerInfo metadata e.g. max-age
     */
    public MapLayerInfo(SimpleFeatureSource remoteSource, MetadataMap layerGroupMetadata) {
        this.remoteFeatureSource = remoteSource;
        this.layerInfo = null;
        name = remoteFeatureSource.getSchema().getTypeName();
        label = name;
        description = "Remote WFS";
        type = TYPE_REMOTE_VECTOR;
        this.layerGroupMetadata = new MetadataMap(layerGroupMetadata);
    }

    public MapLayerInfo(LayerInfo layerInfo) {
        this(layerInfo, new MetadataMap());
    }

    public MapLayerInfo(LayerInfo layerInfo, Locale locale) {
        this(layerInfo, new MetadataMap(), locale);
    }

    public MapLayerInfo(LayerInfo layerInfo, MetadataMap layerGroupMetadata) {
        this(layerInfo, layerGroupMetadata, GeoServerDefaultLocale.get());
    }

    /**
     * @param layerInfo
     * @param layerGroupMetadata override layerInfo metadata e.g. max-age
     */
    public MapLayerInfo(LayerInfo layerInfo, MetadataMap layerGroupMetadata, Locale locale) {
        this.layerInfo = layerInfo;
        this.remoteFeatureSource = null;
        ResourceInfo resource = layerInfo.getResource();
        // handle InlineFeatureStuff
        this.name = resource.prefixedName();
        this.label = getLabel(locale, resource);
        this.description = getDescription(locale, resource);

        this.type = layerInfo.getType().getCode();
        this.layerGroupMetadata = new MetadataMap(layerGroupMetadata);
    }

    private String getLabel(Locale locale, ResourceInfo resourceInfo) {
        String label = resourceInfo.getTitle();
        if (resourceInfo.getInternationalTitle() != null && locale != null) {
            InternationalContentHelper internationalContentHelper =
                    new InternationalContentHelper(locale);
            String localized =
                    internationalContentHelper.getString(
                            resourceInfo.getInternationalTitle(), true);
            if (localized != null) label = localized;
        }
        return label;
    }

    private String getDescription(Locale locale, ResourceInfo resourceInfo) {
        String desc = resourceInfo.getAbstract();
        if (resourceInfo.getInternationalAbstract() != null && locale != null) {
            InternationalContentHelper internationalContentHelper =
                    new InternationalContentHelper(locale);
            String localized = internationalContentHelper.getAbstract(resourceInfo);
            if (localized != null) desc = localized;
        }
        return desc;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    /**
     * The feature source bounds. Mind, it might be null, in that case, grab the lat/lon bounding
     * box and reproject to the native bounds.
     *
     * @return Envelope the feature source bounds.
     */
    public ReferencedEnvelope getBoundingBox() throws Exception {
        if (layerInfo != null) {
            ResourceInfo resource = layerInfo.getResource();
            ReferencedEnvelope bbox = resource.boundingBox();
            // if(bbox == null){
            // bbox = resource.getLatLonBoundingBox();
            // }
            return bbox;
        } else if (this.type == TYPE_REMOTE_VECTOR) {
            return remoteFeatureSource.getBounds();
        }
        return null;
    }

    /**
     * Get the bounding box in latitude and longitude for this layer.
     *
     * @return Envelope the feature source bounds.
     * @throws IOException when an error occurs
     */
    public ReferencedEnvelope getLatLongBoundingBox() throws IOException {
        if (layerInfo != null) {
            ResourceInfo resource = layerInfo.getResource();
            return resource.getLatLonBoundingBox();
        }

        throw new UnsupportedOperationException(
                "getLatLongBoundingBox not " + "implemented for remote sources");
    }

    /** @uml.property name="coverage" */
    public CoverageInfo getCoverage() {
        return (CoverageInfo) layerInfo.getResource();
    }

    /** @uml.property name="description" */
    public String getDescription() {
        return description;
    }

    /** @uml.property name="feature" */
    public FeatureTypeInfo getFeature() {
        return (FeatureTypeInfo) layerInfo.getResource();
    }

    public ResourceInfo getResource() {
        return layerInfo.getResource();
    }

    /** @uml.property name="label" */
    public String getLabel() {
        return label;
    }

    /** @uml.property name="name" */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @uml.property name="type" */
    public int getType() {
        return type;
    }

    public Style getDefaultStyle() {
        if (layerInfo != null) {
            try {
                return layerInfo.getDefaultStyle().getStyle();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    /** Returns the remote feature source in case this layer is a remote WFS layer */
    public SimpleFeatureSource getRemoteFeatureSource() {
        return remoteFeatureSource;
    }

    /**
     * @return the resource SRS name or {@code null} if the underlying resource is not a registered
     *     one
     */
    public String getSRS() {
        if (layerInfo != null) {
            return layerInfo.getResource().getSRS();
        }
        return null;
    }

    /** @return the list of the alternate style names registered for this layer */
    public List<String> getOtherStyleNames() {
        if (layerInfo == null) {
            return Collections.emptyList();
        }
        final List<String> styleNames = new ArrayList<>();

        for (StyleInfo si : layerInfo.getStyles()) {
            styleNames.add(si.getName());
        }
        return styleNames;
    }

    /**
     * Should we add the cache-control: max-age header to maps containing this layer?
     *
     * @return true if we should, false if we should omit the header
     */
    public boolean isCachingEnabled() {
        if (type == TYPE_REMOTE_VECTOR) {
            // we just assume remote WFS is not cacheable since it's just used
            // in feature portrayal requests (which are one off and don't have a way to
            // tell us how often the remote WFS changes)
            return false;
        }

        if (this.isCachingEnabled(this.layerGroupMetadata)) {
            return true;
        }

        if (layerInfo == null) {
            return false;
        }

        ResourceInfo resource = layerInfo.getResource();
        return this.isCachingEnabled(resource.getMetadata());
    }

    private boolean isCachingEnabled(MetadataMap metadata) {
        Boolean value = metadata.get(ResourceInfo.CACHING_ENABLED, Boolean.class);

        return value != null ? value : false;
    }

    /**
     * This value is added the headers of generated maps, marking them as being both "cache-able"
     * and designating the time for which they are to remain valid. The specific header added is
     * "Cache-Control: max-age="
     *
     * @return the number of seconds to be added to the "Cache-Control: max-age=" header, or {@code
     *     0} if not set
     */
    public int getCacheMaxAge() {
        // maxAge may be defined in layer group metadata, but caching
        // may be disabled
        if (this.isCachingEnabled(this.layerGroupMetadata)) {
            return getCacheMaxAge(this.layerGroupMetadata);
        }

        if (layerInfo == null) {
            return 0;
        }

        ResourceInfo resource = layerInfo.getResource();
        return getCacheMaxAge(resource.getMetadata());
    }

    private int getCacheMaxAge(MetadataMap metadata) {
        Integer value = metadata.get(ResourceInfo.CACHE_AGE_MAX, Integer.class);

        return value != null ? value : 0;
    }

    /**
     * If this layers has been setup to reproject data, skipReproject = true will disable
     * reprojection. This method is build especially for the rendering subsystem that should be able
     * to perform a full reprojection on its own, and do generalization before reprojection (thus
     * avoid to reproject all of the original coordinates)
     */
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(
            boolean skipReproject, CoordinateReferenceSystem requestedCRS) throws IOException {
        if (type != TYPE_VECTOR) {
            throw new IllegalArgumentException("Layer type is not vector");
        }

        // ask for enabled() instead of isEnabled() to account for disabled resource/store
        if (!layerInfo.enabled()) {
            throw new IOException(
                    "featureType: "
                            + getName()
                            + " does not have a properly configured "
                            + "datastore");
        }

        FeatureTypeInfo resource = (FeatureTypeInfo) layerInfo.getResource();

        if (resource.getStore() == null || resource.getStore().getDataStore(null) == null) {
            throw new IOException(
                    "featureType: "
                            + getName()
                            + " does not have a properly configured "
                            + "datastore");
        }

        Hints hints = new Hints(ResourcePool.REPROJECT, Boolean.valueOf(!skipReproject));

        if (userMapCRSForWFSNG(resource, requestedCRS)) {
            // a hint for wfs-ng featuresource to keep the crs in query
            // to skip un-necessary re-projection
            hints.put(ResourcePool.MAP_CRS, requestedCRS);
        }

        return resource.getFeatureSource(null, hints);
    }

    public GridCoverageReader getCoverageReader() throws IOException {
        if (type != TYPE_RASTER) {
            throw new IllegalArgumentException("Layer type is not raster");
        }

        CoverageInfo resource = (CoverageInfo) layerInfo.getResource();
        return resource.getGridCoverageReader(null, GeoTools.getDefaultHints());
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        if (layerInfo != null) {
            return layerInfo.getResource().getCRS();
        }
        if (remoteFeatureSource != null) {
            SimpleFeatureType schema = remoteFeatureSource.getSchema();
            return schema.getCoordinateReferenceSystem();
        }
        throw new IllegalStateException();
    }

    public static String getRegionateAttribute(FeatureTypeInfo layerInfo) {
        return layerInfo.getMetadata().get("kml.regionateAttribute", String.class);
    }

    public void setLayerFeatureConstraints(FeatureTypeConstraint[] layerFeatureConstraints) {
        this.layerFeatureConstraints = layerFeatureConstraints;
    }

    public FeatureTypeConstraint[] getLayerFeatureConstraints() {
        return layerFeatureConstraints;
    }

    /**
     * Get layer info.
     *
     * @return layer info
     */
    public LayerInfo getLayerInfo() {
        return layerInfo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + type;
        return result;
    }

    private boolean userMapCRSForWFSNG(
            FeatureTypeInfo resource, CoordinateReferenceSystem coordinateReferenceSystem)
            throws IOException {
        // check if map crs is part of other srs, if yes send put it sindie hend
        String otherSRSListStr = (String) resource.getMetadata().get(FeatureTypeInfo.OTHER_SRS);
        // verify the resource is WFS-NG and contains Other SRS in feature metadata
        if (resource.getStore().getConnectionParameters().get(WFSDataStoreFactory.USEDEFAULTSRS.key)
                        == null
                || otherSRSListStr == null
                || otherSRSListStr.isEmpty()) return false;
        // do nothing if datastore is configure to stay with native remote srs
        if (Boolean.valueOf(
                resource.getStore()
                        .getConnectionParameters()
                        .get(WFSDataStoreFactory.USEDEFAULTSRS.key)
                        .toString())) return false;

        // create list of other srs
        List<String> otherSRSList = Arrays.asList(otherSRSListStr.split(","));
        // check if mapCRS is supported in remote wfs layer
        for (String otherSRS : otherSRSList) {
            try {
                // if no transformation is required, we have a match
                if (!CRS.isTransformationRequired(
                        CRS.decode(otherSRS), coordinateReferenceSystem)) {
                    LOGGER.fine(otherSRS + " SRS found in Other SRS");
                    return true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MapLayerInfo other = (MapLayerInfo) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (type != other.type) return false;
        return true;
    }
}
