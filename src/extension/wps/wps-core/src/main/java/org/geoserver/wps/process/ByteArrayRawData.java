/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Raw data backed by a simple java byte array
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ByteArrayRawData extends AbstractRawData {

    private byte[] data;

    public ByteArrayRawData(byte[] data, String mimeType) {
        super(mimeType);
        this.data = data;
    }

    public ByteArrayRawData(byte[] data, String mimeType, String extension) {
        super(mimeType, extension);
        this.data = data;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }

    @Override
    public String toString() {
        return "ByteArrayRawData [mimeType=" + mimeType + "]";
    }

    public byte[] getData() {
        return data;
    }
}
