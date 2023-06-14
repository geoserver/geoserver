/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.data;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.geoserver.schemalessfeatures.SchemalessFeatureMapper;
import org.geoserver.schemalessfeatures.data.ComplexFeatureReader;
import org.geoserver.schemalessfeatures.data.ComplexFeatureSource;
import org.geoserver.schemalessfeatures.mongodb.mappers.SchemalessMongoToComplexMapper;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geotools.data.Query;
import org.opengis.feature.Feature;

public class MongoComplexReader extends ComplexFeatureReader {

    private MongoCursor<DBObject> cursor;

    private SchemalessFeatureMapper<DBObject> mapper;
    private final Query query;

    public MongoComplexReader(
            MongoCursor<DBObject> cursor, ComplexFeatureSource featureSource, Query query) {
        super(featureSource);
        this.cursor = cursor;
        this.query = query;
        this.mapper =
                new SchemalessMongoToComplexMapper(
                        (DynamicFeatureType) featureSource.getSchema(),
                        query.getCoordinateSystemReproject());
    }

    @Override
    public Feature next() throws IOException, IllegalArgumentException, NoSuchElementException {
        return mapper.buildFeature(cursor.next());
    }

    @Override
    public boolean hasNext() throws IOException {
        return cursor.hasNext();
    }

    @Override
    public void close() throws IOException {
        cursor.close();
    }
}
