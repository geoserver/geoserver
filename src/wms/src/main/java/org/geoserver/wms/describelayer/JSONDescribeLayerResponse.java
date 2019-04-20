/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.WMS;
import org.geotools.ows.wms.LayerDescription;
import org.geotools.util.logging.Logging;

/**
 * A DescribeLayer response specialized in producing Json or JsonP data for a DescribeLayer request.
 *
 * @author carlo cancellieri - GeoSolutions
 */
public class JSONDescribeLayerResponse extends DescribeLayerResponse {

    /** A logger for this class. */
    protected static final Logger LOGGER = Logging.getLogger(JSONDescribeLayerResponse.class);

    /**
     * The MIME type of the format this response produces, supported formats see {@link JSONType}
     */
    private final JSONType type;

    protected final WMS wms;

    /** Constructor for subclasses */
    public JSONDescribeLayerResponse(final WMS wms, final String outputFormat) {
        super(outputFormat);
        this.wms = wms;
        this.type = JSONType.getJSONType(outputFormat);
        if (type == null)
            throw new IllegalArgumentException("Not supported mime type for:" + outputFormat);
    }

    /** Actually write the passed DescribeLayerModel on the OutputStream */
    public void write(DescribeLayerModel layers, DescribeLayerRequest request, OutputStream output)
            throws ServiceException, IOException {

        switch (type) {
            case JSON:
                OutputStreamWriter osw = null;
                Writer outWriter = null;
                try {
                    osw =
                            new OutputStreamWriter(
                                    output, wms.getGeoServer().getSettings().getCharset());
                    outWriter = new BufferedWriter(osw);

                    writeJSON(outWriter, layers);
                } finally {
                    if (outWriter != null) {
                        outWriter.flush();
                    }
                }
                break;
            case JSONP:
                writeJSONP(output, layers);
        }
    }

    private void writeJSONP(OutputStream out, DescribeLayerModel layers) throws IOException {
        // prepare to write out
        OutputStreamWriter osw =
                new OutputStreamWriter(out, wms.getGeoServer().getSettings().getCharset());
        Writer outWriter = new BufferedWriter(osw);

        outWriter.write(getCallbackFunction() + "(");

        writeJSON(outWriter, layers);

        outWriter.write(")");
        outWriter.flush();
    }

    private void writeJSON(Writer outWriter, DescribeLayerModel description) throws IOException {

        try {
            JSONBuilder json = new JSONBuilder(outWriter);
            final List<LayerDescription> layers = description.getLayerDescriptions();
            json.object();
            json.key("version").value(description.getVersion());
            json.key("layerDescriptions");
            json.array();
            for (LayerDescription layer : layers) {
                json.object();
                json.key("layerName").value(layer.getName());
                URL url = layer.getOwsURL();
                json.key("owsURL").value(url != null ? url.toString() : "");
                json.key("owsType").value(layer.getOwsType());
                json.key("typeName").value(layer.getName());
                json.endObject();
            }
            json.endArray();
            json.endObject();
        } catch (JSONException jsonException) {
            ServiceException serviceException =
                    new ServiceException("Error: " + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
    }

    private static String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        } else {
            return JSONType.getCallbackFunction(request.getKvp());
        }
    }

    @Override
    public String getCharset(Operation operation) {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
