/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.filter;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.conversions.Bson;
import org.geoserver.schemalessfeatures.mongodb.MongoSchemalessUtils;
import org.geotools.api.feature.type.Name;
import org.geotools.util.logging.Logging;

/**
 * Class helping to overcome those issues caused by the schemaless approach, eg. the retrieval of
 * the type binding for a PropertyName or of a geometry path for the geometryDescriptor.
 */
public class MongoTypeFinder {

    private static final Logger LOG = Logging.getLogger(MongoTypeFinder.class);

    private MongoCollection<DBObject> collection;

    private Name name;

    public MongoTypeFinder(Name name, MongoCollection<DBObject> collection) {
        this.name = name;
        this.collection = collection;
    }

    /**
     * Get the type of the attribute specified as a string PropertyName
     *
     * @param attribute the string PropertyName
     * @return the type of the attribute
     */
    public Class<?> getAttributeType(String attribute) {

        String mongoPath = MongoSchemalessUtils.toMongoPath(attribute);
        Bson projection =
                Projections.fields(Projections.include(mongoPath), Projections.excludeId());
        Bson query = Filters.ne(mongoPath, null);
        try (MongoCursor<DBObject> cursor =
                collection.find(query).projection(projection).limit(1).cursor()) {
            Class<?> result = null;
            if (cursor.hasNext()) {
                DBObject dbRes = cursor.next();
                result = getFieldType(dbRes);
            }

            return result;
        }
    }

    private Class<?> getFieldType(Object document) {
        if (document instanceof BasicDBList) {
            BasicDBList list = (BasicDBList) document;
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element != null) {
                    Class<?> result = getFieldType(element);
                    if (result != null) return result;
                }
            }
        } else if (document instanceof BasicDBObject) {
            DBObject object = (DBObject) document;
            Set<String> keys = object.keySet();
            for (String k : keys) {
                Object value = object.get(k);
                if (value != null) return getFieldType(value);
            }
        } else {
            return document.getClass();
        }

        return null;
    }

    /**
     * Get the geometry path to be used for the default geometry. The method first tries to find it
     * from a 2dsphere index. If there is no index, then it takes the path of the first geometry
     * attribute found in the collection's documents.
     *
     * @return the geometry path
     */
    public String getGeometryPath() {

        String geometryPath = findGeometryPathByIndex();

        if (geometryPath == null) geometryPath = findGeometryPathFromData();

        if (geometryPath == null)
            LOG.log(
                    Level.WARNING,
                    "No geometry path found for type {0}, in collection {1}",
                    new Object[] {name.toString(), collection.getNamespace().getFullName()});

        return geometryPath;
    }

    /**
     * Get the geometry path from the first geometry index, if exists, found in the MongoCollection.
     *
     * @return the path to the geometry or null if no 2dsphere index has been provided.
     */
    private String findGeometryPathByIndex() {
        String geometryPath = null;
        Set<String> geometries = MongoSchemalessUtils.findIndexedGeometries(collection);
        if (geometries != null && !geometries.isEmpty()) {
            geometryPath = geometries.iterator().next();
            if (geometries.size() > 1) {
                LOG.log(
                        Level.WARNING,
                        "More than one indexed geometry field found for type {0}, selecting {1} (first one encountered with index search of collection {2})",
                        new Object[] {
                            name.toString(), geometryPath, collection.getNamespace().getFullName()
                        });
            }
        }
        return geometryPath;
    }

    private String findGeometryPathFromData() {
        String geomPath = null;
        try (MongoCursor<DBObject> cursor = collection.find().cursor()) {
            while (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                geomPath = findGeometryPathFromData(dbObject, "");
                if (geomPath != null) break;
            }
        }
        return geomPath;
    }

    private String findGeometryPathFromData(Object object, String path) {
        if (object instanceof BasicDBList) {
            BasicDBList list = (BasicDBList) object;
            for (int i = 0; i < list.size(); i++) {
                String geometryPath =
                        findGeometryPathFromData(list.get(i), addPathPart(path, String.valueOf(i)));
                if (geometryPath != null) return geometryPath;
            }
        } else if (object instanceof BasicDBObject) {
            BasicDBObject dbObject = (BasicDBObject) object;
            if (MongoSchemalessUtils.isGeometry(dbObject)) return path;
            Set<String> keys = dbObject.keySet();
            for (String k : keys) {
                String geometryPath =
                        findGeometryPathFromData(dbObject.get(k), addPathPart(path, k));
                if (geometryPath != null) return geometryPath;
            }
        }

        return null;
    }

    private String addPathPart(String path, String toAdd) {
        if (path.trim().equals("")) return toAdd;
        else return path + "." + toAdd;
    }
}
