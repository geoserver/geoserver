/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.filter;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import org.geoserver.schemalessfeatures.mongodb.MongoSchemalessUtils;
import org.geoserver.schemalessfeatures.type.SchemalessFeatureType;
import org.geotools.data.mongodb.AbstractFilterToMongo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/** Schemaless implementation of a visitor to Map a Query object to a MongoDB query */
public class SchemalessFilterToMongo extends AbstractFilterToMongo {

    private MongoSchemalessHelper typeFinder;

    private SchemalessFeatureType featureType;

    public SchemalessFilterToMongo(
            SchemalessFeatureType featureType, MongoCollection<DBObject> collection) {
        super();
        this.featureType = featureType;
        this.typeFinder = new MongoSchemalessHelper(featureType.getName(), collection);
    }

    @Override
    protected String getGeometryPath() {
        Object path =
                featureType
                        .getGeometryDescriptor()
                        .getType()
                        .getUserData()
                        .get(MongoSchemalessUtils.GEOMETRY_PATH);
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
            clazz = typeFinder.getAttributeTypeResult(((PropertyName) e).getPropertyName());
        return clazz;
    }
}
