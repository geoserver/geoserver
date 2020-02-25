/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.Iterator;
import java.util.List;

public interface MonitorDAO {

    String getName();

    void init(MonitorConfig config);

    RequestData init(RequestData data);

    void add(RequestData data);

    void update(RequestData data);

    void save(RequestData data);

    RequestData getRequest(long id);

    List<RequestData> getRequests();

    List<RequestData> getRequests(Query query);

    void getRequests(Query query, RequestDataVisitor visitor);

    long getCount(Query query);

    Iterator<RequestData> getIterator(Query query);

    //    ResourceData getLayer(String name);
    //
    //    List<ResourceData> getLayers();
    //
    //    List<ResourceData> getLayers(MonitorQuery query);
    //
    //    void getLayers(MonitorQuery query, MonitorVisitor<ResourceData> visitor);

    List<RequestData> getOwsRequests();

    List<RequestData> getOwsRequests(String service, String operation, String version);

    void clear();

    void dispose();
}
