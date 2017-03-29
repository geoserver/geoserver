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

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
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
            return getAvailableLayersInternal(workspaceName, storeName, quietOnNotFound);
        case "configured":
            LOGGER.fine(()->logMessage("GET configured WMS layers from ", workspaceName, storeName, null));
            
            return wrapList(getConfiguredLayersInternal(workspaceName, storeName, quietOnNotFound), WMSLayerInfo.class);
        default:
            throw new RestException("Unknown list type "+list, HttpStatus.NOT_IMPLEMENTED);
        }
    }
    
    Collection<WMSStoreInfo> getStoresInternal(NamespaceInfo ns, String storeName, boolean quietOnNotFound) {
        if(Objects.nonNull(storeName)) {
            return Collections.singleton(getStoreInternal(ns, storeName, quietOnNotFound));
        } else {
            return catalog.getStoresByWorkspace(ns.getPrefix(), WMSStoreInfo.class);
        }
    }
    
    List<AvailableResource<WMSLayerInfo>> getAvailableLayersInternal(String workspaceName, String storeName, boolean quietOnNotFound) {
        NamespaceInfo ns = getNamespaceInternal(workspaceName, quietOnNotFound);
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
                    .filter(name -> Objects.isNull(catalog.getResourceByStore(store, name, WMSLayerInfo.class)))
                    .map(AvailableResource<WMSLayerInfo>::new);
            })
            .collect(Collectors.toList());
    }
    
    List<WMSLayerInfo> getConfiguredLayersInternal(String workspaceName, String storeName, boolean quietOnNotFound) {
        NamespaceInfo ns = getNamespaceInternal(workspaceName, quietOnNotFound);
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
            final @PathVariable String layerName, 
            final @RequestParam(name = "quietOnNotFound", required = false, defaultValue = "false") boolean quietOnNotFound) {
        LOGGER.fine(()->logMessage("GET", workspaceName, storeName, layerName));
        
        WMSLayerInfo layer = getResourceInternal(workspaceName, storeName, layerName, quietOnNotFound);
        
        return wrapObject(layer, WMSLayerInfo.class);
    }
    
    protected ResourceNotFoundException notFound(String message, boolean quietOnNotFound) {
        return new ResourceNotFoundException(quietOnNotFound?"":message);
    }
    
    protected NamespaceInfo getNamespaceInternal(String workspaceName, boolean quietOnNotFound) {
        if(Objects.isNull(workspaceName)) {
            throw new NullPointerException();
        } else {
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if(Objects.isNull(ns)) {
                throw notFound("Could not find workspace "+workspaceName, quietOnNotFound);
            } else {
                return ns;
            }
        }
    }
    protected WMSStoreInfo getStoreInternal(NamespaceInfo ns, String storeName, boolean quietOnNotFound) {
        if(Objects.isNull(storeName)) {
            throw new NullPointerException();
        } else {
            WMSStoreInfo store = catalog.getStoreByName(ns.getPrefix(), storeName, WMSStoreInfo.class);
            if(Objects.isNull(ns)) {
                throw notFound("Could not find WMSStore "+storeName + " in workspace "+ns.getPrefix(), quietOnNotFound);
            } else {
                return store;
            }
        }
    }
    protected WMSLayerInfo getResourceInternal(final String workspaceName, @Nullable final String storeName, final String layerName, boolean quietOnNotFound) {
        final NamespaceInfo ns = getNamespaceInternal(workspaceName, quietOnNotFound);
        final WMSLayerInfo layer;
        if(Objects.isNull(layerName)) {
            throw new NullPointerException();
        } else if (Objects.isNull(storeName)) {
            layer = catalog.getResourceByName(ns, layerName, WMSLayerInfo.class);
            if(Objects.isNull(layer)) {
                throw notFound("No such cascaded wms: "+workspaceName+","+layerName, quietOnNotFound);
            } else {
                return layer;
            }
        } else {
            WMSStoreInfo store = getStoreInternal(ns, storeName, quietOnNotFound);
            layer = catalog.getResourceByStore(store, layerName, WMSLayerInfo.class);
            if(Objects.isNull(layer)) {
                throw notFound("Could not find WMSLayer "+layerName+" in store "+storeName + " in workspace "+workspaceName, quietOnNotFound);
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
                final @RequestParam(name = "quietOnNotFound", required = false, defaultValue = "false") boolean quietOnNotFound) {
            LOGGER.fine(()->logMessage("PUT", workspaceName, storeName, layerName));
            
            WMSLayerInfo original = getResourceInternal(workspaceName, storeName, layerName, quietOnNotFound);
            
            new CatalogBuilder(catalog).updateWMSLayer(original, update);
            catalog.validate(original, false).throwIfInvalid();
            catalog.save(original);

        }

    String logMessage(final String message, final String workspaceName, @Nullable final String storeName, @Nullable final String layerName) {
        return message+(Objects.isNull(layerName)?"":(" WMS Layer "+layerName+" in"))+(Objects.isNull(storeName)?"":(" store "+storeName+" in"))+" in workspace "+ workspaceName;
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        super.configurePersister(persister, converter);
        
        persister.getXStream().alias("wmsLayerName", AvailableResource.class);
    }
}
