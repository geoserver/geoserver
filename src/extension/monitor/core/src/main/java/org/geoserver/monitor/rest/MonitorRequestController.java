/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.Converters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

@RestController
@RequestMapping(path = { RestBaseController.ROOT_PATH + "/monitor/requests/{request}",
        RestBaseController.ROOT_PATH + "/monitor/requests" })
public class MonitorRequestController extends RestBaseController {

    static final String CSV_MEDIATYPE_VALUE = "application/csv";

    static final String ZIP_MEDIATYPE_VALUE = "application/zip";

    static final String EXCEL_MEDIATYPE_VALUE = "application/vnd.ms-excel";

    static final MediaType EXCEL_MEDIATYPE = MediaType.valueOf(EXCEL_MEDIATYPE_VALUE);

    static final MediaType ZIP_MEDIATYPE = MediaType.valueOf(ZIP_MEDIATYPE_VALUE);

    static final MediaType CSV_MEDIATYPE = MediaType.valueOf(CSV_MEDIATYPE_VALUE);

    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    Monitor monitor;

    @Autowired
    public MonitorRequestController(Monitor monitor) {
        this.monitor = monitor;
    }

    String[] getFields(String fields) {
        if (fields != null) {
            return fields.split(";");
        } else {
            List<String> props = OwsUtils.getClassProperties(RequestData.class).properties();

            props.remove("Class");
            props.remove("Body");
            props.remove("Error");

            return props.toArray(new String[props.size()]);
        }
    }

    @GetMapping(produces = { MediaType.TEXT_HTML_VALUE })
    @ResponseBody
    protected RestWrapper handleObjectGetHtml(
            @PathVariable(name = "request", required = false) String req,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "live", required = false) Boolean live,
            @RequestParam(name = "fields", required = false) String fieldsSpec) throws Exception {
        MonitorQueryResults results = handleObjectGet(req, from, to, filter, order, offset, count,
                live, fieldsSpec);
        Object object = results.getResult();

        // HTML specific bits
        if (object instanceof RequestData) {
            return wrapObject((RequestData) object, RequestData.class);
        } else {
            final List<RequestData> requests = new ArrayList<>();
            AbstractMonitorRequestConverter.handleRequests(object, new RequestDataVisitor() {
                public void visit(RequestData data, Object... aggregates) {
                    requests.add(data);
                }
            }, monitor);
            return wrapList(requests, RequestData.class);
        }
    }

    /**
     * Template method to get a custom template name
     *
     * @param object The object being serialized.
     */
    protected String getTemplateName(Object o) {
        if (o instanceof RequestData) {
            return "request.html";
        } else {
            return "requests.html";
        }
    }

    @GetMapping(produces = { CSV_MEDIATYPE_VALUE, EXCEL_MEDIATYPE_VALUE, ZIP_MEDIATYPE_VALUE })
    @ResponseBody
    protected MonitorQueryResults handleObjectGet(
            @PathVariable(name = "request", required = false) String req,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "live", required = false) Boolean live,
            @RequestParam(name = "fields", required = false) String fieldsSpec) throws Exception {
        String[] fields = getFields(fieldsSpec);

        if (req == null) {
            Query q = new Query().between(from != null ? parseDate(from) : null,
                    to != null ? parseDate(to) : null);

            // filter
            if (filter != null) {
                try {
                    parseFilter(filter, q);
                } catch (Exception e) {
                    throw new RestException("Error parsing filter " + filter,
                            HttpStatus.BAD_REQUEST, e);
                }
            }

            // sorting
            String sortBy;
            SortOrder sortOrder;
            if (order != null) {
                int semi = order.indexOf(';');
                if (semi != -1) {
                    String[] split = order.split(";");
                    sortBy = split[0];
                    sortOrder = SortOrder.valueOf(split[1]);
                } else {
                    sortBy = order;
                    sortOrder = SortOrder.ASC;
                }

                q.sort(sortBy, sortOrder);
            }

            // limit offset
            q.page(offset, count);

            // live?
            if (live != null) {
                if (live) {
                    q.filter("status",
                            Arrays.asList(org.geoserver.monitor.RequestData.Status.RUNNING,
                                    org.geoserver.monitor.RequestData.Status.WAITING,
                                    org.geoserver.monitor.RequestData.Status.CANCELLING),
                            Comparison.IN);
                } else {
                    q.filter("status",
                            Arrays.asList(org.geoserver.monitor.RequestData.Status.FINISHED,
                                    org.geoserver.monitor.RequestData.Status.FAILED),
                            Comparison.IN);
                }
            }

            return new MonitorQueryResults(q, fields, monitor);
        } else {
            // return the individual
            RequestData data = monitor.getDAO().getRequest(Long.parseLong(req));
            if (data == null) {
                throw new ResourceNotFoundException("No such request" + req);
            }
            return new MonitorQueryResults(data, fields, monitor);
        }
    }

    Date parseDate(String s) {
        try {
            return DATE_FORMAT.parse(s);
        } catch (ParseException e) {
            return Converters.convert(s, Date.class);
        }

    }

    void parseFilter(String filter, Query q) {
        for (String s : filter.split(";")) {
            if ("".equals(s.trim()))
                continue;
            String[] split = s.split(":");

            String left = split[0];
            Object right = split[2];
            if (right.toString().contains(",")) {
                List list = new ArrayList();
                for (String t : right.toString().split(",")) {
                    list.add(parseProperty(left, t));
                }
                right = list;
            } else {
                right = parseProperty(left, right.toString());
            }

            q.and(left, right, Comparison.valueOf(split[1]));
        }
    }

    Object parseProperty(String property, String value) {
        if ("status".equals(property)) {
            return org.geoserver.monitor.RequestData.Status.valueOf(value);
        }

        return value;
    }

    @DeleteMapping
    public void handleObjectDelete(@PathVariable(name = "request") String req) {
        if (req == null) {
            monitor.getDAO().clear();
        }
    }

    // static class ZIPFormat extends StreamDataFormat {
    //
    // List<String> fields;
    //
    // Monitor monitor;
    //
    // CSVFormat csv;
    //
    // protected ZIPFormat(List<String> fields, CSVFormat csv, Monitor monitor) {
    // super(MediaType.APPLICATION_ZIP);
    //
    // this.fields = fields;
    // this.monitor = monitor;
    // this.csv = csv;
    // }
    //
    // @Override
    // protected void write(Object object, OutputStream out) throws IOException {
    // final ZipOutputStream zout = new ZipOutputStream(out);
    //
    // // create the csv entry
    // zout.putNextEntry(new ZipEntry("requests.csv"));
    // csv.write(object, zout);
    //
    // final boolean body = fields.contains("Body");
    // final boolean error = fields.contains("Error");
    //
    // if (object instanceof Query) {
    // monitor.query((Query) object, new RequestDataVisitor() {
    // public void visit(RequestData data, Object... aggregates) {
    // try {
    // writeBodyAndError(data, zout, body, error, true);
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }
    // });
    // } else if (object instanceof List) {
    // for (RequestData data : (List<RequestData>) object) {
    // writeBodyAndError(data, zout, body, error, true);
    // }
    // } else {
    // writeBodyAndError((RequestData) object, zout, body, error, false);
    // }
    //
    // zout.flush();
    // zout.close();
    // }
    //
    // @Override
    // protected Object read(InputStream in) throws IOException {
    // return null;
    // }
    //
    // void writeBodyAndError(RequestData data, ZipOutputStream zout, boolean body, boolean error,
    // boolean postfix) throws IOException {
    //
    // long id = data.getId();
    // if (body && data.getBody() != null) {
    // // TODO: figure out the proper extension for the body file
    // zout.putNextEntry(new ZipEntry(postfix ? "body_" + id + ".txt" : "body.txt"));
    // zout.write(data.getBody());
    // }
    // if (error && data.getError() != null) {
    // zout.putNextEntry(new ZipEntry(postfix ? "error_" + id + ".txt" : "error.txt"));
    // data.getError().printStackTrace(new PrintStream(zout));
    // }
    // }
    // }
    //
    // static class ExcelFormat extends StreamDataFormat {
    //
    // String[] fields;
    //
    // Monitor monitor;
    //
    // protected ExcelFormat(String[] fields, Monitor monitor) {
    // super(MediaType.APPLICATION_EXCEL);
    // this.fields = fields;
    // this.monitor = monitor;
    // }
    //
    // @Override
    // protected Object read(InputStream in) throws IOException {
    // return null;
    // }
    //
    // @Override
    // protected void write(Object object, OutputStream out) throws IOException {
    // // Create the workbook+sheet
    // HSSFWorkbook wb = new HSSFWorkbook();
    // final HSSFSheet sheet = wb.createSheet("requests");
    //
    // // create the header
    // HSSFRow header = sheet.createRow(0);
    // for (int i = 0; i < fields.length; i++) {
    // HSSFCell cell = header.createCell(i);
    // cell.setCellValue(new HSSFRichTextString(fields[i]));
    // }
    //
    // // write out the request
    // handleRequests(object, new RequestDataVisitor() {
    // int i = 1;
    //
    // public void visit(RequestData data, Object... aggregates) {
    // HSSFRow row = sheet.createRow(i++);
    // for (int j = 0; j < fields.length; j++) {
    // HSSFCell cell = row.createCell(j);
    // Object obj = OwsUtils.get(data, fields[j]);
    // if (obj == null) {
    // continue;
    // }
    //
    // if (obj instanceof Date) {
    // cell.setCellValue((Date) obj);
    // } else if (obj instanceof Number) {
    // cell.setCellValue(((Number) obj).doubleValue());
    // } else {
    // cell.setCellValue(new HSSFRichTextString(obj.toString()));
    // }
    // }
    // }
    // }, monitor);
    //
    // // write to output
    // wb.write(out);
    // }
    // }
    //
    // /**
    // * Converts a query object into one that can be used in the query string of a resource request.
    // */
    // public static String toQueryString(Query q) {
    // StringBuffer sb = new StringBuffer("?");
    // if (q.getFromDate() != null) {
    // sb.append("from=").append(DATE_FORMAT.format(q.getFromDate())).append("&");
    // }
    // if (q.getToDate() != null) {
    // sb.append("to=").append(DATE_FORMAT.format(q.getToDate())).append("&");
    // }
    //
    // if (q.getFilter() != null) {
    // FilterEncoder fe = new FilterEncoder();
    // q.getFilter().accept(fe);
    // sb.append("filter=").append(fe.toString());
    // }
    // return sb.toString();
    //
    // }
    //
    // // TODO: put these methods in a utility class
    // public static String asString(Date d) {
    // return DATE_FORMAT.format(d);
    // }
    //
    // public static Date toDate(String s) throws ParseException {
    // return DATE_FORMAT.parse(s);
    // }
    //
    // static class FilterEncoder extends FilterVisitorSupport {
    // StringBuffer sb = new StringBuffer();
    //
    // @Override
    // protected void handleComposite(CompositeFilter f, String type) {
    // if ("OR".equalsIgnoreCase(type)) {
    // throw new IllegalArgumentException("Unable to encode OR filters");
    // }
    //
    // And and = (And) f;
    // for (Filter fil : and.getFilters()) {
    // fil.accept(this);
    // sb.append(";");
    // }
    // sb.setLength(sb.length() - 1);
    // }
    //
    // @Override
    // protected void handleFilter(Filter f) {
    // sb.append(f.getLeft()).append(":").append(f.getType().name()).append(":");
    // if (f.getRight() instanceof Collection) {
    // for (Object o : ((Collection) f.getRight())) {
    // sb.append(o).append(",");
    // }
    // sb.setLength(sb.length() - 1);
    // } else {
    // sb.append(f.getRight());
    // }
    // }
    //
    // @Override
    // public String toString() {
    // return sb.toString();
    // }
    // }
}
