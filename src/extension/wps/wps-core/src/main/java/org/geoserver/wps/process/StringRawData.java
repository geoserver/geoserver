package org.geoserver.wps.process;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class StringRawData extends AbstractRawData {

    private String data;

    public StringRawData(String data, String mimeType) {
        super(mimeType);
        this.data = data;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(data);
    }

    @Override
    public String toString() {
        return "StringRawData [data=" + data + ", mimeType=" + mimeType + ", extension="
                + extension + "]";
    }

}
