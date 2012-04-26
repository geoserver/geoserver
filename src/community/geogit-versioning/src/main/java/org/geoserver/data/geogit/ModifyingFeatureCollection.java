package org.geoserver.data.geogit;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

/**
 * FeatureCollection decorator whose iterators apply feature modifications on the fly
 * 
 * @author groldan
 * 
 */
class ModifyingFeatureCollection extends
        DecoratingFeatureCollection<SimpleFeatureType, SimpleFeature> {

    private final Name[] attributeNames;

    private final Object[] attributeValues;

    private final Map<Iterator<SimpleFeature>, Iterator<SimpleFeature>> openIterators = new ConcurrentHashMap<Iterator<SimpleFeature>, Iterator<SimpleFeature>>();

    protected ModifyingFeatureCollection(final SimpleFeatureCollection delegate,
            final Name[] attributeNames, final Object[] attributeValues) {
        super(delegate);
        this.attributeNames = attributeNames;
        this.attributeValues = attributeValues;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Iterator<SimpleFeature> iterator() {
        final Iterator<SimpleFeature> original = delegate.iterator();
        Iterator<SimpleFeature> modified = Iterators.transform(original, new ModifyFunction());
        openIterators.put(modified, original);
        return modified;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void close(Iterator<SimpleFeature> modifying) {
        Iterator<SimpleFeature> original = openIterators.remove(modifying);
        if (original != null) {
            delegate.close(original);
        }
    }

    @Override
    public SimpleFeatureIterator features() {

        final FeatureIterator<SimpleFeature> original = delegate.features();
        final ModifyFunction modifier = new ModifyFunction();

        SimpleFeatureIterator modifying = new SimpleFeatureIterator() {

            @Override
            public SimpleFeature next() throws NoSuchElementException {
                return modifier.apply(original.next());
            }

            @Override
            public boolean hasNext() {
                return original.hasNext();
            }

            @Override
            public void close() {
                original.close();
            }
        };
        return modifying;
    }

    @Override
    public void close(FeatureIterator<SimpleFeature> close) {
        close.close();
    }

    private class ModifyFunction implements Function<SimpleFeature, SimpleFeature> {

        @Override
        public SimpleFeature apply(final SimpleFeature input) {
            Name attributeName;
            Object attributeValue;
            for (int i = 0; i < attributeNames.length; i++) {
                attributeName = attributeNames[i];
                attributeValue = attributeValues[i];
                input.setAttribute(attributeName, attributeValue);
            }
            return input;
        }
    }
}
