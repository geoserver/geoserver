/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.springframework.util.Assert;

/** Formats the output of a GetTimeSeries response as a JPG or PNG chart or as a CSV file. */
public class GetTimeSeriesResponse extends Response {
    private static final Logger LOGGER = Logging.getLogger(GetTimeSeriesResponse.class);

    protected static final Set<String> outputFormats = new HashSet<String>();

    static {
        outputFormats.add("text/csv");
        outputFormats.add("image/png");
        outputFormats.add("image/jpg");
        outputFormats.add("image/jpeg");
    }

    private WMS wms;

    private static final String ISO8601_2000_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final int IMAGE_HEIGHT = 600, IMAGE_WIDTH = 700;

    public GetTimeSeriesResponse(final WMS wms) {
        super(FeatureCollectionType.class, outputFormats);
        this.wms = wms;
    }

    /** @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation) */
    @Override
    public boolean canHandle(Operation operation) {
        return "GetTimeSeries".equalsIgnoreCase(operation.getId());
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetFeatureInfoRequest request =
                (GetFeatureInfoRequest)
                        OwsUtils.parameter(operation.getParameters(), GetFeatureInfoRequest.class);
        String infoFormat = (String) request.getRawKvp().get("INFO_FORMAT");
        if (infoFormat != null && outputFormats.contains(infoFormat.toLowerCase())) {
            return infoFormat;
        }
        // default
        return "text/csv";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.notNull(value, "value is null");
        Assert.notNull(operation, "operation is null");
        Assert.isTrue(value instanceof FeatureCollectionType, "unrecognized result type:");
        Assert.isTrue(
                operation.getParameters() != null
                        && operation.getParameters().length == 1
                        && operation.getParameters()[0] instanceof GetFeatureInfoRequest);

        GetFeatureInfoRequest request = (GetFeatureInfoRequest) operation.getParameters()[0];
        FeatureCollectionType results = (FeatureCollectionType) value;

        String mime = getMimeType(value, operation);
        if (mime.startsWith("image/")) {
            writeChart(request, results, output, mime);
        } else {
            writeCsv(request, results, output);
        }
    }

    @SuppressWarnings("rawtypes")
    private void writeChart(
            GetFeatureInfoRequest request,
            FeatureCollectionType results,
            OutputStream output,
            String mimeType)
            throws IOException {
        final TimeSeries series = new TimeSeries("time", Millisecond.class);
        String valueAxisLabel = "Value";
        String title = "Time series";
        final String timeaxisLabel = "Date / time";

        final List collections = results.getFeature();
        if (collections.size() > 0) {
            SimpleFeatureCollection fc = (SimpleFeatureCollection) collections.get(0);
            title += " of " + fc.getSchema().getName().getLocalPart();
            valueAxisLabel = fc.getSchema().getDescription().toString();

            try (SimpleFeatureIterator fi = fc.features()) {
                while (fi.hasNext()) {
                    SimpleFeature f = fi.next();
                    Date date = (Date) f.getAttribute("date");
                    Double value = (Double) f.getAttribute("value");
                    if (!Double.isNaN(value)) {
                        series.add(new Millisecond(date), value);
                    }
                }
            }
        }
        XYDataset dataset = new TimeSeriesCollection(series);

        JFreeChart chart =
                ChartFactory.createTimeSeriesChart(
                        title, timeaxisLabel, valueAxisLabel, dataset, false, false, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(new XYLineAndShapeRenderer());
        if (mimeType.startsWith("image/png")) {
            ChartUtilities.writeChartAsPNG(output, chart, IMAGE_WIDTH, IMAGE_HEIGHT);
        } else if (mimeType.equals("image/jpg") || mimeType.equals("image/jpeg")) {
            ChartUtilities.writeChartAsJPEG(output, chart, IMAGE_WIDTH, IMAGE_HEIGHT);
        }
    }

    @SuppressWarnings("rawtypes")
    private void writeCsv(
            GetFeatureInfoRequest request, FeatureCollectionType results, OutputStream output) {
        Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(output, charSet);
        PrintWriter writer = new PrintWriter(osw);

        CoordinateReferenceSystem crs = request.getGetMapRequest().getCrs();
        final Coordinate middle =
                WMS.pixelToWorld(
                        request.getXPixel(),
                        request.getYPixel(),
                        new ReferencedEnvelope(request.getGetMapRequest().getBbox(), crs),
                        request.getGetMapRequest().getWidth(),
                        request.getGetMapRequest().getHeight());

        if (crs instanceof ProjectedCRS) {
            writer.println("# X: " + middle.y);
            writer.println("# Y: " + middle.x);
        } else {
            writer.println("# Latitude: " + middle.y);
            writer.println("# Longitude: " + middle.x);
        }
        final List collections = results.getFeature();
        if (collections.size() > 0) {
            SimpleFeatureCollection fc = (SimpleFeatureCollection) collections.get(0);
            writer.println("Time (UTC)," + fc.getSchema().getDescription().toString());

            DateFormat isoFormatter = new SimpleDateFormat(ISO8601_2000_UTC_PATTERN);
            isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            try (SimpleFeatureIterator fi = fc.features()) {
                while (fi.hasNext()) {
                    SimpleFeature f = fi.next();
                    Date date = (Date) f.getAttribute("date");
                    Double value = (Double) f.getAttribute("value");
                    writer.println(
                            isoFormatter.format(date) + "," + (Double.isNaN(value) ? "" : value));
                }
            }
        }
        writer.flush();
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        Request request = Dispatcher.REQUEST.get();
        if (request != null
                && request.getRawKvp() != null
                && request.getRawKvp().get("QUERY_LAYERS") != null
                && request.getRawKvp().get("INFO_FORMAT") != null) {
            String filename = null;
            String layers = ((String) request.getRawKvp().get("QUERY_LAYERS")).trim();
            if (layers.length() > 0) {
                filename = layers.replace(",", "_").replace(":", "-");

                String format = (String) request.getRawKvp().get("INFO_FORMAT");
                String[] splitted = format.split("/");
                if (splitted.length > 0) {
                    filename = filename += "." + splitted[1];
                }
                return filename;
            }
        }

        // fallback on the default behavior otherwise
        return super.getAttachmentFileName(value, operation);
    }
}
