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

    public CoverageInfo createCoverage() {
        return new CoverageInfoImpl(catalog);
    }

    public CoverageDimensionInfo createCoverageDimension() {
        return new CoverageDimensionImpl();
    }

    public CoverageStoreInfo createCoverageStore() {
        return new CoverageStoreInfoImpl(catalog);
    }

    public DataStoreInfo createDataStore() {
        return new DataStoreInfoImpl(catalog);
    }

    public WMSStoreInfo createWebMapServer() {
        return new WMSStoreInfoImpl(catalog);
    }

    @Override
    public WMTSStoreInfo createWebMapTileServer() {
        return (WMTSStoreInfo) new WMTSStoreInfoImpl(catalog);
    }

    public AttributeTypeInfo createAttribute() {
        return new AttributeTypeInfoImpl();
    }

    public FeatureTypeInfo createFeatureType() {
        return new FeatureTypeInfoImpl(catalog);
    }

    public WMSLayerInfo createWMSLayer() {
        return new WMSLayerInfoImpl(catalog);
    }

    @Override
    public WMTSLayerInfo createWMTSLayer() {
        return new WMTSLayerInfoImpl(catalog);
    }

    public AttributionInfo createAttribution() {
        return new AttributionInfoImpl();
    }

    public LayerInfo createLayer() {
        return new LayerInfoImpl();
    }

    public MapInfo createMap() {
        return new MapInfoImpl();
    }

    public LayerGroupInfo createLayerGroup() {
        return new LayerGroupInfoImpl();
    }

    public LegendInfo createLegend() {
        return new LegendInfoImpl();
    }

    public MetadataLinkInfo createMetadataLink() {
        return new MetadataLinkInfoImpl();
    }

    public DataLinkInfo createDataLink() {
        return new DataLinkInfoImpl();
    }

    public NamespaceInfo createNamespace() {
        return new NamespaceInfoImpl();
    }

    public WorkspaceInfo createWorkspace() {
        return new WorkspaceInfoImpl();
    }

    public StyleInfo createStyle() {
        return new StyleInfoImpl(catalog);
    }

    public Object create(Class clazz) {
        return null;
    }
}
