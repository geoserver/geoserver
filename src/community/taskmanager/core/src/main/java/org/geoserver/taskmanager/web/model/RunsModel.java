/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RunsModel extends GeoServerDataProvider<Run> {

    private static final long serialVersionUID = -5237816029300683075L;

    public static final Property<Run> NAME =
            new BeanProperty<Run>("name", "batchElement.task.fullName");
    public static final Property<Run> START = new BeanProperty<Run>("start", "start");
    public static final Property<Run> END = new BeanProperty<Run>("end", "end");
    public static final Property<Run> STATUS = new BeanProperty<Run>("status", "status");
    public static final Property<Run> MESSAGE = new BeanProperty<Run>("message", "message");

    private IModel<BatchRun> batchRunModel;

    public RunsModel(IModel<BatchRun> batchRunModel) {
        this.batchRunModel = batchRunModel;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<Run>> getProperties() {
        return Arrays.asList(NAME, START, END, STATUS, MESSAGE);
    }

    @Override
    protected List<Run> getItems() {
        return batchRunModel.getObject().getRuns();
    }
}
