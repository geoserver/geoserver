package org.geoserver.monitor.web;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorQuery;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.MonitorQuery.Comparison;

public class OWSDetailsPanel extends OWSSummaryChartBasePanel {

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
        monitor.query(new MonitorQuery().properties("owsOperation")
            .filter("owsService", owsService, Comparison.EQ), g);
        
        return g.getData();
    }

    class DataGatherer implements RequestDataVisitor {

        HashMap<String,Integer> data = new HashMap();
        
        public void visit(RequestData req) {
            String op = req.getOwsOperation().toLowerCase();
            Integer count = data.get(op);
            if (count == null) {
                count = new Integer(1);
            }
            else {
                count = new Integer(count+1);
            }
            
            data.put(op, count);
        }
        
        public HashMap<String, Integer> getData() {
            return data;
        }
        
    }
}
