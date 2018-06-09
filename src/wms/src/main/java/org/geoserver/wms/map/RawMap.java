/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/**
 * An already encoded {@link WebMap} that holds the raw response content in a byte array.
 *
 * @author Gabriel Roldan
 * @see RawMapResponse
 */
public class RawMap extends WebMap {

    private byte[] mapContents;

    private ByteArrayOutputStream buffer;

    private InputStream stream;

    public RawMap(final WMSMapContent mapContent, final byte[] mapContents, final String mimeType) {
        super(mapContent);
        this.mapContents = mapContents;
        setMimeType(mimeType);
    }

    public RawMap(
            final WMSMapContent mapContent,
            final ByteArrayOutputStream buff,
            final String mimeType) {
        super(mapContent);
        this.buffer = buff;
        setMimeType(mimeType);
    }

    public RawMap(final WMSMapContent mapContent, final InputStream stream, final String mimeType) {
        super(mapContent);
        this.stream = stream;
        setMimeType(mimeType);
    }

    public void writeTo(OutputStream out) throws IOException {
        if (mapContents != null) {
            out.write(mapContents);
        } else if (buffer != null) {
            buffer.writeTo(out);
        } else if (stream != null) {
            IOUtils.copy(stream, out);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void disposeInternal() {
        buffer = null;
        mapContents = null;
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ignore) {
                //
            }
        }
    }
}
