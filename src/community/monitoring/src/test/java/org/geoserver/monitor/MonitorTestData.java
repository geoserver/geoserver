package org.geoserver.monitor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.geoserver.monitor.RequestData.Status;
import org.geotools.util.Converters;

public class MonitorTestData {

    static DateFormat FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    
    MonitorDAO dao;
    public MonitorTestData(MonitorDAO dao) {
        this.dao = dao;
    }
    
    public void setup() throws ParseException {
        
        dao.save(dao.init(data(1, "/one", "2010-07-23T15:26:44", "2010-07-23T15:26:59", "RUNNING")));
        dao.save(dao.init(data(2, "/two", "2010-07-23T15:36:44", "2010-07-23T15:36:47", "WAITING")));
        dao.save(dao.init(data(3, "/three", "2010-07-23T15:46:44", "2010-07-23T15:46:52", "FINISHED")));
        dao.save(dao.init(data(4, "/four", "2010-07-23T15:56:44", "2010-07-23T15:56:48", "FAILED")));
        dao.save(dao.init(data(5, "/five", "2010-07-23T16:06:44", "2010-07-23T16:06:45", "RUNNING")));
        dao.save(dao.init(data(6, "/six", "2010-07-23T16:16:44", "2010-07-23T16:16:53", "WAITING")));
        dao.save(dao.init(data(7, "/seven", "2010-07-23T16:26:44", "2010-07-23T16:26:47", "FINISHED")));
        dao.save(dao.init(data(8, "/eight", "2010-07-23T16:36:44", "2010-07-23T16:36:46", "FAILED")));
        dao.save(dao.init(data(9, "/nine", "2010-07-23T16:46:44", "2010-07-23T16:46:53", "CANCELLING")));
        dao.save(dao.init(data(10, "/ten", "2010-07-23T16:56:44", "2010-07-23T16:56:47", "RUNNING")));
    }
    
    RequestData data(long id, String path, String start, String end, String status) throws ParseException {
        RequestData data = new RequestData();
        data.setPath(path);
        //data.setStartTime(FORMAT.parse(start));
        //data.setEndTime(FORMAT.parse(end));
        data.setStartTime(toDate(start));
        data.setEndTime(toDate(end));
        data.setStatus(Status.valueOf(status));
        return data;
    }
    
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
            assertTrue(ids.contains(new Long(id[i])));
        }
    }
    
    public static void assertCoveredInOrder(List<RequestData> datas, int... id) {
        assertEquals(id.length, datas.size());
        for (int i = 0; i < id.length; i++) {
            assertEquals(id[i], datas.get(i).getId());
        }
    }
}
