/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections.PredicateUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example style resource controller
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH+"/workspaces/{workspaceName}")
public class WMSLayerController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(WMSLayerController.class);

    @Autowired
    public WMSLayerController(Catalog catalog) {
        super(catalog);
    }
    
    @GetMapping(value = {"/wmslayers", "/wmsstores/{storeName}/wmslayers"},
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE})
    public Object getLayers(
            final @PathVariable String workspaceName, 
            final @PathVariable(required = false) String storeName,
            final @RequestParam(required = false, defaultValue = "false") boolean quietOnNotFound,
            final @RequestParam(required = false, defaultValue = "configured") String list) {
        switch(list) {
        case "available":
            LOGGER.fine(()->logMessage("GET available WMS layers from ", workspaceName, storeName, null));
            return new AvailableResources(getAvailableLayersInternal(workspaceName, storeName, quietOnNotFound), "wmsLayerName");
        case "configured":
            LOGGER.fine(()->logMessage("GET configured WMS layers from ", workspaceName, storeName, null));
            
            return wrapList(getConfiguredLayersInternal(workspaceName, storeName, quietOnNotFound), WMSLayerInfo.class);
        default:
            throw new RestException("Unknown list type "+list, HttpStatus.NOT_IMPLEMENTED);
        }
    }
    
    Collection<WMSStoreInfo> getStoresInternal(NamespaceInfo ns, String storeName, boolean quietOnNotFound) {
        if(Objects.nonNull(storeName)) {
            return Collections.singleton(getStoreInternal(ns, storeName));
        } else {
            return catalog.getStoresByWorkspace(ns.getPrefix(), WMSStoreInfo.class);
        }
    }
    
    List<String> getAvailableLayersInternal(String workspaceName, String storeName, boolean quietOnNotFound) {
        NamespaceInfo ns = getNamespaceInternal(workspaceName);
        Collection<WMSStoreInfo> stores = getStoresInternal(ns, storeName, quietOnNotFound);
        return stores.stream()
            .flatMap(store->{
                WebMapServer ds;
                try {
                    ds = store.getWebMapServer(null);
                } catch (IOException e) {
                    throw new RestException( "Could not load wms store: " + storeName, HttpStatus.INTERNAL_SERVER_ERROR, e );
                }
                final List<Layer> layerList = ds.getCapabilities().getLayerList();
                return layerList.stream()
                    .map(Layer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isEmpty())
                    .filter(name -> !layerConfigured(store, name));
            })
            .collect(Collectors.toList());
    }
    
    boolean layerConfigured(final WMSStoreInfo store, final String nativeName) {
        final Filter filter = Predicates.and(Predicates.equal("store.name", store.getName()),Predicates.equal("nativeName", nativeName));
        try(CloseableIterator<WMSLayerInfo> it = catalog.list(WMSLayerInfo.class, filter, 0, 1, null)){
            return it.hasNext();
        }
    }
    
    List<WMSLayerInfo> getConfiguredLayersInternal(String workspaceName, String storeName, boolean quietOnNotFound) {
        NamespaceInfo ns = getNamespaceInternal(workspaceName);
        Collection<WMSStoreInfo> stores = getStoresInternal(ns, storeName, quietOnNotFound);
        return stores.stream()
            .flatMap(store->catalog.getResourcesByStore(store, WMSLayerInfo.class).stream())
            .collect(Collectors.toList());
    }
    
    @GetMapping(value = {"/wmslayers/{layerName}", "/wmsstores/{storeName}/wmslayers/{layerName}"},
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE})
    public RestWrapper<WMSLayerInfo> getLayer(
            final @PathVariable String workspaceName, 
            final @PathVariable(required=false) String storeName, 
            final @PathVariable String layerName) {
        LOGGER.fine(()->logMessage("GET", workspaceName, storeName, layerName));
        
        WMSLayerInfo layer = getResourceInternal(workspaceName, storeName, layerName);
        
        return wrapObject(layer, WMSLayerInfo.class);
    }
    
    protected NamespaceInfo getNamespaceInternal(String workspaceName) {
        if(Objects.isNull(workspaceName)) {
            throw new NullPointerException();
        } else {
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if(Objects.isNull(ns)) {
                throw new ResourceNotFoundException("Could not find workspace "+workspaceName);
            } else {
                return ns;
            }
        }
    }
    protected WMSStoreInfo getStoreInternal(NamespaceInfo ns, String storeName) {
        if(Objects.isNull(storeName)) {
            throw new NullPointerException();
        } else {
            WMSStoreInfo store = catalog.getStoreByName(ns.getPrefix(), storeName, WMSStoreInfo.class);
            if(Objects.isNull(ns)) {
                throw new ResourceNotFoundException("Could not find WMSStore "+storeName + " in workspace "+ns.getPrefix());
            } else {
                return store;
            }
        }
    }
    protected WMSLayerInfo getResourceInternal(final String workspaceName, @Nullable final String storeName, final String layerName) {
        final NamespaceInfo ns = getNamespaceInternal(workspaceName);
        final WMSLayerInfo layer;
        if(Objects.isNull(layerName)) {
            throw new NullPointerException();
        } else if (Objects.isNull(storeName)) {
            layer = catalog.getResourceByName(ns, layerName, WMSLayerInfo.class);
            if(Objects.isNull(layer)) {
                throw new ResourceNotFoundException("No such cascaded wms: "+workspaceName+","+layerName);
            } else {
                return layer;
            }
        } else {
            WMSStoreInfo store = getStoreInternal(ns, storeName);
            layer = catalog.getResourceByStore(store, layerName, WMSLayerInfo.class);
            if(Objects.isNull(layer)) {
                throw new ResourceNotFoundException("No such cascaded wms: "+workspaceName+","+layerName);
            } else {
                return layer;
            }
        }
    }
    
    @PutMapping(value = {"/wmslayers/{layerName}", "/wmsstores/{storeName}/wmslayers/{layerName}"},
            consumes = {MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
                    MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
        public void putLayerFromStore(
                @RequestBody WMSLayerInfo update,
                final @PathVariable String workspaceName, 
                final @PathVariable(required=false) String storeName, 
                final @PathVariable String layerName, 
                final @RequestParam(name = "calculate", required = false) String calculate) {
            LOGGER.fine(()->logMessage("PUT", workspaceName, storeName, layerName));
            
            WMSLayerInfo original = getResourceInternal(workspaceName, storeName, layerName);
            calculateOptionalFields(update, original, calculate);
            new CatalogBuilder(catalog).updateWMSLayer(original, update);
            catalog.validate(original, false).throwIfInvalid();
            catalog.getResourcePool().clear(original.getStore());
            catalog.save(original);

        }

    String logMessage(final String message, final String workspaceName, @Nullable final String storeName, @Nullable final String layerName) {
        return message+(Objects.isNull(layerName)?"":(" WMS Layer "+layerName+" in"))+(Objects.isNull(storeName)?"":(" store "+storeName+" in"))+" in workspace "+ workspaceName;
    }
}
