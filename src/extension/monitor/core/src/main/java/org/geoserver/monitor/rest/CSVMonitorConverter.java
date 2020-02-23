/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.feature.type.DateUtil;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert MonitorResutls to a csv file. */
@Component
public class CSVMonitorConverter extends BaseMonitorConverter {

    static Pattern ESCAPE_REQUIRED = Pattern.compile("[\\,\\s\"]");

    private static final class CSVRequestDataVisitor implements RequestDataVisitor {
        private BufferedWriter writer;
        private String[] fields;

        CSVRequestDataVisitor(BufferedWriter writer, String fields[]) {
            this.writer = writer;
            this.fields = fields;
        }

        @Override
        public void visit(RequestData data, Object... aggregates) {
            try {
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
                            string =
                                    string.replaceAll(
                                            "\"", "\"\""); // Double all double quotes to escape
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
                writer.write(sb.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public CSVMonitorConverter() {
        super(MonitorRequestController.CSV_MEDIATYPE);
    }

    @Override
    protected void writeInternal(MonitorQueryResults results, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Object result = results.getResult();
        String[] fields = results.getFields();
        Monitor monitor = results.getMonitor();

        OutputStream os = outputMessage.getBody();
        writeCSVfile(result, fields, monitor, os);
    }

    /**
     * Write CSV file (also called by {@link ZIPMonitorConverter}
     *
     * @param result Query, List or individual RequestData)
     * @param monitor used to cancel output process
     * @param os Output stream (not closed by this method allowing use of zipfile)
     */
    void writeCSVfile(Object result, String[] fields, Monitor monitor, OutputStream os)
            throws IOException {
        final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));

        // write out the header
        StringBuffer sb = new StringBuffer();
        for (String fld : fields) {
            sb.append(fld).append(",");
        }
        sb.setLength(sb.length() - 1);
        w.write(sb.append("\n").toString());

        handleRequests(result, new CSVRequestDataVisitor(w, fields), monitor);

        w.flush();
    }
}
