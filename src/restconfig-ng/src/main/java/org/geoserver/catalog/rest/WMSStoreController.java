package org.geoserver.catalog.rest;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelException;

import org.geoserver.catalog.*;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * WMS store controller
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH+"/workspaces/{workspace}/wmsstores")
public class WMSStoreController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(WMSStoreController.class);

    @Autowired
    public WMSStoreController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public RestWrapper<WMSStoreInfo> getWMSStores(
            @PathVariable(name = "workspace") String workspaceName) {
        List<WMSStoreInfo> wmsStores = catalog
                .getStoresByWorkspace(workspaceName, WMSStoreInfo.class);
        return wrapList(wmsStores, WMSStoreInfo.class);
    }

    @GetMapping(path = "{store}", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<WMSStoreInfo> getWMSStore(
            @PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName) {
        WMSStoreInfo wmsStore = getExistingWMSStore(workspaceName, storeName);
        return wrapObject(wmsStore, WMSStoreInfo.class);
    }
    
    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public ResponseEntity<String> postWMSStoreInfo(@RequestBody WMSStoreInfo wmsStore,
            @PathVariable(name = "workspace") String workspaceName,
            UriComponentsBuilder builder) {
        catalog.validate(wmsStore, true).throwIfInvalid();
        catalog.add(wmsStore);

        String storeName = wmsStore.getName();
        LOGGER.info("POST wms store " + storeName);
        UriComponents uriComponents = builder.path("/workspaces/{workspaceName}/wmsstores/{storeName}")
            .buildAndExpand(workspaceName, storeName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<String>(storeName, headers, HttpStatus.CREATED);
    }

    
    @PutMapping(value = "{store}", consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void putWMSStoreInfo(@RequestBody WMSStoreInfo info,
            @PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName) {
        WMSStoreInfo original = getExistingWMSStore(workspaceName, storeName);
        if(info.getWorkspace() != null && !original.getWorkspace().equals(info.getWorkspace())) {
            throw new RestException("Attempting to move "+storeName+" from "+original.getWorkspace().getName()+" to "+info.getWorkspace().getName()+" via PUT", HttpStatus.FORBIDDEN);
        }
        if(!original.getName().equals(info.getName())) {
            throw new RestException("Attempting to rename "+storeName+" to "+info.getName()+" via PUT", HttpStatus.FORBIDDEN);
        }
        new CatalogBuilder(catalog).updateWMSStore(original, info);
        catalog.validate(original, false).throwIfInvalid();
        catalog.save(original);
        clear(original);

        LOGGER.info("PUT wms store " + workspaceName + "," + storeName);
    }

    private WMSStoreInfo getExistingWMSStore(String workspaceName, String storeName) {
        WMSStoreInfo original = catalog.getStoreByName(workspaceName, storeName, WMSStoreInfo.class);
        if(original == null) {
            throw new ResourceNotFoundException(
                    "No such wms store: " + workspaceName + "," + storeName);
        }
        return original;
    }
    
    @DeleteMapping(value = "{store}")
    public void deleteWMSStoreInfo(@PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName,
            @RequestParam(name = "recurse", required = false, defaultValue = "false") boolean recurse,
            @RequestParam(name = "purge", required = false, defaultValue = "none") String deleteType) throws IOException {
        WMSStoreInfo cs = getExistingWMSStore(workspaceName, storeName);
        if (!recurse) {
            if (!catalog.getResourcesByStore(cs, WMSLayerInfo.class).isEmpty()) {
                throw new RestException("wmsstore not empty", HttpStatus.UNAUTHORIZED);
            }
            catalog.remove(cs);
        } else {
            new CascadeDeleteVisitor(catalog).visit(cs);
        }
        clear(cs);

        LOGGER.info("DELETE wms store " + workspaceName + ":s" + workspaceName);
    }
    
    
    void clear(WMSStoreInfo info) {
        catalog.getResourcePool().clear(info);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return WMSStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(new XStreamPersister.Callback() {
            @Override
            protected Class<WMSStoreInfo> getObjectClass() {
                return WMSStoreInfo.class;
            }

            @Override
            protected CatalogInfo getCatalogObject() {
                @SuppressWarnings("unchecked")
                Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                String workspace = uriTemplateVars.get("workspace");
                String store = uriTemplateVars.get("store");

                if(workspace == null || store == null) {
                    return null;
                }

                return catalog.getStoreByName(workspace, store, WMSStoreInfo.class);
            }

            @Override
            protected void postEncodeWMSStore(WMSStoreInfo cs,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                // add a link to the wmslayers
                writer.startNode("wmslayers");
                converter.encodeCollectionLink("wmslayers", writer);
                writer.endNode();
            }

            @Override
            protected void postEncodeReference(Object obj, String ref, String prefix,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if (obj instanceof WorkspaceInfo) {
                    converter.encodeLink("/workspaces/" + converter.encode(ref), writer);
                }
            }
        });
    }
    

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<WMSStoreInfo>(WMSStoreInfo.class) {

            @Override
            protected void wrapInternal(Map properties, SimpleHash model, WMSStoreInfo store) {
                if (properties == null) {
                    try {
                        properties = model.toMap();
                    } catch (TemplateModelException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                List<Map<String, Map<String, String>>> dsProps = new ArrayList<>();

                List<WMSLayerInfo> resources = catalog.getResourcesByStore(store, WMSLayerInfo.class);
                for (WMSLayerInfo resource : resources){
                    Map<String, String> names = new HashMap<>();
                    names.put("name", resource.getName());
                    dsProps.add(Collections.singletonMap("properties", names));
                }
                if (!dsProps.isEmpty())
                    properties.putIfAbsent("wmsLayers", dsProps);

            }

            @Override
            protected void wrapInternal(SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {
                for (Object w : object) {
                    WMSStoreInfo wk = (WMSStoreInfo) w;
                    wrapInternal(null, model, wk);
                }

            }
        };
        
    }
}