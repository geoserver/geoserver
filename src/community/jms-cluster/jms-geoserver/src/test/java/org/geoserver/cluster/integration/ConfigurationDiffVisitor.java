/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;

/** This visitor can be used to extract to GeoServer configuration differences. */
public final class ConfigurationDiffVisitor {

    private final GeoServer geoServerA;
    private final GeoServer geoServerB;

    private final List<InfoDiff> differences = new ArrayList<>();

    public ConfigurationDiffVisitor(GeoServer geoServerA, GeoServer geoServerB) {
        this.geoServerA = geoServerA;
        this.geoServerB = geoServerB;
        computeDifferences();
    }

    /** Returns the differences found. */
    public List<InfoDiff> differences() {
        return differences;
    }

    /** Visit both GeoServers and register the differences. */
    private void computeDifferences() {
        // check GeoServer global settings differences
        if (!checkEquals(geoServerA.getGlobal(), geoServerB.getGlobal())) {
            differences.add(new InfoDiff(geoServerA.getGlobal(), geoServerB.getGlobal()));
        }
        if (!checkEquals(geoServerA.getLogging(), geoServerB.getLogging())) {
            differences.add(new InfoDiff(geoServerA.getLogging(), geoServerB.getLogging()));
        }
        // check services differences
        computeServicesDifference();
        // check settings differences
        computeSettingsDifference();
    }

    /** Register services differences between the two GeoServers. */
    private void computeServicesDifference() {
        // get all the services available on both GeoServers
        Collection<ServiceInfo> servicesA = getAllServices(geoServerA);
        Collection<ServiceInfo> servicesB = getAllServices(geoServerB);
        // register the services that are only present in GeoServer B has differences
        differences.addAll(
                servicesB
                        .stream()
                        .filter(service -> search(service, servicesA) == null)
                        .map(service -> new InfoDiff(null, service))
                        .collect(Collectors.toList()));
        // iterate over GeoServer A services and compare them with GeoServer B services
        for (ServiceInfo service : servicesA) {
            ServiceInfo otherService = search(service, servicesB);
            if (!checkEquals(service, otherService)) {
                // different service found, the other service may be NULL which means
                // that this service only exists in GeoServer A
                differences.add(new InfoDiff(service, otherService));
            }
        }
    }

    /** Searches a service by is name and workspace on a collection of services. */
    private static ServiceInfo search(ServiceInfo info, Collection<ServiceInfo> collection) {
        for (ServiceInfo candidateInfo : collection) {
            if (checkEqualsByNameAndWorkspace(info, candidateInfo)) {
                // service found
                return candidateInfo;
            }
        }
        // service not found
        return null;
    }

    /** Returns TRUE if the services have the same name and the same workspace. */
    private static boolean checkEqualsByNameAndWorkspace(ServiceInfo infoA, ServiceInfo infoB) {
        return Objects.equals(infoA.getName(), infoB.getName())
                && Objects.equals(infoA.getWorkspace(), infoB.getWorkspace());
    }

    /** Register settings differences between the two GeoServers. */
    private void computeSettingsDifference() {
        // get all the settings available on both GeoServers
        List<SettingsInfo> settingsA = getAllSettings(geoServerA);
        List<SettingsInfo> settingsB = getAllSettings(geoServerB);
        // register the settings that are only present in GeoServer B has differences
        differences.addAll(
                settingsB
                        .stream()
                        .filter(settings -> search(settings, settingsA) == null)
                        .map(settings -> new InfoDiff(null, settings))
                        .collect(Collectors.toList()));
        // iterate over GeoServer A settings and compare them with GeoServer B services
        for (SettingsInfo settings : settingsA) {
            SettingsInfo otherSettings = search(settings, settingsB);
            if (!checkEquals(settings, otherSettings)) {
                // different settings found, the other settings may be NULL which means
                // that this settings only exists in GeoServer A
                differences.add(new InfoDiff(settings, otherSettings));
            }
        }
    }

    /** Searches settings by is title and workspace on a collection of settings. */
    private static SettingsInfo search(SettingsInfo info, Collection<SettingsInfo> collection) {
        for (SettingsInfo candidateInfo : collection) {
            if (Objects.equals(info.getId(), candidateInfo.getId())) {
                // settings found
                return candidateInfo;
            }
        }
        // settings not found
        return null;
    }

    /**
     * Get all services info objects of a GeoServer instance, including the global service and
     * workspace services.
     */
    private static List<ServiceInfo> getAllServices(GeoServer geoServer) {
        List<ServiceInfo> allServices = new ArrayList<>();
        // get global services
        allServices.addAll(geoServer.getServices());
        // get services per workspace
        List<WorkspaceInfo> workspaces = geoServer.getCatalog().getWorkspaces();
        for (WorkspaceInfo workspace : workspaces) {
            // get the services of this workspace
            allServices.addAll(geoServer.getFacade().getServices(workspace));
        }
        return allServices;
    }

    /**
     * Get all settings info objects of a GeoServer instance, this will not include global settings
     * only per workspace settings will be included.
     */
    private static List<SettingsInfo> getAllSettings(GeoServer geoServer) {
        List<SettingsInfo> allSettings = new ArrayList<>();
        // get all settings per workspace
        List<WorkspaceInfo> workspaces = geoServer.getCatalog().getWorkspaces();
        for (WorkspaceInfo workspace : workspaces) {
            // get this workspace settings
            SettingsInfo settings = geoServer.getSettings(workspace);
            if (settings != null) {
                allSettings.add(settings);
            }
        }
        return allSettings;
    }

    /** Compare two GeoServer info objects ignoring the update sequence. */
    private static boolean checkEquals(GeoServerInfo infoA, GeoServerInfo infoB) {
        // for GeoServer infos to have the same update sequence
        infoA = ModificationProxy.unwrap(infoA);
        infoB = ModificationProxy.unwrap(infoB);
        long updateSequenceB = infoB.getUpdateSequence();
        try {
            infoB.setUpdateSequence(infoA.getUpdateSequence());
            // check that the two infos are equal
            return checkEquals((Info) infoA, infoB);
        } finally {
            // restore the original update sequence
            infoB.setUpdateSequence(updateSequenceB);
        }
    }

    private static boolean checkEquals(Info infoA, Info infoB) {
        // if only one of the infos is NULL they are not the same
        if ((infoA == null || infoB == null) && infoA != infoB) {
            return false;
        }
        // force the infos to have the same ID
        infoA = ModificationProxy.unwrap(infoA);
        infoB = ModificationProxy.unwrap(infoB);
        String idB = infoB.getId();
        try {
            OwsUtils.set(infoB, "id", infoA.getId());
            // compare the two infos
            return Objects.equals(infoA, infoB);
        } finally {
            // restore the original ID
            OwsUtils.set(infoB, "id", idB);
        }
    }
}
