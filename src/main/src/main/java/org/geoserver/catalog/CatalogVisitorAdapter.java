/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Adapter for CatalogVisitor which stubs all methods allowing subclasses to selectively implement
 * visit methods of relevance.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class CatalogVisitorAdapter implements CatalogVisitor {

    @Override
    public void visit(Catalog catalog) {}

    @Override
    public void visit(WorkspaceInfo workspace) {}

    @Override
    public void visit(NamespaceInfo workspace) {}

    @Override
    public void visit(DataStoreInfo dataStore) {}

    @Override
    public void visit(CoverageStoreInfo coverageStore) {}

    @Override
    public void visit(WMSStoreInfo wmsStore) {}

    @Override
    public void visit(WMTSStoreInfo wmsStore) {}

    @Override
    public void visit(FeatureTypeInfo featureType) {}

    @Override
    public void visit(CoverageInfo coverage) {}

    @Override
    public void visit(LayerInfo layer) {}

    @Override
    public void visit(LayerGroupInfo layerGroup) {}

    @Override
    public void visit(StyleInfo style) {}

    @Override
    public void visit(WMSLayerInfo wmsLayerInfoImpl) {}

    @Override
    public void visit(WMTSLayerInfo wmsLayerInfoImpl) {}
}
