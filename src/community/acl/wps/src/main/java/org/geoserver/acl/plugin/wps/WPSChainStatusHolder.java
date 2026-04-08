/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.wps;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * @author etj (Emanuele Tajariol @ GeoSolutions) - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
public class WPSChainStatusHolder {

    private static final Logger LOGGER = Logging.getLogger(WPSChainStatusHolder.class);

    /** Maps executionIds onto chain stacks */
    private Map<String, LinkedList<String>> chains = new ConcurrentHashMap<>();

    public WPSChainStatusHolder() {}

    public void stackProcess(String execId, String procName) {
        LinkedList<String> chain = chains.computeIfAbsent(execId, id -> {
            LOGGER.fine("Creating chain for " + execId);
            return new LinkedList<>();
        });
        chain.add(procName);
    }

    /** @return false if proc could not be removed */
    public boolean unstackProcess(String execId, String procName) {
        LinkedList<String> chain = chains.get(execId);
        if (chain == null) {
            LOGGER.severe("No chain for execution ID " + execId);
            return false;
        }
        synchronized (chain) {
            String last = chain.pollLast();
            if (!procName.equals(last)) {
                LOGGER.severe("Returning from ["
                        + procName
                        + "], but last called process was ["
                        + last
                        + "] for execution ID "
                        + execId);
                return false;
            }
            if (chain.isEmpty()) {
                LOGGER.fine("Removing chain for " + execId);
                chains.remove(execId);
            }
        }
        return true;
    }

    public String stackToString(String execId) {
        LinkedList<String> chain = chains.get(execId);
        if (chain == null) {
            return "[MISSING CHAIN]";
        }
        synchronized (chain) {
            return String.join("/", chain);
        }
    }

    public List<String> getCurrentStack(String execId) {
        LinkedList<String> chain = chains.get(execId);
        if (null == chain) return List.of();
        synchronized (chain) {
            return List.copyOf(chain);
        }
    }
}
