/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.data.DataAccessFactory;
import org.geotools.util.logging.Logging;
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
import org.vfny.geoserver.util.DataStoreUtils;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/datastores")
public class DataStoreController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(DataStoreController.class);

    @Autowired
    public DataStoreController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    /*
    Get Mappings
     */

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<DataStoreInfo> dataStoresGet(@PathVariable String workspaceName) {
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new ResourceNotFoundException("No such workspace : " + workspaceName);
        }
        List<DataStoreInfo> dataStores = catalog.getDataStoresByWorkspace(ws);
        return wrapList(dataStores, DataStoreInfo.class);
    }

    @GetMapping(
        path = "{storeName}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<DataStoreInfo> dataStoreGet(
            @PathVariable String workspaceName, @PathVariable String storeName) {

        DataStoreInfo dataStore = getExistingDataStore(workspaceName, storeName);
        return wrapObject(dataStore, DataStoreInfo.class);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> dataStorePost(
            @RequestBody DataStoreInfo dataStore,
            @PathVariable String workspaceName,
            UriComponentsBuilder builder) {

        if (dataStore.getWorkspace() != null) {
            // ensure the specifried workspace matches the one dictated by the uri
            WorkspaceInfo ws = dataStore.getWorkspace();
            if (!workspaceName.equals(ws.getName())) {
                throw new RestException(
                        "Expected workspace "
                                + workspaceName
                                + " but client specified "
                                + ws.getName(),
                        HttpStatus.FORBIDDEN);
            }
        } else {
            dataStore.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        }
        dataStore.setEnabled(true);

        // if no namespace parameter set, set it
        // TODO: we should really move this sort of thing to be something central
        if (!dataStore.getConnectionParameters().containsKey("namespace")) {
            WorkspaceInfo ws = dataStore.getWorkspace();
            NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
            if (ns == null) {
                ns = catalog.getDefaultNamespace();
            }
            if (ns != null) {
                dataStore.getConnectionParameters().put("namespace", ns.getURI());
            }
        }

        // attempt to set the datastore type
        try {
            DataAccessFactory factory =
                    DataStoreUtils.aquireFactory(dataStore.getConnectionParameters());
            dataStore.setType(factory.getDisplayName());
        } catch (Exception e) {
            LOGGER.warning("Unable to determine datastore type from connection parameters");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "", e);
            }
        }

        catalog.validate(dataStore, true).throwIfInvalid();
        catalog.add(dataStore);

        String storeName = dataStore.getName();
        LOGGER.info("POST data store " + storeName);
        UriComponents uriComponents =
                builder.path("/workspaces/{workspaceName}/datastores/{storeName}")
                        .buildAndExpand(workspaceName, storeName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(storeName, headers, HttpStatus.CREATED);
    }

    @PutMapping(
        value = "{storeName}",
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void dataStorePut(
            @RequestBody DataStoreInfo info,
            @PathVariable String workspaceName,
            @PathVariable String storeName) {

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

        LOGGER.info("PUT datastore " + workspaceName + "," + storeName);
    }

    @DeleteMapping(value = "{storeName}")
    public void dataStoreDelete(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @RequestParam(name = "recurse", required = false, defaultValue = "false")
                    boolean recurse,
            @RequestParam(name = "purge", required = false, defaultValue = "none")
                    String deleteType)
            throws IOException {

        DataStoreInfo ds = getExistingDataStore(workspaceName, storeName);
        if (recurse) {
            new CascadeDeleteVisitor(catalog).visit(ds);
        } else {
            try {
                catalog.remove(ds);
            } catch (IllegalArgumentException e) {
                throw new RestException(e.getMessage(), HttpStatus.FORBIDDEN, e);
            }
        }

        LOGGER.info("DELETE datastore " + workspaceName + ":s" + workspaceName);
    }

    private DataStoreInfo getExistingDataStore(String workspaceName, String storeName) {
        DataStoreInfo original = catalog.getDataStoreByName(workspaceName, storeName);
        if (original == null) {
            throw new ResourceNotFoundException(
                    "No such datastore: " + workspaceName + "," + storeName);
        }
        return original;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return DataStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<DataStoreInfo> getObjectClass() {
                        return DataStoreInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspace = uriTemplateVars.get("workspaceName");
                        String datastore = uriTemplateVars.get("storeName");

                        if (workspace == null || datastore == null) {
                            return null;
                        }
                        return catalog.getDataStoreByName(workspace, datastore);
                    }

                    @Override
                    protected void postEncodeDataStore(
                            DataStoreInfo ds,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        // add a link to the featuretypes
                        writer.startNode("featureTypes");
                        converter.encodeCollectionLink("featuretypes", writer);
                        writer.endNode();
                    }

                    @Override
                    protected void postEncodeReference(
                            Object obj,
                            String ref,
                            String prefix,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
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
            protected void wrapInternal(
                    Map properties, SimpleHash model, DataStoreInfo dataStoreInfo) {
                if (properties == null) {
                    try {
                        properties = model.toMap();
                    } catch (TemplateModelException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                List<Map<String, Map<String, String>>> dsProps = new ArrayList<>();

                List<FeatureTypeInfo> featureTypes =
                        catalog.getFeatureTypesByDataStore(dataStoreInfo);
                for (FeatureTypeInfo ft : featureTypes) {
                    Map<String, String> names = new HashMap<>();
                    names.put("name", ft.getName());
                    dsProps.add(Collections.singletonMap("properties", names));
                }
                if (!dsProps.isEmpty()) properties.putIfAbsent("featureTypes", dsProps);
            }

            @Override
            protected void wrapInternal(
                    SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {
                for (Object w : object) {
                    DataStoreInfo wk = (DataStoreInfo) w;
                    wrapInternal(null, model, wk);
                }
            }
        };
    }
}
