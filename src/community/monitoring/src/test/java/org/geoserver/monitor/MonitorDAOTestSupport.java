package org.geoserver.monitor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.geoserver.monitor.MonitorTestData.assertCovered;
import static org.geoserver.monitor.MonitorTestData.assertCoveredInOrder;
import static org.geoserver.monitor.MonitorTestData.toDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.monitor.MonitorQuery.Comparison;
import org.geoserver.monitor.MonitorQuery.SortOrder;
import org.geoserver.monitor.RequestData.Status;
import org.junit.Test;

public abstract class MonitorDAOTestSupport{

    protected static MonitorDAO dao;
    
    protected static void setUpData() throws Exception {
        new MonitorTestData(dao).setup();
    }
    
    @Test
    public void testUpdate() throws Exception {
        RequestData data = dao.getRequest(1);
        data.setPath("/one_updated");
        dao.update(data);
        
        data = dao.getRequest(1);
        assertEquals("/one_updated", data.getPath());
    }
    
    @Test
    public void testGetRequests() throws Exception {
        List<RequestData> requests = dao.getRequests();
        assertEquals(10, requests.size());
        assertCovered(requests, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
    
    
    public void testGetRequestsVisitor() throws Exception {
        final List<RequestData> datas = new ArrayList();
        dao.getRequests(new MonitorQuery().filter("path", "/seven", Comparison.EQ), 
            new RequestDataVisitor() {
                
                public void visit(RequestData data) {
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
        assertCoveredInOrder(dao.getRequests(new MonitorQuery().sort("path", SortOrder.ASC)), 
            8, 5, 4, 9, 1, 7, 6, 10, 3, 2);
    }
    
    @Test
    public void testGetRequestsBetween() throws Exception {
        List<RequestData> datas = dao.getRequests(new MonitorQuery().between(
            toDate("2010-07-23T15:55:00"), toDate("2010-07-23T16:17:00")));

        assertCoveredInOrder(datas, 6, 5, 4);
    }
    
    @Test
    public void testGetRequestsBetween2() throws Exception {
        //test that the query is inclusive, and test sorting
        List<RequestData> datas = dao.getRequests(new MonitorQuery().between(
            toDate("2010-07-23T15:56:44"), toDate("2010-07-23T16:16:44"))
            .sort("startTime", SortOrder.ASC));
        
        assertCoveredInOrder(datas, 4, 5, 6);
    }
 
    @Test
    public void testGetRequestsPaged() throws Exception {
        List<RequestData> datas = dao.getRequests(
            new MonitorQuery().page(5l, 2l).sort("startTime", SortOrder.ASC));
        
        assertCoveredInOrder(datas, 6, 7);
    }
    
    @Test
    public void testGetRequestsFilter() throws Exception {
        assertCoveredInOrder(
            dao.getRequests(new MonitorQuery().filter("path", "/seven", Comparison.EQ)), 7);
    }
    
    @Test
    public void testGetRequestsFilterNull() throws Exception {
        assertEquals(0, dao.getRequests(new MonitorQuery().filter("path", null, Comparison.EQ)).size()); 
        assertEquals(10, dao.getRequests(new MonitorQuery().filter("path", null, Comparison.NEQ)).size());
    }
    
    @Test
    public void testGetRequestsFilterIN() throws Exception {
        List<RequestData> datas = dao.getRequests(
            new MonitorQuery().filter("path", Arrays.asList("/two", "/seven"), Comparison.IN ));
        assertCovered(datas, 2, 7);
    }
    
    @Test
    public void testGetRequestsFilterIN2() throws Exception {
        List<RequestData> datas = dao.getRequests( new MonitorQuery().filter(
            "status", Arrays.asList(Status.RUNNING, Status.WAITING), Comparison.IN ));
        assertCovered(datas, 1, 2, 5, 6, 10);
    }
}
