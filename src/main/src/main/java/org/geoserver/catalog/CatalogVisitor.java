/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Visitor for catalog objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface CatalogVisitor {

    /** Visits the catalog */
    void visit(Catalog catalog);

    /** Visits a workspace. */
    void visit(WorkspaceInfo workspace);

    /** Visits a namespace. */
    void visit(NamespaceInfo workspace);

    /** Visits a data store. */
    void visit(DataStoreInfo dataStore);

    /** Visits a coverage store. */
    void visit(CoverageStoreInfo coverageStore);

    /** Visits a WMS data store. */
    void visit(WMSStoreInfo wmsStore);

    /** Visits a WMTS data store. */
    default void visit(WMTSStoreInfo store) {}

    /** Visits a feature type. */
    void visit(FeatureTypeInfo featureType);

    /** Visits a coverage. */
    void visit(CoverageInfo coverage);

    /** Visits a layer. */
    void visit(LayerInfo layer);

    /** Visits a style. */
    void visit(StyleInfo style);

    /** Visits a layer group.. */
    void visit(LayerGroupInfo layerGroup);

    /** Visits a WMS layer resource */
    void visit(WMSLayerInfo wmsLayer);

    /** Visits a WMTS layer resource */
    default void visit(WMTSLayerInfo layer) {}
}
