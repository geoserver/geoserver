/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;

public class OWSOverviewPanel extends OWSSummaryChartBasePanel {

    private static final long serialVersionUID = 103552954574455043L;

    public OWSOverviewPanel(String id, Monitor monitor, String owsService) {
        super(id, monitor, owsService);
    }

    @Override
    protected String getChartTitle() {
        return "OWS Request Summary";
    }

    @Override
    protected Map<String, Integer> gatherData(Monitor monitor) {
        Query q = new Query();
        q.properties("service").filter("service", null, Comparison.NEQ);

        DataGatherer g = new DataGatherer();
        monitor.query(q, g);

        return g.getData();
    }

    class DataGatherer implements RequestDataVisitor {

        HashMap<String, Integer> data = new HashMap<String, Integer>();

        public void visit(RequestData req, Object... aggregates) {
            String service = req.getService();
            Integer count = data.get(service);
            if (count == null) {
                count = Integer.valueOf(1);
            } else {
                count = Integer.valueOf(count + 1);
            }

            data.put(service, count);
        }

        public HashMap<String, Integer> getData() {
            return data;
        }
    }
}
