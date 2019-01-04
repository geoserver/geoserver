/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import java.io.Closeable;
import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, Closeable {

    /**
     * Closes this stream and releases any system resources associated with it. This method is
     * idempotent, if the stream is already closed then invoking this method has no effect.
     *
     * @throws RuntimeException if an I/O error occurs
     */
    @Override
    public void close();
}
