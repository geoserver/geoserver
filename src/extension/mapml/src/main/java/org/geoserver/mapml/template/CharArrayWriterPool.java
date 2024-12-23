/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.template;

import java.io.CharArrayWriter;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;

/**
 * Simple "pool" like class that instantiates a {@link CharArrayWriter} per OGC request, makes it available, and makes
 * sure it is cleaned up when the request is finished.
 */
public class CharArrayWriterPool extends AbstractDispatcherCallback {

    static final ThreadLocal<CharArrayWriter> WRITER = new ThreadLocal<>();

    /**
     * Returns a {@link CharArrayWriter} attached to the current thread (or a new one, in case this methods it's called
     * outside the context of an OGC request). The writer is guaranteed to be either new, or freshly reset.
     *
     * @return
     */
    public static CharArrayWriter getWriter() {
        // just in case we're not on a OGC request
        if (Dispatcher.REQUEST.get() == null) return new CharArrayWriter();

        CharArrayWriter writer = WRITER.get();
        if (writer == null) {
            writer = new CharArrayWriter();
            WRITER.set(writer);
        } else {
            writer.reset();
        }
        return writer;
    }

    @Override
    public void finished(Request request) {
        WRITER.remove();
    }
}
