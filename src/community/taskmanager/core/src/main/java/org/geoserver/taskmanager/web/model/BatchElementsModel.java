/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class BatchElementsModel extends GeoServerDataProvider<BatchElement> {

    private static final long serialVersionUID = -5237816029300683075L;

    public static final Property<BatchElement> INDEX = new BeanProperty<BatchElement>("index", "");
    public static final Property<BatchElement> NAME =
            new BeanProperty<BatchElement>("name", "task.fullName");
    public static final Property<BatchElement> TYPE =
            new BeanProperty<BatchElement>("type", "task.type");

    private IModel<Batch> batchModel;

    public BatchElementsModel(IModel<Batch> batchModel) {
        this.batchModel = batchModel;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<BatchElement>>
            getProperties() {
        return Arrays.asList(INDEX, NAME, TYPE);
    }

    @Override
    public List<BatchElement> getItems() {
        return new ArrayList<>(batchModel.getObject().getElements());
    }
}
