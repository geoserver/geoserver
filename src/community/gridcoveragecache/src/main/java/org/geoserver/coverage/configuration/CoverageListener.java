/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.configuration;

import java.util.Arrays;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.coverage.CachingGridCoverageReaderCallback;
import org.geoserver.gwc.GWC;

public class CoverageListener implements CatalogListener {

    private final GWC mediator;

    private final Catalog catalog;

    public CoverageListener(final GWC mediator, final Catalog catalog) {
        this.mediator = mediator;
        this.catalog = catalog;
    }
    
    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        CatalogInfo obj = event.getSource();
        if (!(obj instanceof LayerInfo)) {
            return;
        }
        
        // Get the associated resource
        ResourceInfo resource = ((LayerInfo)obj).getResource();
        
        if(!(resource instanceof CoverageInfo)){
            return;
        }
        
        String prefixedName = resource.prefixedName();
        
        // Add the suffix to the prefixedName
        prefixedName = prefixedName + CoverageConfiguration.COVERAGE_LAYER_SUFFIX;
        
        //TileLayer tileLayerByName = mediator.getTileLayerByName(prefixedName);
        boolean tileLayerExists = mediator.tileLayerExists(prefixedName);
        if (!tileLayerExists) {
            return;
        } else {
            // notify the layer has been removed
            mediator.removeTileLayers(Arrays.asList(prefixedName));
            
            // Remove the CoverageTileLayerInfo from the MetadataMap
            CoverageInfo res = (CoverageInfo)resource;
            MetadataMap metadata = res.getMetadata();
            catalog.getResourcePool().clear(res.getStore());
            if (metadata != null && metadata.containsKey(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY)){
                metadata.remove(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY);
                // Avoid Proxy issues
                res = catalog.getCoverage(res.getId());
                catalog.save(res);
            }
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
    }

    @Override
    public void reloaded() {
    }
}
