package org.geoserver.importer.job;

import java.io.Serializable;

import org.geotools.util.DefaultProgressListener;
import org.geotools.util.SimpleInternationalString;

@SuppressWarnings("serial")
public class ProgressMonitor extends DefaultProgressListener implements Serializable {

    public void setTask(String message) {
        super.setTask(new SimpleInternationalString(message));
    };
}
