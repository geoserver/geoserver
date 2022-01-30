/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.wps.ppio.CDataPPIO;

/**
 * A PPIO to generate good looking JSON for the PagedUnique process results
 *
 * @author Sandro Salari - GeoSolutions
 * @author Mauro Bartolomeoli
 */
public class PagedUniqueProcessPPIO extends CDataPPIO {

    static final ObjectMapper MAPPER = new ObjectMapper();

    protected PagedUniqueProcessPPIO() {
        super(
                PagedUniqueProcess.Results.class,
                PagedUniqueProcess.Results.class,
                "application/json");
    }

    @Override
    public Object decode(String input) throws Exception {
        return MAPPER.readValue(input, PagedUniqueProcess.Results.class);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return MAPPER.readValue(input, PagedUniqueProcess.Results.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(Object value, OutputStream os) throws Exception {
        PagedUniqueProcess.Results result = (PagedUniqueProcess.Results) value;
        MAPPER.writeValue(os, result);
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
