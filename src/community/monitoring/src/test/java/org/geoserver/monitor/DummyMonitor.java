package org.geoserver.monitor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

class DummyMonitorDAO implements MonitorDAO {

    RequestData request;
    RequestData last;
    
    public RequestData init(RequestData data) {
        request = data;
        return data;
    }

    public void add(RequestData data) {
    }
    
    public void update(RequestData data) {
    }
    
    public void save(RequestData data) {
        last = request;
        request = null;
    }
    
    public List<RequestData> getRequests() {
        return Arrays.asList(request);
    }
    
    public List<RequestData> getRequests(MonitorQuery query) {
        return null;
    }
    
    public void getRequests(MonitorQuery query, RequestDataVisitor visitor) {
    }
    
    public RequestData getRequest(long id) {
        return null;
    }
    
    public List<RequestData> getHistory() {
        return getRequests();
    }

    public List<RequestData> getLive() {
        return getRequests();
    }

    public RequestData getLast() {
        return last;
    }

    public List<RequestData> getOwsRequests() {
        return null;
    }

    public List<RequestData> getOwsRequests(String service, String operation, String version) {
        return null;
    }
    
    public void clear() {
    }
    
    public void dispose() {
    }

   
}
