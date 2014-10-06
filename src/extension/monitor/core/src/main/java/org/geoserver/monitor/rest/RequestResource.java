/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.geoserver.monitor.And;
import org.geoserver.monitor.CompositeFilter;
import org.geoserver.monitor.Filter;
import org.geoserver.monitor.FilterVisitor;
import org.geoserver.monitor.FilterVisitorSupport;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.format.ReflectiveHTMLFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.type.DateUtil;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;

public class RequestResource extends ReflectiveResource {
    
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
   
    static {
        MediaTypes.registerExtension("csv", new MediaType("application/csv"));
        MediaTypes.registerExtension("zip", MediaType.APPLICATION_ZIP);
        MediaTypes.registerExtension("xls", MediaType.APPLICATION_EXCEL);
    }
    
    Monitor monitor;
    
    public RequestResource(Monitor monitor) {
        this.monitor = monitor;
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = super.createSupportedFormats(request, response);
        formats.add(createCSVFormat(request, response));
        formats.add(createZIPFormat(request, response));
        formats.add(createExcelFormat(request, response));
        return formats;
    }
    
       @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new HTMLFormat(request, response, this, monitor);
    }

    String[] getFields(Request request) {
        String fields = getAttribute("fields");
        
        if (fields != null) {
            return fields.split(";");
        }
        else {
            List<String> props = 
                OwsUtils.getClassProperties(RequestData.class).properties();
            
            props.remove("Class");
            props.remove("Body");
            props.remove("Error");
            
            return props.toArray(new String[props.size()]);
        }
    }
    
    CSVFormat createCSVFormat(Request request, Response response) {
        return new CSVFormat(getFields(request), monitor);
    }
    
    ZIPFormat createZIPFormat(Request request, Response response) {
         String fields = getAttribute("fields");
         List<String> props;
         if (fields == null) {
             props = OwsUtils.getClassProperties(RequestData.class).properties();
         }
         else {
             props = Arrays.asList(fields.split(";"));
          }
        
         return new ZIPFormat(props, createCSVFormat(request, response), monitor);
    }

    ExcelFormat createExcelFormat(Request request, Response response) {
        return new ExcelFormat(getFields(request), monitor);
    }
    @Override
    public boolean allowGet() {
        return true;
    }
    
    @Override
    public boolean allowDelete() {
        return true;
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        String req = getAttribute("request");
        
        if (req == null) {
            //return a collection
            Form form = null;
            if (getRequest().getResourceRef() != null) {
                form = getRequest().getResourceRef().getQueryAsForm();
            }
            else {
                form = new Form();
            }
            
            
            // date range
            String from = form.getFirstValue("from");
            String to = form.getFirstValue("to");

            Query q = new Query().between(
                from != null ? parseDate(from) : null, 
                to != null ? parseDate(to) : null);
            
            //filter
            String filter = form.getFirstValue("filter");
            if (filter != null) {
                try {
                    parseFilter(filter, q);
                }
                catch(Exception e) {
                    throw new RestletException("Error parsing filter " + filter, 
                        Status.CLIENT_ERROR_BAD_REQUEST, e);
                }
            }
            
            //sorting
            String sortBy;
            SortOrder sortOrder;
            
            String order = form.getFirstValue("order");
            if (order != null) {
                int semi = order.indexOf(';');
                if (semi != -1) {
                    String[] split = order.split(";");
                    sortBy = split[0];
                    sortOrder = SortOrder.valueOf(split[1]);
                }
                else {
                    sortBy = order;
                    sortOrder = SortOrder.ASC;
                }
                
                q.sort(sortBy, sortOrder);
            }
            
            //limit offset
            String offset = form.getFirstValue("offset");
            String count = form.getFirstValue("count");
            q.page(offset != null ? Long.parseLong(offset) : null,
                count != null ? Long.parseLong(count) : null);
            
            //live?
            String live = form.getFirstValue("live");
            if (live != null) {
                if ("yes".equalsIgnoreCase(live) || "true".equalsIgnoreCase(live)) {
                    q.filter("status", Arrays.asList(
                        org.geoserver.monitor.RequestData.Status.RUNNING,
                        org.geoserver.monitor.RequestData.Status.WAITING,
                        org.geoserver.monitor.RequestData.Status.CANCELLING), Comparison.IN);
                }
                else {
                    q.filter("status", Arrays.asList(
                        org.geoserver.monitor.RequestData.Status.FINISHED,
                        org.geoserver.monitor.RequestData.Status.FAILED), Comparison.IN);
                }
            }
            
            return q;
        }
        else {
            //return the individual
            RequestData data = monitor.getDAO().getRequest(Long.parseLong(req));
            if (data == null) {
                throw new RestletException("No such request" + req, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return data;
        }
    }
    
    Date parseDate(String s) {
        try {
            return DATE_FORMAT.parse(s);
        } 
        catch (ParseException e) {
            return Converters.convert(s, Date.class);
        }
        
    }
    void parseFilter(String filter, Query q) {
        for (String s : filter.split(";")) {
            if ("".equals(s.trim())) continue;
            String[] split = s.split(":");
        
            String left = split[0];
            Object right = split[2];
            if (right.toString().contains(",")) {
                List list = new ArrayList();
                for (String t : right.toString().split(",")) {
                    list.add(parseProperty(left, t));
                }
                right = list;
            }
            else {
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
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String req = getAttribute("request");
        if (req == null) {
            monitor.getDAO().clear();
        }
    }

    static void handleRequests(Object object, RequestDataVisitor visitor, Monitor monitor) {
        if (object instanceof Query) {
            monitor.query((Query)object, visitor);
        }
        else {
            List<RequestData> requests;
            if (object instanceof List) {
                requests = (List) object;
            }
            else {
                requests = Collections.singletonList((RequestData)object);
            }
            for (RequestData data : requests) {
                visitor.visit(data, null);
            }
        }
    }
    
    static class HTMLFormat extends ReflectiveHTMLFormat {
        
        Monitor monitor;
        protected HTMLFormat(Request request, Response response, Resource resource, Monitor monitor) {
            super(RequestData.class, request, response, resource);
            this.monitor = monitor;
        }
        
        @Override
        public Representation toRepresentation(Object object) {
            if (object instanceof RequestData) {
                return super.toRepresentation(object);
            }
            
            //TODO: stream this!
            final List<RequestData> requests = new ArrayList();
            handleRequests(object, new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        requests.add(data);
                    }
                }, monitor);
            
            
            return super.toRepresentation(requests);
        }
        
        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(RequestResource.class, "");
            return cfg;
        }
        
        @Override
        protected String getTemplateName(Object data) {
            if (data instanceof RequestData) {
                return "request.html";
            }
            else {
                return "requests.html";
            }
        }
    }
    
    static class CSVFormat extends StreamDataFormat {

        Monitor monitor;
        String[] fields;
        
        // Regexp matching problematic characters which trigger quoted text mode.
        static Pattern escapeRequired = Pattern.compile("[\\,\\s\"]");
        
        protected CSVFormat(String[] fields, Monitor monitor) {
            super(new MediaType("application/csv"));
            
            this.fields = fields;
            this.monitor = monitor;
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
            
            //write out the header
            StringBuffer sb = new StringBuffer();
            for (String fld : fields) {
                sb.append(fld).append(",");
            }
            sb.setLength(sb.length()-1);
            w.write(sb.append("\n").toString());
            
            handleRequests(object, new RequestDataVisitor() {
                public void visit(RequestData data, Object... aggregates) {
                    try {
                        writeRequest(data, w);
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    } 
                 }
            }, monitor);
            
            w.flush();
        }
        
        void writeRequest(RequestData data, BufferedWriter w) throws IOException {
            StringBuffer sb = new StringBuffer();
            
            for (String fld : fields) {
                Object val = OwsUtils.get(data, fld);
                if (val instanceof Date) {
                    val = DateUtil.serializeDateTime((Date)val);
                }
                if (val != null) {
                    String string = val.toString();
                    Matcher match = escapeRequired.matcher(string);
                    if(match.find()){ // may need escaping, so escape
                        string=string.replaceAll("\"", "\"\"");// Double all double quotes to escape
                        sb.append("\"");
                        sb.append(string);
                        sb.append("\"");
                    }
                    else { // No need for escaping
                        sb.append(string);
                    }
                }
                sb.append(",");
            }
            sb.setLength(sb.length()-1); // Remove trailing comma
            sb.append("\n");
            w.write(sb.toString());
        }
        
        @Override
        protected Object read(InputStream in) throws IOException {
            return null;
        }
        
    }
    
    static class ZIPFormat extends StreamDataFormat {

        List<String> fields;
        Monitor monitor;
        CSVFormat csv;
        
        protected ZIPFormat(List<String> fields, CSVFormat csv, Monitor monitor) {
            super(MediaType.APPLICATION_ZIP);
            
            this.fields = fields;
            this.monitor = monitor;
            this.csv = csv; 
        }
        
        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            final ZipOutputStream zout = new ZipOutputStream(out);
            
            //create the csv entry
            zout.putNextEntry(new ZipEntry("requests.csv"));
            csv.write(object, zout);
            
            final boolean body = fields.contains("Body");
            final boolean error = fields.contains("Error");
            
            if (object instanceof Query) {
                monitor.query((Query)object, new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        try {
                            writeBodyAndError(data, zout, body, error, true);
                        } 
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            else if (object instanceof List) {
                for (RequestData data : (List<RequestData>)object) {
                    writeBodyAndError(data, zout, body, error, true);
                }
            }
            else {
                writeBodyAndError((RequestData) object, zout, body, error, false);
            }
            
            zout.flush();
            zout.close();
        }
        
        @Override
        protected Object read(InputStream in) throws IOException {
            return null;
        }
        
        void writeBodyAndError(RequestData data, ZipOutputStream zout,
            boolean body, boolean error, boolean postfix) throws IOException {
            
            long id = data.getId();
            if (body && data.getBody() != null) {
                //TODO: figure out the proper extension for the body file
                zout.putNextEntry(new ZipEntry(postfix ? "body_"+id+".txt" : "body.txt"));
                zout.write(data.getBody());
            }
            if (error && data.getError() != null) {
                zout.putNextEntry(new ZipEntry(postfix ? "error_"+id+".txt" : "error.txt"));
                data.getError().printStackTrace(new PrintStream(zout));
            }
        }
    }
    
    static class ExcelFormat extends StreamDataFormat {

        String[] fields;
        Monitor monitor;
        
        protected ExcelFormat(String[] fields, Monitor monitor) {
            super(MediaType.APPLICATION_EXCEL);
            this.fields = fields;
            this.monitor = monitor;
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return null;
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            //Create the workbook+sheet 
            HSSFWorkbook wb = new HSSFWorkbook();
            final HSSFSheet sheet = wb.createSheet("requests");
            
            //create the header
            HSSFRow header = sheet.createRow(0);
            for (int i = 0; i < fields.length; i++) {
                HSSFCell cell = header.createCell(i);
                cell.setCellValue(new HSSFRichTextString(fields[i]));
            }
            
            //write out the request
            handleRequests(object, new RequestDataVisitor() {
                int i = 1;
                public void visit(RequestData data, Object... aggregates) {
                    HSSFRow row = sheet.createRow(i++);
                    for (int j = 0; j < fields.length; j++) {
                        HSSFCell cell = row.createCell(j);
                        Object obj = OwsUtils.get(data, fields[j]);
                        if (obj == null) {
                            continue;
                        }
                        
                        if (obj instanceof Date) {
                            cell.setCellValue((Date)obj);
                        }
                        else if (obj instanceof Number) {
                            cell.setCellValue(((Number)obj).doubleValue());
                        }
                        else {
                            cell.setCellValue(new HSSFRichTextString(obj.toString()));
                        }
                    }
                }
            }, monitor);
            
            //write to output
            wb.write(out);
        }
    }
    
    /**
     * Converts a query object into one that can be used in the query string of a resource request.
     */
    public static String toQueryString(Query q) {
        StringBuffer sb = new StringBuffer("?");
        if (q.getFromDate() != null) {
            sb.append("from=").append(DATE_FORMAT.format(q.getFromDate())).append("&");
        }
        if (q.getToDate() != null) {
            sb.append("to=").append(DATE_FORMAT.format(q.getToDate())).append("&");
        }
        
        if (q.getFilter() != null) {
            FilterEncoder fe = new FilterEncoder();
            q.getFilter().accept(fe);
            sb.append("filter=").append(fe.toString());
        }
        return sb.toString();
        
    }
    
    //TODO: put these methods in a utility class
    public static String asString(Date d) {
        return DATE_FORMAT.format(d);
    }
    
    public static Date toDate(String s) throws ParseException {
        return DATE_FORMAT.parse(s);
    }
    
    static class FilterEncoder extends FilterVisitorSupport {
        StringBuffer sb = new StringBuffer();
        
        @Override
        protected void handleComposite(CompositeFilter f, String type) {
            if ("OR".equalsIgnoreCase(type)) {
                throw new IllegalArgumentException("Unable to encode OR filters");
            }
            
            And and = (And) f;
            for (Filter fil : and.getFilters()) {
                fil.accept(this);
                sb.append(";");
            }
            sb.setLength(sb.length()-1);
        }
        
        @Override
        protected void handleFilter(Filter f) {
            sb.append(f.getLeft()).append(":").append(f.getType().name()).append(":");
            if (f.getRight() instanceof Collection) {
                for (Object o : ((Collection)f.getRight())) {
                    sb.append(o).append(",");
                }
                sb.setLength(sb.length()-1);
            }
            else {
                sb.append(f.getRight());
            }
        }
        
        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
