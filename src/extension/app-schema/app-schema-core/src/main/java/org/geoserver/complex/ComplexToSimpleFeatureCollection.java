/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.complex;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Feature collection Wrapper. Converts complex features to simple features on the fly based on the
 * Layer info rules.
 */
public class ComplexToSimpleFeatureCollection implements SimpleFeatureCollection {

    private static final Logger LOGGER = Logging.getLogger(ComplexToSimpleFeatureCollection.class);

    private final Map<String, String> rulesMap;
    private final FeatureCollection<FeatureType, Feature> delegate;
    private final NamespaceSupport namespaceSupport;

    private final SimpleFeatureType featureType;

    /**
     * Constructor.
     *
     * @param rulesMap the complex to simple features transformation rules map
     * @param featureCollection the source complex features collection
     */
    public ComplexToSimpleFeatureCollection(
            Map<String, String> rulesMap,
            FeatureCollection<FeatureType, Feature> featureCollection,
            NamespaceSupport namespaceSupport) {
        this.rulesMap = new HashMap<>(requireNonNull(rulesMap));
        this.delegate = requireNonNull(featureCollection);
        this.namespaceSupport = requireNonNull(namespaceSupport);
        this.featureType = requireNonNull(buildConvertedType());
        LOGGER.fine(() -> "Converted feature type: " + featureType);
    }

    /**
     * Builds and returns the simple feature type based on the complex type and the transformation
     * rules.
     */
    protected SimpleFeatureType buildConvertedType() {
        FeatureTypeConverter converter =
                new FeatureTypeConverter(delegate.getSchema(), rulesMap, namespaceSupport);
        return converter.produceSimpleType();
    }

    @Override
    public SimpleFeatureType getSchema() {
        return featureType;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new ComplexToSimpleFeatureIterator(delegate.features());
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        return null;
    }

    @Override
    public SimpleFeatureCollection sort(SortBy order) {
        return null;
    }

    @Override
    public String getID() {
        return delegate.getID();
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        DataUtilities.visit(this, visitor, progress);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return delegate.getBounds();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        return delegate.containsAll(o);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Object[] toArray() {
        return getFeaturesList().toArray();
    }

    @Override
    public <O> O[] toArray(O[] a) {
        return getFeaturesList().toArray(a);
    }

    private List<SimpleFeature> getFeaturesList() {
        try (SimpleFeatureIterator featureIterator = features()) {
            List<SimpleFeature> featuresList = new ArrayList<>();
            while (featureIterator.hasNext()) {
                featuresList.add(featureIterator.next());
            }
            return featuresList;
        }
    }

    /** Feature iterator wrapper, converts every complex feature into a simple feature. */
    class ComplexToSimpleFeatureIterator implements SimpleFeatureIterator {

        private FeatureIterator<Feature> delegate;

        public ComplexToSimpleFeatureIterator(FeatureIterator<Feature> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature simpleFeature = convert(delegate.next());
            LOGGER.log(Level.FINE, "Converted simple feature: {0}", simpleFeature);
            return simpleFeature;
        }

        /**
         * Transform the original complex feature into a simple feature, using the convention and
         * rules. Returns the resulting simple feature.
         */
        private SimpleFeature convert(Feature feature) {
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
            for (PropertyDescriptor descriptor : featureType.getDescriptors()) {
                Name name = descriptor.getName();
                Object attributeValue = getComplexAttributeValue(name, feature);
                builder.set(name, attributeValue);
            }
            return builder.buildFeature(feature.getIdentifier().getID());
        }

        /**
         * Returns the attribute value from the original complex feature based on its simple name.
         */
        private Object getComplexAttributeValue(Name name, Feature feature) {
            String simpleName = name.getLocalPart();
            String attrPath = rulesMap.get(simpleName);
            // if it's a rule based attribute, use the expression
            if (attrPath != null) {
                AttributeExpressionImpl expression =
                        new AttributeExpressionImpl(attrPath, namespaceSupport);
                return expression.evaluate(feature);
            }
            // not rule based, look up simple feature based on simple name
            Optional<Property> propertyOpt =
                    feature.getProperties().stream()
                            .filter(prop -> simpleName.equals(prop.getName().getLocalPart()))
                            .findFirst();
            if (propertyOpt.isPresent()) {
                return propertyOpt.get().getValue();
            }
            // no value found for this attribute, return null
            return null;
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
