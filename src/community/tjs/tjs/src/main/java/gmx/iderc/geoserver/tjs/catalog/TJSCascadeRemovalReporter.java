/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.catalog;

import org.geoserver.catalog.CascadeDeleteVisitor;

import java.util.*;

/**
 * Visits the specified objects cascading down to contained/related objects,
 * and collects information about which objects will be removed or modified
 * once the root objects are cascade deleted with {@link CascadeDeleteVisitor}
 */
public class TJSCascadeRemovalReporter implements TJSCatalogVisitor {

    public void visit(TJSCatalogObject object) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void visit(FrameworkInfo framework) {
        //buscar los datasets que dependen de este framework y agregarlos porque se van a
        //borrar, Alvaro Javier Fuentes Suarez
        List<DatasetInfo> datasets = catalog.getDatasetsByFramework(framework.getId());
        for (DatasetInfo datasetInfo : datasets) {
            datasetInfo.accept(this);
        }

        add(framework, ModificationType.DELETE);
    }

    public void visit(DatasetInfo dataset) {
        add(dataset, ModificationType.DELETE);
    }

    @Override
    public void visit(JoinedMapInfo joinedMap) {
        //nada por ahora, Alvaro Javier
    }

    public void visit(DataStoreInfo dataStore) {
        //buscar los datasets que dependen de este dataset y agregarlos porque se van a
        //borrar, Alvaro Javier Fuentes Suarez
        List<DatasetInfo> datasets = catalog.getDatasets(dataStore.getId());
        for (DatasetInfo datasetInfo : datasets) {
            datasetInfo.accept(this);
        }
        add(dataStore, ModificationType.DELETE);
    }

    /**
     * The various types of modifications a catalog object can be subjected to
     * in case of cascade removal. They are sorted from stronger to weaker.
     */
    public enum ModificationType {
        DELETE, STYLE_RESET, EXTRA_STYLE_REMOVED, GROUP_CHANGED;
    }

    /**
     * The catalog used to drill down into the containment hierarchy
     */
    TJSCatalog catalog;

    /**
     * The set of objects collected during the scan
     */
    Map<TJSCatalogObject, ModificationType> objects;

    /**
     * Used to track which layers are going to be removed from a group, if
     * we remove them all the group will have to be removed as well
     */
//    Map<LayerGroupInfo, Set<LayerInfo>> groups;
    public TJSCascadeRemovalReporter(TJSCatalog catalog) {
        this.catalog = catalog;
        reset();
    }

    public void visit(TJSCatalog catalog) {

    }

    /**
     * Resets the visitor so that it can be reused for another search
     */
    public void reset() {
        this.objects = new HashMap<TJSCatalogObject, ModificationType>();
        //this.groups = new HashMap<LayerGroupInfo, Set<LayerInfo>>();
    }

    /**
     * Returns the objects that will be affected by the removal, filtering them by type and by kind
     * of modification they will sustain as a consequence of the removal
     *
     * @param <T>
     * @param catalogClass  The type of object to be searched for, or null if no type filtering is desired
     * @param modifications The kind of modification to be searched for, or null if no modification type
     *                      filtering is desired
     * @return
     */
    public <T> List<T> getObjects(Class<T> catalogClass, ModificationType... modifications) {
        List<T> result = new ArrayList<T>();
        List<ModificationType> mods = (modifications == null || modifications.length == 0) ?
                                              null : Arrays.asList(modifications);
        for (TJSCatalogObject ci : objects.keySet()) {
            if (catalogClass == null || catalogClass.isAssignableFrom(ci.getClass())) {
                if (mods == null || mods.contains(objects.get(ci)))
                    result.add((T) ci);
            }
        }
        return result;
    }

    /**
     * Allows removal of the specified objects from the reachable set (usually, the user
     * will not want the roots to be part of the set)
     *
     * @param objects
     */
    public void removeAll(Collection<? extends TJSCatalogObject> objects) {
        for (TJSCatalogObject ci : objects) {
            this.objects.remove(ci);
        }
    }

    /**
     * Adds a TJSCatalogObject into the objects map, eventually overriding the
     * type if the modification is stronger that the one already registered
     */
    void add(TJSCatalogObject ci, ModificationType type) {
        ModificationType oldType = objects.get(ci);
        if (oldType == null || oldType.compareTo(type) > 0) {
            objects.put(ci, type);
        }
    }


}
