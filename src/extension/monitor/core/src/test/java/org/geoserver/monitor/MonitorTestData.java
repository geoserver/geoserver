/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.geoserver.monitor.RequestData.Status;
import org.geotools.util.Converters;

public class MonitorTestData {

    static DateFormat FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    MonitorDAO dao;
    List<RequestData> data;
    boolean extended;

    public MonitorTestData(MonitorDAO dao) {
        this(dao, true);
    }

    public MonitorTestData(MonitorDAO dao, boolean extended) {
        this.dao = dao;
        this.data = new ArrayList();
        this.extended = extended;
    }

    public List<RequestData> getData() {
        return data;
    }

    public void setup() throws ParseException {

        data.add(data(1, "/one", "2010-07-23T15:26:44", "2010-07-23T15:26:59", "RUNNING"));
        data.add(data(2, "/two", "2010-07-23T15:36:44", "2010-07-23T15:36:47", "WAITING"));
        data.add(data(3, "/three", "2010-07-23T15:46:44", "2010-07-23T15:46:52", "FINISHED"));
        data.add(data(4, "/four", "2010-07-23T15:56:44", "2010-07-23T15:56:48", "FAILED"));
        data.add(data(5, "/five", "2010-07-23T16:06:44", "2010-07-23T16:06:45", "RUNNING"));
        data.add(data(6, "/six", "2010-07-23T16:16:44", "2010-07-23T16:16:53", "WAITING"));
        data.add(data(7, "/seven", "2010-07-23T16:26:44", "2010-07-23T16:26:47", "FINISHED"));
        data.add(data(8, "/eight", "2010-07-23T16:36:44", "2010-07-23T16:36:46", "FAILED"));
        data.add(data(9, "/nine", "2010-07-23T16:46:44", "2010-07-23T16:46:53", "CANCELLING"));
        data.add(data(10, "/ten", "2010-07-23T16:56:44", "2010-07-23T16:56:47", "RUNNING"));

        if (extended) {
            data.add(
                    data(
                            11,
                            "/foo",
                            "2010-08-23T15:26:44",
                            "2010-08-23T15:26:59",
                            "RUNNING",
                            "foo",
                            "x",
                            "widgets"));
            data.add(
                    data(
                            12,
                            "/bar",
                            "2010-08-23T15:36:44",
                            "2010-08-23T15:36:47",
                            "WAITING",
                            "bar",
                            "y",
                            "things"));
            ;
            data.add(
                    data(
                            13,
                            "/baz",
                            "2010-08-23T15:46:44",
                            "2010-08-23T15:46:52",
                            "FINISHED",
                            "baz",
                            "x",
                            "stuff"));
            data.add(
                    data(
                            14,
                            "/bam",
                            "2010-08-23T15:56:44",
                            "2010-08-23T15:56:48",
                            "FAILED",
                            "bam",
                            "x",
                            "widgets",
                            "things"));
            data.add(
                    data(
                            15,
                            "/foo",
                            "2010-08-23T16:06:44",
                            "2010-08-23T16:06:45",
                            "RUNNING",
                            "foo",
                            "x",
                            "things",
                            "stuff"));
            data.add(
                    data(
                            16,
                            "/foo",
                            "2010-08-23T16:16:44",
                            "2010-08-23T16:16:53",
                            "WAITING",
                            "foo",
                            "x",
                            "stuff"));
            data.add(
                    data(
                            17,
                            "/bar",
                            "2010-08-23T16:26:44",
                            "2010-08-23T16:26:47",
                            "FINISHED",
                            "bar",
                            "z",
                            "things",
                            "stuff"));
            data.add(
                    data(
                            18,
                            "/bam",
                            "2010-08-23T16:36:44",
                            "2010-08-23T16:36:46",
                            "FAILED",
                            "bam",
                            "y",
                            "widgets"));
            data.add(
                    data(
                            19,
                            "/bam",
                            "2010-08-23T16:46:44",
                            "2010-08-23T16:46:53",
                            "CANCELLING",
                            "bam",
                            "y",
                            "stuff"));
            data.add(
                    data(
                            20,
                            "/foo",
                            "2010-08-23T16:56:44",
                            "2010-08-23T16:56:47",
                            "RUNNING",
                            "foo",
                            "x",
                            "things"));
        }

        // subclass hook
        addTestData(data);

        for (RequestData r : data) {
            dao.save(dao.init(r));
        }
    }

    protected RequestData data(long id, String path, String start, String end, String status)
            throws ParseException {
        RequestData data = new RequestData();
        data.setPath(path);
        data.setStartTime(toDate(start));
        data.setEndTime(toDate(end));
        data.setStatus(Status.valueOf(status));
        return data;
    }

    protected RequestData data(
            long id,
            String path,
            String start,
            String end,
            String status,
            String owsService,
            String owsOperation,
            String... layers)
            throws ParseException {
        RequestData data = data(id, path, start, end, status);
        data.setService(owsService);
        data.setOperation(owsOperation);
        data.setResources(Arrays.asList(layers));

        return data;
    }

    protected void addTestData(List<RequestData> datas) throws ParseException {}

    public static Date toDate(String s) {
        return Converters.convert(s, Date.class);
    }

    public static void assertCovered(List<RequestData> datas, int... id) {
        assertEquals(id.length, datas.size());
        HashSet<Long> ids = new HashSet();
        for (RequestData data : datas) {
            ids.add(data.getId());
        }

        for (int i = 0; i < id.length; i++) {
            assertTrue(ids.contains(Long.valueOf(id[i])));
        }
    }

    public static void assertCoveredInOrder(List<RequestData> datas, int... id) {
        assertEquals(id.length, datas.size());
        for (int i = 0; i < id.length; i++) {
            assertEquals(id[i], datas.get(i).getId());
        }
    }
}
