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
import java.util.logging.Logger;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
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

/** Coverage store controller */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/coveragestores")
public class CoverageStoreController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(CoverageStoreController.class);

    @Autowired
    public CoverageStoreController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<CoverageStoreInfo> coverageStoresGet(@PathVariable String workspaceName) {

        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new ResourceNotFoundException("No such workspace : " + workspaceName);
        }
        List<CoverageStoreInfo> coverageStores = catalog.getCoverageStoresByWorkspace(ws);
        return wrapList(coverageStores, CoverageStoreInfo.class);
    }

    @GetMapping(
        path = "{storeName}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<CoverageStoreInfo> coverageStoreGet(
            @PathVariable String workspaceName, @PathVariable String storeName) {

        CoverageStoreInfo coverageStore = getExistingCoverageStore(workspaceName, storeName);
        return wrapObject(coverageStore, CoverageStoreInfo.class);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> coverageStorePost(
            @RequestBody CoverageStoreInfo coverageStore,
            @PathVariable String workspaceName,
            UriComponentsBuilder builder) {

        catalog.validate(coverageStore, true).throwIfInvalid();
        catalog.add(coverageStore);

        String storeName = coverageStore.getName();
        LOGGER.info("POST coverage store " + storeName);
        UriComponents uriComponents =
                builder.path("/workspaces/{workspaceName}/coveragestores/{storeName}")
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
    public void coverageStorePut(
            @RequestBody CoverageStoreInfo info,
            @PathVariable String workspaceName,
            @PathVariable String storeName) {

        CoverageStoreInfo original = getExistingCoverageStore(workspaceName, storeName);

        new CatalogBuilder(catalog).updateCoverageStore(original, info);
        catalog.validate(original, false).throwIfInvalid();
        catalog.save(original);
        clear(original);

        LOGGER.info("PUT coverage store " + workspaceName + "," + storeName);
    }

    private CoverageStoreInfo getExistingCoverageStore(String workspaceName, String storeName) {
        CoverageStoreInfo original = catalog.getCoverageStoreByName(workspaceName, storeName);
        if (original == null) {
            throw new ResourceNotFoundException(
                    "No such coverage store: " + workspaceName + "," + storeName);
        }
        return original;
    }

    @DeleteMapping(value = "{storeName}")
    public void coverageStoreDelete(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @RequestParam(name = "recurse", required = false, defaultValue = "false")
                    boolean recurse,
            @RequestParam(name = "purge", required = false, defaultValue = "none")
                    String deleteType)
            throws IOException {

        CoverageStoreInfo cs = getExistingCoverageStore(workspaceName, storeName);
        if (!recurse) {
            if (!catalog.getCoveragesByCoverageStore(cs).isEmpty()) {
                throw new RestException("coveragestore not empty", HttpStatus.UNAUTHORIZED);
            }
            catalog.remove(cs);
        } else {
            new CascadeDeleteVisitor(catalog).visit(cs);
        }
        delete(deleteType, cs);
        clear(cs);

        LOGGER.info("DELETE coverage store " + workspaceName + ":s" + workspaceName);
    }

    /**
     * Check the deleteType parameter in order to decide whether to delete some data too (all, or
     * just metadata).
     */
    private void delete(String deleteType, CoverageStoreInfo cs) throws IOException {
        if (!deleteType.equalsIgnoreCase("none")
                && (deleteType.equalsIgnoreCase("all")
                        || deleteType.equalsIgnoreCase("metadata"))) {

            final boolean deleteData = deleteType.equalsIgnoreCase("all");
            GridCoverageReader reader = cs.getGridCoverageReader(null, null);
            if (reader instanceof StructuredGridCoverage2DReader) {
                ((StructuredGridCoverage2DReader) reader).delete(deleteData);
            }
        }
    }

    void clear(CoverageStoreInfo info) {
        catalog.getResourcePool().clear(info);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return CoverageStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<CoverageStoreInfo> getObjectClass() {
                        return CoverageStoreInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspace = uriTemplateVars.get("workspaceName");
                        String coveragestore = uriTemplateVars.get("storeName");

                        if (workspace == null || coveragestore == null) {
                            return null;
                        }
                        return catalog.getCoverageStoreByName(workspace, coveragestore);
                    }

                    @Override
                    protected void postEncodeCoverageStore(
                            CoverageStoreInfo cs,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        // add a link to the coverages
                        writer.startNode("coverages");
                        converter.encodeCollectionLink("coverages", writer);
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
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<CoverageStoreInfo>(CoverageStoreInfo.class) {

            @Override
            protected void wrapInternal(
                    Map properties, SimpleHash model, CoverageStoreInfo dataStoreInfo) {
                if (properties == null) {
                    try {
                        properties = model.toMap();
                    } catch (TemplateModelException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                List<Map<String, Map<String, String>>> csProps = new ArrayList<>();

                List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore(dataStoreInfo);
                for (CoverageInfo coverage : coverages) {
                    Map<String, String> names = new HashMap<>();
                    names.put("name", coverage.getName());
                    csProps.add(Collections.singletonMap("properties", names));
                }
                if (!csProps.isEmpty()) {
                    properties.putIfAbsent("coverages", csProps);
                }
            }

            @Override
            protected void wrapInternal(
                    SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {
                for (Object w : object) {
                    CoverageStoreInfo wk = (CoverageStoreInfo) w;
                    wrapInternal(null, model, wk);
                }
            }
        };
    }
}
