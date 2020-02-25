/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for WMTS resources.
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
@RestController
@ControllerAdvice
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/wmtslayers",
        RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/wmtsstores/{storeName}/layers"
    }
)
public class WMTSLayerController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(WMTSLayerController.class);

    @Autowired
    public WMTSLayerController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public Object layersGet(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @RequestParam(required = false, defaultValue = "false") boolean quietOnNotFound,
            @RequestParam(required = false, defaultValue = "configured") String list) {
        switch (list) {
            case "available":
                LOGGER.fine(
                        () ->
                                logMessage(
                                        "GET available WMTS layers from ",
                                        workspaceName,
                                        storeName,
                                        null));
                return new AvailableResources(
                        getAvailableLayersInternal(workspaceName, storeName, quietOnNotFound),
                        "wmtsLayerName");
            case "configured":
                LOGGER.fine(
                        () ->
                                logMessage(
                                        "GET configured WMTS layers from ",
                                        workspaceName,
                                        storeName,
                                        null));

                return wrapList(
                        getConfiguredLayersInternal(workspaceName, storeName, quietOnNotFound),
                        WMTSLayerInfo.class);
            default:
                throw new RestException("Unknown list type " + list, HttpStatus.NOT_IMPLEMENTED);
        }
    }

    Collection<WMTSStoreInfo> getStoresInternal(
            NamespaceInfo ns, String storeName, boolean quietOnNotFound) {
        if (Objects.nonNull(storeName)) {
            return Collections.singleton(getStoreInternal(ns, storeName));
        } else {
            return catalog.getStoresByWorkspace(ns.getPrefix(), WMTSStoreInfo.class);
        }
    }

    List<String> getAvailableLayersInternal(
            String workspaceName, String storeName, boolean quietOnNotFound) {
        NamespaceInfo ns = getNamespaceInternal(workspaceName);
        Collection<WMTSStoreInfo> stores = getStoresInternal(ns, storeName, quietOnNotFound);
        return stores.stream()
                .flatMap(
                        store -> {
                            WebMapTileServer ds;
                            try {
                                ds = store.getWebMapTileServer(null);
                            } catch (IOException e) {
                                throw new RestException(
                                        "Could not load wmts store: " + storeName,
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        e);
                            }
                            final List<WMTSLayer> layerList = ds.getCapabilities().getLayerList();
                            return layerList
                                    .stream()
                                    .map(Layer::getName)
                                    .filter(Objects::nonNull)
                                    .filter(name -> !name.isEmpty())
                                    .filter(name -> !layerConfigured(store, name));
                        })
                .collect(Collectors.toList());
    }

    boolean layerConfigured(final WMTSStoreInfo store, final String nativeName) {
        final Filter filter =
                Predicates.and(
                        Predicates.equal("store.name", store.getName()),
                        Predicates.equal("nativeName", nativeName));
        try (CloseableIterator<WMTSLayerInfo> it =
                catalog.list(WMTSLayerInfo.class, filter, 0, 1, null)) {
            return it.hasNext();
        }
    }

    List<WMTSLayerInfo> getConfiguredLayersInternal(
            String workspaceName, String storeName, boolean quietOnNotFound) {
        NamespaceInfo ns = getNamespaceInternal(workspaceName);
        Collection<WMTSStoreInfo> stores = getStoresInternal(ns, storeName, quietOnNotFound);
        return stores.stream()
                .flatMap(store -> catalog.getResourcesByStore(store, WMTSLayerInfo.class).stream())
                .collect(Collectors.toList());
    }

    @GetMapping(
        value = "/{layerName}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<WMTSLayerInfo> layerGet(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable String layerName) {

        LOGGER.fine(() -> logMessage("GET", workspaceName, storeName, layerName));

        WMTSLayerInfo layer = getResourceInternal(workspaceName, storeName, layerName);

        return wrapObject(layer, WMTSLayerInfo.class);
    }

    protected NamespaceInfo getNamespaceInternal(String workspaceName) {
        if (Objects.isNull(workspaceName)) {
            throw new NullPointerException();
        } else {
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if (Objects.isNull(ns)) {
                throw new ResourceNotFoundException("Could not find workspace " + workspaceName);
            } else {
                return ns;
            }
        }
    }

    protected WMTSStoreInfo getStoreInternal(NamespaceInfo ns, String storeName) {
        if (Objects.isNull(storeName)) {
            throw new NullPointerException();
        } else {
            return catalog.getStoreByName(ns.getPrefix(), storeName, WMTSStoreInfo.class);
        }
    }

    protected WMTSLayerInfo getResourceInternal(
            final String workspaceName, @Nullable final String storeName, final String layerName) {
        final NamespaceInfo ns = getNamespaceInternal(workspaceName);
        final WMTSLayerInfo layer;
        if (Objects.isNull(layerName)) {
            throw new NullPointerException();
        } else if (Objects.isNull(storeName)) {
            layer = catalog.getResourceByName(ns, layerName, WMTSLayerInfo.class);
            if (Objects.isNull(layer)) {
                throw new ResourceNotFoundException(
                        "No such cascaded wmts: " + workspaceName + "," + layerName);
            } else {
                return layer;
            }
        } else {
            WMTSStoreInfo store = getStoreInternal(ns, storeName);
            layer = catalog.getResourceByStore(store, layerName, WMTSLayerInfo.class);
            if (Objects.isNull(layer)) {
                throw new ResourceNotFoundException(
                        "No such cascaded wmts: " + workspaceName + "," + layerName);
            } else {
                return layer;
            }
        }
    }

    @PutMapping(
        value = "/{layerName}",
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void layerPut(
            @RequestBody WMTSLayerInfo update,
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable String layerName,
            @RequestParam(name = "calculate", required = false) String calculate) {

        LOGGER.fine(() -> logMessage("PUT", workspaceName, storeName, layerName));

        WMTSLayerInfo original = getResourceInternal(workspaceName, storeName, layerName);
        calculateOptionalFields(update, original, calculate);
        new CatalogBuilder(catalog).updateWMTSLayer(original, update);
        catalog.validate(original, false).throwIfInvalid();
        catalog.getResourcePool().clear(original.getStore());
        catalog.save(original);
    }

    @DeleteMapping(value = "/{layerName}")
    public void layerDelete(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable String layerName,
            @RequestParam(name = "recurse", defaultValue = "false") boolean recurse) {

        LOGGER.fine(() -> logMessage("DELETE", workspaceName, storeName, layerName));

        WMTSLayerInfo resource = this.getResourceInternal(workspaceName, storeName, layerName);

        List<LayerInfo> layers = catalog.getLayers(resource);

        if (recurse) {
            // by recurse we clear out all the layers that public this resource
            for (LayerInfo l : layers) {
                catalog.remove(l);
                LOGGER.info("DELETE WMTS layer " + l.getName());
            }
        } else {
            if (!layers.isEmpty()) {
                throw new RestException("wmts layer referenced by layer(s)", HttpStatus.FORBIDDEN);
            }
        }

        catalog.remove(resource);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> layerPost(
            @RequestBody WMTSLayerInfo resource,
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            UriComponentsBuilder builder)
            throws Exception {

        String resourceName = handleObjectPost(resource, workspaceName, storeName);
        LOGGER.fine(() -> logMessage("POST", workspaceName, storeName, resourceName));
        UriComponents uriComponents =
                Objects.isNull(storeName)
                        ? builder.path("/workspaces/{workspaceName}/wmtslayers/{wmtslayer}")
                                .buildAndExpand(workspaceName, resourceName)
                        : builder.path(
                                        "/workspaces/{workspaceName}/wmtsstores/{storeName}/layers/{wmtslayer}")
                                .buildAndExpand(workspaceName, storeName, resourceName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(resourceName, headers, HttpStatus.CREATED);
    }

    String logMessage(
            final String message,
            final String workspaceName,
            @Nullable final String storeName,
            @Nullable final String layerName) {
        return message
                + (Objects.isNull(layerName) ? "" : (" WMTS Layer " + layerName + " in"))
                + (Objects.isNull(storeName) ? "" : (" store " + storeName + " in"))
                + " in workspace "
                + workspaceName;
    }

    private String handleObjectPost(WMTSLayerInfo resource, String workspaceName, String storeName)
            throws Exception {
        NamespaceInfo ns = getNamespaceInternal(workspaceName);
        WMTSStoreInfo store;

        if (resource.getStore() != null) {
            if (Objects.nonNull(storeName)
                    && !Objects.equals(storeName, resource.getStore().getName())) {
                throw new RestException(
                        "Expected wmts store "
                                + storeName
                                + " but client specified "
                                + resource.getStore().getName(),
                        HttpStatus.FORBIDDEN);
            }
            store = resource.getStore();
        } else {
            store = getStoreInternal(ns, storeName);
            resource.setStore(store);
        }

        // ensure workspace/namespace matches up
        if (resource.getNamespace() != null) {
            if (!workspaceName.equals(resource.getNamespace().getPrefix())) {
                throw new RestException(
                        "Expected workspace "
                                + workspaceName
                                + " but client specified "
                                + resource.getNamespace().getPrefix(),
                        HttpStatus.FORBIDDEN);
            }
        } else {
            resource.setNamespace(catalog.getNamespaceByPrefix(workspaceName));
        }
        resource.setEnabled(true);

        NamespaceInfo foundns = resource.getNamespace();
        if (foundns != null && !foundns.getPrefix().equals(workspaceName)) {
            LOGGER.warning(
                    "Namespace: "
                            + ns.getPrefix()
                            + " does not match workspace: "
                            + workspaceName
                            + ", overriding.");
            foundns = null;
        }

        if (foundns == null) {
            // infer from workspace
            resource.setNamespace(ns);
        }

        // fill in missing information
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(store);
        cb.initWMTSLayer(resource);

        resource.setEnabled(true);
        catalog.validate(resource, true).throwIfInvalid();
        catalog.add(resource);

        // create a layer for the feature type
        catalog.add(new CatalogBuilder(catalog).buildLayer(resource));

        return resource.getName();
    }

    // Works with the callback bellow to fix the enabled property
    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return WMTSLayerInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<WMTSLayerInfo> getObjectClass() {
                        return WMTSLayerInfo.class;
                    }

                    // Tries to get the object so XStream can unmarshall over top of it.
                    // A hack to avoid overwriting the enabled property on a PUT
                    @Override
                    protected CatalogInfo getCatalogObject() {
                        @SuppressWarnings("unchecked")
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspaceName = uriTemplateVars.get("workspaceName");
                        String storeName = uriTemplateVars.get("storeName");
                        String layerName = uriTemplateVars.get("layerName");

                        if (workspaceName == null || storeName == null || layerName == null) {
                            return null;
                        }
                        WMTSStoreInfo store =
                                catalog.getStoreByName(
                                        workspaceName, storeName, WMTSStoreInfo.class);
                        if (store == null) {
                            return null;
                        }
                        return catalog.getResourceByStore(store, layerName, WMTSLayerInfo.class);
                    }

                    @Override
                    protected void postEncodeReference(
                            Object obj,
                            String ref,
                            String prefix,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        if (obj instanceof NamespaceInfo) {
                            NamespaceInfo ns = (NamespaceInfo) obj;
                            converter.encodeLink(
                                    "/namespaces/" + converter.encode(ns.getPrefix()), writer);
                        }
                        if (obj instanceof WMTSStoreInfo) {
                            WMTSStoreInfo store = (WMTSStoreInfo) obj;
                            converter.encodeLink(
                                    "/workspaces/"
                                            + converter.encode(store.getWorkspace().getName())
                                            + "/wmtsstores/"
                                            + converter.encode(store.getName()),
                                    writer);
                        }
                    }
                });
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<WMTSLayerInfo>(WMTSLayerInfo.class) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            protected void wrapInternal(Map properties, SimpleHash model, WMTSLayerInfo object) {
                try {
                    properties.put("boundingBox", object.boundingBox());
                    properties.put("capabilitiesURL", object.getStore().getCapabilitiesURL());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
