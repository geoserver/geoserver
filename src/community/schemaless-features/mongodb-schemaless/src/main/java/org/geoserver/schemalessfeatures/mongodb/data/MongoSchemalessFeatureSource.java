/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.data;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bson.Document;
import org.geoserver.schemalessfeatures.data.ComplexContentDataAccess;
import org.geoserver.schemalessfeatures.data.SchemalessFeatureSource;
import org.geoserver.schemalessfeatures.mongodb.MongoSchemalessUtils;
import org.geoserver.schemalessfeatures.mongodb.filter.MongoTypeFinder;
import org.geoserver.schemalessfeatures.mongodb.filter.SchemalessFilterToMongo;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.DataUtilities;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.mongodb.MongoCollectionMeta;
import org.geotools.data.mongodb.MongoFilterSplitter;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class MongoSchemalessFeatureSource extends SchemalessFeatureSource {

    private MongoCollection<DBObject> collection;

    private MongoTypeFinder dataTypeFinder;

    public MongoSchemalessFeatureSource(
            Name name, MongoCollection<DBObject> collection, ComplexContentDataAccess store) {
        super(name, store);
        this.collection = collection;
        this.dataTypeFinder = new MongoTypeFinder(name, collection);
    }

    @Override
    protected GeometryDescriptor getGeometryDescriptor() {
        String geometryPath = dataTypeFinder.getGeometryPath();
        if (geometryPath == null) return null;
        AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();
        attributeBuilder.setBinding(Geometry.class);
        String geometryAttributeName;
        if (geometryPath.indexOf(".") != -1) {
            String[] splitted = geometryPath.split("\\.");
            geometryAttributeName = splitted[splitted.length - 1];
        } else {
            geometryAttributeName = geometryPath;
        }
        attributeBuilder.setName(geometryAttributeName);
        attributeBuilder.setNamespaceURI(name.getNamespaceURI());
        attributeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        GeometryType type = attributeBuilder.buildGeometryType();
        type.getUserData().put(MongoSchemalessUtils.GEOMETRY_PATH, geometryPath);
        return attributeBuilder.buildDescriptor(name(name.getNamespaceURI(), geometryPath), type);
    }

    @Override
    protected FeatureReader<FeatureType, Feature> getReaderInteranl(Query query) {
        List<Filter> postFilterList = new ArrayList<>();
        FeatureReader<FeatureType, Feature> reader =
                new MongoComplexReader(toCursor(query, postFilterList), this, query);
        if (!postFilterList.isEmpty())
            return new FilteringFeatureReader<>(reader, postFilterList.get(0));
        return reader;
    }

    @Override
    protected int getCountInteral(Query query) {
        DBObject queryDBO = new BasicDBObject();

        Filter f = query.getFilter();
        if (!isAll(f)) {
            Filter[] split = splitFilter(f);
            queryDBO = toQuery(split[0]);
            if (!isAll(split[1])) {
                return -1;
            }
        }
        return Long.valueOf(collection.countDocuments((BasicDBObject) queryDBO)).intValue();
    }

    MongoCursor<DBObject> toCursor(Query q, java.util.List<Filter> postFilter) {
        DBObject query = new BasicDBObject();

        Filter f = q.getFilter();
        if (!isAll(f)) {
            Filter[] split = splitFilter(f);
            query = toQuery(split[0]);
            if (!isAll(split[1])) {
                postFilter.add(split[1]);
            }
        }

        FindIterable<DBObject> it;
        String[] propertyNames = q.getPropertyNames();
        if (propertyNames != Query.ALL_NAMES) {
            DBObject keys = new BasicDBObject();
            for (String p : propertyNames) {
                keys.put(MongoSchemalessUtils.toMongoPath(p), 1);
            }
            // add properties from post filters
            for (Filter postF : postFilter) {
                String[] attributeNames = DataUtilities.attributeNames(postF);
                for (String attrName : attributeNames) {
                    if (attrName != null && !attrName.isEmpty() && !keys.containsField(attrName)) {
                        keys.put(MongoSchemalessUtils.toMongoPath(attrName), 1);
                    }
                }
            }
            String geometryPath = getGeometryPath();
            if (geometryPath != null && !keys.containsField(geometryPath)) {
                keys.put(geometryPath, 1);
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(String.format("find(%s, %s)", query, keys));
            }
            it = collection.find((BasicDBObject) query).projection((BasicDBObject) keys);
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(String.format("find(%s)", query));
            }
            it = collection.find((BasicDBObject) query);
        }

        if (q.getStartIndex() != null && q.getStartIndex() != 0) {
            it = it.skip(q.getStartIndex());
        }
        if (q.getMaxFeatures() != Integer.MAX_VALUE) {
            it = it.limit(q.getMaxFeatures());
        }

        if (q.getSortBy() != null) {
            BasicDBObject orderBy = new BasicDBObject();
            for (SortBy sortBy : q.getSortBy()) {
                if (sortBy.getPropertyName() != null) {
                    String propName = sortBy.getPropertyName().getPropertyName();
                    String property = MongoSchemalessUtils.toMongoPath(propName);
                    orderBy.append(property, sortBy.getSortOrder() == SortOrder.ASCENDING ? 1 : -1);
                }
            }
            it = it.sort(orderBy);
        }

        return it.cursor();
    }

    DBObject toQuery(Filter f) {
        if (isAll(f)) {
            return new BasicDBObject();
        }

        // apply filter reprojection to EPSG:4326
        SimpleReprojectingVisitor reprojectingFilterVisitor = new SimpleReprojectingVisitor();
        f = (Filter) f.accept(reprojectingFilterVisitor, null);

        SchemalessFilterToMongo v = new SchemalessFilterToMongo(getSchema(), collection);

        return (DBObject) f.accept(v, null);
    }

    Filter[] splitFilter(Filter f) {
        PostPreProcessFilterSplittingVisitor splitter =
                new MongoFilterSplitter(
                        getDataStore().getFilterCapabilities(),
                        null,
                        null,
                        new MongoCollectionMeta(getIndexesInfoMap())) {
                    @Override
                    protected void visitBinaryComparisonOperator(BinaryComparisonOperator filter) {
                        Expression expression1 = filter.getExpression1();
                        Expression expression2 = filter.getExpression2();
                        if (expression1 instanceof PropertyName && expression2 instanceof Literal) {
                            preStack.push(filter);
                        } else if (expression2 instanceof PropertyName
                                && expression1 instanceof Literal) {
                            preStack.push(filter);
                        } else {
                            super.visitBinaryComparisonOperator(filter);
                        }
                    }
                };
        f.accept(splitter, null);
        return new Filter[] {splitter.getFilterPre(), splitter.getFilterPost()};
    }

    private Map<String, String> getIndexesInfoMap() {
        Map<String, String> indexes = new HashMap<>();
        for (Document doc : collection.listIndexes()) {
            Document key = (Document) doc.get("key");
            if (key != null) {
                for (Map.Entry indexData : key.entrySet()) {
                    indexes.put(indexData.getKey().toString(), indexData.getValue().toString());
                }
            }
        }
        return indexes;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        QueryCapabilities capabilities =
                new QueryCapabilities() {
                    @Override
                    public boolean isOffsetSupported() {
                        return true;
                    }

                    @Override
                    public boolean supportsSorting(SortBy... sortAttributes) {
                        return true;
                    }
                };
        return capabilities;
    }

    private String getGeometryPath() {
        GeometryDescriptor descriptor = getGeometryDescriptor();
        if (descriptor == null) return null;
        return descriptor
                .getType()
                .getUserData()
                .get(MongoSchemalessUtils.GEOMETRY_PATH)
                .toString();
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query q) throws IOException {
        String geometryPath = getGeometryPath();
        if (geometryPath != null) {
            q = new Query(q);
            q.setPropertyNames(MongoSchemalessUtils.toPropertyName(geometryPath));
        }
        return super.getBoundsInternal(q);
    }

    /** Reprojects all geometry and referenced envelope literals to EPSG:4326. */
    private class SimpleReprojectingVisitor extends DuplicatingFilterVisitor {
        @Override
        public Object visit(Literal expression, Object extraData) {
            if (expression.getValue() instanceof Geometry) {
                Geometry geom = (Geometry) expression.getValue();
                CoordinateReferenceSystem crs = JTS.getCRS(geom);
                if (crs != null && !CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
                    try {
                        geom =
                                JTS.transform(
                                        geom,
                                        CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84));
                    } catch (MismatchedDimensionException
                            | TransformException
                            | FactoryException e) {
                        throw new RuntimeException(e);
                    }
                }
                return ff.literal(geom);
            } else if (expression.getValue() instanceof ReferencedEnvelope) {
                ReferencedEnvelope env = (ReferencedEnvelope) expression.getValue();
                CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                if (crs != null && !CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
                    try {
                        Envelope transformedEnv =
                                JTS.transform(
                                        env,
                                        CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84));
                        env = new ReferencedEnvelope(transformedEnv, DefaultGeographicCRS.WGS84);
                    } catch (TransformException e) {
                        throw new RuntimeException(e);
                    } catch (FactoryException e) {
                        throw new RuntimeException(e);
                    }
                }
                return ff.literal(env);
            }
            return super.visit(expression, extraData);
        }
    }
}
