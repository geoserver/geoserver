/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.wps.ppio.CDataPPIO;

public class LongitudinalProfileProcessResultsPPIO extends CDataPPIO {

    static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build());

    public LongitudinalProfileProcessResultsPPIO() {
        super(
                LongitudinalProfileProcess.LongitudinalProfileProcessResult.class,
                LongitudinalProfileProcess.LongitudinalProfileProcessResult.class,
                "application/json");
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        // encode value (with LongitudinalProfileProcessResult type) into JSON output stream
        LongitudinalProfileProcess.LongitudinalProfileProcessResult result =
                (LongitudinalProfileProcess.LongitudinalProfileProcessResult) value;
        MAPPER.writeValue(os, result);
    }

    @Override
    public Object decode(String input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public PPIODirection getDirection() {
        return PPIODirection.ENCODING;
    }
}
