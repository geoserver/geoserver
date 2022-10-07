/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.wps;

import java.util.logging.Logger;
import org.geoserver.geofence.wpscommon.ChainStatusHolder;
import org.geoserver.wps.ChainedProcessListener;
import org.geotools.util.logging.Logging;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public class GeoFenceProcessListener implements ChainedProcessListener {

    private static final Logger LOGGER = Logging.getLogger(GeoFenceProcessListener.class);

    private ChainStatusHolder statusHolder;

    public GeoFenceProcessListener(ChainStatusHolder statusHolder) {
        this.statusHolder = statusHolder;
    }

    @Override
    public void started(String executionId, String processName, boolean chained) {
        //        LOGGER.warning(
        //                "*** SUBMITTED XID:" + executionId + " PROCESS:" + processName + " CH:" +
        // chained);
        statusHolder.stackProcess(executionId, processName);
        LOGGER.warning(
                "ExecId:"
                        + executionId
                        + " Current process stack is "
                        + statusHolder.stackToString(executionId)
                        + " --- +"
                        + processName);
    }

    @Override
    public void completed(String executionId, String processName) {
        //        LOGGER.warning("*** RETURNED XID:" + executionId + " PROCESS:" + processName);
        statusHolder.unstackProcess(executionId, processName);
        LOGGER.warning(
                "ExecId:"
                        + executionId
                        + " Current process stack is "
                        + statusHolder.stackToString(executionId)
                        + " --- -"
                        + processName);
    }

    @Override
    public void dismissed(String executionId, String processName) {
        //        LOGGER.warning("*** FAILED XID:" + executionId + " PROCESS:" + processName);
        statusHolder.unstackProcess(executionId, processName);
        LOGGER.warning(
                "ExecId:"
                        + executionId
                        + " Current process stack is "
                        + statusHolder.stackToString(executionId)
                        + " --- !"
                        + processName);
    }

    @Override
    public void failed(String executionId, String processName, Exception e) {
        //        LOGGER.warning("*** FAILED XID:" + executionId + " PROCESS:" + processName);
        statusHolder.unstackProcess(executionId, processName);
        LOGGER.warning(
                "ExecId:"
                        + executionId
                        + " Current process stack is "
                        + statusHolder.stackToString(executionId)
                        + " --- !"
                        + processName);
    }
}
