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

public class OWSDetailsPanel extends OWSSummaryChartBasePanel {

    private static final long serialVersionUID = 8958233133832178632L;

    public OWSDetailsPanel(String id, Monitor monitor, String owsService) {
        super(id, monitor, owsService);
    }

    @Override
    protected String getChartTitle() {
        return owsService + " Request Summary";
    }

    @Override
    protected Map<String, Integer> gatherData(Monitor monitor) {
        DataGatherer g = new DataGatherer();
        monitor.query(
                new Query().properties("operation").filter("service", owsService, Comparison.EQ),
                g);

        return g.getData();
    }

    class DataGatherer implements RequestDataVisitor {

        HashMap<String, Integer> data = new HashMap<String, Integer>();

        public void visit(RequestData req, Object... aggregates) {
            String op = req.getOperation().toLowerCase();
            Integer count = data.get(op);
            if (count == null) {
                count = Integer.valueOf(1);
            } else {
                count = Integer.valueOf(count + 1);
            }

            data.put(op, count);
        }

        public HashMap<String, Integer> getData() {
            return data;
        }
    }
}
