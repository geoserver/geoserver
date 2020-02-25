/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.job;

import java.io.Serializable;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.util.SimpleInternationalString;

@SuppressWarnings("serial")
public class ProgressMonitor extends DefaultProgressListener implements Serializable {

    public void setTask(String message) {
        super.setTask(new SimpleInternationalString(message));
    };
}
