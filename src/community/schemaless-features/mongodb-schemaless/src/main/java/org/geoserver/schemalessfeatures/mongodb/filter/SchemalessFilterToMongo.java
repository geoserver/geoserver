/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.filter;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import org.geoserver.schemalessfeatures.mongodb.MongoSchemalessUtils;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.mongodb.AbstractFilterToMongo;

/** Schemaless implementation of a visitor to Map a Query object to a MongoDB query */
public class SchemalessFilterToMongo extends AbstractFilterToMongo {

    private MongoTypeFinder typeFinder;

    public SchemalessFilterToMongo(FeatureType featureType, MongoCollection<DBObject> collection) {
        super();
        setFeatureType(featureType);
        this.typeFinder = new MongoTypeFinder(featureType.getName(), collection);
    }

    @Override
    protected String getGeometryPath() {
        Object path =
                featureType
                        .getGeometryDescriptor()
                        .getType()
                        .getUserData()
                        .get(MongoSchemalessUtils.GEOMETRY_PATH);
        if (path == null) path = typeFinder.getGeometryPath();

        if (path != null) return path.toString();
        else return null;
    }

    @Override
    protected String getPropertyPath(String prop) {
        return MongoSchemalessUtils.toMongoPath(prop);
    }

    @Override
    protected Class<?> getValueTypeInternal(Expression e) {
        Class<?> clazz = null;
        if (e instanceof PropertyName)
            clazz = typeFinder.getAttributeType(((PropertyName) e).getPropertyName());
        return clazz;
    }
}
