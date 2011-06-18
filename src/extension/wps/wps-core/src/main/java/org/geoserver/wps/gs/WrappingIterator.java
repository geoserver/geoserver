/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.Iterator;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * An iterator wrapping a {@link SimpleFeatureIterator} and exposing its close method
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
class WrappingIterator implements Iterator<SimpleFeature> {
    SimpleFeatureIterator delegate;

    public WrappingIterator(SimpleFeatureIterator delegate) {
        super();
        this.delegate = delegate;
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public SimpleFeature next() {
        return delegate.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        delegate.close();
    }
}