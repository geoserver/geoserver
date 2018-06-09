/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.Map;

/**
 * Factory used to create catalog objects.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface CatalogFactory {

    /** Creates a new data store. */
    DataStoreInfo createDataStore();

    /** Creates a new web map server connection */
    WMSStoreInfo createWebMapServer();

    /** Creates a new Web Map Tile Server connection. */
    WMTSStoreInfo createWebMapTileServer();

    /** Creats a new metadata link. */
    MetadataLinkInfo createMetadataLink();

    /** Creates a new data link. */
    DataLinkInfo createDataLink();

    /** Creates a new coverage store. */
    CoverageStoreInfo createCoverageStore();

    /** Creates a new attribute type. */
    AttributeTypeInfo createAttribute();

    /** Creates a new feature type. */
    FeatureTypeInfo createFeatureType();

    /** Creates a new coverage. */
    CoverageInfo createCoverage();

    /** Creates a new WMS layer */
    WMSLayerInfo createWMSLayer();

    /** creates a new WMTS layer */
    WMTSLayerInfo createWMTSLayer();

    /** Creates a new coverage dimension. */
    CoverageDimensionInfo createCoverageDimension();

    /** Creates a new legend. */
    LegendInfo createLegend();

    /** Creates a new attribution record. */
    AttributionInfo createAttribution();

    /** Creates a new layer. */
    LayerInfo createLayer();

    /** Creates a new map. */
    MapInfo createMap();

    /** Creates a new base map. */
    LayerGroupInfo createLayerGroup();

    /** Creates a new style. */
    StyleInfo createStyle();

    /** Creates new namespace. */
    NamespaceInfo createNamespace();

    /** Creates a new workspace. */
    WorkspaceInfo createWorkspace();

    /**
     * Extensible factory method.
     *
     * <p>This method should lookup the appropritae instance of {@link Extension} to create the
     * object. The lookup mechanism is specific to the runtime environement.
     *
     * @param clazz The class of object to create.
     * @return The new object.
     */
    <T extends Object> T create(Class<T> clazz);

    /** Factory extension. */
    interface Extension {

        /**
         * Determines if the extension can create objects of the specified class.
         *
         * @param clazz The class of object to create.
         */
        <T extends Object> boolean canCreate(Class<T> clazz);

        /**
         * Creates an instance of the specified class.
         *
         * <p>This method is only called if {@link #canCreate(Class)} returns <code>true</code>.
         *
         * @param clazz The class of object to create.
         * @param context A context to initialize the object.
         * @return The new object.
         */
        <T extends Object> T create(Class<T> clazz, Map<Object, Object> context);
    }
}
