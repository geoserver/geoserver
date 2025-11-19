/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2025 Vlaams Instituut voor de Zee
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.Reader;

/**
 * This Reader works in 2 phases. The first phase clones every read result into a buffer. The second phase starts when
 * calling rewind. It first replays the buffer, then passes through to the parent.
 */
public class RewindableReader extends Reader {
    final StringBuilder buffer = new StringBuilder();
    final Reader parent;
    boolean isFirstPhase = true;

    /**
     * Creates a new RewindableReader.
     *
     * @param parent – a Reader object providing the underlying stream.
     */
    public RewindableReader(Reader parent) {
        this.parent = parent;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (isFirstPhase) {
            // First pass.  Ask the parent, and keep a copy
            int result = parent.read(cbuf, off, len);
            if (result > 0) {
                buffer.append(cbuf, off, result);
            }
            return result;
        } else if (!buffer.isEmpty()) {
            // Second pass.  Use the buffer and delete
            int bufLen = buffer.length();
            int bufReadLen = Math.min(len, bufLen);
            buffer.getChars(0, bufReadLen, cbuf, off);
            buffer.delete(0, bufReadLen);
            // We might have read less than requested.  We can't ask the parent, as
            // that will break the result of ready()
            return bufReadLen;
        } else {
            // Second pass and buffer is empty.  Ask the parent.
            return parent.read(cbuf, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        // We will be closed twice.  Ignore close requests from the first phase
        if (!isFirstPhase) {
            parent.close();
        }
    }

    /**
     * Rewinds the reader. From now on, the buffer will be replayed and closing the stream becomes possible. You should
     * call this only once.
     */
    public void rewind() {
        isFirstPhase = false;
    }

    public boolean ready() throws IOException {
        if (!isFirstPhase && !buffer.isEmpty()) {
            // We're replaying the buffer
            return true;
        }
        return parent.ready();
    }
}
