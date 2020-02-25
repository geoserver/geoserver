/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
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

    public MapLayerInfo(SimpleFeatureSource remoteSource) {
        this.remoteFeatureSource = remoteSource;
        this.layerInfo = null;
        name = remoteFeatureSource.getSchema().getTypeName();
        label = name;
        description = "Remote WFS";
        type = TYPE_REMOTE_VECTOR;
    }

    public MapLayerInfo(LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
        this.remoteFeatureSource = null;
        ResourceInfo resource = layerInfo.getResource();

        // handle InlineFeatureStuff
        this.name = resource.prefixedName();
        this.label = resource.getTitle();
        this.description = resource.getAbstract();

        this.type = layerInfo.getType().getCode();
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
        final List<String> styleNames = new ArrayList<String>();

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
        if (layerInfo == null) {
            return false;
        }
        if (type == TYPE_REMOTE_VECTOR) {
            // we just assume remote WFS is not cacheable since it's just used
            // in feature portrayal requests (which are one off and don't have a way to
            // tell us how often the remote WFS changes)
            return false;
        }
        ResourceInfo resource = layerInfo.getResource();
        MetadataMap metadata = resource.getMetadata();
        Boolean cachingEnabled = null;
        if (metadata != null) {
            cachingEnabled = metadata.get(ResourceInfo.CACHING_ENABLED, Boolean.class);
        }
        return cachingEnabled == null ? false : cachingEnabled.booleanValue();
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
        if (layerInfo == null) {
            return 0;
        }
        ResourceInfo resource = layerInfo.getResource();
        Integer val = resource.getMetadata().get(ResourceInfo.CACHE_AGE_MAX, Integer.class);
        return val == null ? 0 : val;
    }

    /**
     * If this layers has been setup to reproject data, skipReproject = true will disable
     * reprojection. This method is build especially for the rendering subsystem that should be able
     * to perform a full reprojection on its own, and do generalization before reprojection (thus
     * avoid to reproject all of the original coordinates)
     */
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(
            boolean skipReproject, CoordinateReferenceSystem coordinateReferenceSystem)
            throws IOException {
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

        if (userMapCRSForWFSNG(resource, coordinateReferenceSystem)) {
            // a hint for wfs-ng featuresource to keep the crs in query
            // to skip un-necessary re-projection
            hints.put(ResourcePool.MAP_CRS, coordinateReferenceSystem);
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
        // verify the resource is WFS-NG and contains Other SRS in feature metadata
        if (resource.getStore().getConnectionParameters().get(WFSDataStoreFactory.USEDEFAULTSRS.key)
                        == null
                || resource.getMetadata().get(FeatureTypeInfo.OTHER_SRS) == null) return false;
        // do nothing if datastore is configure to stay with native remote srs
        if (Boolean.valueOf(
                resource.getStore()
                        .getConnectionParameters()
                        .get(WFSDataStoreFactory.USEDEFAULTSRS.key)
                        .toString())) return false;

        // check if map crs is part of other srs, if yes send put it sindie hend
        // read all identifiers of this CRS into a list
        List<String> identifiers =
                coordinateReferenceSystem
                        .getIdentifiers()
                        .stream()
                        .map(r -> r.toString())
                        .collect(Collectors.toList());
        String otherSRSList = (String) resource.getMetadata().get(FeatureTypeInfo.OTHER_SRS);
        // check if mapCRS is supported in remote wfs layer
        for (String crs : identifiers)
            if (otherSRSList.contains(crs)) {
                // also verify axis order if matched
                try {
                    return CRS.equalsIgnoreMetadata(CRS.decode(crs), coordinateReferenceSystem);
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
