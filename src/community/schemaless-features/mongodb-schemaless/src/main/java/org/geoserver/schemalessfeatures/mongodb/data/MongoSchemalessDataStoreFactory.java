/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.data;

import java.io.IOException;
import java.util.Map;
import org.geotools.api.data.DataAccessFactory;

public class MongoSchemalessDataStoreFactory implements DataAccessFactory {

    public static final Param NAMESPACE =
            new Param("namespace", String.class, "Namespace prefix", false);
    public static final Param CONNECTION_STRING =
            new Param(
                    "MongoDBUri",
                    String.class,
                    "MongoDB URI",
                    true,
                    "mongodb://localhost/<database name>");

    @Override
    public String getDisplayName() {
        return "MongoDB Schemaless";
    }

    @Override
    public String getDescription() {
        return getDisplayName();
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] {NAMESPACE, CONNECTION_STRING};
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public MongoComplexContentDataAccess createDataStore(Map<String, ?> params) throws IOException {
        MongoComplexContentDataAccess dataStore =
                new MongoComplexContentDataAccess((String) CONNECTION_STRING.lookUp(params));
        String uri = (String) NAMESPACE.lookUp(params);
        if (uri != null) {
            dataStore.setNamespaceURI(uri);
        }
        return dataStore;
    }
}
