/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert MonitorResutls to a zip file (containing csv files). */
@Component
public class ZIPMonitorConverter extends BaseMonitorConverter {

    CSVMonitorConverter csv = new CSVMonitorConverter();

    public ZIPMonitorConverter() {
        super(MonitorRequestController.ZIP_MEDIATYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void writeInternal(MonitorQueryResults results, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Object object = results.getResult();
        Monitor monitor = results.getMonitor();
        List<String> fields = new ArrayList<>(Arrays.asList(results.getFields()));
        final boolean body = fields.remove("Body");
        final boolean error = fields.remove("Error");

        final ZipOutputStream zout = new ZipOutputStream(outputMessage.getBody());

        // create the csv entry
        zout.putNextEntry(new ZipEntry("requests.csv"));
        String[] csvFields = (String[]) fields.toArray(new String[fields.size()]);
        csv.writeCSVfile(object, csvFields, monitor, zout);

        if (object instanceof Query) {
            monitor.query(
                    (Query) object,
                    new RequestDataVisitor() {
                        public void visit(RequestData data, Object... aggregates) {
                            try {
                                writeBodyAndError(data, zout, body, error, true);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
        } else if (object instanceof List) {
            for (RequestData data : (List<RequestData>) object) {
                writeBodyAndError(data, zout, body, error, true);
            }
        } else {
            writeBodyAndError((RequestData) object, zout, body, error, false);
        }

        zout.flush();
        zout.close();
    }

    void writeBodyAndError(
            RequestData data, ZipOutputStream zout, boolean body, boolean error, boolean postfix)
            throws IOException {

        long id = data.getId();
        if (body && data.getBody() != null) {
            // TODO: figure out the proper extension for the body file
            zout.putNextEntry(new ZipEntry(postfix ? "body_" + id + ".txt" : "body.txt"));
            zout.write(data.getBody());
        }
        if (error && data.getError() != null) {
            zout.putNextEntry(new ZipEntry(postfix ? "error_" + id + ".txt" : "error.txt"));
            data.getError().printStackTrace(new PrintStream(zout));
        }
    }
}
