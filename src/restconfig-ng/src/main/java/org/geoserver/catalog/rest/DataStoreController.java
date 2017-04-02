/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import org.geoserver.catalog.*;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by vickdw on 3/27/17.
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH+"/workspaces/{workspace}/datastores")
public class DataStoreController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(DataStoreController.class);

    @Autowired
    public DataStoreController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    /*
    Get Mappings
     */

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public RestWrapper<DataStoreInfo> getDataStores(
            @PathVariable(name = "workspace") String workspaceName) {
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if(ws == null) {
            throw new ResourceNotFoundException("No such workspace : " + workspaceName);
        }
        List<DataStoreInfo> dataStores = catalog
                .getDataStoresByWorkspace(ws);
        return wrapList(dataStores, DataStoreInfo.class);
    }

    @GetMapping(path = "{store}", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<DataStoreInfo> getCoverageStore(
            @PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName) {
        DataStoreInfo dataStore = getExistingDataStore(workspaceName, storeName);
        return wrapObject(dataStore, DataStoreInfo.class);
    }

    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public ResponseEntity<String> postDataStoreInfo(@RequestBody DataStoreInfo dataStore,
                                                        @PathVariable(name = "workspace") String workspaceName,
                                                        UriComponentsBuilder builder) {
        catalog.validate(dataStore, true).throwIfInvalid();
        catalog.add(dataStore);

        String storeName = dataStore.getName();
        LOGGER.info("POST data store " + storeName);
        UriComponents uriComponents = builder.path("/workspaces/{workspaceName}/datastores/{storeName}")
                .buildAndExpand(workspaceName, storeName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<String>(storeName, headers, HttpStatus.CREATED);
    }

    @PutMapping(value = "{store}", consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void putCoverageStoreInfo(@RequestBody DataStoreInfo info,
                                     @PathVariable(name = "workspace") String workspaceName,
                                     @PathVariable(name = "store") String storeName) {
        DataStoreInfo original = getExistingDataStore(workspaceName, storeName);

        if (!original.getName().equalsIgnoreCase(info.getName())) {
            throw new RestException("can not change name of a datastore", HttpStatus.FORBIDDEN);
        }

        if (!original.getWorkspace().getName().equalsIgnoreCase(info.getWorkspace().getName())) {
            throw new RestException("can not change name of a workspace", HttpStatus.FORBIDDEN);
        }

        new CatalogBuilder(catalog).updateDataStore(original, info);
        catalog.validate(original, false).throwIfInvalid();
        catalog.save(original);
        clear(original);

        LOGGER.info("PUT datastore " + workspaceName + "," + storeName);
    }

    @DeleteMapping(value = "{store}")
    public void deleteDataStoreInfo(@PathVariable(name = "workspace") String workspaceName,
                                        @PathVariable(name = "store") String storeName,
                                        @RequestParam(name = "recurse", required = false, defaultValue = "false") boolean recurse,
                                        @RequestParam(name = "purge", required = false, defaultValue = "none") String deleteType) throws IOException {
        DataStoreInfo ds = getExistingDataStore(workspaceName, storeName);
        if (!recurse) {
            if (!catalog.getStoresByWorkspace(workspaceName, DataStoreInfo.class).isEmpty()) {
                for (DataStoreInfo dataStoreInfo : catalog.getStoresByWorkspace(workspaceName, DataStoreInfo.class)) {
                    if (dataStoreInfo.getName().equalsIgnoreCase(storeName)){
                        break;
                    }
                    throw new RestException("datastore not empty", HttpStatus.FORBIDDEN);
                }
            }
            catalog.remove(ds);
        } else {
            new CascadeDeleteVisitor(catalog).visit(ds);
        }
        catalog.remove(ds);
        clear(ds);

        LOGGER.info("DELETE datastore " + workspaceName + ":s" + workspaceName);
    }

    private DataStoreInfo getExistingDataStore(String workspaceName, String storeName) {
        DataStoreInfo original = catalog.getDataStoreByName(workspaceName, storeName);
        if(original == null) {
            throw new ResourceNotFoundException(
                    "No such datastore: " + workspaceName + "," + storeName);
        }
        return original;
    }

    void clear(DataStoreInfo info) {
        catalog.getResourcePool().clear(info);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return DataStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(new XStreamPersister.Callback() {
            @Override
            protected Class<DataStoreInfo> getObjectClass() {
                return DataStoreInfo.class;
            }

            @Override
            protected CatalogInfo getCatalogObject() {
                Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                String workspace = uriTemplateVars.get("workspace");
                String datastore = uriTemplateVars.get("store");

                if (workspace == null || datastore == null) {
                    return null;
                }
                return catalog.getDataStoreByName(workspace, datastore);
            }

            @Override
            protected void postEncodeDataStore(DataStoreInfo ds,
                                                   HierarchicalStreamWriter writer, MarshallingContext context) {
                // add a link to the featuretypes
                writer.startNode("featureTypes");
                converter.encodeCollectionLink("featuretypes", writer);
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
    protected String getTemplateName(Object object) {
        if (object instanceof DataStoreInfo) {
            return "DataStoreInfo";
        }
        return null;
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<DataStoreInfo>(DataStoreInfo.class) {

            @Override
            protected void wrapInternal(Map properties, SimpleHash model, DataStoreInfo dataStoreInfo) {
                if (properties == null) {
                    try {
                        properties = model.toMap();
                    } catch (TemplateModelException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                List<Map<String, Map<String, String>>> dsProps = new ArrayList<>();

                List<FeatureTypeInfo> featureTypes = catalog.getFeatureTypesByDataStore(dataStoreInfo);
                for (FeatureTypeInfo ft : featureTypes){
                    Map<String, String> names = new HashMap<>();
                    names.put("name", ft.getName());
                    dsProps.add(Collections.singletonMap("properties", names));
                }
                if (!dsProps.isEmpty())
                    properties.putIfAbsent("featureTypes", dsProps);

            }

            @Override
            protected void wrapInternal(SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {
                for (Object w : object) {
                    DataStoreInfo wk = (DataStoreInfo) w;
                    wrapInternal(null, model, wk);
                }

            }
        };
    }
}
