/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.RawDataEncoderDelegate;
import org.geoserver.wps.process.AbstractRawData;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.StreamRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.logging.Logging;

public class RawDataPPIO extends ComplexPPIO {
    static final Logger LOGGER = Logging.getLogger(RawDataPPIO.class);

    WPSResourceManager resourceManager;

    protected RawDataPPIO(WPSResourceManager resourceManager) {
        super(RawData.class, RawData.class, AbstractRawData.BINARY_MIME);
        this.resourceManager = resourceManager;
    }

    @Override
    public Object decode(final InputStream input) throws Exception {
        LOGGER.warning("Creating raw data out of a plain input stream, "
                + "this won't work with asynch requests and won't provide the mime type provided");
        return new StreamRawData(AbstractRawData.BINARY_MIME, input);
    }

    public Object decode(InputStream input, String mimeType, boolean asynchronous) throws Exception {
        if (asynchronous) {
            Resource tmp = resourceManager.getTemporaryResource(".bin");
            File file = tmp.file();
            FileUtils.copyInputStreamToFile(input, file);
            return new FileRawData(file, mimeType);
        } else {
            return new StreamRawData(mimeType, input);
        }
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        RawData rd = (RawData) value;
        IOUtils.copy(rd.getInputStream(), os);
    }

    @Override
    public String getFileExtension(Object value) {
        RawData rd;
        if (value instanceof RawDataEncoderDelegate) {
            rd = ((RawDataEncoderDelegate) value).getRawData();
        } else {
            rd = (RawData) value;
        }
        String extension = rd.getFileExtension();
        if (extension == null) {
            return AbstractRawData.DEFAULT_EXTENSION;
        } else {
            return extension;
        }
    }

}
