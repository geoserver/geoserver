/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.template.ObjectWrapper;
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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import freemarker.template.SimpleHash;

/**
 * Feature type controller
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class FeatureTypeController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(CoverageStoreController.class);

    @Autowired
    public FeatureTypeController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(path = { "/workspaces/{workspaceName}/featuretypes",
            "/workspaces/{workspaceName}/datastores/{datastoreName}/featuretypes" }, produces = {
                    MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE })
    public Object getFeatureTypes(
            @PathVariable(name = "workspaceName", required = true) String workspaceName,
            @PathVariable(name = "datastoreName", required = false) String datastoreName,
            @RequestParam(name = "list", required = true, defaultValue = "configured") String list) {

        ensureResourcesExist(workspaceName, datastoreName);

        if ("available".equalsIgnoreCase(list) || "available_with_geom".equalsIgnoreCase(list)) {
            DataStoreInfo info = catalog.getDataStoreByName(workspaceName, datastoreName);
            if (info == null) {
                throw new ResourceNotFoundException("No such datastore: " + datastoreName);
            }

            // flag to control whether to filter out types without geometry
            boolean skipNoGeom = "available_with_geom".equalsIgnoreCase(list);

            // list of available feature types
            List<String> available = new ArrayList<String>();
            try {
                DataStore ds = (DataStore) info.getDataStore(null);

                String[] featureTypeNames = ds.getTypeNames();
                for (String featureTypeName : featureTypeNames) {
                    FeatureTypeInfo ftinfo = catalog.getFeatureTypeByDataStore(info,
                            featureTypeName);
                    if (ftinfo == null) {
                        // not in catalog, add it
                        // check whether to filter by geometry
                        if (skipNoGeom) {
                            try {
                                FeatureType featureType = ds.getSchema(featureTypeName);
                                if (featureType.getGeometryDescriptor() == null) {
                                    // skip
                                    continue;
                                }
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING,
                                        "Unable to load schema for feature type " + featureTypeName,
                                        e);
                            }
                        }
                        available.add(featureTypeName);
                    }
                }
            } catch (IOException e) {
                throw new ResourceNotFoundException("Could not load datastore: " + datastoreName);
            }

            return new StringsList(available, "featureTypeName");
        } else {
            List<FeatureTypeInfo> fts;

            if (datastoreName != null) {
                DataStoreInfo dataStore = catalog.getDataStoreByName(workspaceName, datastoreName);
                fts = catalog.getFeatureTypesByDataStore(dataStore);
            } else {
                NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
                fts = catalog.getFeatureTypesByNamespace(ns);
            }

            return wrapList(fts, FeatureTypeInfo.class);
        }

    }

    @PostMapping(path = { "/workspaces/{workspaceName}/featuretypes",
            "/workspaces/{workspaceName}/datastores/{datastoreName}/featuretypes" }, consumes = {
                    CatalogController.TEXT_JSON, MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity postFeatureType(
            @PathVariable(name = "workspaceName", required = true) String workspace,
            @PathVariable(name = "datastoreName", required = false) String dataStore,
            @RequestBody FeatureTypeInfo featureType, UriComponentsBuilder builder)
            throws Exception {

        ensureResourcesExist(workspace, dataStore);

        // ensure the store matches up
        if (featureType.getStore() != null) {
            if (!dataStore.equals(featureType.getStore().getName())) {
                throw new RestException("Expected datastore " + dataStore + " but client specified "
                        + featureType.getStore().getName(), HttpStatus.FORBIDDEN);
            }
        } else {
            featureType.setStore(catalog.getDataStoreByName(workspace, dataStore));
        }

        // ensure workspace/namespace matches up
        if (featureType.getNamespace() != null) {
            if (!workspace.equals(featureType.getNamespace().getPrefix())) {
                throw new RestException("Expected workspace " + workspace + " but client specified "
                        + featureType.getNamespace().getPrefix(), HttpStatus.FORBIDDEN);
            }
        } else {
            featureType.setNamespace(catalog.getNamespaceByPrefix(workspace));
        }
        featureType.setEnabled(true);

        // now, does the feature type exist? If not, create it
        DataStoreInfo ds = catalog.getDataStoreByName(workspace, dataStore);
        DataAccess gtda = ds.getDataStore(null);
        if (gtda instanceof DataStore) {
            String typeName = featureType.getName();
            if (featureType.getNativeName() != null) {
                typeName = featureType.getNativeName();
            }
            boolean typeExists = false;
            DataStore gtds = (DataStore) gtda;
            for (String name : gtds.getTypeNames()) {
                if (name.equals(typeName)) {
                    typeExists = true;
                    break;
                }
            }

            // check to see if this is a virtual JDBC feature type
            MetadataMap mdm = featureType.getMetadata();
            boolean virtual = mdm != null && mdm.containsKey(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);

            if (!virtual && !typeExists) {
                gtds.createSchema(buildFeatureType(featureType));
                // the attributes created might not match up 1-1 with the actual spec due to
                // limitations of the data store, have it re-compute them
                featureType.getAttributes().clear();
                List<String> typeNames = Arrays.asList(gtds.getTypeNames());
                // handle Oracle oddities
                // TODO: use the incoming store capabilites API to better handle the name transformation
                if (!typeNames.contains(typeName) && typeNames.contains(typeName.toUpperCase())) {
                    featureType.setNativeName(featureType.getName().toLowerCase());
                }
            }
        }

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.initFeatureType(featureType);

        // attempt to fill in metadata from underlying feature source
        try {
            FeatureSource featureSource = gtda
                    .getFeatureSource(new NameImpl(featureType.getNativeName()));
            if (featureSource != null) {
                cb.setupMetadata(featureType, featureSource);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to fill in metadata from underlying feature source",
                    e);
        }

        if (featureType.getStore() == null) {
            // get from requests
            featureType.setStore(ds);
        }

        NamespaceInfo ns = featureType.getNamespace();
        if (ns != null && !ns.getPrefix().equals(workspace)) {
            // TODO: change this once the two can be different and we untie namespace
            // from workspace
            LOGGER.warning("Namespace: " + ns.getPrefix() + " does not match workspace: "
                    + workspace + ", overriding.");
            ns = null;
        }

        if (ns == null) {
            // infer from workspace
            ns = catalog.getNamespaceByPrefix(workspace);
            featureType.setNamespace(ns);
        }

        featureType.setEnabled(true);
        catalog.validate(featureType, true).throwIfInvalid();
        catalog.add(featureType);

        // create a layer for the feature type
        catalog.add(new CatalogBuilder(catalog).buildLayer(featureType));

        LOGGER.info("POST feature type" + dataStore + "," + featureType.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                builder.path("/workspaces/{workspaceName}/datastores/{datastoreName}/featuretypes/"
                        + featureType.getName()).buildAndExpand(workspace, dataStore).toUri());
        return new ResponseEntity<String>("", headers, HttpStatus.CREATED);
    }

    @GetMapping(path = { "/workspaces/{workspaceName}/featuretypes/{featureTypeName}",
            "/workspaces/{workspaceName}/datastores/{datastoreName}/featuretypes/{featureTypeName}" }, produces = {
                    MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE })
    public RestWrapper getFeatureType(
            @PathVariable(name = "workspaceName", required = true) String workspaceName,
            @PathVariable(name = "datastoreName", required = false) String datastoreName,
            @PathVariable(name = "featureTypeName", required = true) String featureTypeName,
            @RequestParam(name = "quietOnNotFound", required = false, defaultValue = "false") Boolean quietOnNotFound) {

        ensureResourcesExist(workspaceName, datastoreName);

        if (datastoreName != null && catalog.getFeatureTypeByDataStore(
                catalog.getDataStoreByName(workspaceName, datastoreName),
                featureTypeName) == null) {
            throw new ResourceNotFoundException("No such feature type: " + workspaceName + ","
                    + datastoreName + "," + featureTypeName);
        } else {
            // look up by workspace/namespace
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);

            if (ns == null || catalog.getFeatureTypeByName(ns, featureTypeName) == null) {
                throw new ResourceNotFoundException(
                        "No such feature type: " + workspaceName + "," + featureTypeName);
            }
        }

        FeatureTypeInfo ftInfo;

        if (datastoreName == null) {
            LOGGER.fine("GET feature type" + workspaceName + "," + featureTypeName);

            // grab the corresponding namespace for this workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if (ns != null) {
                ftInfo = catalog.getFeatureTypeByName(ns, featureTypeName);
            } else {
                return wrapObject(null, FeatureTypeInfo.class,
                        "No namespace found for prefix: " + workspaceName, quietOnNotFound);
            }
        } else { // datastore != null
            LOGGER.fine("GET feature type" + datastoreName + "," + featureTypeName);
            DataStoreInfo dsInfo = catalog.getDataStoreByName(workspaceName, datastoreName);
            ftInfo = catalog.getFeatureTypeByDataStore(dsInfo, featureTypeName);
        }

        String msgIfNotFound;
        if (datastoreName != null) {
            msgIfNotFound = "No such feature type: " + workspaceName + "," + datastoreName + ","
                    + featureTypeName;
        } else {
            msgIfNotFound = "No such feature type: " + workspaceName + "," + featureTypeName;
        }

        return wrapObject(ftInfo, FeatureTypeInfo.class, msgIfNotFound, quietOnNotFound);
    }

    @PutMapping(path = { "/workspaces/{workspaceName}/featuretypes/{featureTypeName}",
            "/workspaces/{workspaceName}/datastores/{datastoreName}/featuretypes/{featureTypeName}" }, produces = {
                    MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE })
    public void putFeatureType(
            @PathVariable(name = "workspaceName", required = true) String workspaceName,
            @PathVariable(name = "datastoreName", required = false) String datastoreName,
            @PathVariable(name = "featureTypeName", required = true) String featureTypeName,
            @RequestBody(required = true) FeatureTypeInfo featureTypeUpdate,
            @RequestParam(name = "recalculate", required = false) String recalculate) {

        ensureResourcesExist(workspaceName, datastoreName);

        if (datastoreName != null
                && catalog.getFeatureTypeByDataStore(catalog.getDataStoreByName(workspaceName),
                        featureTypeName) == null) {

            throw new ResourceNotFoundException("No such feature type: " + workspaceName + ","
                    + datastoreName + "," + featureTypeName);
        } else {
            // look up by workspace/namespace
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if (ns == null || catalog.getFeatureTypeByName(ns, featureTypeName) == null) {
                throw new ResourceNotFoundException(
                        "No such feature type: " + workspaceName + "," + featureTypeName);
            }
        }

        DataStoreInfo ds = catalog.getDataStoreByName(workspaceName, datastoreName);
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByDataStore(ds, featureTypeName);
        Map<String, Serializable> parametersCheck = featureTypeInfo.getStore()
                .getConnectionParameters();

        calculateOptionalFields(featureTypeUpdate, featureTypeInfo, recalculate);
        CatalogBuilder helper = new CatalogBuilder(catalog);
        helper.updateFeatureType(featureTypeInfo, featureTypeUpdate);

        catalog.validate(featureTypeInfo, false).throwIfInvalid();
        catalog.save(featureTypeInfo);
        catalog.getResourcePool().clear(featureTypeInfo);

        Map<String, Serializable> parameters = featureTypeInfo.getStore().getConnectionParameters();
        MetadataMap mdm = featureTypeInfo.getMetadata();
        boolean virtual = mdm != null && mdm.containsKey(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);

        if (!virtual && parameters.equals(parametersCheck)) {
            LOGGER.info("PUT FeatureType" + datastoreName + "," + featureTypeName
                    + " updated metadata only");
        } else {
            LOGGER.info("PUT featureType" + datastoreName + "," + featureTypeName
                    + " updated metadata and data access");
            catalog.getResourcePool().clear(featureTypeInfo.getStore());
        }

    }

    @DeleteMapping(path = { "/workspaces/{workspaceName}/featuretypes/{featureTypeName}",
            "/workspaces/{workspaceName}/datastores/{datastoreName}/featuretypes/{featureTypeName}" }, produces = {
                    MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE })
    public void deleteFeatureType(
            @PathVariable(name = "workspaceName", required = true) String workspaceName,
            @PathVariable(name = "datastoreName", required = false) String datastoreName,
            @PathVariable(name = "featureTypeName", required = true) String featureTypeName,
            @RequestParam(name = "recurse", defaultValue = "false") Boolean recurse) {

        ensureResourcesExist(workspaceName, datastoreName);

        if (datastoreName != null
                && catalog.getFeatureTypeByDataStore(catalog.getDataStoreByName(workspaceName, datastoreName),
                        featureTypeName) == null) {
            throw new RestException("No such feature type: " + workspaceName + "," + datastoreName
                    + "," + featureTypeName, HttpStatus.NOT_FOUND);
        } else {
            // look up by workspace/namespace
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if (ns == null || catalog.getFeatureTypeByName(ns, featureTypeName) == null) {

                throw new RestException(
                        "No such feature type: " + workspaceName + "," + featureTypeName,
                        HttpStatus.NOT_FOUND);
            }
        }

        DataStoreInfo ds = catalog.getDataStoreByName(workspaceName, datastoreName);
        FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore(ds, featureTypeName);
        List<LayerInfo> layers = catalog.getLayers(ft);

        if (recurse) {
            // by recurse we clear out all the layers that public this resource
            for (LayerInfo l : layers) {
                catalog.remove(l);
                LOGGER.info("DELETE layer " + l.getName());
            }
        } else {
            if (!layers.isEmpty()) {
                throw new RestException("feature type referenced by layer(s)",
                        HttpStatus.FORBIDDEN);
            }
        }

        catalog.remove(ft);
        LOGGER.info("DELETE feature type" + datastoreName + "," + featureTypeName);
    }

    /**
     * Check if the provided workspace and datastore exist. <br/>
     * <br/>
     * If the parameter is null, no check is performed. <br/>
     * <br/>
     * If the workspaceName / datastoreName parameter is provided but the corresponding resource does not exist, throws a 404 exception.
     * 
     * @param workspaceName
     * @param datastoreName
     */
    public void ensureResourcesExist(String workspaceName, String datastoreName) {
        // ensure referenced resources exist
        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("No such workspace: " + workspaceName);
        }

        if (datastoreName != null
                && catalog.getDataStoreByName(workspaceName, datastoreName) == null) {
            throw new ResourceNotFoundException(
                    "No such datastore: " + workspaceName + "," + datastoreName);
        }
    }

    SimpleFeatureType buildFeatureType(FeatureTypeInfo fti) {
        // basic checks
        if (fti.getName() == null) {
            throw new RestException("Trying to create new feature type inside the store, "
                    + "but no feature type name was specified", HttpStatus.BAD_REQUEST);
        } else if (fti.getAttributes() == null || fti.getAttributes() == null) {
            throw new RestException("Trying to create new feature type inside the store, "
                    + "but no attributes were specified", HttpStatus.BAD_REQUEST);
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
    public boolean supports(MethodParameter methodParameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return FeatureTypeInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        String method = attrs.getRequest().getMethod();

        if ("GET".equalsIgnoreCase(method)) {
            persister.setHideFeatureTypeAttributes();
        }

        persister.setCallback(new XStreamPersister.Callback() {
            @Override
            protected Class<FeatureTypeInfo> getObjectClass() {
                return FeatureTypeInfo.class;
            }

            @Override
            protected CatalogInfo getCatalogObject() {
                Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder
                        .getRequestAttributes()
                        .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                                RequestAttributes.SCOPE_REQUEST);
                String workspace = uriTemplateVars.get("workspaceName");
                String featuretype = uriTemplateVars.get("featureTypeName");
                String datastore = uriTemplateVars.get("datastoreName");

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
            protected void postEncodeReference(Object obj, String ref, String prefix,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if (obj instanceof NamespaceInfo) {
                    NamespaceInfo ns = (NamespaceInfo) obj;
                    converter.encodeLink("/namespaces/" + converter.encode(ns.getPrefix()), writer);
                }
                if (obj instanceof DataStoreInfo) {
                    DataStoreInfo ds = (DataStoreInfo) obj;
                    converter
                            .encodeLink(
                                    "/workspaces/" + converter.encode(ds.getWorkspace().getName())
                                            + "/datastores/" + converter.encode(ds.getName()),
                                    writer);
                }
            }

            @Override
            protected void postEncodeFeatureType(FeatureTypeInfo ft,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
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
