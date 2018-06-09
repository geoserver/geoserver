/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class ParametersModel extends GeoServerDataProvider<Parameter> {

    private static final long serialVersionUID = 7539336498086934903L;

    public static final Property<Parameter> NAME = new BeanProperty<Parameter>("name", "name");
    public static final Property<Parameter> VALUE = new BeanProperty<Parameter>("value", "value");

    private IModel<Task> taskModel;

    public ParametersModel(IModel<Task> taskModel) {
        this.taskModel = taskModel;
    }

    @Override
    protected List<Property<Parameter>> getProperties() {
        return Arrays.asList(NAME, VALUE);
    }

    @Override
    protected List<Parameter> getItems() {
        return new ArrayList<Parameter>(taskModel.getObject().getParameters().values());
    }
}
