/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

public class LoggingProgressListener implements ProgressListener {

    static final Logger LOGGER = Logging.getLogger(LoggingProgressListener.class);

    @Override
    public InternationalString getTask() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTask(InternationalString task) {
        // TODO Auto-generated method stub

    }

    @Override
    public void started() {
        // TODO Auto-generated method stub

    }

    @Override
    public void progress(float percent) {
        // TODO Auto-generated method stub

    }

    @Override
    public float getProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void complete() {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isCanceled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCanceled(boolean cancel) {
        // TODO Auto-generated method stub

    }

    @Override
    public void warningOccurred(String source, String location, String warning) {
        LOGGER.log(Level.WARNING, warning);
    }

    @Override
    public void exceptionOccurred(Throwable exception) {
        LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
    }
}
