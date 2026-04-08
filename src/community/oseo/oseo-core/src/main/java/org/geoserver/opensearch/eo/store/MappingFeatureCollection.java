/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.logging.Logger;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseFeatureCollection;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.logging.Logging;

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
    @SuppressWarnings("PMD.CloseResource")
    public FeatureIterator<Feature> features() {
        PushbackFeatureIterator<SimpleFeature> iterator = new PushbackFeatureIterator<>(features.features());
        // scan through the joined features and map them
        return new FeatureIterator<>() {

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
    public void accepts(
            org.geotools.api.feature.FeatureVisitor visitor, org.geotools.api.util.ProgressListener progress)
            throws IOException {
        if (visitor instanceof MinVisitor minVisitor) {
            Expression expression = minVisitor.getExpression();
            String expressionString = getExpressionString(expression);
            String sourceField = sourcePropertyMapper.getSourceName(expressionString);
            MinVisitor withFieldNameVisitor = new MinVisitor(sourceField);
            features.accepts(withFieldNameVisitor, progress);
            minVisitor.setValue(withFieldNameVisitor.getResult().getValue());
        } else if (visitor instanceof MaxVisitor maxVisitor) {
            Expression expression = maxVisitor.getExpression();
            String expressionString = getExpressionString(expression);
            String sourceField = sourcePropertyMapper.getSourceName(expressionString);
            MaxVisitor withFieldNameVisitor = new MaxVisitor(sourceField);
            features.accepts(withFieldNameVisitor, progress);
            maxVisitor.setValue(withFieldNameVisitor.getResult().getValue());
        } else if (visitor instanceof UniqueVisitor uniqueVisitor) {
            Expression expression = uniqueVisitor.getExpression();
            String expressionString = getExpressionString(expression);
            String sourceField = sourcePropertyMapper.getSourceName(expressionString);
            UniqueVisitor withFieldNameVisitor = new UniqueVisitor(sourceField);
            features.accepts(withFieldNameVisitor, progress);
            uniqueVisitor.setValue(withFieldNameVisitor.getResult().getValue());
        } else {
            super.accepts(visitor, progress);
        }
    }

    private String getExpressionString(Expression expression) {
        if (expression instanceof AttributeExpressionImpl impl) {
            return impl.getPropertyName();
        } else {
            return expression.toString();
        }
    }
}
