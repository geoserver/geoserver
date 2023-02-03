/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.wpscommon;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.util.AccessInfoUtils;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public class WPSHelper implements ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(WPSHelper.class);

    private RuleReaderService ruleService;

    ChainStatusHolder statusHolder = null;
    ExecutionIdRetriever executionIdRetriever = null;
    private boolean helperAvailable = false;

    public WPSHelper(RuleReaderService ruleService) {
        this.ruleService = ruleService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            statusHolder = applicationContext.getBean(ChainStatusHolder.class);
            executionIdRetriever = applicationContext.getBean(ExecutionIdRetriever.class);
            helperAvailable = true;
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.severe(
                    "GeoFence-WPS integration classes not available. Please include geofence-wps module.");
        }
    }

    /**
     * Resolve limits according to running process
     *
     * @param wpsAccessInfo Pre-computed accessInfo for default WPS access.
     * @return a WPSAccessInfo where the WKT of the AccessInfo should not be taken into
     *     consideration since the geometries are more up-to-date. Returns null if no forther
     *     resolution was computed.
     */
    public AccessInfoUtils.WPSAccessInfo resolveWPSAccess(
            Request req, RuleFilter filter, AccessInfo wpsAccessInfo) {
        if (!helperAvailable) {
            LOGGER.warning("WPSHelper not available");
            // For more security we should deny the access, anyway let's tell
            // the caller we did nothing and it should go an as usual
            // in order not to break existing installations.
            return null;
        }

        String execId = executionIdRetriever.getCurrentExecutionId();
        List<String> procNames = statusHolder.getCurrentStack(execId);

        List<AccessInfo> procAccessInfo = new LinkedList<>();

        for (String procName : procNames) {
            LOGGER.fine("Retrieving AccessInfo for proc " + procName);
            RuleFilter r = filter.clone();
            r.setSubfield(procName);

            AccessInfo accessInfo = ruleService.getAccessInfo(r);
            if (accessInfo.getGrant() == GrantType.DENY) {
                // shortcut: if at least one process is not allowed for current resource, do
                // not evaluate the other procs
                LOGGER.fine("Process " + procName + " not allowed to operate on layer");
                return new AccessInfoUtils.WPSAccessInfo(AccessInfo.DENY_ALL, null, null);
            }
            if (!accessInfo.equals(wpsAccessInfo)) {
                procAccessInfo.add(accessInfo);
            } else {
                // No specific rules for this proc, we're getting the generic WPS we already have
                LOGGER.fine("Skipping accessInfo for " + procName);
            }
        }

        // if we have at least one procAccessInfo, we should not consider the main wpsAccessInfo,
        // bc the rules generating it are also considered in the more cohomprensive procAccessInfo
        if (procAccessInfo.isEmpty()) {
            return null;
        } else {
            return AccessInfoUtils.intersect(procAccessInfo.toArray(new AccessInfo[0]));
        }
    }
}
