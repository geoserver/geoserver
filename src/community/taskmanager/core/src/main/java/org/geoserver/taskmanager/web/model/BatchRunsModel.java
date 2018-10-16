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
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class BatchRunsModel extends GeoServerDataProvider<BatchRun> {

    private static final long serialVersionUID = -5237816029300683075L;

    public static final Property<BatchRun> START = new BeanProperty<BatchRun>("start", "start");
    public static final Property<BatchRun> END = new BeanProperty<BatchRun>("end", "end");
    public static final Property<BatchRun> STATUS = new BeanProperty<BatchRun>("status", "status");
    public static final Property<BatchRun> MESSAGE =
            new BeanProperty<BatchRun>("message", "message");
    public static final Property<BatchRun> STOP =
            new AbstractProperty<BatchRun>("stop") {

                private static final long serialVersionUID = -978472501994535469L;

                @Override
                public Object getPropertyValue(BatchRun item) {
                    return null;
                }
            };

    private IModel<Batch> batchModel;

    public BatchRunsModel(IModel<Batch> batchModel) {
        this.batchModel = batchModel;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<BatchRun>>
            getProperties() {
        return Arrays.asList(START, END, STATUS, STOP, MESSAGE);
    }

    @Override
    protected List<BatchRun> getItems() {
        List<BatchRun> list = new ArrayList<BatchRun>(batchModel.getObject().getBatchRuns());
        list.removeIf(br -> br.getRuns().isEmpty());
        return list;
    }
}
