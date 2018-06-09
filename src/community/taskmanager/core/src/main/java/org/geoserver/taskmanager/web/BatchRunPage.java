/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.RunsModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class BatchRunPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = -5111795911981486778L;

    private IModel<Batch> batchModel;

    private IModel<BatchRun> batchRunModel;

    private GeoServerTablePanel<Run> runsPanel;

    public BatchRunPage(IModel<Batch> batchModel, IModel<BatchRun> batchRunModel, Page parentPage) {
        this.batchModel = batchModel;
        this.batchRunModel = batchRunModel;
        setReturnPage(parentPage);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(
                new SimpleAjaxLink<String>(
                        "nameLink", new PropertyModel<String>(batchModel, "fullName")) {
                    private static final long serialVersionUID = -9184383036056499856L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(new BatchPage(batchModel, getPage()));
                    }
                });

        add(new Label("startLabel", new PropertyModel<String>(batchRunModel, "start")));

        add(
                new AjaxLink<Object>("refresh") {

                    private static final long serialVersionUID = 3905640474193868255L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        batchRunModel.setObject(
                                TaskManagerBeans.get().getDao().reload(batchRunModel.getObject()));
                        ((MarkupContainer) runsPanel.get("listContainer").get("items")).removeAll();
                        target.add(runsPanel);
                    }
                });

        // the tasks panel
        add(runsPanel = runPanel());
        runsPanel.setOutputMarkupId(true);
        runsPanel.setSelectable(false);

        add(
                new AjaxLink<Object>("close") {
                    private static final long serialVersionUID = -6892944747517089296L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }

    protected GeoServerTablePanel<Run> runPanel() {
        return new GeoServerTablePanel<Run>("runPanel", new RunsModel(batchRunModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<Run> runModel, Property<Run> property) {
                return null;
            }
        };
    }
}
