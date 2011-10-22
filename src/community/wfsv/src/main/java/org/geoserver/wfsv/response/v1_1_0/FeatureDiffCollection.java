/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_1_0;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureDiffReader;
import org.geotools.data.postgis.FeatureDiffReaderImpl;


/**
 * To really wrap a FeatureDiffReader into a freemarker template, we have to
 * wrap it into a collection. This allows the reader to be scrolled over without
 * the need to copy it memory.
 *
 * @author Andrea Aime - TOPP
 *
 */
public class FeatureDiffCollection extends AbstractCollection {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");
    private FeatureDiffReader reader;

    public FeatureDiffCollection(FeatureDiffReader reader) {
        this.reader = reader;
    }

    public Iterator iterator() {
        try {
            return new FeatureDiffReaderIterator(new FeatureDiffReaderImpl((FeatureDiffReaderImpl) reader));
        } catch (Exception e) {
            close(reader);
            throw new RuntimeException(e);
        }
    }

    public int size() {
        FeatureDiffReader clone = null;

        try {
            clone = new FeatureDiffReaderImpl((FeatureDiffReaderImpl) reader);

            int size = 0;

            while (clone.hasNext())
                size++;

            return size;
        } catch (Exception e) {
            close(reader);
            throw new RuntimeException(e);
        } finally {
            if (clone != null) {
                close(clone);
            }
        }
    }

    private void close(FeatureDiffReader r) {
        try {
            r.close();
            r = null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing diff reader wrapping iterator", e);
        }
    }

    private static class FeatureDiffReaderIterator implements Iterator {
        private FeatureDiffReader reader;

        public FeatureDiffReaderIterator(FeatureDiffReader reader) {
            this.reader = reader;
        }

        public boolean hasNext() {
            try {
                boolean hasNext = reader.hasNext();

                if (!hasNext) {
                    close();
                }

                return hasNext;
            } catch (IOException e) {
                close();
                throw new RuntimeException(e);
            }
        }

        private void close() {
            try {
                reader.close();
                reader = null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error closing diff reader wrapping iterator", e);
            }
        }

        public Object next() {
            try {
                return reader.next();
            } catch (Exception e) {
                close();
                throw new RuntimeException(e);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Feature diffs are read only");
        }
    }
}
