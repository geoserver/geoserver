/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.wps;

import java.util.logging.Logger;
import org.geoserver.wps.ChainedProcessListener;
import org.geotools.util.logging.Logging;

/**
 * @author etj (Emanuele Tajariol @ GeoSolutions) Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
public class WPSProcessListener implements ChainedProcessListener {

    private static final Logger LOGGER = Logging.getLogger(WPSProcessListener.class);

    private WPSChainStatusHolder statusHolder;

    public WPSProcessListener(WPSChainStatusHolder statusHolder) {
        this.statusHolder = statusHolder;
    }

    @Override
    public void started(String executionId, String processName, boolean chained) {
        statusHolder.stackProcess(executionId, processName);
        LOGGER.warning("ExecId:"
                + executionId
                + " Current process stack is "
                + statusHolder.stackToString(executionId)
                + " --- +"
                + processName);
    }

    @Override
    public void completed(String executionId, String processName) {
        // LOGGER.warning("*** RETURNED XID:" + executionId + " PROCESS:" +
        // processName);
        statusHolder.unstackProcess(executionId, processName);
        LOGGER.warning("ExecId:"
                + executionId
                + " Current process stack is "
                + statusHolder.stackToString(executionId)
                + " --- -"
                + processName);
    }

    @Override
    public void dismissed(String executionId, String processName) {
        // LOGGER.warning("*** FAILED XID:" + executionId + " PROCESS:" + processName);
        statusHolder.unstackProcess(executionId, processName);
        LOGGER.warning("ExecId:"
                + executionId
                + " Current process stack is "
                + statusHolder.stackToString(executionId)
                + " --- !"
                + processName);
    }

    @Override
    public void failed(String executionId, String processName, Exception e) {
        // LOGGER.warning("*** FAILED XID:" + executionId + " PROCESS:" + processName);
        statusHolder.unstackProcess(executionId, processName);
        LOGGER.warning("ExecId:"
                + executionId
                + " Current process stack is "
                + statusHolder.stackToString(executionId)
                + " --- !"
                + processName);
    }
}
