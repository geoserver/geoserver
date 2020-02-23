/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.util.Arrays;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 *
 * <p>Writes unmarshalled items into the temporary {@link Catalog} in memory.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogItemWriter<T> extends CatalogWriter<T> {

    public CatalogItemWriter(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        if (this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(List<? extends T> items) {
        for (T item : items) {
            try {
                if (item instanceof WorkspaceInfo) {
                    WorkspaceInfo wsInfo = (WorkspaceInfo) item;
                    WorkspaceInfo source = getCatalog().getWorkspaceByName(wsInfo.getName());
                    if (source == null) {
                        getCatalog().add(wsInfo);
                        getCatalog().save(getCatalog().getWorkspace(wsInfo.getId()));
                    }
                } else if (item instanceof NamespaceInfo) {
                    NamespaceInfo source =
                            getCatalog().getNamespaceByPrefix(((NamespaceInfo) item).getPrefix());
                    if (source == null) {
                        getCatalog().add((NamespaceInfo) item);
                        getCatalog()
                                .save(getCatalog().getNamespace(((NamespaceInfo) item).getId()));
                    }
                } else if (item instanceof DataStoreInfo) {
                    DataStoreInfo dsInfo = (DataStoreInfo) item;
                    DataStoreInfo source = getCatalog().getDataStoreByName(dsInfo.getName());
                    if (source == null) {
                        getCatalog().add(dsInfo);
                        getCatalog().save(getCatalog().getDataStore(dsInfo.getId()));
                    }
                } else if (item instanceof CoverageStoreInfo) {
                    CoverageStoreInfo source =
                            getCatalog()
                                    .getCoverageStoreByName(((CoverageStoreInfo) item).getName());
                    if (source == null) {
                        getCatalog().add((CoverageStoreInfo) item);
                        getCatalog()
                                .save(
                                        getCatalog()
                                                .getCoverageStore(
                                                        ((CoverageStoreInfo) item).getId()));
                    }
                } else if (item instanceof ResourceInfo) {
                    ResourceInfo resourceInfo = (ResourceInfo) item;
                    if (getCatalog()
                                            .getResourceByName(
                                                    resourceInfo.getName(), FeatureTypeInfo.class)
                                    == null
                            && getCatalog()
                                            .getResourceByName(
                                                    resourceInfo.getName(), CoverageInfo.class)
                                    == null) {
                        Class clz = null;
                        if (item instanceof FeatureTypeInfo) {
                            clz = FeatureTypeInfo.class;
                        } else if (item instanceof CoverageInfo) {
                            clz = CoverageInfo.class;
                        }
                        getCatalog().add(resourceInfo);
                        getCatalog().save(getCatalog().getResource(resourceInfo.getId(), clz));
                    }
                } else if (item instanceof LayerInfo) {
                    LayerInfo layerInfo = (LayerInfo) item;
                    if (layerInfo.getName() != null) {
                        LayerInfo source = getCatalog().getLayerByName(layerInfo.getName());
                        if (source == null) {
                            getCatalog().add((LayerInfo) item);
                            getCatalog().save(getCatalog().getLayer(layerInfo.getId()));
                        }
                    }
                } else if (item instanceof StyleInfo) {
                    StyleInfo source = getCatalog().getStyleByName(((StyleInfo) item).getName());
                    if (source == null) {
                        getCatalog().add((StyleInfo) item);
                        getCatalog().save(getCatalog().getStyle(((StyleInfo) item).getId()));
                    }
                } else if (item instanceof LayerGroupInfo) {
                    try {
                        LayerGroupInfo layerGroupInfo = (LayerGroupInfo) item;
                        getCatalog().add(layerGroupInfo);
                        getCatalog().save(getCatalog().getLayerGroup(layerGroupInfo.getId()));
                    } catch (Exception e) {
                        if (getCurrentJobExecution() != null) {
                            getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                        }
                    }
                }
            } catch (Exception e) {
                logValidationExceptions((T) null, e);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Nothing to do.
    }

    /** Setter for resource. Represents a file that can be written. */
    @Override
    public void setResource(Resource resource) {
        // Nothing to do
    }
}
