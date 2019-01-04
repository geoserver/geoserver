/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class DummyMonitorDAO implements MonitorDAO {

    RequestData request;
    RequestData last;

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public void init(MonitorConfig config) {}

    public RequestData init(RequestData data) {
        request = data;
        return data;
    }

    public void add(RequestData data) {}

    public void update(RequestData data) {}

    public void save(RequestData data) {
        last = request;
        request = null;
    }

    public List<RequestData> getRequests() {
        return Arrays.asList(request);
    }

    public List<RequestData> getRequests(Query query) {
        return null;
    }

    public void getRequests(Query query, RequestDataVisitor visitor) {}

    public RequestData getRequest(long id) {
        return null;
    }

    public long getCount(Query query) {
        return 0;
    }

    public Iterator<RequestData> getIterator(Query query) {
        // TODO Auto-generated method stub
        return null;
    }

    public ResourceData getLayer(String name) {
        return null;
    }

    public List<ResourceData> getLayers() {
        return null;
    }

    public List<ResourceData> getLayers(Query query) {
        return null;
    }

    public void getLayers(Query query, MonitorVisitor<ResourceData> visitor) {}

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

    public void clear() {}

    public void dispose() {}
}
