/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.geoserver.monitor.MonitorTestData.assertCovered;
import static org.geoserver.monitor.MonitorTestData.assertCoveredInOrder;
import static org.geoserver.monitor.MonitorTestData.toDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData.Status;
import org.junit.Test;

public abstract class MonitorDAOTestSupport {

    protected static MonitorTestData testData;
    protected static MonitorDAO dao;

    protected static void setUpData() throws Exception {
        testData = new MonitorTestData(dao);
        testData.setup();
    }

    @Test
    public void testUpdate() throws Exception {
        RequestData data = dao.getRequest(1);
        data.setPath("/one_updated");
        dao.update(data);

        data = dao.getRequest(1);
        assertEquals("/one_updated", data.getPath());

        data.getResources().add("one_layer");
        dao.update(data);

        data = dao.getRequest(1);
        assertEquals(1, data.getResources().size());

        assertEquals("one_layer", data.getResources().get(0));
    }

    @Test
    public void testGetRequests() throws Exception {
        List<RequestData> requests = dao.getRequests();
        assertEquals(testData.getData().size(), requests.size());
        // assertCovered(requests, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertCovered(requests, range(1, 20));
    }

    int[] range(int low, int high) {
        int[] nums = new int[high - low + 1];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = low + i;
        }
        return nums;
    }

    @Test
    public void testGetRequestsVisitor() throws Exception {
        final List<RequestData> datas = new ArrayList();
        dao.getRequests(
                new Query().filter("path", "/seven", Comparison.EQ),
                new RequestDataVisitor() {

                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                    }
                });

        assertCoveredInOrder(datas, 7);
    }

    @Test
    public void testGetRequestById() throws Exception {
        assertTrue(dao.getRequest(8) != null);
        assertEquals("/eight", dao.getRequest(8).getPath());
    }

    @Test
    public void testGetRequestsSorted() throws Exception {
        assertCoveredInOrder(
                dao.getRequests(
                        new Query().filter("id", 11l, Comparison.LT).sort("path", SortOrder.ASC)),
                8,
                5,
                4,
                9,
                1,
                7,
                6,
                10,
                3,
                2);
    }

    @Test
    public void testGetRequestsBetween() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .between(
                                        toDate("2010-07-23T15:55:00"),
                                        toDate("2010-07-23T16:17:00")));

        assertCoveredInOrder(datas, 6, 5, 4);
    }

    @Test
    public void testGetRequestsBetween2() throws Exception {
        // test that the query is inclusive, and test sorting
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .between(
                                        toDate("2010-07-23T15:56:44"),
                                        toDate("2010-07-23T16:16:44"))
                                .sort("startTime", SortOrder.ASC));

        assertCoveredInOrder(datas, 4, 5, 6);
    }

    @Test
    public void testGetRequestsPaged() throws Exception {
        List<RequestData> datas =
                dao.getRequests(new Query().page(5l, 2l).sort("startTime", SortOrder.ASC));

        assertCoveredInOrder(datas, 6, 7);
    }

    @Test
    public void testGetRequestsFilter() throws Exception {
        assertCoveredInOrder(
                dao.getRequests(new Query().filter("path", "/seven", Comparison.EQ)), 7);
    }

    @Test
    public void testGetRequestsFilterNull() throws Exception {
        assertEquals(0, dao.getRequests(new Query().filter("path", null, Comparison.EQ)).size());
        assertEquals(
                testData.getData().size(),
                dao.getRequests(new Query().filter("path", null, Comparison.NEQ)).size());
    }

    @Test
    public void testGetRequestsFilterIN() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query().filter("path", Arrays.asList("/two", "/seven"), Comparison.IN));
        assertCovered(datas, 2, 7);
    }

    @Test
    public void testGetRequestsFilterIN2() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .filter(
                                        "status",
                                        Arrays.asList(Status.RUNNING, Status.WAITING),
                                        Comparison.IN));
        assertCovered(datas, 1, 2, 5, 6, 10, 11, 12, 15, 16, 20);
    }

    @Test
    public void testGetCount() throws Exception {
        assertEquals(4, dao.getCount(new Query().filter("path", "/foo", Comparison.EQ)));
    }

    @Test
    public void testGetIterator() throws Exception {
        Iterator<RequestData> it =
                dao.getIterator(
                        new Query().filter("path", Arrays.asList("/two", "/seven"), Comparison.IN));

        assertTrue(it.hasNext());
        RequestData data = it.next();
        assertEquals("/two", data.getPath());

        assertTrue(it.hasNext());
        data = it.next();
        assertEquals("/seven", data.getPath());

        assertFalse(it.hasNext());
    }
}
