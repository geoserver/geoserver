/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sf.json.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorTestData;
import org.geoserver.monitor.RequestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.type.DateUtil;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class RequestControllerTest extends GeoServerSystemTestSupport {

    private Monitor monitor;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data
    }

    @Before
    public void setupMonitorContents() throws ParseException {
        monitor = applicationContext.getBean(Monitor.class);
        monitor.getDAO().dispose();
        new MonitorTestData(monitor.getDAO(), false).setup();
    }

    @Test
    public void testGetAllHTML() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/monitor/requests.html");
        assertEquals(200, response.getStatus());
        // System.out.println(response.getContentAsString());

        // this check is's actual XML
        org.jsoup.nodes.Document document = Jsoup.parse(response.getContentAsString());
        // testing the first element
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/monitor/requests/1.html",
                document.select("a:contains(1)").attr("href"));
        assertEquals("RUNNING", document.select("tr.even > td").get(1).text());
    }

    @Test
    public void testGetHTMLById() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/monitor/requests/5.html");
        assertEquals(200, response.getStatus());
        // System.out.println(response.getContentAsString());

        // this check is's actual XML
        org.jsoup.nodes.Document document = Jsoup.parse(response.getContentAsString());
        // the structure is different, check the title
        assertEquals("Request 5", document.select("#content > h1 > span").text());
    }

    /**
     * This is undocumented/accidental behavior of 2.10.x (and previous) that actually got used by
     * other projects, adding it back preserving its original structure (pure XStream reflection)
     * even if it's really hard on the eyes....
     */
    @Test
    public void testGetXMLById() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/monitor/requests/5.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // print(dom);

        assertXpathEvaluatesTo("5", "/org.geoserver.monitor.RequestData/id", dom);
        assertXpathEvaluatesTo("RUNNING", "/org.geoserver.monitor.RequestData/status", dom);
    }

    /**
     * This is undocumented/accidental behavior of 2.10.x (and previous) that actually got used by
     * other projects, adding it back preserving its original structure (pure XStream reflection)
     * even if it's really hard on the eyes....
     */
    @Test
    public void testGetJSONById() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/monitor/requests/5.json");
        assertEquals(200, response.getStatus());

        JSONObject json = (JSONObject) json(response);
        // print(json);

        JSONObject data = json.getJSONObject("org.geoserver.monitor.RequestData");
        assertNotNull(data);
        assertEquals("5", data.getString("id"));
        assertEquals("RUNNING", data.getString("status"));
    }

    @Test
    public void testGetAllCSV() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime");
        assertEquals(200, response.getStatus());
        // System.out.println(response.getContentAsString());

        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                new ByteArrayInputStream(response.getContentAsByteArray())));
        String line = in.readLine();
        assertEquals("id,path,startTime", line);

        Iterator<RequestData> it = monitor.getDAO().getRequests().iterator();
        while ((line = in.readLine()) != null) {
            assertTrue(it.hasNext());

            RequestData data = it.next();
            String expected =
                    data.getId()
                            + ","
                            + data.getPath()
                            + ","
                            + DateUtil.serializeDateTime(data.getStartTime());
            assertEquals(expected, line);
        }

        assertFalse(it.hasNext());
    }

    @Test
    public void testDelete() throws Exception {
        // delete all
        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/monitor/requests");
        assertEquals(200, response.getStatus());

        assertEquals(0, monitor.getDAO().getRequests().size());
    }

    @Test
    public void testGetAllExcel() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.xls?fields=id;path;startTime");
        assertEquals(200, response.getStatus());

        HSSFWorkbook wb =
                new HSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
        HSSFSheet sheet = wb.getSheet("requests");

        Iterator<Row> rows = sheet.iterator();
        Iterator<RequestData> it = monitor.getDAO().getRequests().iterator();

        assertTrue(rows.hasNext());
        Row row = rows.next();
        assertEquals("id", row.getCell(0).getStringCellValue());
        assertEquals("path", row.getCell(1).getStringCellValue());
        assertEquals("startTime", row.getCell(2).getStringCellValue());

        while (rows.hasNext()) {
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

        // setup a single value in the DAO
        Date startTime = new Date();
        Throwable throwable = new Throwable();
        RequestData data = new RequestData();
        data.setId(12345);
        data.setPath("/foo");
        data.setStartTime(startTime);

        data.setBody("<foo></foo>".getBytes());
        data.setError(throwable);

        monitor.getDAO().dispose();
        monitor.getDAO().add(data);

        // running request
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests/12345.zip?fields=id;path;startTime;Error;Body");
        assertEquals(200, response.getStatus());

        ZipInputStream zin =
                new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()));
        ZipEntry entry = null;

        boolean requests = false;
        boolean body = false;
        boolean error = false;

        while ((entry = zin.getNextEntry()) != null) {
            if ("requests.csv".equals(entry.getName())) {
                requests = true;

                String expected =
                        "id,path,startTime\n12345,/foo," + DateUtil.serializeDateTime(startTime);
                assertEquals(expected, readEntry(zin));
            } else if ("body.txt".equals(entry.getName())) {
                body = true;
                assertEquals("<foo></foo>", readEntry(zin));
            } else if ("error.txt".equals(entry.getName())) {
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

    @Test
    public void testGetDateRange() throws Exception {
        // running request
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&from=2010-07-23T15:56:44&to=2010-07-23T16:16:44");
        assertEquals(200, response.getStatus());

        assertCoveredInOrder(response, 6, 5, 4);
    }

    @Test
    public void testGetDateRangeWithTimeZone() throws Exception {
        // running request
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&from=2010-07-23T15:56:44+0000&to=2010-07-23T16:16:44+0000");
        assertEquals(200, response.getStatus());

        assertCoveredInOrder(response, 6, 5, 4);
    }

    @Test
    public void testFilter() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&filter=path:EQ:/seven");
        assertEquals(200, response.getStatus());

        assertCoveredInOrder(response, 7);
    }

    @Test
    public void testFilterIn() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&filter=path:IN:/seven,/six,/five");
        assertEquals(200, response.getStatus());

        assertCovered(response, 5, 6, 7);
    }

    @Test
    public void testFilterStatus() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&filter=status:EQ:WAITING");
        assertEquals(200, response.getStatus());

        assertCovered(response, 2, 6);
    }

    @Test
    public void testSorting() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&order=path");
        assertEquals(200, response.getStatus());
        assertCoveredInOrder(response, 8, 5, 4, 9, 1, 7, 6, 10, 3, 2);

        response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&order=path;ASC");
        assertEquals(200, response.getStatus());
        assertCoveredInOrder(response, 8, 5, 4, 9, 1, 7, 6, 10, 3, 2);

        response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&order=path;DESC");
        assertEquals(200, response.getStatus());
        assertCoveredInOrder(response, 2, 3, 10, 6, 7, 1, 9, 4, 5, 8);
    }

    @Test
    public void testPaging() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&order=startTime&offset=5&count=2");
        assertEquals(200, response.getStatus());
        assertCoveredInOrder(response, 6, 7);
    }

    @Test
    public void testLive() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/monitor/requests.csv?fields=id;path;startTime&live=yes");
        assertEquals(200, response.getStatus());
        assertCovered(response, 1, 2, 5, 6, 9, 10);
    }

    private void assertCoveredInOrder(MockHttpServletResponse response, int... expectedIds)
            throws IOException {
        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                new ByteArrayInputStream(response.getContentAsByteArray())));
        // skip the header line
        String line = in.readLine();

        Iterator<Integer> it = Arrays.stream(expectedIds).iterator();
        while ((line = in.readLine()) != null) {
            assertTrue(it.hasNext());

            Integer id = it.next();
            assertThat(line, startsWith("" + id));
        }

        assertFalse(it.hasNext());
    }

    public static void assertCovered(MockHttpServletResponse response, int... expectedIds)
            throws IOException {
        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                new ByteArrayInputStream(response.getContentAsByteArray())));
        // skip the header line
        String line = in.readLine();

        Set<Integer> actualIds = new HashSet<>();
        while ((line = in.readLine()) != null) {
            String[] split = line.split("\\s*,\\s*");
            actualIds.add(Integer.parseInt(split[0]));
        }

        assertEquals(expectedIds.length, actualIds.size());
        for (int i = 0; i < expectedIds.length; i++) {
            assertThat(actualIds, hasItem(expectedIds[i]));
        }
    }
}
