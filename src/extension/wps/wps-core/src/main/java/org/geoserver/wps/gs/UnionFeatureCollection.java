package org.geoserver.wps.gs;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * A process providing the union between two feature collections
 * 
 * @author Gianni Barrotta - Sinergis
 * @author Andrea Di Nora - Sinergis
 * @author Pietro Arena - Sinergis
 * 
 */
@DescribeProcess(title = "union", description = "Returns a SQL like union between two feature collections " +
		"(will contain attributes from both collections, if two attributes are not the same type " +
		"they will be turned into strings)")
public class UnionFeatureCollection implements GeoServerProcess {

    static final String SCHEMA_NAME = "Union_Layer";

    @DescribeResult(description = "feature collection containg the union between the two feature collections")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "first feature collection", description = "First feature collection") SimpleFeatureCollection firstFeatures,
            @DescribeParameter(name = "second feature collection", description = "Second feature collection") SimpleFeatureCollection secondFeatures)
            throws ClassNotFoundException {
        if (!(firstFeatures.features().next().getDefaultGeometry().getClass().equals(secondFeatures
                .features().next().getDefaultGeometry().getClass()))) {
            throw new WPSException("Different default geometries, cannot perform union");
        } else {
            return new UnitedFeatureCollection(firstFeatures, secondFeatures);
        }
    }

    static class UnitedFeatureCollection extends DecoratingSimpleFeatureCollection {

        SimpleFeatureCollection features;

        SimpleFeatureType schema;

        public UnitedFeatureCollection(SimpleFeatureCollection delegate,
                SimpleFeatureCollection features) throws ClassNotFoundException {
            super(delegate);
            this.features = features;

            // Create schema containing the attributes from both the feature collections
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor descriptor : delegate.getSchema().getAttributeDescriptors()) {
                if (sameNames(features.getSchema(), descriptor)
                        && !sameTypes(features.getSchema(), descriptor)) {
                    AttributeTypeBuilder builder = new AttributeTypeBuilder();
                    builder.setName(descriptor.getLocalName());
                    builder.setNillable(descriptor.isNillable());
                    builder.setBinding(String.class);
                    builder.setMinOccurs(descriptor.getMinOccurs());
                    builder.setMaxOccurs(descriptor.getMaxOccurs());
                    builder.setDefaultValue(descriptor.getDefaultValue());
                    builder.setCRS(this.delegate.features().next().getFeatureType()
                            .getCoordinateReferenceSystem());
                    AttributeDescriptor attributeDescriptor = builder.buildDescriptor(descriptor
                            .getName(), builder.buildType());
                    tb.add(attributeDescriptor);
                } else {
                    tb.add(descriptor);
                }
            }
            for (AttributeDescriptor descriptor : features.getSchema().getAttributeDescriptors()) {
                if (!sameNames(delegate.getSchema(), descriptor)
                        && !sameTypes(delegate.getSchema(), descriptor)) {
                    tb.add(descriptor);
                }
            }

            tb.setCRS(delegate.getSchema().getCoordinateReferenceSystem());
            tb.setNamespaceURI(delegate.getSchema().getName().getNamespaceURI());
            tb.setName(delegate.getSchema().getName());
            this.schema = tb.buildFeatureType();
        }

        @Override
        public SimpleFeatureIterator features() {
            return new UnitedFeatureIterator(delegate.features(), delegate, features.features(),
                    features, getSchema());
        }

        @Override
        public Iterator<SimpleFeature> iterator() {
            return new WrappingIterator(features());
        }

        @Override
        public void close(Iterator<SimpleFeature> close) {
            if (close instanceof WrappingIterator) {
                ((WrappingIterator) close).close();
            }
        }

        @Override
        public SimpleFeatureType getSchema() {
            return this.schema;
        }

        private boolean sameNames(SimpleFeatureType schema, AttributeDescriptor f) {
            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (descriptor.getName().equals(f.getName())) {
                    return true;
                }
            }
            return false;
        }

        private boolean sameTypes(SimpleFeatureType schema, AttributeDescriptor f) {
            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (descriptor.getType().equals(f.getType())) {
                    return true;
                }
            }
            return false;
        }
    }

    static class UnitedFeatureIterator implements SimpleFeatureIterator {

        SimpleFeatureIterator firstDelegate;

        SimpleFeatureIterator secondDelegate;

        SimpleFeatureCollection firstCollection;

        SimpleFeatureCollection secondCollection;

        SimpleFeatureBuilder fb;

        SimpleFeature next;

        int iterationIndex = 0;

        public UnitedFeatureIterator(SimpleFeatureIterator firstDelegate,
                SimpleFeatureCollection firstCollection, SimpleFeatureIterator secondDelegate,
                SimpleFeatureCollection secondCollection, SimpleFeatureType schema) {
            this.firstDelegate = firstDelegate;
            this.secondDelegate = secondDelegate;
            this.firstCollection = firstCollection;
            this.secondCollection = secondCollection;
            fb = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            firstDelegate.close();
            secondDelegate.close();
        }

        public boolean hasNext() {

            while (next == null && firstDelegate.hasNext()) {
                SimpleFeature f = firstDelegate.next();
                for (PropertyDescriptor property : fb.getFeatureType().getDescriptors()) {
                    fb.set(property.getName(), f.getAttribute(property.getName()));

                }
                next = fb.buildFeature(Integer.toString(iterationIndex));
                fb.reset();
                iterationIndex++;
            }
            while (next == null && secondDelegate.hasNext() && !firstDelegate.hasNext()) {
                SimpleFeature f = secondDelegate.next();
                for (PropertyDescriptor property : fb.getFeatureType().getDescriptors()) {
                    fb.set(property.getName(), f.getAttribute(property.getName()));
                }
                next = fb.buildFeature(Integer.toString(iterationIndex));
                fb.reset();
                iterationIndex++;
            }
            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }

            SimpleFeature result = next;
            next = null;
            return result;
        }

    }
}