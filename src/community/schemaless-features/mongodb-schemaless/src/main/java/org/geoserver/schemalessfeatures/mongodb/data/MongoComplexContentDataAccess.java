/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.data;

import com.mongodb.ConnectionString;
import com.mongodb.DBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.schemalessfeatures.data.ComplexContentDataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Within;
import org.geotools.filter.FilterCapabilities;

public class MongoComplexContentDataAccess extends ComplexContentDataAccess {

    private MongoDatabase database;

    private MongoClient client;

    public MongoComplexContentDataAccess(String connectionString) {
        super();
        ConnectionString connection = getConnectionString(connectionString);
        this.client = createMongoClient(connection);
        this.database = createMongoDatabase(connection.getDatabase());
    }

    @Override
    protected List<Name> createTypeNames() {
        return getCollectionNames().stream().map(s -> name(s)).collect(Collectors.toList());
    }

    @Override
    public FeatureSource<FeatureType, Feature> getFeatureSource(Name typeName) throws IOException {
        if (!getCollectionNames().contains(typeName.getLocalPart()))
            throw new IOException("Type with name " + typeName.getLocalPart() + " not found");
        MongoCollection<DBObject> collection =
                database.getCollection(typeName.getLocalPart(), DBObject.class);
        return new MongoSchemalessFeatureSource(typeName, collection, this);
    }

    private final ConnectionString getConnectionString(String dataStoreURI) {
        if (dataStoreURI == null) {
            throw new IllegalArgumentException("ConnectionString cannot be null");
        }
        if (!dataStoreURI.startsWith("mongodb://")) {
            throw new IllegalArgumentException(
                    "incorrect scheme for URI, expected to begin with \"mongodb://\", found URI of \""
                            + dataStoreURI
                            + "\"");
        }
        ConnectionString connectionString = new ConnectionString(dataStoreURI);
        if (connectionString == null) {
            throw new RuntimeException(
                    "unable to obtain a MongoDB connectionString from URI " + dataStoreURI);
        }
        return connectionString;
    }

    private final MongoClient createMongoClient(ConnectionString connectionString) {
        try {
            return MongoClients.create(connectionString);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unknown mongodb host(s): " + connectionString.toString(), e);
        }
    }

    private final MongoDatabase createMongoDatabase(String databaseName) {
        MongoDatabase database = null;
        if (client != null) {
            database = client.getDatabase(databaseName);
            if (database == null) {
                client.close();
                throw new IllegalArgumentException(
                        "Unknown mongodb database, \"" + databaseName + "\"");
            }
        }
        return database;
    }

    @Override
    protected FilterCapabilities createFilterCapabilities() {
        {
            FilterCapabilities capabilities = new FilterCapabilities();
            capabilities.addAll(FilterCapabilities.LOGICAL_OPENGIS);

            capabilities.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
            capabilities.addType(PropertyIsNull.class);
            capabilities.addType(PropertyIsBetween.class);
            capabilities.addType(PropertyIsLike.class);

            capabilities.addType(BBOX.class);
            capabilities.addType(Intersects.class);
            capabilities.addType(Within.class);
            capabilities.addType(DWithin.class);

            capabilities.addType(Id.class);

            return capabilities;
        }
    }

    @Override
    public void dispose() {
        client.close();
    }

    private Set<String> getCollectionNames() {
        Set<String> collectionNames = new LinkedHashSet<>();
        database.listCollectionNames().forEach(n -> collectionNames.add(n));
        return collectionNames;
    }
}
