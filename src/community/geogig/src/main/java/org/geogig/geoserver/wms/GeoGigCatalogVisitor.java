/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.wms;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
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
import org.locationtech.geogig.model.ObjectId;

import com.google.common.base.Optional;

/**
 * CatalogListener to ensure GeoGig indexes are created when Time or Elevation dimensions are enabled on GeoGig layers.
 */
public class GeoGigCatalogVisitor implements CatalogVisitor {

    private static final Logger LOGGER = Logging.getLogger(GeoGigCatalogVisitor.class);

    private static final ExecutorService INDEX_SERVICE = Executors.newSingleThreadExecutor();

    private static final ExecutorService FUTURE_SERVICE = Executors.newFixedThreadPool(4);

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

    private void createOrUpdateIndex(final GeoGigDataStore dataStore, final String layerName,
            final String[] extraAttribues) {
        // schedule the index creation
        final Future<Optional<ObjectId>> indexId = INDEX_SERVICE.submit(
                new Callable<Optional<ObjectId>>() {

            @Override
            public Optional<ObjectId> call() throws Exception {
                return dataStore.createOrUpdateIndex(layerName, extraAttribues);
            }
        });
        // handle the Future and generate an error log if the index create/update fails
        FUTURE_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Optional<ObjectId> objId = indexId.get();
                    if (!objId.isPresent()) {
                        LOGGER.log(Level.WARNING,
                                "Index creation or update finished, but resulted in no Index on Layer: {0}",
                                layerName);
                    }
                } catch (ExecutionException eex) {
                    LOGGER.log(Level.WARNING, "Index could not be created or updated on Layer: " +
                            layerName, eex);
                } catch (InterruptedException iex) {
                    LOGGER.log(Level.WARNING,
                            "Index may not have been created or updated on Layer: " +
                                    layerName, iex);
                }
            }
        });
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
                GeoGigDataStore geogigDataStore = null;
                try {
                    geogigDataStore = GeoGigDataStore.class.cast(
                            ((DataStoreInfo) store).getDataStore(null));
                } catch (Exception ex) {
                    throw new CatalogException("Error accessing GeoGig DataStore", ex);
                }
                if (!geogigDataStore.getAutoIndexing()) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(String.format(
                                "GeoGig DataStore is not configured for automatic indexing."));
                    }
                    // skip it
                    return;
                }
                // get the metadata for the layer resource
                final MetadataMap metadata = resource.getMetadata();
                final String[] timeAttr = getAttribute(metadata, "time");
                final String[] elevationAttr = getAttribute(metadata, "elevation");
                String[] dimensions = Stream.concat(Arrays.stream(timeAttr),
                        Arrays.stream(elevationAttr)).toArray(String[]::new);
                // create the indexes
                createOrUpdateIndex(geogigDataStore, resource.getName(), dimensions);
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
