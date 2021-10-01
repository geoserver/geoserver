/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.schemalessfeatures.filter.SchemalessPropertyAccessorFactory;
import org.geotools.data.mongodb.MongoUtil;

public class MongoSchemalessUtils {

    public static final String GEOMETRY_PATH = "geometry_path";

    /**
     * Find indexed geometry attributes in the collection
     *
     * @param collection the collection into which search for indexed geometry attributes
     * @return a Set of geometry attributes
     */
    public static Set<String> findIndexedGeometries(MongoCollection collection) {
        @SuppressWarnings("unchecked")
        List<DBObject> indexes =
                (List<DBObject>)
                        collection.listIndexes(DBObject.class).into(new ArrayList<DBObject>());
        return MongoUtil.findIndexedFields(indexes, "2dsphere");
    }

    /**
     * Maps a string porperty name to a MongoDB json path
     *
     * @param pn the property name
     * @return the MongoDB json path
     */
    public static String toMongoPath(String pn) {
        String[] splittedPn = pn.split("/");
        StringBuilder sb = new StringBuilder("");
        String prev = null;
        for (int i = 0; i < splittedPn.length; i++) {
            String xpathStep = splittedPn[i];
            if (xpathStep.indexOf(":") != -1) xpathStep = xpathStep.split(":")[1];
            String nameCapitalized =
                    prev != null
                            ? prev.substring(0, 1).toUpperCase()
                                    + prev.substring(1)
                                    + SchemalessPropertyAccessorFactory.NESTED_FEATURE_SUFFIX
                            : null;
            if (!xpathStep.equals(nameCapitalized)) {
                sb.append(xpathStep);
                if (i != splittedPn.length - 1) sb.append(".");
            }
            prev = xpathStep;
        }
        return sb.toString();
    }

    /**
     * Convert a mongoPath to a PropertyName
     *
     * @param mongoPath the mongoPath
     * @return a string representation of PropertyName
     */
    public static String toPropertyName(String mongoPath) {
        return mongoPath.replaceAll("\\.", "/");
    }

    public static boolean isGeometry(DBObject object) {
        Set keys = object.keySet();

        if (!keys.contains("type") || keys.size() != 2) {
            return false;
        }

        final String type = (String) object.get("type");
        boolean isColl = "GeometryCollection".equals(type);
        // Geometry object must have 2 attributs: "type" and "coordinates" or "geometries" (for
        // GeometryCollection)
        return (isColl && keys.contains("geometries")) || (!isColl && keys.contains("coordinates"));
    }
}
