/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.wms;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

/**
 * CatalogListener to ensure GeoGig indexes are created when Time or Elevation dimensions are enabled on GeoGig layers.
 */
public class GeoGigCatalogVisitor implements CatalogVisitor {

    private static final Logger LOGGER = Logging.getLogger(GeoGigCatalogVisitor.class);

    private String[] getAttribute(final MetadataMap metadata, final String attributeName) {
        if (metadata != null && metadata.containsKey(attributeName)) {
            DimensionInfo dimension = metadata.get(attributeName, DimensionInfo.class);
            final String attribute = dimension.getAttribute();
            final String endAttribute = dimension.getEndAttribute();
            return new String[] {attribute, endAttribute};
        }
        return new String[]{};
    }

    @Override
    public void visit(Catalog catalog) {
        // do nothing
    }

    @Override
    public void visit(WorkspaceInfo workspace) {
        // do nothing
    }

    @Override
    public void visit(NamespaceInfo workspace) {
        // do nothing
    }

    @Override
    public void visit(DataStoreInfo dataStore) {
        // do nothing
    }

    @Override
    public void visit(CoverageStoreInfo coverageStore) {
        // do nothing
    }

    @Override
    public void visit(WMSStoreInfo wmsStore) {
        // do nothing
    }

    @Override
    public void visit(FeatureTypeInfo featureType) {
        // do nothing
    }

    @Override
    public void visit(CoverageInfo coverage) {
        // do nothing
    }

    @Override
    public void visit(LayerInfo layer) {
        // get the Resource for this layer
        final ResourceInfo resource = layer.getResource();
        if (resource != null) {
            // get the Store to see if it's a GeoGig store
            final StoreInfo store = resource.getStore();
            if (GeoGigDataStoreFactory.DISPLAY_NAME.equals(store.getType())) {
                // it's a GeoGig dataStore
                try {
                    final GeoGigDataStore geogigDataStore = GeoGigDataStore.class.cast(
                            ((DataStoreInfo) store).getDataStore(null));
                    if (!geogigDataStore.getAutoIndexing()) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine(String.format("GeoGig DataStore is not configured for automatic indexing."));
                        }
                        // skip it
                        return;
                    }
                    // get the metadata for the layer resource
                    final MetadataMap metadata = resource.getMetadata();
                    final String[] timeAttr = getAttribute(metadata, "time");
                    final String[] elevationAttr = getAttribute(metadata, "elevation");
                    String[] dimensions = Stream.concat(Arrays.stream(timeAttr), Arrays.stream(elevationAttr))
                            .toArray(String[]::new);
                    // create the indexes
                    geogigDataStore.createOrUpdateIndex(resource.getNativeName(), dimensions);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
    }

    @Override
    public void visit(StyleInfo style) {
        // do nothing
    }

    @Override
    public void visit(LayerGroupInfo layerGroup) {
        // do nothing
    }

    @Override
    public void visit(WMSLayerInfo wmsLayer) {
        // do nothing
    }

}
