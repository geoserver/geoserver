/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.File;
import java.net.URL;

/**
 * Driver that wraps another driver and returns a {@link PatchPathTrackingReader} to track the path of the current
 * element being read, for use in patch application.
 */
public class PatchTrackingDriver implements HierarchicalStreamDriver {
    private final HierarchicalStreamDriver delegate;

    public PatchTrackingDriver(HierarchicalStreamDriver delegate) {
        this.delegate = delegate;
    }

    @Override
    public HierarchicalStreamReader createReader(java.io.InputStream in) {
        return new PatchPathTrackingReader(delegate.createReader(in));
    }

    @Override
    public HierarchicalStreamReader createReader(URL in) {
        return new PatchPathTrackingReader(delegate.createReader(in));
    }

    @Override
    public HierarchicalStreamReader createReader(File in) {
        return new PatchPathTrackingReader(delegate.createReader(in));
    }

    @Override
    public HierarchicalStreamReader createReader(java.io.Reader in) {
        return new PatchPathTrackingReader(delegate.createReader(in));
    }

    @Override
    public HierarchicalStreamWriter createWriter(java.io.OutputStream out) {
        return delegate.createWriter(out);
    }

    @Override
    public HierarchicalStreamWriter createWriter(java.io.Writer out) {
        return delegate.createWriter(out);
    }
}
