/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.tiles;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;

/**
 * This represents a webmap that is backed by a file (i.e. geopkg).
 *
 * <p>See {@link RawMap}, which is similar, but backed with a byte[]
 */
public class FileBackedRawMap extends WebMap implements Closeable {

    File underlyingFile;

    public FileBackedRawMap(
            final WMSMapContent context, final File underlyingFile, final String mimeType) {
        super(context);
        this.underlyingFile = underlyingFile;
        setMimeType(mimeType);
    }

    /**
     * Write the underlying file to the outputstream. DELETES the underlying file after writing.
     *
     * @param out stream to write to
     * @throws IOException
     */
    public void writeTo(OutputStream out) throws IOException {
        if (underlyingFile == null) {
            throw new IOException("underlying file is not present!");
        }
        try (final BufferedInputStream bin =
                new BufferedInputStream(new FileInputStream(underlyingFile))) {
            IOUtils.copy(bin, out);
            out.flush();
            close();
        }
    }

    /**
     * Deletes the underlying file!
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (underlyingFile != null) {
            underlyingFile.delete();
            underlyingFile = null;
        }
    }

    @Override
    protected void disposeInternal() {
        // close() deletes the underlying file.
        try {
            close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
