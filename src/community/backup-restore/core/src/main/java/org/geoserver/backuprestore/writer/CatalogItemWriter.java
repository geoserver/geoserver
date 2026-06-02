/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.util.Arrays;
import java.util.function.Function;
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
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.util.logging.Logging;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.core.io.WritableResource;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 *
 * <p>Writes unmarshalled items into the temporary {@link Catalog} in memory.
 *
 * <p>An existing catalog object is matched first by id (when the archive preserved ids, i.e. it was produced with
 * {@code BK_PRESERVE_IDS}) and then by name. This way an id-based restore into the same instance is an idempotent
 * no-op, while a migration into a foreign catalog (whose ids never collide) falls back to the name check and adds the
 * object keeping its original id. When the archive carries no ids (the legacy default) the id lookup short-circuits to
 * {@code null} and the behaviour is exactly the previous name-based one.
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

    /**
     * Looks an object up by its (preserved) id, or returns {@code null} when the id is absent - i.e. a legacy
     * id-stripped archive - so the caller transparently falls back to the name-based lookup.
     */
    private static <X> X findById(String id, Function<String, X> byId) {
        return id != null ? byId.apply(id) : null;
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
            if (findById(layerGroupInfo.getId(), getCatalog()::getLayerGroup) == null) {
                getCatalog().add(layerGroupInfo);
                getCatalog().save(getCatalog().getLayerGroup(layerGroupInfo.getId()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception writting layer group : " + layerGroupInfo, e);
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    private void write(StyleInfo styleInfo) {
        StyleInfo source = findById(styleInfo.getId(), getCatalog()::getStyle);
        if (source == null) {
            // Use workspace-aware lookup for workspace-specific styles
            WorkspaceInfo ws = styleInfo.getWorkspace();
            // Resolve workspace proxy to actual workspace so the XML is written with ID reference
            // instead of name reference. This is critical for GeoServer to load the style correctly
            // on restart (CatalogLoaderSanitizer.validate() compares workspace IDs).
            if (ws != null) {
                WorkspaceInfo resolvedWs = ResolvingProxy.resolve(getCatalog(), ws);
                if (resolvedWs != null) {
                    // Unwrap ModificationProxy to access StyleInfoImpl directly,
                    // since styleInfo may be wrapped in a proxy during restore.
                    StyleInfo unwrapped = ModificationProxy.unwrap(styleInfo);
                    if (unwrapped instanceof StyleInfoImpl) {
                        ((StyleInfoImpl) unwrapped).setWorkspace(resolvedWs);
                    }
                    ws = resolvedWs;
                }
            }
            source = (ws != null)
                    ? getCatalog().getStyleByName(ws, styleInfo.getName())
                    : getCatalog().getStyleByName(styleInfo.getName());
        }
        if (source == null) {
            getCatalog().add(styleInfo);
            getCatalog().save(getCatalog().getStyle((styleInfo).getId()));
        }
    }

    private void write(LayerInfo layerInfo) {
        LayerInfo source = findById(layerInfo.getId(), getCatalog()::getLayer);
        if (source == null && layerInfo.getName() != null) {
            source = getCatalog().getLayerByName(layerInfo.getName());
        }
        if (source == null && layerInfo.getName() != null) {
            getCatalog().add(layerInfo);
            getCatalog().save(getCatalog().getLayer(layerInfo.getId()));
        }
    }

    private void write(ResourceInfo resourceInfo) {
        ResourceInfo source = findById(resourceInfo.getId(), id -> getCatalog().getResource(id, ResourceInfo.class));
        if (source == null
                && getCatalog().getResourceByName(resourceInfo.getName(), FeatureTypeInfo.class) == null
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
        CoverageStoreInfo source = findById(csInfo.getId(), getCatalog()::getCoverageStore);
        if (source == null) {
            source = getCatalog().getCoverageStoreByName((csInfo).getName());
        }
        if (source == null) {
            getCatalog().add(csInfo);
            getCatalog().save(getCatalog().getCoverageStore((csInfo).getId()));
        }
    }

    private void write(DataStoreInfo dsInfo) {
        DataStoreInfo source = findById(dsInfo.getId(), getCatalog()::getDataStore);
        if (source == null) {
            source = getCatalog().getDataStoreByName(dsInfo.getName());
        }
        if (source == null) {
            getCatalog().add(dsInfo);
            getCatalog().save(getCatalog().getDataStore(dsInfo.getId()));
        }
    }

    private void write(WMSStoreInfo wmsInfo) {
        WMSStoreInfo source = findById(wmsInfo.getId(), getCatalog()::getWMSStore);
        if (source == null) {
            source = getCatalog().getWMSStoreByName(wmsInfo.getName());
        }
        if (source == null) {
            getCatalog().add(wmsInfo);
            getCatalog().save(getCatalog().getWMSStore(wmsInfo.getId()));
        }
    }

    private void write(WMTSStoreInfo wmtsInfo) {
        WMTSStoreInfo source = findById(wmtsInfo.getId(), getCatalog()::getWMTSStore);
        if (source == null) {
            source = getCatalog().getWMTSStoreByName(wmtsInfo.getName());
        }
        if (source == null) {
            getCatalog().add(wmtsInfo);
            getCatalog().save(getCatalog().getWMTSStore(wmtsInfo.getId()));
        }
    }

    private void write(NamespaceInfo nsInfo) {
        NamespaceInfo source = findById(nsInfo.getId(), getCatalog()::getNamespace);
        if (source == null) {
            source = getCatalog().getNamespaceByPrefix((nsInfo).getPrefix());
        }
        if (source == null) {
            getCatalog().add(nsInfo);
            getCatalog().save(getCatalog().getNamespace((nsInfo).getId()));
        }
    }

    private void write(WorkspaceInfo wsInfo) {
        WorkspaceInfo source = findById(wsInfo.getId(), getCatalog()::getWorkspace);
        if (source == null) {
            source = getCatalog().getWorkspaceByName(wsInfo.getName());
        }
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
