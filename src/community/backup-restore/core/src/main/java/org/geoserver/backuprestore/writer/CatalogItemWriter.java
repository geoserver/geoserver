/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.util.Arrays;
import java.util.List;
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
    public void write(List<? extends T> items) {
        for (T item : items) {
            try {
                if (item instanceof WorkspaceInfo) {
                    write((WorkspaceInfo) item);
                } else if (item instanceof NamespaceInfo) {
                    write((NamespaceInfo) item);
                } else if (item instanceof DataStoreInfo) {
                    write((DataStoreInfo) item);
                } else if (item instanceof WMSStoreInfo) {
                    write((WMSStoreInfo) item);
                } else if (item instanceof WMTSStoreInfo) {
                    write((WMTSStoreInfo) item);
                } else if (item instanceof CoverageStoreInfo) {
                    write((CoverageStoreInfo) item);
                } else if (item instanceof ResourceInfo) {
                    write((ResourceInfo) item);
                } else if (item instanceof LayerInfo) {
                    write((LayerInfo) item);
                } else if (item instanceof StyleInfo) {
                    write((StyleInfo) item);
                } else if (item instanceof LayerGroupInfo) {
                    write((LayerGroupInfo) item);
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
                && getCatalog().getResourceByName(resourceInfo.getName(), CoverageInfo.class)
                        == null) {
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
    public void setResource(Resource resource) {
        // Nothing to do
    }
}
