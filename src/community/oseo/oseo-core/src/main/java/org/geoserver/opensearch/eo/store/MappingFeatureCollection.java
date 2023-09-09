/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.logging.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseFeatureCollection;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Similar to gt-transform code, but building complex features on a custom transformation
 *
 * @author Andrea Aime - GeoSolution
 */
class MappingFeatureCollection extends BaseFeatureCollection<FeatureType, Feature> {

    static final Logger LOGGER = Logging.getLogger(MappingFeatureCollection.class);

    private SimpleFeatureCollection features;

    private Function<PushbackFeatureIterator<SimpleFeature>, Feature> mapper;

    private SourcePropertyMapper sourcePropertyMapper;

    public MappingFeatureCollection(
            FeatureType schema,
            SimpleFeatureCollection features,
            Function<PushbackFeatureIterator<SimpleFeature>, Feature> mapper) {
        super(schema);
        this.features = features;
        this.mapper = mapper;
        this.sourcePropertyMapper = new SourcePropertyMapper(schema);
    }

    @Override
    public FeatureIterator<Feature> features() {
        PushbackFeatureIterator<SimpleFeature> iterator =
                new PushbackFeatureIterator<>(features.features());
        // scan through the joined features and map them
        return new FeatureIterator<Feature>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Feature next() throws NoSuchElementException {
                Feature mapped = mapper.apply(iterator);
                return mapped;
            }

            @Override
            public void close() {
                iterator.close();
            }
        };
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        if (visitor instanceof MinVisitor) {
            Expression expression = ((MinVisitor) visitor).getExpression();
            String expressionString = getExpressionString(expression);
            String sourceField = sourcePropertyMapper.getSourceName(expressionString);
            MinVisitor withFieldNameVisitor = new MinVisitor(sourceField);
            features.accepts(withFieldNameVisitor, progress);
            ((MinVisitor) visitor).setValue(withFieldNameVisitor.getResult().getValue());
        } else if (visitor instanceof MaxVisitor) {
            Expression expression = ((MaxVisitor) visitor).getExpression();
            String expressionString = getExpressionString(expression);
            String sourceField = sourcePropertyMapper.getSourceName(expressionString);
            MaxVisitor withFieldNameVisitor = new MaxVisitor(sourceField);
            features.accepts(withFieldNameVisitor, progress);
            ((MaxVisitor) visitor).setValue(withFieldNameVisitor.getResult().getValue());
        } else if (visitor instanceof UniqueVisitor) {
            Expression expression = ((UniqueVisitor) visitor).getExpression();
            String expressionString = getExpressionString(expression);
            String sourceField = sourcePropertyMapper.getSourceName(expressionString);
            UniqueVisitor withFieldNameVisitor = new UniqueVisitor(sourceField);
            features.accepts(withFieldNameVisitor, progress);
            ((UniqueVisitor) visitor).setValue(withFieldNameVisitor.getResult().getValue());
        } else {
            super.accepts(visitor, progress);
        }
    }

    private String getExpressionString(Expression expression) {
        if (expression instanceof AttributeExpressionImpl) {
            return ((AttributeExpressionImpl) expression).getPropertyName();
        } else {
            return expression.toString();
        }
    }
}
