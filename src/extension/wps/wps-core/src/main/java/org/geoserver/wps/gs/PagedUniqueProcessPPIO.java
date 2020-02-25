/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.geoserver.wps.ppio.CDataPPIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * A PPIO to generate good looking JSON for the PagedUnique process results
 *
 * @author Sandro Salari - GeoSolutions
 * @author Mauro Bartolomeoli
 */
public class PagedUniqueProcessPPIO extends CDataPPIO {

    private static final JSONParser parser = new JSONParser();

    protected PagedUniqueProcessPPIO() {
        super(
                PagedUniqueProcess.Results.class,
                PagedUniqueProcess.Results.class,
                "application/json");
    }

    @Override
    public Object decode(String input) throws Exception {
        return parser.parse(input);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Reader reader = new InputStreamReader(input);
        return parser.parse(reader);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        PagedUniqueProcess.Results result = (PagedUniqueProcess.Results) value;
        JSONObject obj = new JSONObject();
        obj.put("featureTypeName", result.getFeatureTypeName());
        obj.put("fieldName", result.getFieldName());
        obj.put("size", result.getSize());
        obj.put("values", result.getValues());
        Writer writer = new OutputStreamWriter(os);
        obj.writeJSONString(writer);
        writer.flush();
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
