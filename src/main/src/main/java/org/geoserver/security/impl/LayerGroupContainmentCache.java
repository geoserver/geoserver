/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;

/**
 * A cache for layer group containment, it speeds up looking up layer groups containing a particular
 * layer (recursively). * The class is thread safe.
 *
 * @author Andrea Aime - GeoSolutions
 */
class LayerGroupContainmentCache {

    /** Builds a concurrent set wrapping a {@link ConcurrentHashMap} */
    static final Function<? super String, ? extends Set<LayerGroupSummary>> CONCURRENT_SET_BUILDER =
            k -> Collections.newSetFromMap(new ConcurrentHashMap<LayerGroupSummary, Boolean>());

    /** Verifies a certain {@link PublishedInfo} is actually a {@link LayerInfo} */
    static final Predicate<PublishedInfo> IS_LAYER =
            p ->
                    p != null
                            && p.getId() != null
                            && p instanceof LayerInfo
                            && ((LayerInfo) p).getResource() != null;

    /** Verifies a certain {@link PublishedInfo} is actually a {@link LayerGroupInfo} */
    static final Predicate<PublishedInfo> IS_GROUP =
            p -> p != null && p.getId() != null && p instanceof LayerGroupInfo;

    /** Lookup from layer group id to group parent information */
    Map<String, LayerGroupSummary> groupCache = new ConcurrentHashMap<>();

    /**
     * Lookup from {@link ResourceInfo} id to groups directly containing its associated layers (the
     * transitive containment is computed by suing {@link LayerGroupSummary}
     */
    Map<String, Set<LayerGroupSummary>> resourceContainmentCache = new ConcurrentHashMap<>();

    private Catalog catalog;

    public LayerGroupContainmentCache(Catalog catalog) {
        this.catalog = catalog;
        catalog.addListener(new CatalogChangeListener());
        buildLayerGroupCaches();
    }

    private void buildLayerGroupCaches() {
        groupCache.clear();
        resourceContainmentCache.clear();
        List<LayerGroupInfo> groups = catalog.getLayerGroups();

        // first populate the basic structure
        for (LayerGroupInfo lg : groups) {
            addGroupInfo(lg);
        }

        // now populate the containment structure
        for (LayerGroupInfo lg : groups) {
            registerContainedGroups(lg);
        }
    }

    private void registerContainedGroups(LayerGroupInfo lg) {
        lg.getLayers()
                .stream()
                .filter(IS_GROUP)
                .forEach(
                        p -> {
                            String containerId = lg.getId();
                            String containedId = p.getId();
                            LayerGroupSummary container = groupCache.get(containerId);
                            LayerGroupSummary contained = groupCache.get(containedId);
                            if (container != null && contained != null) {
                                contained.containerGroups.add(container);
                            }
                        });
    }

    private void addGroupInfo(LayerGroupInfo lg) {
        LayerGroupSummary groupData = new LayerGroupSummary(lg);
        groupCache.put(lg.getId(), groupData);
        lg.getLayers()
                .stream()
                .filter(IS_LAYER)
                .forEach(
                        p -> {
                            String id = ((LayerInfo) p).getResource().getId();
                            Set<LayerGroupSummary> containers =
                                    resourceContainmentCache.computeIfAbsent(
                                            id, CONCURRENT_SET_BUILDER);
                            containers.add(groupData);
                        });
    }

    private void clearGroupInfo(LayerGroupInfo lg) {
        LayerGroupSummary data = groupCache.remove(lg.getId());
        // clear the resource containment cache
        lg.getLayers()
                .stream()
                .filter(IS_LAYER)
                .forEach(
                        p -> {
                            String rid = ((LayerInfo) p).getResource().getId();
                            synchronized (rid) {
                                Set<LayerGroupSummary> containers =
                                        resourceContainmentCache.get(rid);
                                if (containers != null) {
                                    containers.remove(data);
                                }
                            }
                        });
        // this group does not contain anything anymore, remove from containment
        for (LayerGroupSummary d : groupCache.values()) {
            // will be removed by equality
            d.containerGroups.remove(new LayerGroupSummary(lg));
        }
    }

    /** Returns all groups containing directly or indirectly containing the resource */
    public Collection<LayerGroupSummary> getContainerGroupsFor(ResourceInfo resource) {
        String id = resource.getId();
        Set<LayerGroupSummary> groups = resourceContainmentCache.get(id);
        if (groups == null) {
            return Collections.emptyList();
        }
        Set<LayerGroupSummary> result = new HashSet<>();
        for (LayerGroupSummary lg : groups) {
            collectContainers(lg, result);
        }
        return result;
    }

    /**
     * Returns all groups containing directly or indirectly the specified group, and relevant for
     * security (e.g., anything but {@link LayerGroupInfo.Mode#SINGLE} ones
     */
    public Collection<LayerGroupSummary> getContainerGroupsFor(LayerGroupInfo lg) {
        String id = lg.getId();
        if (id == null) {
            return Collections.emptyList();
        }
        LayerGroupSummary summary = groupCache.get(id);
        if (summary == null) {
            return Collections.emptyList();
        }

        Set<LayerGroupSummary> result = new HashSet<>();
        for (LayerGroupSummary container : summary.getContainerGroups()) {
            collectContainers(container, result);
        }
        return result;
    }

    /**
     * Recursively collects the group and all its containers in the <data>groups</data> collection
     */
    private void collectContainers(LayerGroupSummary lg, Set<LayerGroupSummary> groups) {
        if (!groups.contains(lg)) {
            if (lg.getMode() != LayerGroupInfo.Mode.SINGLE) {
                groups.add(lg);
            }
            for (LayerGroupSummary container : lg.containerGroups) {
                collectContainers(container, groups);
            }
        }
    }

    /**
     * Information summary about a layer group, just enough information to avoid performing linear
     * searches against the catalog to match against rules and scan layer containment upwards
     */
    static class LayerGroupSummary {
        String id;

        String workspace;

        String name;

        LayerGroupInfo.Mode mode;

        Set<LayerGroupSummary> containerGroups;

        LayerGroupSummary(LayerGroupInfo lg) {
            this.id = lg.getId();
            this.workspace = lg.getWorkspace() != null ? lg.getWorkspace().getName() : null;
            this.name = lg.getName();
            this.mode = lg.getMode();
            containerGroups = CONCURRENT_SET_BUILDER.apply(null);
        }

        LayerGroupSummary(LayerGroupSummary other) {
            this.id = other.id;
            this.workspace = other.workspace;
            this.name = other.name;
            this.mode = other.mode;
            containerGroups = other.containerGroups;
        }

        public String getId() {
            return id;
        }

        public String getWorkspace() {
            return workspace;
        }

        public String getName() {
            return name;
        }

        public LayerGroupInfo.Mode getMode() {
            return mode;
        }

        public Set<LayerGroupSummary> getContainerGroups() {
            return containerGroups;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof LayerGroupSummary)) {
                return false;
            }

            LayerGroupSummary other = (LayerGroupSummary) obj;
            return Objects.equals(this.id, other.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id);
        }

        public String[] getPath() {
            if (workspace == null) {
                return new String[] {name};
            } else {
                return new String[] {workspace, name};
            }
        }

        @Override
        public String toString() {
            return "LayerGroupSummary [id="
                    + id
                    + ", workspace="
                    + workspace
                    + ", name="
                    + name
                    + ", mode="
                    + mode
                    + ", containerGroups="
                    + containerGroups
                    + "]";
        }

        public String prefixedName() {
            if (workspace == null) {
                return name;
            } else {
                return workspace + ":" + name;
            }
        }
    }

    /**
     * This listener keeps the "layer group" flags in the authorization tree current, in order to
     * optimize the application of layer group containment rules
     */
    final class CatalogChangeListener implements CatalogListener {

        @Override
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
                addGroupInfo(lg);
                registerContainedGroups(lg);
            }
        }

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            if (event.getSource() instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
                clearGroupInfo(lg);
            }
            // no need to listen to workspace or layer removal, these will cascade to
            // layer groups
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
            final CatalogInfo source = event.getSource();
            if (source instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
                // was the layer group renamed, moved, or its contents changed?
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateGroupName(lg.getId(), newName);
                }
                int wsIdx = event.getPropertyNames().indexOf("workspace");
                if (wsIdx != -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(wsIdx);
                    updateGroupWorkspace(lg.getId(), newWorkspace);
                }
                int layerIdx = event.getPropertyNames().indexOf("layers");
                if (layerIdx != -1) {
                    List<PublishedInfo> oldLayers =
                            (List<PublishedInfo>) event.getOldValues().get(layerIdx);
                    List<PublishedInfo> newLayers =
                            (List<PublishedInfo>) event.getNewValues().get(layerIdx);
                    updateContainedLayers(groupCache.get(lg.getId()), oldLayers, newLayers);
                }
                int modeIdx = event.getPropertyNames().indexOf("mode");
                if (modeIdx != -1) {
                    Mode newMode = (Mode) event.getNewValues().get(modeIdx);
                    updateGroupMode(lg.getId(), newMode);
                }
            } else if (source instanceof WorkspaceInfo) {
                int nameIdx = event.getPropertyNames().indexOf("name");
                if (nameIdx != -1) {
                    String oldName = (String) event.getOldValues().get(nameIdx);
                    String newName = (String) event.getNewValues().get(nameIdx);
                    updateWorkspaceNames(oldName, newName);
                }
            }
        }

        private void updateGroupMode(String id, Mode newMode) {
            LayerGroupSummary summary = groupCache.get(id);
            summary.mode = newMode;
        }

        private void updateContainedLayers(
                LayerGroupSummary groupSummary,
                List<PublishedInfo> oldLayers,
                List<PublishedInfo> newLayers) {

            // process layers that are no more contained
            final HashSet<PublishedInfo> removedLayers = new HashSet<>(oldLayers);
            removedLayers.removeAll(newLayers);
            for (PublishedInfo removed : removedLayers) {
                if (removed instanceof LayerInfo) {
                    String resourceId = ((LayerInfo) removed).getResource().getId();
                    Set<LayerGroupSummary> containers = resourceContainmentCache.get(resourceId);
                    if (containers != null) {
                        synchronized (resourceId) {
                            containers.remove(groupSummary);
                            if (containers.isEmpty()) {
                                resourceContainmentCache.remove(resourceId, containers);
                            }
                        }
                    }
                } else {
                    LayerGroupInfo child = (LayerGroupInfo) removed;
                    LayerGroupSummary summary = groupCache.get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.remove(groupSummary);
                    }
                }
            }

            // add the layers that are newly contained
            final HashSet<PublishedInfo> addedLayers = new HashSet<>(newLayers);
            addedLayers.removeAll(oldLayers);
            for (PublishedInfo added : addedLayers) {
                if (added instanceof LayerInfo) {
                    String resourceId = ((LayerInfo) added).getResource().getId();
                    synchronized (resourceId) {
                        Set<LayerGroupSummary> containers =
                                resourceContainmentCache.computeIfAbsent(
                                        resourceId, CONCURRENT_SET_BUILDER);
                        containers.add(groupSummary);
                    }
                } else {
                    LayerGroupInfo child = (LayerGroupInfo) added;
                    LayerGroupSummary summary = groupCache.get(child.getId());
                    if (summary != null) {
                        summary.containerGroups.add(groupSummary);
                    }
                }
            }
        }

        private void updateGroupWorkspace(String id, WorkspaceInfo newWorkspace) {
            LayerGroupSummary summary = groupCache.get(id);
            if (summary != null) {
                summary.workspace = newWorkspace == null ? null : newWorkspace.getName();
            }
        }

        private void updateGroupName(String id, String newName) {
            LayerGroupSummary summary = groupCache.get(id);
            if (summary != null) {
                summary.name = newName;
            }
        }

        private void updateWorkspaceNames(String oldName, String newName) {
            groupCache
                    .values()
                    .stream()
                    .filter(lg -> Objects.equals(lg.workspace, oldName))
                    .forEach(lg -> lg.workspace = newName);
        }

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
            // nothing to do here

        }

        @Override
        public void reloaded() {
            // rebuild the containment cache
            buildLayerGroupCaches();
        }
    }
}
