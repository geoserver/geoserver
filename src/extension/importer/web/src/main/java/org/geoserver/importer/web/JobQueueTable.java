/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.job.Task;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.AbstractProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class JobQueueTable extends GeoServerTablePanel<Task<ImportContext>> {

    static final Property<Task<ImportContext>> IMPORT =
            new AbstractProperty<Task<ImportContext>>("import") {
                @Override
                public Object getPropertyValue(Task<ImportContext> item) {
                    // have to check for null since the job might be out of the queue
                    return item != null ? item.toString() : "null";
                }
            };

    static final Property<Task<ImportContext>> STATUS =
            new AbstractProperty<Task<ImportContext>>("status") {
                @Override
                public Object getPropertyValue(Task<ImportContext> item) {
                    if (item == null) {
                        return "Finished";
                    }

                    return item.isCancelled()
                            ? "Cancelled"
                            : item.isDone() ? "Finished" : item.isStarted() ? "Running" : "Pending";
                }
            };

    public JobQueueTable(String id) {
        super(
                id,
                new GeoServerDataProvider<Task<ImportContext>>() {

                    @Override
                    protected List<Property<Task<ImportContext>>> getProperties() {
                        return Arrays.asList(IMPORT, STATUS);
                    }

                    @Override
                    protected List<Task<ImportContext>> getItems() {
                        return ImporterWebUtils.importer().getTasks();
                    }

                    @Override
                    protected IModel<Task<ImportContext>> newModel(Task<ImportContext> object) {
                        return new JobModel(object);
                    }
                });

        setOutputMarkupId(true);
        setFilterable(false);
        getTopPager().setVisible(false);
    }

    @Override
    protected Component getComponentForProperty(
            String id,
            IModel<Task<ImportContext>> itemModel,
            Property<Task<ImportContext>> property) {
        return new Label(id, property.getModel(itemModel));
    }

    static class JobModel implements IModel<Task<ImportContext>> {

        Long jobid;

        JobModel(Task<ImportContext> job) {
            jobid = job.getId();
        }

        @Override
        public void detach() {}

        @Override
        public Task<ImportContext> getObject() {
            return ImporterWebUtils.importer().getTask(jobid);
        }

        @Override
        public void setObject(Task<ImportContext> object) {}
    }
}
