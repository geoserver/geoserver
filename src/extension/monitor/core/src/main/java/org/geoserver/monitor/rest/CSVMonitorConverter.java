/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.feature.type.DateUtil;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class CSVMonitorConverter extends AbstractMonitorRequestConverter {
    
    static Pattern ESCAPE_REQUIRED = Pattern.compile("[\\,\\s\"]");

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MonitorRequestController.CSV_MEDIATYPE);
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MonitorQueryResults results = (MonitorQueryResults) t;
        Object result = results.getResult();
        String[] fields = results.getFields();
        Monitor monitor = results.getMonitor();

        OutputStream os = outputMessage.getBody();
        write(result, fields, monitor, os);
    }

    void write(Object result, String[] fields, Monitor monitor, OutputStream os)
            throws IOException {
        final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));

        // write out the header
        StringBuffer sb = new StringBuffer();
        for (String fld : fields) {
            sb.append(fld).append(",");
        }
        sb.setLength(sb.length() - 1);
        w.write(sb.append("\n").toString());

        handleRequests(result, new RequestDataVisitor() {
            public void visit(RequestData data, Object... aggregates) {
                try {
                    writeRequest(data, w, fields);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, monitor);

        w.flush();
    }

    void writeRequest(RequestData data, BufferedWriter w, String[] fields) throws IOException {
        StringBuffer sb = new StringBuffer();

        for (String fld : fields) {
            Object val = OwsUtils.get(data, fld);
            if (val instanceof Date) {
                val = DateUtil.serializeDateTime((Date) val);
            }
            if (val != null) {
                String string = val.toString();
                Matcher match = ESCAPE_REQUIRED.matcher(string);
                if (match.find()) { // may need escaping, so escape
                    string = string.replaceAll("\"", "\"\"");// Double all double quotes to escape
                    sb.append("\"");
                    sb.append(string);
                    sb.append("\"");
                } else { // No need for escaping
                    sb.append(string);
                }
            }
            sb.append(",");
        }
        sb.setLength(sb.length() - 1); // Remove trailing comma
        sb.append("\n");
        w.write(sb.toString());
    }

}
