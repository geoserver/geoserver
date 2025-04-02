/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.wps.gs.GetCoveragesValue;

/** A PPIO that handles the ValuesAtPoint process, which returns wrapped values from a coverage at a given location. */
public class ValuesAtPointPPIO extends CDataPPIO {
    private final ObjectMapper mapper = new ObjectMapper();
    public static final String CONTENT_TYPE = "application/json";

    public ValuesAtPointPPIO() {
        super(GetCoveragesValue.ValuesAtPoint.class, GetCoveragesValue.ValuesAtPoint.class, CONTENT_TYPE);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return mapper.readValue(input, GetCoveragesValue.ValuesAtPoint.class);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        mapper.writeValue(os, value);
    }

    @Override
    public Object decode(String input) throws Exception {
        return mapper.readValue(input, GetCoveragesValue.ValuesAtPoint.class);
    }
}
