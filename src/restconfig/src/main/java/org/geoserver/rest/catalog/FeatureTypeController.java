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
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
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
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** Feature type controller */
@RestController
@ControllerAdvice
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/featuretypes",
        RestBaseController.ROOT_PATH
                + "/workspaces/{workspaceName}/datastores/{storeName}/featuretypes"
    }
)
public class FeatureTypeController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(CoverageStoreController.class);

    @Autowired
    public FeatureTypeController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public Object featureTypesGet(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @RequestParam(defaultValue = "configured") String list) {

        if ("available".equalsIgnoreCase(list)
                || "available_with_geom".equalsIgnoreCase(list)
                || "all".equalsIgnoreCase(list)) {
            DataStoreInfo info = getExistingDataStore(workspaceName, storeName);

            // flag to control whether to filter out types without geometry
            boolean skipNoGeom = "available_with_geom".equalsIgnoreCase(list);

            // list of available feature types
            List<String> featureTypes = new ArrayList<>();
            try {
                DataStore ds = (DataStore) info.getDataStore(null);

                String[] featureTypeNames = ds.getTypeNames();
                for (String featureTypeName : featureTypeNames) {
                    FeatureTypeInfo ftinfo =
                            catalog.getFeatureTypeByDataStore(info, featureTypeName);
                    if (ftinfo == null) {
                        // The feature type is not in catalog, so add it to the return list.
                        // check whether to filter by geometry
                        if (skipNoGeom) {
                            try {
                                FeatureType featureType = ds.getSchema(featureTypeName);
                                if (featureType.getGeometryDescriptor() == null) {
                                    // skip
                                    continue;
                                }
                            } catch (IOException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Unable to load schema for feature type " + featureTypeName,
                                        e);
                            }
                        }
                        featureTypes.add(featureTypeName);
                    } else if ("all".equalsIgnoreCase(list)) {
                        // The feature type is already configured, but "all" was specified, so add
                        // it to the return list.
                        featureTypes.add(featureTypeName);
                    }
                }
            } catch (IOException e) {
                throw new ResourceNotFoundException("Could not load datastore: " + storeName);
            }

            return new StringsList(featureTypes, "featureTypeName");
        } else {
            List<FeatureTypeInfo> fts;

            if (storeName != null) {
                DataStoreInfo dataStore = catalog.getDataStoreByName(workspaceName, storeName);
                fts = catalog.getFeatureTypesByDataStore(dataStore);
            } else {
                NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
                fts = catalog.getFeatureTypesByNamespace(ns);
            }

            return wrapList(fts, FeatureTypeInfo.class);
        }
    }

    @PostMapping(
        consumes = {
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public ResponseEntity featureTypePost(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @RequestBody FeatureTypeInfo ftInfo,
            UriComponentsBuilder builder)
            throws Exception {

        final DataStoreInfo dsInfo = getExistingDataStore(workspaceName, storeName);
        // ensure the store matches up
        if (ftInfo.getStore() != null && storeName != null) {
            if (!storeName.equals(ftInfo.getStore().getName())) {
                throw new RestException(
                        "Expected datastore "
                                + storeName
                                + " but client specified "
                                + ftInfo.getStore().getName(),
                        HttpStatus.FORBIDDEN);
            }
            // HACK: override the StoreInfo in case there's a store named the same on a
            // different workspace. The FeatureTypeInfo deserialization doesn't know how to
            // disambiguate to ftInfo may come in with the wrong Store
            ftInfo.setStore(dsInfo);
        } else {
            ftInfo.setStore(dsInfo);
        }

        // ensure workspace/namespace matches up
        if (ftInfo.getNamespace() != null) {
            if (!workspaceName.equals(ftInfo.getNamespace().getPrefix())) {
                throw new RestException(
                        "Expected workspace "
                                + workspaceName
                                + " but client specified "
                                + ftInfo.getNamespace().getPrefix(),
                        HttpStatus.FORBIDDEN);
            }
        } else {
            ftInfo.setNamespace(catalog.getNamespaceByPrefix(workspaceName));
        }
        ftInfo.setEnabled(true);

        // now, does the feature type exist? If not, create it
        DataAccess dataAccess = dsInfo.getDataStore(null);
        if (dataAccess instanceof DataStore) {
            String typeName = ftInfo.getName();
            if (ftInfo.getNativeName() != null) {
                typeName = ftInfo.getNativeName();
            }
            boolean typeExists = false;
            DataStore dataStore = (DataStore) dataAccess;
            for (String name : dataStore.getTypeNames()) {
                if (name.equals(typeName)) {
                    typeExists = true;
                    break;
                }
            }

            // check to see if this is a virtual JDBC feature type
            MetadataMap mdm = ftInfo.getMetadata();
            boolean virtual = mdm != null && mdm.containsKey(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);

            if (!virtual && !typeExists) {
                dataStore.createSchema(buildFeatureType(ftInfo));
                // the attributes created might not match up 1-1 with the actual spec due to
                // limitations of the data store, have it re-compute them
                ftInfo.getAttributes().clear();
                List<String> typeNames = Arrays.asList(dataStore.getTypeNames());
                // handle Oracle oddities
                // TODO: use the incoming store capabilites API to better handle the name
                // transformation
                if (!typeNames.contains(typeName) && typeNames.contains(typeName.toUpperCase())) {
                    ftInfo.setNativeName(ftInfo.getName().toLowerCase());
                }
            }
        }

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.initFeatureType(ftInfo);

        // attempt to fill in metadata from underlying feature source
        try {
            FeatureSource featureSource =
                    dataAccess.getFeatureSource(new NameImpl(ftInfo.getNativeName()));
            if (featureSource != null) {
                cb.setupMetadata(ftInfo, featureSource);
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING, "Unable to fill in metadata from underlying feature source", e);
        }

        if (ftInfo.getStore() == null) {
            // get from requests
            ftInfo.setStore(dsInfo);
        }

        NamespaceInfo ns = ftInfo.getNamespace();
        if (ns != null && !ns.getPrefix().equals(workspaceName)) {
            // TODO: change this once the two can be different and we untie namespace
            // from workspace
            LOGGER.warning(
                    "Namespace: "
                            + ns.getPrefix()
                            + " does not match workspace: "
                            + workspaceName
                            + ", overriding.");
            ns = null;
        }

        if (ns == null) {
            // infer from workspace
            ns = catalog.getNamespaceByPrefix(workspaceName);
            ftInfo.setNamespace(ns);
        }

        ftInfo.setEnabled(true);
        catalog.validate(ftInfo, true).throwIfInvalid();
        catalog.add(ftInfo);

        // create a layer for the feature type
        catalog.add(new CatalogBuilder(catalog).buildLayer(ftInfo));

        LOGGER.info("POST feature type" + storeName + "," + ftInfo.getName());

        UriComponents uriComponents;
        if (storeName == null) {
            uriComponents =
                    builder.path("/workspaces/{workspaceName}/featuretypes/{featureTypeName}")
                            .buildAndExpand(workspaceName, ftInfo.getName());
        } else {
            uriComponents =
                    builder.path(
                                    "/workspaces/{workspaceName}/datastores/{storeName}/featuretypes/{featureTypeName}")
                            .buildAndExpand(workspaceName, storeName, ftInfo.getName());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>("", headers, HttpStatus.CREATED);
    }

    @GetMapping(
        path = "/{featureTypeName}",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper featureTypeGet(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable String featureTypeName,
            @RequestParam(name = "quietOnNotFound", required = false, defaultValue = "false")
                    Boolean quietOnNotFound) {

        FeatureTypeInfo ftInfo;
        if (storeName != null) {
            DataStoreInfo dsInfo = getExistingDataStore(workspaceName, storeName);
            ftInfo = catalog.getFeatureTypeByDataStore(dsInfo, featureTypeName);
        } else {
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            ftInfo = catalog.getFeatureTypeByName(ns, featureTypeName);
        }
        checkFeatureTypeExists(ftInfo, workspaceName, storeName, featureTypeName);

        return wrapObject(ftInfo, FeatureTypeInfo.class);
    }

    @PutMapping(
        path = "/{featureTypeName}",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public void featureTypePut(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable String featureTypeName,
            @RequestBody FeatureTypeInfo featureTypeUpdate,
            @RequestParam(name = "recalculate", required = false) String recalculate) {

        DataStoreInfo dsInfo = getExistingDataStore(workspaceName, storeName);
        FeatureTypeInfo ftInfo = catalog.getFeatureTypeByDataStore(dsInfo, featureTypeName);
        checkFeatureTypeExists(ftInfo, workspaceName, storeName, featureTypeName);
        Map<String, Serializable> parametersCheck = ftInfo.getStore().getConnectionParameters();

        calculateOptionalFields(featureTypeUpdate, ftInfo, recalculate);
        CatalogBuilder helper = new CatalogBuilder(catalog);
        helper.updateFeatureType(ftInfo, featureTypeUpdate);

        catalog.validate(ftInfo, false).throwIfInvalid();
        catalog.save(ftInfo);
        catalog.getResourcePool().clear(ftInfo);

        Map<String, Serializable> parameters = ftInfo.getStore().getConnectionParameters();
        MetadataMap mdm = ftInfo.getMetadata();
        boolean virtual = mdm != null && mdm.containsKey(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);

        if (!virtual && parameters.equals(parametersCheck)) {
            LOGGER.info(
                    "PUT FeatureType"
                            + storeName
                            + ","
                            + featureTypeName
                            + " updated metadata only");
        } else {
            LOGGER.info(
                    "PUT featureType"
                            + storeName
                            + ","
                            + featureTypeName
                            + " updated metadata and data access");
            catalog.getResourcePool().clear(ftInfo.getStore());
        }
    }

    @DeleteMapping(
        path = "{featureTypeName}",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public void featureTypeDelete(
            @PathVariable String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable String featureTypeName,
            @RequestParam(name = "recurse", defaultValue = "false") Boolean recurse) {

        DataStoreInfo dsInfo = getExistingDataStore(workspaceName, storeName);
        FeatureTypeInfo ftInfo = catalog.getFeatureTypeByDataStore(dsInfo, featureTypeName);
        checkFeatureTypeExists(ftInfo, workspaceName, storeName, featureTypeName);
        List<LayerInfo> layers = catalog.getLayers(ftInfo);

        if (recurse) {
            // by recurse we clear out all the layers that public this resource
            for (LayerInfo l : layers) {
                catalog.remove(l);
                LOGGER.info("DELETE layer " + l.getName());
            }
        } else {
            if (!layers.isEmpty()) {
                throw new RestException(
                        "feature type referenced by layer(s)", HttpStatus.FORBIDDEN);
            }
        }

        catalog.remove(ftInfo);
        LOGGER.info("DELETE feature type" + storeName + "," + featureTypeName);
    }

    /** If the feature type doesn't exists throws a REST exception with HTTP 404 code. */
    private void checkFeatureTypeExists(
            FeatureTypeInfo featureType,
            String workspaceName,
            String storeName,
            String featureTypeName) {
        if (featureType == null && storeName == null) {
            throw new ResourceNotFoundException(
                    String.format("No such feature type: %s,%s", workspaceName, featureTypeName));
        } else if (featureType == null) {
            throw new ResourceNotFoundException(
                    String.format(
                            "No such feature type: %s,%s,%s",
                            workspaceName, storeName, featureTypeName));
        }
    }

    /** Helper method that find a store based on the workspace name and store name. */
    private DataStoreInfo getExistingDataStore(String workspaceName, String storeName) {
        DataStoreInfo original = catalog.getDataStoreByName(workspaceName, storeName);
        if (original == null) {
            throw new ResourceNotFoundException(
                    "No such data store: " + workspaceName + "," + storeName);
        }
        return original;
    }

    SimpleFeatureType buildFeatureType(FeatureTypeInfo fti) {
        // basic checks
        if (fti.getName() == null) {
            throw new RestException(
                    "Trying to create new feature type inside the store, "
                            + "but no feature type name was specified",
                    HttpStatus.BAD_REQUEST);
        } else if (fti.getAttributes() == null || fti.getAttributes().isEmpty()) {
            throw new RestException(
                    "Trying to create new feature type inside the store, "
                            + "but no attributes were specified",
                    HttpStatus.BAD_REQUEST);
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        if (fti.getNativeName() != null) {
            builder.setName(fti.getNativeName());
        } else {
            builder.setName(fti.getName());
        }
        if (fti.getNativeCRS() != null) {
            builder.setCRS(fti.getNativeCRS());
        } else if (fti.getCRS() != null) {
            builder.setCRS(fti.getCRS());
        } else if (fti.getSRS() != null) {
            builder.setSRS(fti.getSRS());
        }
        for (AttributeTypeInfo ati : fti.getAttributes()) {
            if (ati.getLength() != null && ati.getLength() > 0) {
                builder.length(ati.getLength());
            }
            builder.nillable(ati.isNillable());
            builder.add(ati.getName(), ati.getBinding());
        }
        return builder.buildFeatureType();
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return FeatureTypeInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {

        ServletRequestAttributes attrs = (ServletRequestAttributes) getNonNullRequestAttributes();
        String method = attrs.getRequest().getMethod();

        if ("GET".equalsIgnoreCase(method)) {
            persister.setHideFeatureTypeAttributes();
        }

        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<FeatureTypeInfo> getObjectClass() {
                        return FeatureTypeInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspace = uriTemplateVars.get("workspaceName");
                        String featuretype = uriTemplateVars.get("featureTypeName");
                        String datastore = uriTemplateVars.get("storeName");

                        if (workspace == null || datastore == null || featuretype == null) {
                            return null;
                        }
                        DataStoreInfo ds = catalog.getDataStoreByName(workspace, datastore);
                        if (ds == null) {
                            return null;
                        }
                        return catalog.getFeatureTypeByDataStore(ds, featuretype);
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
                        if (obj instanceof DataStoreInfo) {
                            DataStoreInfo ds = (DataStoreInfo) obj;
                            converter.encodeLink(
                                    "/workspaces/"
                                            + converter.encode(ds.getWorkspace().getName())
                                            + "/datastores/"
                                            + converter.encode(ds.getName()),
                                    writer);
                        }
                    }

                    @Override
                    protected void postEncodeFeatureType(
                            FeatureTypeInfo ft,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        try {
                            writer.startNode("attributes");
                            context.convertAnother(ft.attributes());
                            writer.endNode();
                        } catch (IOException e) {
                            throw new RuntimeException("Could not get native attributes", e);
                        }
                    }
                });
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<FeatureTypeInfo>(FeatureTypeInfo.class) {
            @Override
            protected void wrapInternal(Map properties, SimpleHash model, FeatureTypeInfo object) {
                try {
                    properties.put("boundingBox", object.boundingBox());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
