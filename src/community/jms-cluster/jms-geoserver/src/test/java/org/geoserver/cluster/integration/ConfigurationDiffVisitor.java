/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    private <T extends Info> List<InfoDiff> computeDifferences(
            Map<String, T> objectsA,
            Map<String, T> objectsB,
            BiFunction<T, T, Boolean> equivalenceFunction) {

        MapDifference<String, T> diff =
                Maps.difference(
                        objectsA,
                        objectsB,
                        new Equivalence<T>() {
                            protected @Override boolean doEquivalent(T a, T b) {
                                return equivalenceFunction.apply(a, b);
                            }

                            protected @Override int doHash(T t) {
                                throw new UnsupportedOperationException(
                                        "unexpected, method not used");
                            }
                        });
        // more cumbersome than needed to aid in debugging
        List<InfoDiff> toAdd =
                diff.entriesOnlyOnLeft()
                        .values()
                        .stream()
                        .map(a -> new InfoDiff(a, null))
                        .collect(Collectors.toList());
        List<InfoDiff> toRemove =
                diff.entriesOnlyOnRight()
                        .values()
                        .stream()
                        .map(b -> new InfoDiff(null, b))
                        .collect(Collectors.toList());

        List<InfoDiff> toUpdate =
                diff.entriesDiffering()
                        .values()
                        .stream()
                        .map(d -> new InfoDiff(d.leftValue(), d.rightValue()))
                        .collect(Collectors.toList());

        return Stream.concat(Stream.concat(toAdd.stream(), toRemove.stream()), toUpdate.stream())
                .collect(Collectors.toList());
    }

    /** Register services differences between the two GeoServers. */
    private void computeServicesDifference() {
        // get all the services available on both GeoServers
        Map<String, ServiceInfo> servicesA = getAllServices(geoServerA);
        Map<String, ServiceInfo> servicesB = getAllServices(geoServerB);

        List<InfoDiff> serviceDiffs = computeDifferences(servicesA, servicesB, this::checkEquals);
        differences.addAll(serviceDiffs);
    }

    /** Register settings differences between the two GeoServers. */
    private void computeSettingsDifference() {
        // get all the settings available on both GeoServers
        Map<String, SettingsInfo> settingsA = getAllSettings(geoServerA);
        Map<String, SettingsInfo> settingsB = getAllSettings(geoServerB);

        List<InfoDiff> serviceDiffs = computeDifferences(settingsA, settingsB, this::checkEquals);
        differences.addAll(serviceDiffs);
    }

    /**
     * Get all services info objects of a GeoServer instance, including the global service and
     * workspace services.
     */
    private static Map<String, ServiceInfo> getAllServices(GeoServer geoServer) {
        List<ServiceInfo> allServices = new ArrayList<>();
        // get global services
        allServices.addAll(geoServer.getServices());
        // get services per workspace
        List<WorkspaceInfo> workspaces = geoServer.getCatalog().getWorkspaces();
        for (WorkspaceInfo workspace : workspaces) {
            // get the services of this workspace
            allServices.addAll(geoServer.getFacade().getServices(workspace));
        }
        return allServices.stream().collect(Collectors.toMap(ServiceInfo::getId, s -> s));
    }

    /**
     * Get all settings info objects of a GeoServer instance, this will not include global settings
     * only per workspace settings will be included.
     */
    private static Map<String, SettingsInfo> getAllSettings(GeoServer geoServer) {
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
        return allSettings.stream().collect(Collectors.toMap(SettingsInfo::getId, s -> s));
    }

    /** Compare two GeoServer info objects ignoring the update sequence. */
    private boolean checkEquals(GeoServerInfo infoA, GeoServerInfo infoB) {
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

    private boolean checkEquals(Info infoA, Info infoB) {
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
