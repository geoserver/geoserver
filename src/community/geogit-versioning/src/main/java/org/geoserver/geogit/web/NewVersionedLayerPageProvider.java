/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geogit.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.feature.type.Name;

/**
 * Provides a list of resources for a specific data store
 * 
 * @author groldan
 */
@SuppressWarnings("serial")
public class NewVersionedLayerPageProvider extends GeoServerDataProvider<VersionedLayerInfo> {

    private static final Comparator<VersionedLayerInfo> GEOM_COMPARATOR = new Comparator<VersionedLayerInfo>() {
        public int compare(VersionedLayerInfo o1, VersionedLayerInfo o2) {
            Class<?> gt1 = o1.getGeometryType();
            Class<?> gt2 = o2.getGeometryType();
            if (gt1 == null) {
                return gt2 == null ? 0 : -1;
            }
            return gt1.getName().compareTo(gt2.getName());
        }
    };

    public static final Property<VersionedLayerInfo> PUBLISHED = new BeanProperty<VersionedLayerInfo>(
            "status", "published");

    public static final Property<VersionedLayerInfo> NAME = new BeanProperty<VersionedLayerInfo>(
            "name", "name");

    public static final Property<VersionedLayerInfo> GEOMTYPE = new BeanProperty<VersionedLayerInfo>(
            "type", "geometryType") {

        @Override
        public Comparator<VersionedLayerInfo> getComparator() {
            return GEOM_COMPARATOR;
        }
    };

    private boolean showPublished;

    private String storeId;

    @Override
    protected List<VersionedLayerInfo> getItems() {

        // return an empty list in case we still don't know about the store
        if (storeId == null) {
            return new ArrayList<VersionedLayerInfo>();
        }

        // else, grab the resource list
        try {
            DataStoreInfo dstore = getCatalog().getStore(storeId, DataStoreInfo.class);
            Catalog catalog = getCatalog();
            List<FeatureTypeInfo> featureTypes = catalog.getFeatureTypesByDataStore(dstore);
            List<Name> typeNames = new ArrayList<Name>(featureTypes.size());

            for (FeatureTypeInfo fti : featureTypes) {
                if (fti.enabled()) {
                    Name typeName = fti.getQualifiedName();
                    typeNames.add(typeName);
                }
            }

            List<VersionedLayerInfo> items = new ArrayList<VersionedLayerInfo>(
                    VersionedLayerDetachableModel.getItems(typeNames));

            // return by natural order
            Collections.sort(items);
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Could not list layers for this store, "
                    + "an error occurred retrieving them: " + e.getMessage(), e);
        }

    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    @Override
    protected List<VersionedLayerInfo> getFilteredItems() {
        List<VersionedLayerInfo> resources = super.getFilteredItems();
        if (showPublished)
            return resources;

        List<VersionedLayerInfo> unconfigured = new ArrayList<VersionedLayerInfo>();
        for (VersionedLayerInfo resource : resources) {
            if (!resource.isPublished()) {
                unconfigured.add(resource);
            }
        }
        return unconfigured;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Property<VersionedLayerInfo>> getProperties() {
        return Collections.unmodifiableList(Arrays.asList(PUBLISHED, GEOMTYPE, NAME));
    }

    public void setShowPublished(boolean showPublished) {
        this.showPublished = showPublished;
    }

}
