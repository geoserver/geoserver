/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.bdb;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBase;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.util.FastOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.geoserver.config.util.XStreamPersister;

/** @param <T> */
public class XStreamInfoSerialBinding<T> extends SerialBase implements EntryBinding<T> {

    private final XStreamPersister xstreamPersister;
    private boolean compress = true;

    private static final boolean DEBUG =
            "true".equals(System.getProperty("org.opengeo.importer.xstream.debug"));

    private final Class<T> target;

    public XStreamInfoSerialBinding(
            final XStreamPersister xstreamPersister, final Class<T> target) {
        this.xstreamPersister = xstreamPersister;
        this.target = target;

        setSerialBufferSize(512);
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public T entryToObject(DatabaseEntry entry) {

        byte[] data = entry.getData();
        InputStream in = new ByteArrayInputStream(data, entry.getOffset(), entry.getSize());

        try {
            if (compress) {
                in = new LZFInputStream(in);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        T info;

        try {
            if (DEBUG) {
                ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                ByteStreams.copy(in, tmp);
                System.err.println("Read: " + tmp.toString());
                System.err.flush();
                in = new ByteArrayInputStream(tmp.toByteArray());
            }
            info = xstreamPersister.load(in, target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    public void objectToEntry(final T info, DatabaseEntry entry) {

        FastOutputStream serialOutput = super.getSerialOutput(info);
        OutputStream out = serialOutput;
        if (compress) {
            out = new LZFOutputStream(serialOutput);
        }
        if (DEBUG) {
            out = new TeeOutputStream(out, System.out);
        }
        try {
            xstreamPersister.save(info, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final byte[] bytes = serialOutput.getBufferBytes();
        final int offset = 0;
        final int length = serialOutput.getBufferLength();
        entry.setData(bytes, offset, length);
    }
}
