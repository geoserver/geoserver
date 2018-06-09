/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class TasksModel extends GeoServerDataProvider<Task> {

    private static final long serialVersionUID = -5237816029300683075L;

    public static final Property<Task> NAME = new BeanProperty<Task>("name", "name");
    public static final Property<Task> TYPE = new BeanProperty<Task>("type", "type");
    public static final Property<Task> PARAMETERS =
            new BeanProperty<Task>("parameters", "parameters");

    private IModel<Configuration> configurationModel;

    public TasksModel(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<Task>> getProperties() {
        return Arrays.asList(NAME, TYPE, PARAMETERS);
    }

    @Override
    protected List<Task> getItems() {
        return new ArrayList<>(configurationModel.getObject().getTasks().values());
    }
}
