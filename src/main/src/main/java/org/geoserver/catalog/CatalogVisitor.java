/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import org.geoserver.catalog.impl.WMSLayerInfoImpl;

/**
 * Visitor for catalog objects.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface CatalogVisitor {
    
    /**
     * Visits the catalog
     */
    void visit( Catalog catalog );
    
    /**
     * Visits a workspace.
     */
    void visit( WorkspaceInfo workspace );
    
    /**
     * Visits a namespace.
     */
    void visit( NamespaceInfo workspace );
    
    /**
     * Visits a data store.
     */
    void visit( DataStoreInfo dataStore ); 

    /**
     * Visits a coverage store.
     */
    void visit( CoverageStoreInfo coverageStore );
    
    /**
     * Visits a WMS data store.
     */
    void visit( WMSStoreInfo wmsStore ); 

    /**
     * Visits a feature type.
     */
    void visit( FeatureTypeInfo featureType );
    
    /**
     * Visits a coverage.
     */
    void visit( CoverageInfo coverage );
    
    /**
     * Visits a layer.
     */
    void visit( LayerInfo layer );
    
    /**
     * Visits a style.
     */
    void visit( StyleInfo style );
    
    /**
     * Visits a layer group..
     */
    void visit( LayerGroupInfo layerGroup );

    /**
     * Visits a WMS layer resource
     * @param wmsLayerInfoImpl
     */
    void visit(WMSLayerInfo wmsLayer);
}
