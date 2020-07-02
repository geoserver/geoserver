/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.rest.RestException;
import org.geotools.data.DataAccess;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.mongodb.MongoDataStore;
import org.opengis.feature.type.Name;
import org.springframework.http.HttpStatus;

/** Util methods for AppSchema centric logic. */
class AppSchemaUtils {

    /**
     * Returns the internal MongoDB store from the App-Schema data store with the provided store ID.
     */
    static MongoDataStore getMongoStoreById(
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

    /** Returns the MongoDB schemas in use based on the store id. */
    static Set<String> extractUsedSchemas(AppSchemaDataAccess appSchemaStore, String storeId)
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

    /** Returns the store ID for the MongoDataStore provided instance. */
    static String getStoreId(AppSchemaDataAccess appSchemaStore, MongoDataStore mongoStore)
            throws IOException {
        for (Name etn : appSchemaStore.getNames()) {
            FeatureTypeMapping featureTypeMapping = appSchemaStore.getMappingByName(etn);
            if (Objects.equals(mongoStore, featureTypeMapping.getSource().getDataStore()))
                return featureTypeMapping.getSourceDatastoreId().orElse(null);
        }
        return null;
    }

    /**
     * Retrieves the internal MongoDB datastores for the provided mapping names and fills the
     * provided MongoDataStore Set with the result.
     *
     * @param appSchemaStore the App-Schema data store
     * @param names the Mapping names
     * @param mongoStores the Set to fill with the internal MongoDB datastores found
     */
    static void fillMongoStoresSet(
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
}
