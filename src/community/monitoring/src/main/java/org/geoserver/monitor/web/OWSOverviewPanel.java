package org.geoserver.monitor.web;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorQuery;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.MonitorQuery.Comparison;

public class OWSOverviewPanel extends OWSSummaryChartBasePanel {

    public OWSOverviewPanel(String id, Monitor monitor, String owsService) {
        super(id, monitor, owsService);
    }

    @Override
    protected String getChartTitle() {
        return "OWS Request Summary";
    }
    
    @Override
    protected Map<String,Integer> gatherData(Monitor monitor) {
        MonitorQuery q = new MonitorQuery();
        q.properties("owsService").filter("owsService", null, Comparison.NEQ);
        
        DataGatherer g = new DataGatherer();
        monitor.query(q, g);
        
        return g.getData();
    }
    
    class DataGatherer implements RequestDataVisitor {

        HashMap<String,Integer> data = new HashMap();
        
        public void visit(RequestData req) {
            String service = req.getOwsService();
            Integer count = data.get(service);
            if (count == null) {
                count = new Integer(1);
            }
            else {
                count = new Integer(count+1);
            }
            
            data.put(service, count);
        }
        
        public HashMap<String, Integer> getData() {
            return data;
        }
        
    }
}
