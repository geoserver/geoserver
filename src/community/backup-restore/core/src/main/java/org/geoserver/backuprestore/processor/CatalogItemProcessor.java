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
    public T process(T resource) throws Exception {
        if (resource != null) {

            authenticate();

            if (isNew()) {
                // Disabling additional validators
                ((CatalogImpl) getCatalog()).setExtendedValidation(false);

                // Resolving Collections
                OwsUtils.resolveCollections(resource);
            }

            LOGGER.info(
                    "Processing resource: "
                            + resource
                            + " - Progress: ["
                            + getCurrentJobExecution().getProgress()
                            + "]");

            if (resource instanceof WorkspaceInfo) {
                WorkspaceInfo ws = ((WorkspaceInfo) resource);

                if (filteredResource(ws, false) && ws != null) {
                    return null;
                }

                if (filterIsValid() && getCatalog().getWorkspaceByName(ws.getName()) == null) {
                    getCatalog().add(ws);
                    getCatalog().save(getCatalog().getWorkspace(ws.getId()));
                }

                if (!filterIsValid() && !validateWorkspace((WorkspaceInfo) resource, isNew())) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    logValidationExceptions(resource, null);
                    return null;
                }
            } else if (resource instanceof DataStoreInfo) {
                WorkspaceInfo ws =
                        ((DataStoreInfo) resource).getWorkspace() != null
                                ? getCatalog()
                                        .getWorkspaceByName(
                                                ((DataStoreInfo) resource).getWorkspace().getName())
                                : null;

                if (ws == null && filterIsValid()) {
                    DataStoreInfo source =
                            backupFacade
                                    .getCatalog()
                                    .getDataStoreByName(((DataStoreInfo) resource).getName());
                    if (source != null && source.getWorkspace() != null) {
                        ws = getCatalog().getWorkspaceByName(source.getWorkspace().getName());
                        if (ws == null) {
                            return null;
                        }
                        ((DataStoreInfo) resource).setWorkspace(ws);
                        getCatalog().add(((DataStoreInfo) resource));
                        getCatalog()
                                .save(
                                        getCatalog()
                                                .getDataStore(((DataStoreInfo) resource).getId()));
                    }
                }

                if (filteredResource(resource, ws, true, StoreInfo.class)) {
                    return null;
                }

                if (!filterIsValid() && !validateDataStore((DataStoreInfo) resource, isNew())) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    logValidationExceptions(resource, null);
                    return null;
                }
            } else if (resource instanceof CoverageStoreInfo) {
                WorkspaceInfo ws =
                        ((CoverageStoreInfo) resource).getWorkspace() != null
                                ? getCatalog()
                                        .getWorkspaceByName(
                                                ((CoverageStoreInfo) resource)
                                                        .getWorkspace()
                                                        .getName())
                                : null;

                if (ws == null && filterIsValid()) {
                    CoverageStoreInfo source =
                            backupFacade
                                    .getCatalog()
                                    .getCoverageStoreByName(
                                            ((CoverageStoreInfo) resource).getName());
                    if (source != null && source.getWorkspace() != null) {
                        ws = getCatalog().getWorkspaceByName(source.getWorkspace().getName());
                        if (ws == null) {
                            return null;
                        }
                        ((CoverageStoreInfo) resource).setWorkspace(ws);
                        getCatalog().add(((CoverageStoreInfo) resource));
                        getCatalog()
                                .save(
                                        getCatalog()
                                                .getCoverageStore(
                                                        ((CoverageStoreInfo) resource).getId()));
                    }
                }

                if (filteredResource(resource, ws, true, StoreInfo.class)) {
                    return null;
                }

                if (!filterIsValid()
                        && !validateCoverageStore((CoverageStoreInfo) resource, isNew())) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    logValidationExceptions(resource, null);
                    return null;
                }
            } else if (resource instanceof ResourceInfo) {
                WorkspaceInfo ws =
                        ((ResourceInfo) resource).getStore() != null
                                        && ((ResourceInfo) resource).getStore().getWorkspace()
                                                != null
                                ? getCatalog()
                                        .getWorkspaceByName(
                                                ((ResourceInfo) resource)
                                                        .getStore()
                                                        .getWorkspace()
                                                        .getName())
                                : null;

                if (((ResourceInfo) resource).getStore() == null && filterIsValid()) {
                    Class clz = null;
                    if (resource instanceof FeatureTypeInfo) {
                        clz = FeatureTypeInfo.class;
                    } else if (resource instanceof CoverageInfo) {
                        clz = CoverageInfo.class;
                    }
                    ResourceInfo source =
                            backupFacade
                                    .getCatalog()
                                    .getResourceByName(((ResourceInfo) resource).getName(), clz);
                    if (source != null && source.getStore() != null) {
                        StoreInfo ds =
                                getCatalog().getStoreByName(source.getStore().getName(), clz);
                        if (ds == null) {
                            return null;
                        }
                        ((ResourceInfo) resource).setStore(ds);
                        getCatalog().add(((ResourceInfo) resource));
                        getCatalog()
                                .save(
                                        getCatalog()
                                                .getResource(
                                                        ((ResourceInfo) resource).getId(), clz));
                    }
                }

                if (filteredResource(resource, ws, true, ResourceInfo.class)) {
                    return null;
                }

                /* if (!filterIsValid()
                        && resource == null
                        && !validateResource((ResourceInfo) resource, isNew())) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    logValidationExceptions(resource, null);
                    return null;
                } */
            } else if (resource instanceof LayerInfo) {
                ValidationResult result = null;
                try {
                    WorkspaceInfo ws =
                            ((LayerInfo) resource).getResource() != null
                                            && ((LayerInfo) resource).getResource().getStore()
                                                    != null
                                            && ((LayerInfo) resource)
                                                            .getResource()
                                                            .getStore()
                                                            .getWorkspace()
                                                    != null
                                    ? getCatalog()
                                            .getWorkspaceByName(
                                                    ((LayerInfo) resource)
                                                            .getResource()
                                                            .getStore()
                                                            .getWorkspace()
                                                            .getName())
                                    : null;

                    if (((LayerInfo) resource).getResource() == null) {
                        return null;
                    }

                    if (filteredResource(resource, ws, true, LayerInfo.class)) {
                        return null;
                    }

                    if (!filterIsValid() && resource != null) {
                        result = this.getCatalog().validate((LayerInfo) resource, isNew());
                        if (!result.isValid()) {
                            logValidationExceptions(resource, null);
                            return null;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning(
                            "Could not validate the resource "
                                    + resource
                                    + " due to the following issue: "
                                    + e.getLocalizedMessage());
                    logValidationExceptions(result, e);
                    return null;
                }
            } else if (resource instanceof StyleInfo) {
                ValidationResult result = null;
                try {
                    WorkspaceInfo ws =
                            ((StyleInfo) resource).getWorkspace() != null
                                    ? getCatalog()
                                            .getWorkspaceByName(
                                                    ((StyleInfo) resource).getWorkspace().getName())
                                    : null;

                    if (filteredResource(resource, ws, false, StyleInfo.class)) {
                        return null;
                    }

                    if (!filterIsValid() && resource != null) {
                        result = this.getCatalog().validate((StyleInfo) resource, isNew());
                        if (!result.isValid()) {
                            logValidationExceptions(resource, null);
                            return null;
                        }
                    }
                } catch (Exception e) {
                    logValidationExceptions(result, e);
                    return null;
                }
            } else if (resource instanceof LayerGroupInfo) {
                ValidationResult result = null;
                try {
                    WorkspaceInfo ws =
                            ((LayerGroupInfo) resource).getWorkspace() != null
                                    ? getCatalog()
                                            .getWorkspaceByName(
                                                    ((LayerGroupInfo) resource)
                                                            .getWorkspace()
                                                            .getName())
                                    : null;

                    if (filteredResource(resource, ws, false, LayerGroupInfo.class)) {
                        return null;
                    }

                    if (!filterIsValid() && resource != null) {
                        result = this.getCatalog().validate((LayerGroupInfo) resource, isNew());
                        if (!result.isValid()) {
                            logValidationExceptions(resource, null);
                            return null;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING, "Error occurred while trying to Reload a Resource!", e);
                    if (getCurrentJobExecution() != null) {
                        getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                    }
                }
            }

            return resource;
        }

        return null;
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
}
