/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.wpscommon;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public class ChainStatusHolder {

    private static final Logger LOGGER = Logging.getLogger(ChainStatusHolder.class);

    /** Maps executionIds onto chain stacks */
    private Map<String, LinkedList<String>> chains = new HashMap<>();

    public ChainStatusHolder() {}

    public void stackProcess(String execId, String procName) {
        LinkedList<String> chain = chains.getOrDefault(execId, null);
        if (chain == null) {
            LOGGER.fine("Creating chain for " + execId);
            chain = new LinkedList<>();
            chains.put(execId, chain);
        }
        chain.add(procName);
    }

    /** @return false if proc could not be removed */
    public boolean unstackProcess(String execId, String procName) {
        LinkedList<String> chain = chains.getOrDefault(execId, null);
        if (chain == null) {
            LOGGER.severe("No chain for execution ID " + execId);
            return false;
            // throw new IllegalStateException("No chain for execution ID " + execId);
        }
        String last = chain.pollLast();
        if (!procName.equals(last)) {
            LOGGER.severe(
                    "Returning from ["
                            + procName
                            + "], but last called process was ["
                            + last
                            + "] for execution ID "
                            + execId);
            return false;
            //                throw new IllegalStateException(
            //                        "Returning from ["
            //                                + procName
            //                                + "], but last called process was ["
            //                                + last
            //                                + "] for execution ID "
            //                                + execId);
        }
        if (chain.isEmpty()) {
            LOGGER.fine("Removing chain for " + execId);
            chains.remove(execId);
        }
        return true;
    }

    public String stackToString(String execId) {
        LinkedList<String> chain = chains.getOrDefault(execId, null);
        if (chain == null) {
            return "[MISSING CHAIN]";
            //                throw new IllegalStateException("No chain for execution ID " +
            // execId);
        }
        return String.join("/", chain);
    }

    public List<String> getCurrentStack(String execId) {
        LinkedList<String> chain = chains.getOrDefault(execId, null);
        return chain == null ? null : Collections.unmodifiableList(chain);
    }
}
