/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.geoserver.importer.ImportContext;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

class MappingIterator implements Iterator<ImportContext>, AutoCloseable {

    SimpleFeatureIterator delegate;
    ImportContextMapper mapper;
    boolean closed = false;

    public MappingIterator(SimpleFeatureIterator delegate, ImportContextMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public void close() throws Exception {
        delegate.close();
        this.closed = true;
    }

    @Override
    public boolean hasNext() {
        try {
            if (closed) {
                return false;
            }
            boolean hasNext = delegate.hasNext();
            if (!hasNext) {
                close();
            }
            return hasNext;
        } catch (Exception e) {
            delegate.close();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImportContext next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            SimpleFeature feature = delegate.next();
            return mapper.toContext(feature);
        } catch (Exception e) {
            delegate.close();
            throw new RuntimeException(e);
        }
    }
}
