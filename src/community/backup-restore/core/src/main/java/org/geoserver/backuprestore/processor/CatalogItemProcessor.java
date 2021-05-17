/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.processor;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemProcessor;

/**
 * Concrete Spring Batch {@link ItemProcessor}.
 *
 * <p>Processes {@link Catalog} resource items while reading.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogItemProcessor<T> extends BackupRestoreItem<T> implements ItemProcessor<T, T> {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogItemProcessor.class);

    Class<T> clazz;

    @Override
    protected void initialize(StepExecution stepExecution) {}

    /** Default Constructor. */
    public CatalogItemProcessor(Class<T> clazz, Backup backupFacade) {
        super(backupFacade);
        this.clazz = clazz;
    }

    /** @return the clazz */
    public Class<T> getClazz() {
        return clazz;
    }

    @Override
    public T process(T item) throws Exception {
        LOGGER.log(Level.FINE, "Processing resource: {0}", item);
        if (item == null) {
            return null;
        }
        authenticate();

        if (isNew()) {
            // Disabling additional validators
            ((CatalogImpl) getCatalog()).setExtendedValidation(false);
            LOGGER.log(Level.FINE, "Extended validation disabled for resource: {0}", item);

            // Resolving Collections
            OwsUtils.resolveCollections(item);
        }

        LOGGER.info(
                () ->
                        String.format(
                                "Processing resource: %s - Progress: [%s]",
                                item, getCurrentJobExecution().getProgress()));

        if (item instanceof WorkspaceInfo) return process((WorkspaceInfo) item);
        if (item instanceof CoverageStoreInfo) return process((CoverageStoreInfo) item);
        if (item instanceof DataStoreInfo) return process((DataStoreInfo) item);
        if (item instanceof ResourceInfo) return process((ResourceInfo) item);
        if (item instanceof LayerInfo) return process((LayerInfo) item);
        if (item instanceof LayerGroupInfo) return process((LayerGroupInfo) item);
        if (item instanceof StyleInfo) return process((StyleInfo) item);

        return item;
    }

    private T process(DataStoreInfo ds) throws Exception {
        LOGGER.log(Level.FINE, "Processing datastore: {0}", ds);
        WorkspaceInfo ws = resolveWorkspace(ds);

        if (ws == null && filterIsValid()) {
            Catalog catalog = getCatalog();
            DataStoreInfo source = backupFacade.getCatalog().getDataStoreByName(ds.getName());
            LOGGER.log(Level.FINE, "Found source datastore: {0}", source);
            if (source != null && source.getWorkspace() != null) {
                ws = catalog.getWorkspaceByName(source.getWorkspace().getName());
                if (ws == null) {
                    LOGGER.log(Level.WARNING, "Workspace not found for datastore: {0}", ds);
                    return null;
                }
                ds.setWorkspace(ws);
                catalog.add(ds);
                DataStoreInfo addedDs = catalog.getDataStore(ds.getId());
                catalog.save(addedDs);
                LOGGER.log(Level.FINE, "Saved datastore into catalog: {0}", addedDs);
            }
        }

        if (filteredResource(getClazz().cast(ds), ws, true, StoreInfo.class)) {
            LOGGER.log(Level.FINE, "Filtered out datastore : {0}", ds);
            return null;
        }

        if (!filterIsValid() && !validateDataStore(ds, isNew())) {
            LOGGER.log(Level.WARNING, "Skipped invalid resource: {0}", ds);
            logValidationExceptions(getClazz().cast(ds), null);
            return null;
        }
        return getClazz().cast(ds);
    }

    private T process(StyleInfo style) throws Exception {
        ValidationResult result = null;
        try {
            WorkspaceInfo ws = resolveWorkspace(style);

            if (filteredResource(getClazz().cast(style), ws, false, StyleInfo.class)) {
                return null;
            }

            if (!filterIsValid()) {
                Catalog catalog = getCatalog();
                result = catalog.validate(style, isNew());
                if (!result.isValid()) {
                    LOGGER.log(Level.SEVERE, "Style is not valid: {0}", style);
                    logValidationResult(result, style);
                    logValidationExceptions(getClazz().cast(style), null);
                    return null;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception during style processing.", e);
            logValidationExceptions(result, e);
            return null;
        }
        return getClazz().cast(style);
    }

    private T process(LayerGroupInfo lg) {
        ValidationResult result = null;
        try {
            WorkspaceInfo ws = resolveWorkspace(lg);

            if (filteredResource(getClazz().cast(lg), ws, false, LayerGroupInfo.class)) {
                return null;
            }

            if (!filterIsValid()) {
                Catalog catalog = getCatalog();
                result = catalog.validate(lg, isNew());
                if (!result.isValid()) {
                    LOGGER.log(Level.SEVERE, "LayerGroup is not valid: {0}", lg);
                    logValidationResult(result, lg);
                    logValidationExceptions(getClazz().cast(lg), null);
                    return null;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to process a Resource!", e);
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
        }
        return getClazz().cast(lg);
    }

    private T process(LayerInfo layer) throws Exception {
        ValidationResult result = null;
        try {
            ResourceInfo layerResouce = layer.getResource();
            if (layerResouce == null) {
                return null;
            }

            WorkspaceInfo ws = resolveWorkspace(layer);

            if (filteredResource(getClazz().cast(layer), ws, true, LayerInfo.class)) {
                return null;
            }

            if (!filterIsValid()) {
                Catalog catalog = getCatalog();
                result = catalog.validate(layer, isNew());
                if (!result.isValid()) {
                    LOGGER.log(Level.SEVERE, "Layer is not valid: {0}", layer);
                    logValidationResult(result, layer);
                    logValidationExceptions(getClazz().cast(layer), null);
                    return null;
                }
            }
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not validate the resource "
                            + layer
                            + " due to the following issue: "
                            + e.getLocalizedMessage());
            logValidationExceptions(result, e);
            return null;
        }
        return getClazz().cast(layer);
    }

    private T process(ResourceInfo resource) {
        StoreInfo itemStore = resource.getStore();
        WorkspaceInfo ws = resolveWorkspace(resource);

        if (itemStore == null && filterIsValid()) {
            Catalog catalog = getCatalog();
            Class<? extends ResourceInfo> clz = null;
            Class<? extends StoreInfo> storeClz = null;
            if (resource instanceof FeatureTypeInfo) {
                clz = FeatureTypeInfo.class;
                storeClz = DataStoreInfo.class;
            } else if (resource instanceof CoverageInfo) {
                clz = CoverageInfo.class;
                storeClz = CoverageStoreInfo.class;
            }

            ResourceInfo source =
                    backupFacade.getCatalog().getResourceByName(resource.getName(), clz);
            if (source != null && source.getStore() != null) {
                StoreInfo store = catalog.getStoreByName(source.getStore().getName(), storeClz);
                if (store == null) {
                    LOGGER.log(Level.SEVERE, "Resource info not found on catalog: {0}", resource);
                    return null;
                }
                resource.setStore(store);
                catalog.add(resource);
                catalog.save(catalog.getResource(resource.getId(), clz));
            }
        }

        if (filteredResource(getClazz().cast(resource), ws, true, ResourceInfo.class)) {
            LOGGER.log(Level.FINE, "Resource filtered out: {0}", resource);
            return null;
        }

        /*
         * if (!filterIsValid() && resource == null && !validateResource((ResourceInfo) resource,
         * isNew())) { LOGGER.warning("Skipped invalid resource: " + resource);
         * logValidationExceptions(resource, null); return null; }
         */
        return getClazz().cast(resource);
    }

    private T process(CoverageStoreInfo store) throws Exception {
        WorkspaceInfo ws = resolveWorkspace(store);

        if (ws == null && filterIsValid()) {
            Catalog catalog = getCatalog();
            CoverageStoreInfo source =
                    backupFacade.getCatalog().getCoverageStoreByName(store.getName());
            if (source != null && source.getWorkspace() != null) {
                ws = catalog.getWorkspaceByName(source.getWorkspace().getName());
                if (ws == null) {
                    LOGGER.log(Level.SEVERE, "Workspace not found for store: {0}", store);
                    return null;
                }
                store.setWorkspace(ws);
                catalog.add(store);
                catalog.save(catalog.getCoverageStore(store.getId()));
            }
        }

        if (filteredResource(getClazz().cast(store), ws, true, StoreInfo.class)) {
            LOGGER.log(Level.FINE, "Store filtered out: {0}", store);
            return null;
        }

        if (!filterIsValid() && !validateCoverageStore(store, isNew())) {
            LOGGER.warning("Skipped invalid resource: " + store);
            logValidationExceptions(getClazz().cast(store), null);
            return null;
        }
        return getClazz().cast(store);
    }

    private T process(WorkspaceInfo item) throws Exception {
        if (filteredResource(item, false)) {
            return null;
        }
        if (filterIsValid() && null == resolveWorkspace(item)) {
            Catalog catalog = getCatalog();
            catalog.add(item);
            catalog.save(catalog.getWorkspace(item.getId()));
        }

        if (!filterIsValid() && !validateWorkspace(item, isNew())) {
            LOGGER.warning("Skipped invalid resource: " + item);
            logValidationExceptions(getClazz().cast(item), null);
            return null;
        }
        return getClazz().cast(item);
    }

    /**
     * Being sure the associated {@link NamespaceInfo} exists and is available on the GeoServer
     * Catalog.
     *
     * @param {@link WorkspaceInfo} resource
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateWorkspace(WorkspaceInfo resource, boolean isNew) throws Exception {
        final NamespaceInfo ns = this.getCatalog().getNamespaceByPrefix(resource.getName());
        if (ns == null) {
            return false;
        }

        ValidationResult result = null;
        try {
            result = this.getCatalog().validate(resource, isNew);
            if (!result.isValid()) {
                LOGGER.log(Level.SEVERE, "Workspace is not valid: {0}", resource);
                logValidationResult(result, resource);
            }
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not validate the resource "
                            + resource
                            + " due to the following issue: "
                            + e.getLocalizedMessage());
            logValidationExceptions(result, e);
            return false;
        }

        return true;
    }

    /**
     * Being sure the associated {@link WorkspaceInfo} exists and is available on the GeoServer
     * Catalog.
     *
     * <p>Also if a default {@link DataStoreInfo} has not been defined for the current {@link
     * WorkspaceInfo}, set this one as default.
     *
     * @param {@link DataStoreInfo} resource
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateDataStore(DataStoreInfo resource, boolean isNew) throws Exception {
        final WorkspaceInfo ws =
                this.getCatalog().getWorkspaceByName(resource.getWorkspace().getName());
        if (ws == null) {
            return false;
        }

        ValidationResult result = null;
        try {
            result = this.getCatalog().validate(resource, isNew);
            if (!result.isValid()) {
                LOGGER.log(Level.SEVERE, "Store is not valid: {0}", resource);
                logValidationResult(result, resource);
            }
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not validate the resource "
                            + resource
                            + " due to the following issue: "
                            + e.getLocalizedMessage());
            logValidationExceptions(result, e);
            return false;
        }

        resource.setWorkspace(ws);

        return true;
    }

    /**
     * Being sure the associated {@link WorkspaceInfo} exists and is available on the GeoServer
     * Catalog.
     *
     * @param {@link CoverageStoreInfo} resource
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateCoverageStore(CoverageStoreInfo resource, boolean isNew)
            throws Exception {
        final WorkspaceInfo ws =
                this.getCatalog().getWorkspaceByName(resource.getWorkspace().getName());
        if (ws == null) {
            return false;
        }

        ValidationResult result = null;
        try {
            result = this.getCatalog().validate(resource, isNew);
            if (!result.isValid()) {
                LOGGER.log(Level.SEVERE, "Store is not valid: {0}", resource);
                logValidationResult(result, resource);
            }
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not validate the resource "
                            + resource
                            + " due to the following issue: "
                            + e.getLocalizedMessage());
            return logValidationExceptions(result, e);
        }

        resource.setWorkspace(ws);

        return true;
    }

    /**
     * Being sure the associated {@link StoreInfo} exists and is available on the GeoServer Catalog.
     *
     * @param {@link ResourceInfo} resource
     * @return boolean indicating whether the resource is valid or not.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean validateResource(ResourceInfo resource, boolean isNew) {
        try {
            final StoreInfo store = resource.getStore();
            final NamespaceInfo namespace = resource.getNamespace();

            if (store == null) {
                return logValidationExceptions((T) resource, null);
            }

            final Class storeClazz =
                    (store instanceof DataStoreInfo
                            ? DataStoreInfo.class
                            : CoverageStoreInfo.class);
            final StoreInfo ds = this.getCatalog().getStoreByName(store.getName(), storeClazz);

            if (ds != null) {
                resource.setStore(ds);
            } else {
                return logValidationExceptions((T) resource, null);
            }

            ResourceInfo existing =
                    getCatalog().getResourceByStore(store, resource.getName(), ResourceInfo.class);
            if (existing != null && !existing.getId().equals(resource.getId())) {
                final String msg =
                        "Resource named '"
                                + resource.getName()
                                + "' already exists in store: '"
                                + store.getName()
                                + "'";
                return logValidationExceptions((T) resource, new RuntimeException(msg));
            }

            existing =
                    getCatalog()
                            .getResourceByName(namespace, resource.getName(), ResourceInfo.class);
            if (existing != null && !existing.getId().equals(resource.getId())) {
                final String msg =
                        "Resource named '"
                                + resource.getName()
                                + "' already exists in namespace: '"
                                + namespace.getPrefix()
                                + "'";
                return logValidationExceptions((T) resource, new RuntimeException(msg));
            }

            return true;
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not validate the resource "
                            + resource
                            + " due to the following issue: "
                            + e.getLocalizedMessage());
            return logValidationExceptions((T) resource, e);
        }
    }

    private WorkspaceInfo resolveWorkspace(CatalogInfo item) {
        WorkspaceInfo ws = null;
        if (item instanceof WorkspaceInfo) {
            ws = (WorkspaceInfo) item;
        } else if (item instanceof StoreInfo) {
            ws = ((StoreInfo) item).getWorkspace();
        } else if (item instanceof ResourceInfo) {
            StoreInfo store = ((ResourceInfo) item).getStore();
            ws = store == null ? null : store.getWorkspace();
        } else if (item instanceof LayerInfo) {
            ResourceInfo resource = ((LayerInfo) item).getResource();
            StoreInfo store = resource == null ? null : resource.getStore();
            ws = store == null ? null : store.getWorkspace();
        } else if (item instanceof LayerGroupInfo) {
            ws = ((LayerGroupInfo) item).getWorkspace();
        } else if (item instanceof StyleInfo) {
            ws = ((StyleInfo) item).getWorkspace();
        } else {
            throw new IllegalArgumentException("Don't know how to extract workspace from " + item);
        }
        return ws == null ? null : getCatalog().getWorkspaceByName(ws.getName());
    }

    private void logValidationResult(ValidationResult result, CatalogInfo resourceInfo) {
        if (result == null || result.getErrors() == null) {
            return;
        }
        for (RuntimeException ex : result.getErrors()) {
            LOGGER.log(
                    Level.SEVERE,
                    "Exception during validation for resource info: " + resourceInfo,
                    ex);
        }
    }
}
