/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.iterator;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TODO: implement at least partially listiterator
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class IteratorList<T> extends AbstractList<T> {

    public class GeneratorIterator implements Iterator<T> {

        private Iterator<T> generator;

        public GeneratorIterator(Iterator<T> generator) {
            this.generator = generator;
        }

        @Override
        public boolean hasNext() {
            return generator.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return generator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    IteratorFactory<T> generatorFactory;

    public IteratorList(IteratorFactory<T> generatorFactory) {
        this.generatorFactory = generatorFactory;
    }

    @Override
    public Iterator iterator() {
        return new GeneratorIterator(generatorFactory.newIterator());
    }

    @Override
    public T get(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return -1;
    }
}
