/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

public class CatalogFactoryImpl implements CatalogFactory {

    Catalog catalog;

    public CatalogFactoryImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public CoverageInfo createCoverage() {
        return new CoverageInfoImpl(catalog);
    }

    @Override
    public CoverageDimensionInfo createCoverageDimension() {
        return new CoverageDimensionImpl();
    }

    @Override
    public CoverageStoreInfo createCoverageStore() {
        return new CoverageStoreInfoImpl(catalog);
    }

    @Override
    public DataStoreInfo createDataStore() {
        return new DataStoreInfoImpl(catalog);
    }

    @Override
    public WMSStoreInfo createWebMapServer() {
        return new WMSStoreInfoImpl(catalog);
    }

    @Override
    public WMTSStoreInfo createWebMapTileServer() {
        return new WMTSStoreInfoImpl(catalog);
    }

    @Override
    public AttributeTypeInfo createAttribute() {
        return new AttributeTypeInfoImpl();
    }

    @Override
    public FeatureTypeInfo createFeatureType() {
        return new FeatureTypeInfoImpl(catalog);
    }

    @Override
    public WMSLayerInfo createWMSLayer() {
        return new WMSLayerInfoImpl(catalog);
    }

    @Override
    public WMTSLayerInfo createWMTSLayer() {
        return new WMTSLayerInfoImpl(catalog);
    }

    @Override
    public AttributionInfo createAttribution() {
        return new AttributionInfoImpl();
    }

    @Override
    public LayerInfo createLayer() {
        return new LayerInfoImpl();
    }

    @Override
    public MapInfo createMap() {
        return new MapInfoImpl();
    }

    @Override
    public LayerGroupInfo createLayerGroup() {
        return new LayerGroupInfoImpl();
    }

    @Override
    public LegendInfo createLegend() {
        return new LegendInfoImpl();
    }

    @Override
    public MetadataLinkInfo createMetadataLink() {
        return new MetadataLinkInfoImpl();
    }

    @Override
    public DataLinkInfo createDataLink() {
        return new DataLinkInfoImpl();
    }

    @Override
    public NamespaceInfo createNamespace() {
        return new NamespaceInfoImpl();
    }

    @Override
    public WorkspaceInfo createWorkspace() {
        return new WorkspaceInfoImpl();
    }

    @Override
    public StyleInfo createStyle() {
        return new StyleInfoImpl(catalog);
    }

    @Override
    public <T extends Object> T create(Class<T> clazz) {
        return null;
    }
}
