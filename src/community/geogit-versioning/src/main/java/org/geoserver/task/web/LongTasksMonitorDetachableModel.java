package org.geoserver.task.web;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.task.LongTaskMonitor;

public class LongTasksMonitorDetachableModel extends LoadableDetachableModel<LongTaskMonitor> {

    private static final long serialVersionUID = 1L;

    @Override
    protected LongTaskMonitor load() {
        LongTaskMonitor monitor = GeoServerExtensions.bean(LongTaskMonitor.class);
        if (monitor == null) {
            throw new IllegalStateException("No " + LongTaskMonitor.class.getName()
                    + " found in the GeoSever extensions");
        }
        return monitor;
    }

}
