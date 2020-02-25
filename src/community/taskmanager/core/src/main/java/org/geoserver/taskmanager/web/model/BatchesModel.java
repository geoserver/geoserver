/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.springframework.security.core.context.SecurityContextHolder;

public class BatchesModel extends GeoServerDataProvider<Batch> {

    private static final long serialVersionUID = -8246320435114536132L;

    public static class StringReversePropertyComparator<T> implements Comparator<T> {
        Property<T> property;

        public StringReversePropertyComparator(Property<T> property) {
            this.property = property;
        }

        public int compare(T o1, T o2) {
            Object p1 = property.getPropertyValue(o1);
            Object p2 = property.getPropertyValue(o2);

            // what if any property is null? We assume null < (not null)
            if (p1 == null) return p2 != null ? -1 : 0;
            else if (p2 == null) return 1;

            return new StringBuffer(p1.toString())
                    .reverse()
                    .toString()
                    .compareTo(new StringBuffer(p2.toString()).reverse().toString());
        }
    }

    public static final Property<Batch> WORKSPACE =
            new BeanProperty<Batch>("workspace", "workspace");
    public static final Property<Batch> DESCRIPTION =
            new BeanProperty<Batch>("description", "description");
    public static final Property<Batch> FREQUENCY =
            new BeanProperty<Batch>("frequency", "frequency") {
                private static final long serialVersionUID = -5039727601429342722L;

                @Override
                public Comparator<Batch> getComparator() {
                    return new StringReversePropertyComparator<Batch>(this);
                }
            };
    public static final Property<Batch> ENABLED = new BeanProperty<Batch>("enabled", "enabled");
    public static final Property<Batch> NAME = new BeanProperty<Batch>("name", "name");
    public static final Property<Batch> STARTED =
            new AbstractProperty<Batch>("started") {
                private static final long serialVersionUID = 6588177543318699677L;

                @Override
                public Object getPropertyValue(Batch batch) {
                    if (batch.getId() != null) {
                        if (batch.getLatestBatchRun() != null) {
                            return batch.getLatestBatchRun().getStart();
                        }
                    }
                    return null;
                }
            };

    public static final Property<Batch> STATUS =
            new AbstractProperty<Batch>("status") {

                private static final long serialVersionUID = 6588177543318699677L;

                @Override
                public Object getPropertyValue(Batch batch) {
                    if (batch.getId() != null) {
                        if (batch.getLatestBatchRun() != null) {
                            return batch.getLatestBatchRun().getStatus();
                        }
                    }
                    return null;
                }
            };

    public static final Property<Batch> RUN =
            new AbstractProperty<Batch>("run") {

                private static final long serialVersionUID = -978472501994535469L;

                @Override
                public Object getPropertyValue(Batch item) {
                    return null;
                }
            };

    public static final Property<Batch> FULL_NAME =
            new AbstractProperty<Batch>("name") {
                private static final long serialVersionUID = 6588177543318699677L;

                @Override
                public Object getPropertyValue(Batch item) {
                    return item.getFullName();
                }
            };

    private IModel<Configuration> configurationModel;

    private List<Batch> list;

    public BatchesModel() {}

    public BatchesModel(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    protected List<Property<Batch>> getProperties() {
        return Arrays.asList(
                WORKSPACE,
                configurationModel == null ? FULL_NAME : NAME,
                DESCRIPTION,
                FREQUENCY,
                ENABLED,
                STARTED,
                RUN,
                STATUS);
    }

    public void reset() {
        list = null;
    }

    @Override
    protected List<Batch> getItems() {
        if (list == null) {
            if (configurationModel == null) {
                list = TaskManagerBeans.get().getDao().getViewableBatches();
            } else {
                if (configurationModel.getObject().getId() != null) {
                    TaskManagerBeans.get()
                            .getDao()
                            .loadLatestBatchRuns(configurationModel.getObject());
                }
                list = new ArrayList<>(configurationModel.getObject().getBatches().values());
            }

            list.removeIf(
                    b ->
                            !TaskManagerBeans.get()
                                    .getSecUtil()
                                    .isReadable(
                                            SecurityContextHolder.getContext().getAuthentication(),
                                            b));
        }

        return list;
    }
}
