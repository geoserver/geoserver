/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

/**
 * Raw data backed by a simple java {@link String}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StringRawData extends AbstractRawData {

    private String data;

    public StringRawData(String data, String mimeType) {
        super(mimeType);
        this.data = data;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(data, "UTF-8");
    }

    @Override
    public String toString() {
        return "StringRawData [data="
                + data
                + ", mimeType="
                + mimeType
                + ", extension="
                + extension
                + "]";
    }

    public String getData() {
        return data;
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }
}
