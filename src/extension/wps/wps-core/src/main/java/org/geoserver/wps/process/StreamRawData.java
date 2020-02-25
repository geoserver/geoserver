/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.io.InputStream;

/**
 * A RawData based on a single InputStream.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StreamRawData extends AbstractRawData {

    InputStream inputStream;

    public StreamRawData(String mimeType, InputStream inputStream, String extension) {
        super(mimeType, extension);
        this.inputStream = inputStream;
    }

    public StreamRawData(String mimeType, InputStream inputStream) {
        super(mimeType);
        this.inputStream = inputStream;
    }

    public String getMimeType() {
        return mimeType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getFileExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "StreamRawData [mimeType="
                + mimeType
                + ", inputStream="
                + inputStream
                + ", extension="
                + extension
                + "]";
    }
}
