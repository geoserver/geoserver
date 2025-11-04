/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.NonNull;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 *
 * <p>Writes unmarshalled items into the temporary {@link Catalog} in memory.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogItemWriter<T> extends CatalogWriter<T> {

    private static final Logger LOGGER = Logging.getLogger(CatalogItemWriter.class);

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
    public void write(@NonNull Chunk<? extends T> chunk) {
        for (T item : chunk) {
            try {
                if (item instanceof WorkspaceInfo info9) {
                    write(info9);
                } else if (item instanceof NamespaceInfo info8) {
                    write(info8);
                } else if (item instanceof DataStoreInfo info7) {
                    write(info7);
                } else if (item instanceof WMSStoreInfo info6) {
                    write(info6);
                } else if (item instanceof WMTSStoreInfo info5) {
                    write(info5);
                } else if (item instanceof CoverageStoreInfo info4) {
                    write(info4);
                } else if (item instanceof ResourceInfo info3) {
                    write(info3);
                } else if (item instanceof LayerInfo info2) {
                    write(info2);
                } else if (item instanceof StyleInfo info1) {
                    write(info1);
                } else if (item instanceof LayerGroupInfo info) {
                    write(info);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception writting catalog item : " + item, e);
                logValidationExceptions((T) null, e);
            }
        }
    }

    private void write(LayerGroupInfo layerGroupInfo) {
        try {
            getCatalog().add(layerGroupInfo);
            getCatalog().save(getCatalog().getLayerGroup(layerGroupInfo.getId()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception writting layer group : " + layerGroupInfo, e);
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    private void write(StyleInfo styleInfo) {
        StyleInfo source = getCatalog().getStyleByName((styleInfo).getName());
        if (source == null) {
            getCatalog().add(styleInfo);
            getCatalog().save(getCatalog().getStyle((styleInfo).getId()));
        }
    }

    private void write(LayerInfo layerInfo) {
        if (layerInfo.getName() != null) {
            LayerInfo source = getCatalog().getLayerByName(layerInfo.getName());
            if (source == null) {
                getCatalog().add(layerInfo);
                getCatalog().save(getCatalog().getLayer(layerInfo.getId()));
            }
        }
    }

    private void write(ResourceInfo resourceInfo) {
        if (getCatalog().getResourceByName(resourceInfo.getName(), FeatureTypeInfo.class) == null
                && getCatalog().getResourceByName(resourceInfo.getName(), CoverageInfo.class) == null) {
            Class<? extends ResourceInfo> clz = null;
            if (resourceInfo instanceof FeatureTypeInfo) {
                clz = FeatureTypeInfo.class;
            } else if (resourceInfo instanceof CoverageInfo) {
                clz = CoverageInfo.class;
            }
            getCatalog().add(resourceInfo);
            getCatalog().save(getCatalog().getResource(resourceInfo.getId(), clz));
        }
    }

    private void write(CoverageStoreInfo csInfo) {
        CoverageStoreInfo source = getCatalog().getCoverageStoreByName((csInfo).getName());
        if (source == null) {
            getCatalog().add(csInfo);
            getCatalog().save(getCatalog().getCoverageStore((csInfo).getId()));
        }
    }

    private void write(DataStoreInfo dsInfo) {
        DataStoreInfo source = getCatalog().getDataStoreByName(dsInfo.getName());
        if (source == null) {
            getCatalog().add(dsInfo);
            getCatalog().save(getCatalog().getDataStore(dsInfo.getId()));
        }
    }

    private void write(WMSStoreInfo wmsInfo) {
        WMSStoreInfo source = getCatalog().getWMSStoreByName(wmsInfo.getName());
        if (source == null) {
            getCatalog().add(wmsInfo);
            getCatalog().save(getCatalog().getWMSStore(wmsInfo.getId()));
        }
    }

    private void write(WMTSStoreInfo wmtsInfo) {
        WMTSStoreInfo source = getCatalog().getWMTSStoreByName(wmtsInfo.getName());
        if (source == null) {
            getCatalog().add(wmtsInfo);
            getCatalog().save(getCatalog().getWMTSStore(wmtsInfo.getId()));
        }
    }

    private void write(NamespaceInfo nsInfo) {
        NamespaceInfo source = getCatalog().getNamespaceByPrefix((nsInfo).getPrefix());
        if (source == null) {
            getCatalog().add(nsInfo);
            getCatalog().save(getCatalog().getNamespace((nsInfo).getId()));
        }
    }

    private void write(WorkspaceInfo wsInfo) {
        WorkspaceInfo source = getCatalog().getWorkspaceByName(wsInfo.getName());
        if (source == null) {
            getCatalog().add(wsInfo);
            getCatalog().save(getCatalog().getWorkspace(wsInfo.getId()));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Nothing to do.
    }

    /** Setter for resource. Represents a file that can be written. */
    @Override
    public void setResource(WritableResource resource) {
        // Nothing to do
    }
}
