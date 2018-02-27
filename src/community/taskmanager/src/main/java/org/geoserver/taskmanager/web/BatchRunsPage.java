/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.web.model.BatchRunsModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class BatchRunsPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = -5111795911981486778L;

    private IModel<Batch> batchModel;
    
    private GeoServerTablePanel<BatchRun> runsPanel;
        
    public BatchRunsPage(IModel<Batch> batchModel, Page parentPage) {
        this.batchModel = batchModel;
        setReturnPage(parentPage);
    }
    
    @Override
    public void onInitialize() {
        super.onInitialize();
        
        add(new SimpleAjaxLink<String>("nameLink", new PropertyModel<String>(batchModel, "fullName")) {
            private static final long serialVersionUID = -9184383036056499856L;
            
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new BatchPage(batchModel, getPage()));
            }
        });
        
        //the tasks panel
        add(runsPanel = runPanel());
        runsPanel.setSelectable(false);

        add(new AjaxLink<Object>("close") {
            private static final long serialVersionUID = -6892944747517089296L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn();
            }            
        });
        
    }
    
    
    protected GeoServerTablePanel<BatchRun> runPanel() {
        return new GeoServerTablePanel<BatchRun>("runsPanel", 
                new BatchRunsModel(batchModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;
            
            @SuppressWarnings("unchecked")
            @Override
            protected Component getComponentForProperty(String id, IModel<BatchRun> runModel,
                    Property<BatchRun> property) {
                if (property.equals(BatchRunsModel.START)) {
                    return new SimpleAjaxLink<String>(id, (IModel<String>) property.getModel(runModel)) {
                        private static final long serialVersionUID = -9184383036056499856L;
                        
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            setResponsePage(new BatchRunPage(batchModel, runModel, getPage()));
                        }
                    };
                }
                return null;
            }
        };
    }
    

}
