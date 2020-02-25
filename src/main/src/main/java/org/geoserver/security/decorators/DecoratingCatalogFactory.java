/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
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
import org.geotools.util.decorate.AbstractDecorator;

/**
 * Delegates all methods to the provided delegate. Suclasses will override methods in order to
 * perform their decoration work
 *
 * @author Niels Charlier
 */
public class DecoratingCatalogFactory extends AbstractDecorator<CatalogFactory>
        implements CatalogFactory {

    public DecoratingCatalogFactory(CatalogFactory delegate) {
        super(delegate);
    }

    @Override
    public DataStoreInfo createDataStore() {
        return delegate.createDataStore();
    }

    @Override
    public WMSStoreInfo createWebMapServer() {
        return delegate.createWebMapServer();
    }

    @Override
    public WMTSStoreInfo createWebMapTileServer() {
        return delegate.createWebMapTileServer();
    }

    @Override
    public MetadataLinkInfo createMetadataLink() {
        return delegate.createMetadataLink();
    }

    @Override
    public DataLinkInfo createDataLink() {
        return delegate.createDataLink();
    }

    @Override
    public CoverageStoreInfo createCoverageStore() {
        return delegate.createCoverageStore();
    }

    @Override
    public AttributeTypeInfo createAttribute() {
        return delegate.createAttribute();
    }

    @Override
    public FeatureTypeInfo createFeatureType() {
        return delegate.createFeatureType();
    }

    @Override
    public CoverageInfo createCoverage() {
        return delegate.createCoverage();
    }

    @Override
    public WMSLayerInfo createWMSLayer() {
        return delegate.createWMSLayer();
    }

    @Override
    public WMTSLayerInfo createWMTSLayer() {
        return delegate.createWMTSLayer();
    }

    @Override
    public CoverageDimensionInfo createCoverageDimension() {
        return delegate.createCoverageDimension();
    }

    @Override
    public LegendInfo createLegend() {
        return delegate.createLegend();
    }

    @Override
    public AttributionInfo createAttribution() {
        return delegate.createAttribution();
    }

    @Override
    public LayerInfo createLayer() {
        return delegate.createLayer();
    }

    @Override
    public MapInfo createMap() {
        return delegate.createMap();
    }

    @Override
    public LayerGroupInfo createLayerGroup() {
        return delegate.createLayerGroup();
    }

    @Override
    public StyleInfo createStyle() {
        return delegate.createStyle();
    }

    @Override
    public NamespaceInfo createNamespace() {
        return delegate.createNamespace();
    }

    @Override
    public WorkspaceInfo createWorkspace() {
        return delegate.createWorkspace();
    }

    @Override
    public <T> T create(Class<T> clazz) {
        return delegate.create(clazz);
    }
}
