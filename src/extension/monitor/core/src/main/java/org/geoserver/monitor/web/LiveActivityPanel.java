/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData.Status;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class LiveActivityPanel extends Panel {

    public LiveActivityPanel(String id) {
        super(id);
        
        GeoServerTablePanel<RequestData> requests = new GeoServerTablePanel<RequestData>("requests",
                new LiveRequestDataProvider()) {
            @Override
            protected Component getComponentForProperty(String id, IModel<RequestData> itemModel,
                    Property<RequestData> property) {
                Object prop = ((BeanProperty<RequestData>) property)
                        .getPropertyValue((RequestData) itemModel.getObject());

                String value = prop != null ? prop.toString() : "";
                return new Label(id, value);
            }
        };
        add(requests);
    }

    
    static class LiveRequestDataProvider extends GeoServerDataProvider<RequestData> {

        static final Property<RequestData> ID = new BeanProperty("id", "id");
        static final Property<RequestData> PATH = new BeanProperty("path", "path");
        static final Property<RequestData> STATUS = new BeanProperty("status", "status");
        
        @Override
        protected List<RequestData> getItems() {
            MonitorDAO dao = getApplication().getBeanOfType(Monitor.class).getDAO();
            Query q = new Query().filter("status", 
                Arrays.asList(Status.RUNNING, Status.WAITING, Status.CANCELLING), Comparison.IN);
            
            return dao.getRequests(q);
        }

        @Override
        protected List<Property<RequestData>> getProperties() {
            return Arrays.asList(ID, PATH, STATUS); 
        }
     
    }
    
}
