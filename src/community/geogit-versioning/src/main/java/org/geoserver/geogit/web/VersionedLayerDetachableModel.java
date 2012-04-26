/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geogit.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.geogit.GEOGIT;
import org.geoserver.web.GeoServerApplication;
import org.opengis.feature.type.Name;

public class VersionedLayerDetachableModel extends LoadableDetachableModel<VersionedLayerInfo> {

    private static final long serialVersionUID = 1L;

    private Name typeName;

    public VersionedLayerDetachableModel(VersionedLayerInfo versionedLayerInfo) {
        this.typeName = versionedLayerInfo.getName();
    }

    @Override
    protected VersionedLayerInfo load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        return load(catalog, typeName);
    }

    private static VersionedLayerInfo load(FeatureTypeInfo featureType) {
        final Name featureTypeName = featureType.getQualifiedName();
        final GEOGIT geogitFacade = GEOGIT.get();
        final boolean published = geogitFacade.isReplicated(featureTypeName);
        VersionedLayerInfo versionedLayerInfo = new VersionedLayerInfo(featureType);
        versionedLayerInfo.setPublished(published);
        return versionedLayerInfo;
    }

    public static List<VersionedLayerInfo> getItems() {
        List<Name> syncedNames;
        try {
            syncedNames = GEOGIT.get().listLayers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getItems(syncedNames);
    }

    public static List<VersionedLayerInfo> getItems(final List<Name> typeNames) {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<VersionedLayerInfo> types = new ArrayList<VersionedLayerInfo>();
        for (Name typeName : typeNames) {
            VersionedLayerInfo versionedLayerInfo = load(catalog, typeName);
            types.add(versionedLayerInfo);
        }
        return types;
    }

    private static VersionedLayerInfo load(final Catalog catalog, final Name typeName) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(typeName);
        VersionedLayerInfo versionedLayerInfo;
        if (null == featureType) {
            versionedLayerInfo = new VersionedLayerInfo(typeName);
        } else {
            versionedLayerInfo = load(featureType);
        }
        return versionedLayerInfo;
    }

}