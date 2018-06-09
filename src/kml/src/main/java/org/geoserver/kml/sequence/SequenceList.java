/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TODO: implement at least partially listiterator
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class SequenceList<T> extends AbstractList<T> {

    public class GeneratorIterator implements Iterator<T> {

        private Sequence<T> generator;

        private T item;

        public GeneratorIterator(Sequence<T> generator) {
            this.generator = generator;
            this.item = generator.next();
        }

        @Override
        public boolean hasNext() {
            return item != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            T next = this.item;
            this.item = generator.next();
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    SequenceFactory<T> generatorFactory;

    public SequenceList(SequenceFactory<T> generatorFactory) {
        this.generatorFactory = generatorFactory;
    }

    @Override
    public Iterator iterator() {
        return new GeneratorIterator(generatorFactory.newSequence());
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
