/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import static org.geoserver.monitor.MonitorTestData.assertCovered;
import static org.geoserver.monitor.MonitorTestData.assertCoveredInOrder;
import static org.geoserver.monitor.MonitorTestData.toDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.geoserver.monitor.MemoryMonitorDAO;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.MonitorTestData;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.rest.PageInfo;
import org.geotools.feature.type.DateUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class RequestResourceTest {

    static Monitor monitor;
    static MonitorTestData testData;
    RequestResource resource;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        monitor = new Monitor(new MemoryMonitorDAO());
        testData = new MonitorTestData(monitor.getDAO(), false);
        testData.setup();
    }
    
    @Before
    public void setUp() throws Exception {
        resource = new RequestResource(monitor);
    }
    
    @Test
    public void testGetAll() throws Exception {
        Request req = new Request();
        Response res = new Response(req);
        
        resource.init(null, req, res);
        Query q = (Query) resource.handleObjectGet();
        
        assertEquals(monitor.getDAO().getRequests().size(), 
            monitor.getDAO().getRequests(q).size());
    }
    
    @Test
    public void testGetAllHTML() throws Exception {
        Request req = new Request();
        PageInfo page = new PageInfo();
        page.setBasePath("foo");
        page.setPagePath("bar");
        page.setBaseURL("baz");
        req.getAttributes().put(PageInfo.KEY, page);
        Response res = new Response(req);
        
        RequestResource.HTMLFormat format = 
            new RequestResource.HTMLFormat(req, res, resource, monitor);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.toRepresentation(monitor.getDAO().getRequests()).write(out);
        
        assertTrue(new String(out.toByteArray()).startsWith("<html>"));
    }
    
    @Test
    public void testGetAllCSV() throws Exception {
        RequestResource.CSVFormat format = new RequestResource.CSVFormat(
            new String[]{"id", "path", "startTime"}, monitor);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.toRepresentation(monitor.getDAO().getRequests()).write(out);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(out.toByteArray())));
        String line = in.readLine();
        assertEquals("id,path,startTime", line);
        
        Iterator<RequestData> it = monitor.getDAO().getRequests().iterator();
        while((line = in.readLine()) != null) {
            assertTrue(it.hasNext());
            
            RequestData data = it.next();
            String expected = data.getId() + "," + data.getPath() + "," + 
                DateUtil.serializeDateTime(data.getStartTime());
            assertEquals(expected, line);
        }
        
        assertFalse(it.hasNext());
    }
    
    @Test
    public void testGetAllExcel() throws Exception {
        RequestResource.ExcelFormat format = new RequestResource.ExcelFormat(
            new String[]{"id", "path", "startTime"}, monitor);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.toRepresentation(monitor.getDAO().getRequests()).write(out);
        
        HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
        HSSFSheet sheet = wb.getSheet("requests");
        
        Iterator<Row> rows = sheet.iterator();
        Iterator<RequestData> it = monitor.getDAO().getRequests().iterator();
        
        assertTrue(rows.hasNext());
        Row row = rows.next();
        assertEquals("id", row.getCell(0).getStringCellValue());
        assertEquals("path", row.getCell(1).getStringCellValue());
        assertEquals("startTime", row.getCell(2).getStringCellValue());
       
        while(rows.hasNext()) {
            row = rows.next();
            
            assertTrue(it.hasNext());
            RequestData data = it.next();
            
            assertEquals((double) data.getId(), row.getCell(0).getNumericCellValue(), 0.1);
            assertEquals(data.getPath(), row.getCell(1).getStringCellValue());
            assertEquals(data.getStartTime(), row.getCell(2).getDateCellValue());
        }
        
        assertFalse(it.hasNext());
    }
    
    @Test
    public void testGetZIP() throws Exception {
        RequestResource.CSVFormat csv = new RequestResource.CSVFormat(
            new String[]{"id", "path", "startTime"}, monitor);
        RequestResource.ZIPFormat zip = new RequestResource.ZIPFormat(
            Arrays.asList("id", "path", "startTime", "Error", "Body"), csv, monitor);
        
        Date startTime  = new Date();
        Throwable throwable = new Throwable();
        RequestData data = new RequestData();
        data.setId(12345);
        data.setPath("/foo");
        data.setStartTime(startTime);
        
        data.setBody("<foo></foo>".getBytes());
        data.setError(throwable);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        zip.toRepresentation(data).write(out);
        
        ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
        ZipEntry entry = null;
        
        boolean requests = false;
        boolean body = false;
        boolean error = false;
        
        while((entry = zin.getNextEntry()) != null) {
            if ("requests.csv".equals(entry.getName())) {
                requests = true;
                
                String expected = "id,path,startTime\n12345,/foo," + DateUtil.serializeDateTime(startTime);
                assertEquals(expected, readEntry(zin));
            }
            else if ("body.txt".equals(entry.getName())) {
                body = true;
                assertEquals("<foo></foo>", readEntry(zin));
            }
            else if ("error.txt".equals(entry.getName())) {
                error = true;
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                PrintStream stream = new PrintStream(bout);
                throwable.printStackTrace(stream);
                stream.flush();
                
                assertEquals(new String(bout.toByteArray()).trim(), readEntry(zin));
            }
        }
        
        assertTrue(requests);
        assertTrue(body);
        assertTrue(error);
    }
    
    String readEntry(ZipInputStream zin) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = -1;
        while ((n = zin.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        
        return new String(out.toByteArray()).trim();
    }

    public void testGetAllCSVQuery() throws Exception {
        RequestResource.CSVFormat format = new RequestResource.CSVFormat(
                new String[]{"id", "path", "startTime"}, monitor);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.toRepresentation(new Query()).write(out);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(out.toByteArray())));
        String line = in.readLine();
        assertEquals("id,path,startTime", line);
        
        Iterator<RequestData> it = monitor.getDAO().getRequests().iterator();
        while((line = in.readLine()) != null) {
            assertTrue(it.hasNext());
            
            RequestData data = it.next();
            String expected = data.getId() + "," + data.getPath() + "," + 
                DateUtil.serializeDateTime(data.getStartTime());
            assertEquals(expected, line);
        }
        
        assertFalse(it.hasNext());
    }
    
    @Test
    public void testGetById() throws Exception {
        Request req = new Request();
        req.getAttributes().put("request", 2);
        
        Response res = new Response(req);
        
        resource.init(null, req, res);
        RequestData data = (RequestData) resource.handleObjectGet();
        assertEquals("/two", data.getPath());
    }
    
    @Test
    public void testGetByIdHTML() throws Exception {
        Request req = new Request();
        Response res = new Response(req);
        
        RequestResource.HTMLFormat format = 
            new RequestResource.HTMLFormat(req, res, resource, monitor);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.toRepresentation(monitor.getDAO().getRequest(2)).write(out);
        
        assertTrue(new String(out.toByteArray()).startsWith("<html>"));
    }
    
    @Test
    public void testGetDateRange() throws Exception {
        Request req = new Request();
        setKVP(req, "from", "2010-07-23T15:56:44", "to", "2010-07-23T16:16:44");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query) resource.handleObjectGet();
        List<RequestData> datas = (List<RequestData>) monitor.getDAO().getRequests(q);
        
        assertCoveredInOrder(datas, 6, 5, 4);
    }
    
    @Test
    public void testGetDateRangeWithTimeZone() throws Exception {
        Calendar c = Calendar.getInstance();
        c.setTime(toDate("2010-07-23T15:56:44"));
        
        long off = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
        off = off / 1000 / 60 / 60;
        
        String z = Math.abs(off) < 10 ? "0" + Math.abs(off) : ""+Math.abs(off);
        z += "00";
        z = off < 0 ? "-" + z : z;
        
        Request req = new Request();
        setKVP(req, "from", "2010-07-23T15:56:44+0000", "to", "2010-07-23T16:16:44+0000");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query) resource.handleObjectGet();
        List<RequestData> datas = (List<RequestData>) monitor.getDAO().getRequests(q);
        
        assertCoveredInOrder(datas, 6, 5, 4);
    }
    
    @Test
    public void testFilter() throws Exception {
        Request req = new Request();
        setKVP(req, "filter", "path:EQ:/seven");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query) resource.handleObjectGet();
        List<RequestData> datas = (List<RequestData>) monitor.getDAO().getRequests(q);
        
        assertCoveredInOrder(datas, 7);
    }
    
    @Test
    public void testFilterIn() throws Exception {
        Request req = new Request();
        setKVP(req, "filter", "path:IN:/seven,/six,/five");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query) resource.handleObjectGet();
        List<RequestData> datas = (List<RequestData>) monitor.getDAO().getRequests(q);
        
        assertCovered(datas, 5, 6, 7);
    }
    
    @Test
    public void testFilterStatus() throws Exception {
        Request req = new Request();
        setKVP(req, "filter", "status:EQ:WAITING");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query) resource.handleObjectGet();
        List<RequestData> datas = (List<RequestData>) monitor.getDAO().getRequests(q);
        
        assertCovered(datas, 2,6);
    }
    
    @Test
    public void testSorting() throws Exception {
        Request req = new Request();
        setKVP(req, "order", "path");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query)resource.handleObjectGet();
        List<RequestData> datas = monitor.getDAO().getRequests(q); 
        assertCoveredInOrder(datas, 8, 5, 4, 9, 1, 7, 6, 10, 3, 2);
        
        setKVP(req, "order", "path;ASC");
        res = new Response(req);
        resource.init(null, req, res);
        
        q = (Query)resource.handleObjectGet();
        datas = monitor.getDAO().getRequests(q);
        assertCoveredInOrder(datas, 8, 5, 4, 9, 1, 7, 6, 10, 3, 2);
        
        setKVP(req, "order", "path;DESC");
        res = new Response(req);
        resource.init(null, req, res);
        
        q = (Query)resource.handleObjectGet();
        datas = monitor.getDAO().getRequests(q);
        assertCoveredInOrder(datas, 2, 3, 10, 6, 7, 1, 9, 4, 5, 8);
    }
    
    @Test
    public void testPaging() throws Exception {
        Request req = new Request();
        setKVP(req, "order", "startTime", "offset", "5", "count", "2");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query)resource.handleObjectGet();
        List<RequestData> datas = monitor.getDAO().getRequests(q);
         
        assertCoveredInOrder(datas, 6, 7);
    }
    
    @Test
    public void testLive() throws Exception {
        Request req = new Request();
        setKVP(req, "live", "yes");
        
        Response res = new Response(req);
        resource.init(null, req, res);
        
        Query q = (Query) resource.handleObjectGet();;
        List<RequestData> datas = monitor.getDAO().getRequests(q); 
        
        assertCovered(datas, 1, 2, 5, 6, 9, 10);
    }
    
    void setKVP(Request req, String... kvp) {
        Reference ref = new Reference();
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < kvp.length; i += 2) {
            sb.append(kvp[i]).append("=").append(kvp[i+1]).append("&");
        }
        sb.setLength(sb.length()-1);
        ref.setQuery(sb.toString());
        
        req.setResourceRef(ref);
    }
    
    @Test
    public void testToQueryString() throws Exception {
        Date from = toDate("2010-07-23T15:56:44");
        Date to = toDate("2010-07-23T16:16:44");
        
        Query q = new Query().between(from, to);
        q.filter("service", "WFS", Comparison.EQ).and("status", "RUNNING", Comparison.EQ)
            .and("path", Arrays.asList("/foo", "/bar"), Comparison.IN);
        
        String qs = RequestResource.toQueryString(q);
        assertEquals('?', qs.charAt(0));
        qs = qs.substring(1);
        
        Map<String,String> kvp = new HashMap();
        for (String s : qs.split("&")) {
            kvp.put(s.split("=")[0], s.split("=")[1]);
        }
        
        assertEquals(from, RequestResource.DATE_FORMAT.parse(kvp.get("from")));
        assertEquals(to, RequestResource.DATE_FORMAT.parse(kvp.get("to")));
        
        assertEquals("service:EQ:WFS;status:EQ:RUNNING;path:IN:/foo,/bar", kvp.get("filter"));
    }
}
