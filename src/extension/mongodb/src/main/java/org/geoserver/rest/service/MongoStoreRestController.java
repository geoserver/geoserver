/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.rest.AbstractGeoServerController;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geotools.data.DataAccess;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.mongodb.MongoDataStore;
import org.geotools.data.mongodb.MongoSchemaInitParams;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rest controller for handling operations over MongoDB datastores. */
@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.TEXT_HTML_VALUE,
        MediaType.APPLICATION_XML_VALUE
    }
)
public class MongoStoreRestController extends AbstractGeoServerController {

    private static final Logger LOGGER = Logging.getLogger(MongoStoreRestController.class);

    @Autowired
    public MongoStoreRestController(GeoServer geoServer) {
        super(geoServer);
    }

    @ExceptionHandler(RestException.class)
    public ResponseEntity<?> handleRestException(RestException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<Object>(ex.toString(), headers, ex.getStatus());
    }

    /** Clears schema files and entries for the provided MongoDB store. */
    @PostMapping(value = "/appschemastores/{appschemaStoreName}/datastores/{storeId}/cleanSchemas")
    public ResponseEntity<?> clearSchema(
            @PathVariable String workspaceName,
            @PathVariable String appschemaStoreName,
            @PathVariable String storeId)
            throws IOException {
        LOGGER.info("Executing mongoDB clear schema endpoint.");
        // check all parameters required
        if (StringUtils.isBlank(workspaceName)
                || StringUtils.isBlank(appschemaStoreName)
                || StringUtils.isBlank(storeId)) {
            throw new RestException("All parameters are required.", HttpStatus.BAD_REQUEST);
        }
        DataStoreInfo appSchemaStoreInfo = getAppschemaStoreInfo(workspaceName, appschemaStoreName);
        // check if it is an app-schema store type
        final DataAccess<? extends FeatureType, ? extends Feature> store =
                appSchemaStoreInfo.getDataStore(null);
        if (!(store instanceof AppSchemaDataAccess)) {
            throw new RestException("Datastore is not an App-Schema one.", HttpStatus.BAD_REQUEST);
        }
        AppSchemaDataAccess appSchemaStore = (AppSchemaDataAccess) store;
        // check for internal datastore
        MongoDataStore mongoStore = null;
        List<Name> names = appSchemaStore.getNames();
        for (Name ename : names) {
            FeatureTypeMapping mapping = appSchemaStore.getMappingByName(ename);
            if (mapping.getSourceDatastoreId().filter(id -> storeId.equals(id)).isPresent()) {
                DataAccess internalStore = mapping.getSource().getDataStore();
                if (!(internalStore instanceof MongoDataStore)) {
                    throw new RestException(
                            "Internal Datastore is not a MongoDB one.", HttpStatus.BAD_REQUEST);
                }
                mongoStore = (MongoDataStore) internalStore;
                break;
            }
        }
        if (mongoStore == null) {
            throw new RestException("Internal Datastore not found.", HttpStatus.BAD_REQUEST);
        }
        List<String> typeNames = mongoStore.getSchemaStore().typeNames();
        LOGGER.log(Level.INFO, "Found {0} schemas for deleting.", typeNames.size());
        for (String et : typeNames) {
            LOGGER.log(Level.INFO, "Deleting schema: {0}", et);
            mongoStore.getSchemaStore().deleteSchema(new NameImpl(et));
        }
        mongoStore.cleanEntries();
        return ResponseEntity.ok().build();
    }

    private DataStoreInfo getAppschemaStoreInfo(String workspaceName, String appschemaStoreName) {
        // check exists workspaceName and appschemaStoreName
        WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (workspaceInfo == null) {
            throw new RestException("Workspace not found.", HttpStatus.BAD_REQUEST);
        }
        // check exists app-schema store
        DataStoreInfo appSchemaStoreInfo =
                geoServer.getCatalog().getDataStoreByName(workspaceInfo, appschemaStoreName);
        if (appSchemaStoreInfo == null) {
            throw new RestException("Appschema datastore not found.", HttpStatus.BAD_REQUEST);
        }
        return appSchemaStoreInfo;
    }

    /** Clears schema files and entries for all internal MongoDB stores. */
    @PostMapping(value = "/appschemastores/{appschemaStoreName}/cleanSchemas")
    public ResponseEntity<?> clearAllSchemas(
            @PathVariable String workspaceName, @PathVariable String appschemaStoreName)
            throws IOException {
        LOGGER.info("Executing mongoDB clear schema endpoint.");
        // check all parameters required
        if (StringUtils.isBlank(workspaceName) || StringUtils.isBlank(appschemaStoreName)) {
            throw new RestException("All parameters are required.", HttpStatus.BAD_REQUEST);
        }
        DataStoreInfo appSchemaStoreInfo = getAppschemaStoreInfo(workspaceName, appschemaStoreName);
        // check if it is an app-schema store type
        final DataAccess<? extends FeatureType, ? extends Feature> store =
                appSchemaStoreInfo.getDataStore(null);
        if (!(store instanceof AppSchemaDataAccess)) {
            throw new RestException("Datastore is not an App-Schema one.", HttpStatus.BAD_REQUEST);
        }
        AppSchemaDataAccess appSchemaStore = (AppSchemaDataAccess) store;
        // check for internal datastore
        List<Name> names = appSchemaStore.getNames();
        final Set<MongoDataStore> mongoStores = new HashSet<>();
        fillMongoStoresSet(appSchemaStore, names, mongoStores);
        if (mongoStores.isEmpty()) {
            throw new RestException(
                    "Internal MongoDB Datastores not found.", HttpStatus.BAD_REQUEST);
        }
        // clear schemas for every store
        for (MongoDataStore st : mongoStores) {
            List<String> typeNames = st.getSchemaStore().typeNames();
            LOGGER.log(Level.INFO, "Found {0} schemas for deleting.", typeNames.size());
            for (String et : typeNames) {
                LOGGER.log(Level.INFO, "Deleting schema: {0}", et);
                st.getSchemaStore().deleteSchema(new NameImpl(et));
            }
            st.cleanEntries();
        }
        return ResponseEntity.ok().build();
    }

    /** Rebuilds schema files and entries for all internal MongoDB stores. */
    @PostMapping(value = "/appschemastores/{appschemaStoreName}/rebuildMongoSchemas")
    public ResponseEntity<?> rebuildAllSchemas(
            @PathVariable String workspaceName,
            @PathVariable String appschemaStoreName,
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) Integer max)
            throws IOException {
        // check all parameters required
        if (StringUtils.isBlank(workspaceName) || StringUtils.isBlank(appschemaStoreName)) {
            throw new RestException("All parameters are required.", HttpStatus.BAD_REQUEST);
        }
        checkSchemaGenerationParameters(ids, max);
        DataStoreInfo appSchemaStoreInfo = getAppschemaStoreInfo(workspaceName, appschemaStoreName);
        // check if it is an app-schema store type
        final DataAccess<? extends FeatureType, ? extends Feature> store =
                appSchemaStoreInfo.getDataStore(null);
        if (!(store instanceof AppSchemaDataAccess)) {
            throw new RestException("Datastore is not an App-Schema one.", HttpStatus.BAD_REQUEST);
        }
        final AppSchemaDataAccess appSchemaStore = (AppSchemaDataAccess) store;
        // check for internal datastore
        List<Name> names = appSchemaStore.getNames();
        final Set<MongoDataStore> mongoStores = new HashSet<>();
        fillMongoStoresSet(appSchemaStore, names, mongoStores);
        if (mongoStores.isEmpty()) {
            throw new RestException(
                    "Internal MongoDB Datastores not found.", HttpStatus.BAD_REQUEST);
        }
        // clear schemas for every store
        for (MongoDataStore st : mongoStores) {
            MongoSchemaInitParams schemaInitParams =
                    MongoSchemaInitParams.builder()
                            .maxObjects(max != null ? max : 1)
                            .ids(StringUtils.isNotBlank(ids) ? ids.split(",") : new String[] {})
                            .build();
            String storeId = getStoreId(appSchemaStore, st);
            if (storeId == null) {
                LOGGER.warning("Error retrieving an internal store id.");
                continue;
            }
            final Set<String> usedSchemas = extractUsedSchemas(appSchemaStore, storeId);
            List<String> typeNames = st.getSchemaStore().typeNames();
            LOGGER.log(Level.INFO, "Found {0} schemas for deleting.", typeNames.size());
            for (String et : typeNames) {
                LOGGER.log(Level.INFO, "Deleting schema: {0}", et);
                st.getSchemaStore().deleteSchema(new NameImpl(et));
            }
            st.cleanEntries();
            st.setSchemaInitParams(schemaInitParams);
            // rebuild schemas
            for (String et : usedSchemas) {
                LOGGER.log(Level.INFO, "Rebuilding store schema: {0}", et);
                ContentFeatureSource featureSource = st.getFeatureSource(et);
                SimpleFeatureType simpleFeatureType = featureSource.getFeatures().getSchema();
                st.getSchemaStore().storeSchema(simpleFeatureType);
            }
        }
        geoServer.getCatalog().save(appSchemaStoreInfo);
        return ResponseEntity.ok().build();
    }

    private String getStoreId(AppSchemaDataAccess appSchemaStore, MongoDataStore mongoStore)
            throws IOException {
        for (Name etn : appSchemaStore.getNames()) {
            FeatureTypeMapping featureTypeMapping = appSchemaStore.getMappingByName(etn);
            if (Objects.equals(mongoStore, featureTypeMapping.getSource().getDataStore()))
                return featureTypeMapping.getSourceDatastoreId().orElse(null);
        }
        return null;
    }

    private void fillMongoStoresSet(
            final AppSchemaDataAccess appSchemaStore,
            List<Name> names,
            final Set<MongoDataStore> mongoStores)
            throws IOException {
        for (Name ename : names) {
            FeatureTypeMapping mapping = appSchemaStore.getMappingByName(ename);
            DataAccess internalStore = mapping.getSource().getDataStore();
            if (internalStore instanceof MongoDataStore) {
                mongoStores.add((MongoDataStore) internalStore);
            }
        }
    }

    /** Clears schema files and entries for the provided MongoDB store. */
    @PostMapping(
        value = "/appschemastores/{appschemaStoreName}/datastores/{storeId}/rebuildMongoSchemas"
    )
    public ResponseEntity<?> rebuildSchema(
            @PathVariable String workspaceName,
            @PathVariable String appschemaStoreName,
            @PathVariable String storeId,
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) Integer max,
            @RequestParam(required = false) String schema)
            throws IOException {
        LOGGER.info("Executing mongoDB clear schema endpoint.");
        // check all parameters required
        if (StringUtils.isBlank(workspaceName)
                || StringUtils.isBlank(appschemaStoreName)
                || StringUtils.isBlank(storeId)) {
            throw new RestException("All parameters are required.", HttpStatus.BAD_REQUEST);
        }
        checkSchemaGenerationParameters(ids, max);
        DataStoreInfo appSchemaStoreInfo = getAppschemaStoreInfo(workspaceName, appschemaStoreName);
        // check if it is an app-schema store type
        final DataAccess<? extends FeatureType, ? extends Feature> store =
                appSchemaStoreInfo.getDataStore(null);
        if (!(store instanceof AppSchemaDataAccess)) {
            throw new RestException("Datastore is not an App-Schema one.", HttpStatus.BAD_REQUEST);
        }
        final AppSchemaDataAccess appSchemaStore = (AppSchemaDataAccess) store;
        // check for internal datastore
        final MongoDataStore mongoStore = getMongoStoreById(storeId, appSchemaStore);
        MongoSchemaInitParams schemaInitParams =
                MongoSchemaInitParams.builder()
                        .maxObjects(max != null ? max : 1)
                        .ids(StringUtils.isNotBlank(ids) ? ids.split(",") : new String[] {})
                        .build();
        final Set<String> usedSchemas = extractUsedSchemas(appSchemaStore, storeId);
        List<String> typeNames = mongoStore.getSchemaStore().typeNames();
        LOGGER.log(Level.INFO, "Found {0} schemas for deleting.", typeNames.size());
        for (String et : typeNames) {
            LOGGER.log(Level.INFO, "Deleting schema: {0}", et);
            mongoStore.getSchemaStore().deleteSchema(new NameImpl(et));
        }
        mongoStore.cleanEntries();
        mongoStore.setSchemaInitParams(schemaInitParams);
        // if a schema name is provided, build it only
        if (StringUtils.isNotBlank(schema)) {
            ContentFeatureSource featureSource = mongoStore.getFeatureSource(schema);
            SimpleFeatureType simpleFeatureType = featureSource.getFeatures().getSchema();
            mongoStore.getSchemaStore().storeSchema(simpleFeatureType);
        } else {
            // extract schemas used in featureMappings and rebuild only them
            for (String name : usedSchemas) {
                LOGGER.log(Level.INFO, "Rebuilding store schema: {0}", name);
                ContentFeatureSource featureSource = mongoStore.getFeatureSource(name);
                SimpleFeatureType simpleFeatureType = featureSource.getFeatures().getSchema();
                mongoStore.getSchemaStore().storeSchema(simpleFeatureType);
            }
        }
        geoServer.getCatalog().save(appSchemaStoreInfo);
        return ResponseEntity.ok().build();
    }

    private Set<String> extractUsedSchemas(AppSchemaDataAccess appSchemaStore, String storeId)
            throws IOException {
        final List<Name> names = appSchemaStore.getNames();
        final Set<String> schemas = new HashSet<>();
        for (Name en : names) {
            FeatureTypeMapping mapping = appSchemaStore.getMappingByName(en);
            String eid = mapping.getSourceDatastoreId().orElse(null);
            if (Objects.equals(storeId, eid)) {
                Name schemaName = mapping.getSource().getSchema().getName();
                schemas.add(schemaName.getLocalPart());
            }
        }
        return Collections.unmodifiableSet(schemas);
    }

    private MongoDataStore getMongoStoreById(
            String storeId, final AppSchemaDataAccess appSchemaStore) throws IOException {
        MongoDataStore mongoStore = null;
        List<Name> names = appSchemaStore.getNames();
        for (Name ename : names) {
            FeatureTypeMapping mapping = appSchemaStore.getMappingByName(ename);
            if (mapping.getSourceDatastoreId().filter(id -> storeId.equals(id)).isPresent()) {
                DataAccess internalStore = mapping.getSource().getDataStore();
                if (!(internalStore instanceof MongoDataStore)) {
                    throw new RestException(
                            "Internal Datastore is not a MongoDB one.", HttpStatus.BAD_REQUEST);
                }
                mongoStore = (MongoDataStore) internalStore;
                break;
            }
        }
        if (mongoStore == null) {
            throw new RestException("Internal Datastore not found.", HttpStatus.BAD_REQUEST);
        }
        return mongoStore;
    }

    private void checkSchemaGenerationParameters(String ids, Integer max) {
        if (StringUtils.isBlank(ids) && max == null) {
            throw new RestException(
                    "At least one schema generation parameter is required: 'ids' or 'max'",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
