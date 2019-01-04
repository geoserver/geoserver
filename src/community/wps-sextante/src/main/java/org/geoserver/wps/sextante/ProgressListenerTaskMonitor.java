/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import es.unex.sextante.core.ITaskMonitor;
import org.geotools.text.Text;
import org.opengis.util.ProgressListener;

/**
 * This class represents a task monitor for a SEXTANTE algorithm that updates a progress monitor
 * used in a GeoTools process.
 *
 * @author volaya
 */
public class ProgressListenerTaskMonitor implements ITaskMonitor {

    private ProgressListener m_ProgressListener;

    public ProgressListenerTaskMonitor(ProgressListener progressListener) {

        m_ProgressListener = progressListener;
    }

    public void close() {

        if (m_ProgressListener != null) {
            m_ProgressListener.complete();
            m_ProgressListener.dispose();
        }
    }

    public boolean isCanceled() {

        if (m_ProgressListener != null) {
            return m_ProgressListener.isCanceled();
        } else {
            return false;
        }
    }

    public void setProgress(int step) {

        if (m_ProgressListener != null) {
            m_ProgressListener.progress((float) step);
        }
    }

    public void setProgress(int step, int totalNumberOfSteps) {

        if (m_ProgressListener != null) {
            m_ProgressListener.progress((float) (step / totalNumberOfSteps) * 100);
        }
    }

    public void setProgressText(String text) {

        if (m_ProgressListener != null) {
            m_ProgressListener.setTask(Text.text(text));
        }
    }

    public void setDeterminate(boolean determinate) {}

    public void setProcessDescription(String description) {

        if (m_ProgressListener != null) {
            m_ProgressListener.setTask(Text.text(description));
        }
    }

    @Override
    public void setDescriptionPrefix(String prefix) {

        if (m_ProgressListener != null) {
            m_ProgressListener.setTask(Text.text(prefix));
        }
    }
}
