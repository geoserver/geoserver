package org.geoserver.wps.web;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.wps.executor.ExecutionStatus;

/**
 * Shows the status of currently running processes
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessStatusPage extends GeoServerSecuredPage {

    private GeoServerTablePanel<ExecutionStatus> table;

    public ProcessStatusPage() {
        ProcessStatusProvider provider = new ProcessStatusProvider();

        table = new GeoServerTablePanel<ExecutionStatus>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<ExecutionStatus> property) {
                // have the base class create a label for us
                return null;
            }

        };
        table.setOutputMarkupId(true);
        add(table);

    }

}
