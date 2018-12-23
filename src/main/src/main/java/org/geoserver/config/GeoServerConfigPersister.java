/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
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
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * Handles the persistence of configuration files when changes happen to the catalog, such as
 * rename, remove and change of workspace.
 */
public class GeoServerConfigPersister
        implements CatalogListener, ConfigurationListener, ExtensionPriority {

    /** logging instance */
    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    GeoServerResourceLoader rl;
    GeoServerDataDirectory dd;
    XStreamPersister xp;

    public GeoServerConfigPersister(GeoServerResourceLoader rl, XStreamPersister xp) {
        this.rl = rl;
        this.dd = new GeoServerDataDirectory(rl);
        this.xp = xp;
    }

    public void handleAddEvent(CatalogAddEvent event) {
        Object source = event.getSource();
        try {
            if (source instanceof WorkspaceInfo) {
                addWorkspace((WorkspaceInfo) source);
            } else if (source instanceof NamespaceInfo) {
                addNamespace((NamespaceInfo) source);
            } else if (source instanceof DataStoreInfo) {
                addDataStore((DataStoreInfo) source);
            } else if (source instanceof WMTSStoreInfo) {
                addWMTSStore((WMTSStoreInfo) source);
            } else if (source instanceof WMSStoreInfo) {
                addWMSStore((WMSStoreInfo) source);
            } else if (source instanceof FeatureTypeInfo) {
                addFeatureType((FeatureTypeInfo) source);
            } else if (source instanceof CoverageStoreInfo) {
                addCoverageStore((CoverageStoreInfo) source);
            } else if (source instanceof CoverageInfo) {
                addCoverage((CoverageInfo) source);
            } else if (source instanceof WMSLayerInfo) {
                addWMSLayer((WMSLayerInfo) source);
            } else if (source instanceof WMTSLayerInfo) {
                addWMTSLayer((WMTSLayerInfo) source);
            } else if (source instanceof LayerInfo) {
                addLayer((LayerInfo) source);
            } else if (source instanceof StyleInfo) {
                addStyle((StyleInfo) source);
            } else if (source instanceof LayerGroupInfo) {
                addLayerGroup((LayerGroupInfo) source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleModifyEvent(CatalogModifyEvent event) {
        Object source = event.getSource();

        try {
            // here we handle name changes
            int i = event.getPropertyNames().indexOf("name");
            if (i > -1) {
                String newName = (String) event.getNewValues().get(i);

                if (source instanceof WorkspaceInfo) {
                    renameWorkspace((WorkspaceInfo) source, newName);
                } else if (source instanceof StoreInfo) {
                    renameStore((StoreInfo) source, newName);
                } else if (source instanceof ResourceInfo) {
                    renameResource((ResourceInfo) source, newName);
                } else if (source instanceof StyleInfo) {
                    renameStyle((StyleInfo) source, newName);
                } else if (source instanceof LayerGroupInfo) {
                    renameLayerGroup((LayerGroupInfo) source, newName);
                }
            }

            // handle the case of a store changing workspace
            if (source instanceof StoreInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(i);
                    Resource oldDir = dd.get((StoreInfo) source);
                    moveResToDir(oldDir, dd.get(newWorkspace));
                }
            }

            // handle the case of a feature type changing store
            if (source instanceof FeatureTypeInfo) {
                i = event.getPropertyNames().indexOf("store");
                if (i > -1) {
                    StoreInfo newStore = (StoreInfo) event.getNewValues().get(i);
                    Resource oldDir = dd.get((FeatureTypeInfo) source);
                    Resource newDir = dd.get(newStore);
                    moveResToDir(oldDir, newDir);
                }
            }

            // handle the case of a layer group changing workspace
            if (source instanceof LayerGroupInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    final WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(i);
                    final Resource oldRes = dd.config((LayerGroupInfo) source);
                    final Resource newDir = dd.getLayerGroups(newWorkspace);
                    moveResToDir(oldRes, newDir);
                }
            }

            // handle default workspace
            if (source instanceof Catalog) {
                i = event.getPropertyNames().indexOf("defaultWorkspace");
                if (i > -1) {
                    WorkspaceInfo defWorkspace = (WorkspaceInfo) event.getNewValues().get(i);
                    // SG don't bother with a default workspace if we do not have one
                    if (defWorkspace != null) {
                        persist(defWorkspace, dd.getWorkspaces("default.xml"));
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        Object source = event.getSource();
        try {
            if (source instanceof WorkspaceInfo) {
                modifyWorkspace((WorkspaceInfo) source);
            } else if (source instanceof DataStoreInfo) {
                modifyDataStore((DataStoreInfo) source);
            } else if (source instanceof WMTSStoreInfo) {
                modifyWMTSStore((WMTSStoreInfo) source);
            } else if (source instanceof WMSStoreInfo) {
                modifyWMSStore((WMSStoreInfo) source);
            } else if (source instanceof NamespaceInfo) {
                modifyNamespace((NamespaceInfo) source);
            } else if (source instanceof FeatureTypeInfo) {
                modifyFeatureType((FeatureTypeInfo) source);
            } else if (source instanceof CoverageStoreInfo) {
                modifyCoverageStore((CoverageStoreInfo) source);
            } else if (source instanceof CoverageInfo) {
                modifyCoverage((CoverageInfo) source);
            } else if (source instanceof WMSLayerInfo) {
                modifyWMSLayer((WMSLayerInfo) source);
            } else if (source instanceof WMTSLayerInfo) {
                modifyWMTSLayer((WMTSLayerInfo) source);
            } else if (source instanceof LayerInfo) {
                modifyLayer((LayerInfo) source);
            } else if (source instanceof StyleInfo) {
                modifyStyle((StyleInfo) source);
            } else if (source instanceof LayerGroupInfo) {
                modifyLayerGroup((LayerGroupInfo) source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleRemoveEvent(CatalogRemoveEvent event) {
        Object source = event.getSource();
        try {
            if (source instanceof WorkspaceInfo) {
                removeWorkspace((WorkspaceInfo) source);
            } else if (source instanceof NamespaceInfo) {
                removeNamespace((NamespaceInfo) source);
            } else if (source instanceof DataStoreInfo) {
                removeDataStore((DataStoreInfo) source);
            } else if (source instanceof FeatureTypeInfo) {
                removeFeatureType((FeatureTypeInfo) source);
            } else if (source instanceof CoverageStoreInfo) {
                removeCoverageStore((CoverageStoreInfo) source);
            } else if (source instanceof CoverageInfo) {
                removeCoverage((CoverageInfo) source);
            } else if (source instanceof WMTSStoreInfo) {
                removeWMTSStore((WMTSStoreInfo) source);
            } else if (source instanceof WMTSLayerInfo) {
                removeWMTSLayer((WMTSLayerInfo) source);
            } else if (source instanceof WMSStoreInfo) {
                removeWMSStore((WMSStoreInfo) source);
            } else if (source instanceof WMSLayerInfo) {
                removeWMSLayer((WMSLayerInfo) source);
            } else if (source instanceof LayerInfo) {
                removeLayer((LayerInfo) source);
            } else if (source instanceof StyleInfo) {
                removeStyle((StyleInfo) source);
            } else if (source instanceof LayerGroupInfo) {
                removeLayerGroup((LayerGroupInfo) source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleGlobalChange(
            GeoServerInfo global,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {}

    public void handlePostGlobalChange(GeoServerInfo global) {
        try {
            persist(global, dd.config(global));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleSettingsAdded(SettingsInfo settings) {
        handleSettingsPostModified(settings);
    }

    public void handleSettingsModified(
            SettingsInfo settings,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        // handle case of settings changing workspace
        int i = propertyNames.indexOf("workspace");
        if (i > -1) {
            WorkspaceInfo newWorkspace = (WorkspaceInfo) newValues.get(i);
            LOGGER.fine("Moving settings '" + settings + " to workspace: " + newWorkspace);

            moveResToDir(dd.config(settings), dd.get(newWorkspace));
        }
    }

    public void handleSettingsPostModified(SettingsInfo settings) {
        LOGGER.fine("Persisting settings " + settings);
        try {
            persist(settings, dd.config(settings));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleSettingsRemoved(SettingsInfo settings) {
        LOGGER.fine("Removing settings " + settings);
        rmRes(dd.config(settings));
    }

    public void handleLoggingChange(
            LoggingInfo logging,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {}

    public void handlePostLoggingChange(LoggingInfo logging) {
        try {
            persist(logging, dd.config(logging));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleServiceAdded(ServiceInfo service) {}

    public void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {}

    public void handlePostServiceChange(ServiceInfo service) {}

    public void handleServiceRemove(ServiceInfo service) {}

    public void reloaded() {}

    // workspaces
    private void addWorkspace(WorkspaceInfo ws) throws IOException {
        LOGGER.fine("Persisting workspace " + ws.getName());
        Resource xml = dd.config(ws);
        persist(ws, xml);
    }

    private void renameWorkspace(WorkspaceInfo ws, String newName) throws IOException {
        LOGGER.fine("Renaming workspace " + ws.getName() + "to " + newName);
        Resource directory = dd.get(ws);
        renameRes(directory, newName);
    }

    private void modifyWorkspace(WorkspaceInfo ws) throws IOException {
        LOGGER.fine("Persisting workspace " + ws.getName());
        Resource r = dd.config(ws);
        persist(ws, r);
    }

    private void removeWorkspace(WorkspaceInfo ws) throws IOException {
        LOGGER.fine("Removing workspace " + ws.getName());
        Resource directory = dd.get(ws);
        rmRes(directory);
    }

    // namespaces
    private void addNamespace(NamespaceInfo ns) throws IOException {
        LOGGER.fine("Persisting namespace " + ns.getPrefix());
        Resource xml = dd.config(ns);
        persist(ns, xml);
    }

    private void modifyNamespace(NamespaceInfo ns) throws IOException {
        LOGGER.fine("Persisting namespace " + ns.getPrefix());
        Resource xml = dd.config(ns);
        persist(ns, xml);
    }

    private void removeNamespace(NamespaceInfo ns) throws IOException {
        LOGGER.fine("Removing namespace " + ns.getPrefix());
        Resource directory = dd.get(ns);
        rmRes(directory);
    }

    // datastores
    private void addDataStore(DataStoreInfo ds) throws IOException {
        LOGGER.fine("Persisting datastore " + ds.getName());
        Resource xml = dd.config(ds);
        persist(ds, xml);
    }

    private void renameStore(StoreInfo s, String newName) throws IOException {
        LOGGER.fine("Renaming store " + s.getName() + "to " + newName);
        Resource directory = dd.get(s);
        renameRes(directory, newName);
    }

    private void modifyDataStore(DataStoreInfo ds) throws IOException {
        LOGGER.fine("Persisting datastore " + ds.getName());
        Resource xml = dd.config(ds);
        persist(ds, xml);
    }

    private void removeDataStore(DataStoreInfo ds) throws IOException {
        LOGGER.fine("Removing datastore " + ds.getName());
        Resource directory = dd.get(ds);
        rmRes(directory);
    }

    // feature types
    private void addFeatureType(FeatureTypeInfo ft) throws IOException {
        LOGGER.fine("Persisting feature type " + ft.getName());
        Resource xml = dd.config(ft);
        persist(ft, xml);
    }

    private void renameResource(ResourceInfo r, String newName) throws IOException {
        LOGGER.fine("Renaming resource " + r.getName() + " to " + newName);
        Resource directory = dd.get(r);
        renameRes(directory, newName);
    }

    private void modifyFeatureType(FeatureTypeInfo ft) throws IOException {
        LOGGER.fine("Persisting feature type " + ft.getName());
        Resource xml = dd.config(ft);
        persist(ft, xml);
    }

    private void removeFeatureType(FeatureTypeInfo ft) throws IOException {
        LOGGER.fine("Removing feature type " + ft.getName());
        Resource directory = dd.get(ft);
        rmRes(directory);
    }

    // coverage stores
    private void addCoverageStore(CoverageStoreInfo cs) throws IOException {
        LOGGER.fine("Persisting coverage store " + cs.getName());
        Resource xml = dd.config(cs);
        persist(cs, xml);
    }

    private void modifyCoverageStore(CoverageStoreInfo cs) throws IOException {
        LOGGER.fine("Persisting coverage store " + cs.getName());
        Resource r = dd.config(cs);
        persist(cs, r);
    }

    private void removeCoverageStore(CoverageStoreInfo cs) throws IOException {
        LOGGER.fine("Removing coverage store " + cs.getName());
        Resource r = dd.get(cs);
        rmRes(r);
    }

    // coverages
    private void addCoverage(CoverageInfo c) throws IOException {
        LOGGER.fine("Persisting coverage " + c.getName());
        Resource xml = dd.config(c);
        persist(c, xml);
    }

    private void modifyCoverage(CoverageInfo c) throws IOException {
        LOGGER.fine("Persisting coverage " + c.getName());
        Resource xml = dd.config(c);
        persist(c, xml);
    }

    private void removeCoverage(CoverageInfo c) throws IOException {
        LOGGER.fine("Removing coverage " + c.getName());
        Resource directory = dd.get(c);
        rmRes(directory);
    }

    // wms stores
    private void addWMSStore(WMSStoreInfo wmss) throws IOException {
        LOGGER.fine("Persisting wms store " + wmss.getName());
        Resource xml = dd.config(wmss);
        persist(wmss, xml);
    }

    private void modifyWMSStore(WMSStoreInfo wmss) throws IOException {
        LOGGER.fine("Persisting wms store " + wmss.getName());
        Resource xml = dd.config(wmss);
        persist(wmss, xml);
    }

    private void removeWMSStore(WMSStoreInfo wmss) throws IOException {
        LOGGER.fine("Removing datastore " + wmss.getName());
        Resource directory = dd.get(wmss);
        rmRes(directory);
    }

    // wms layers
    private void addWMSLayer(WMSLayerInfo wms) throws IOException {
        LOGGER.fine("Persisting wms layer " + wms.getName());
        Resource xml = dd.config(wms);
        persist(wms, xml);
    }

    private void modifyWMSLayer(WMSLayerInfo wms) throws IOException {
        LOGGER.fine("Persisting wms layer" + wms.getName());
        Resource xml = dd.config(wms);
        persist(wms, xml);
    }

    private void removeWMSLayer(WMSLayerInfo wms) throws IOException {
        LOGGER.fine("Removing wms layer " + wms.getName());
        Resource directory = dd.get(wms);
        rmRes(directory);
    }

    // wmts stores
    private void addWMTSStore(WMTSStoreInfo wmss) throws IOException {
        LOGGER.fine("Persisting wmts store " + wmss.getName());
        Resource xml = dd.config(wmss);
        persist(wmss, xml);
    }

    private void modifyWMTSStore(WMTSStoreInfo wmss) throws IOException {
        LOGGER.fine("Persisting wmts store " + wmss.getName());
        Resource xml = dd.config(wmss);
        persist(wmss, xml);
    }

    private void removeWMTSStore(WMTSStoreInfo wmss) throws IOException {
        LOGGER.fine("Removing  wmts datastore " + wmss.getName());
        Resource directory = dd.get(wmss);
        rmRes(directory);
    }

    // wmts layers
    private void addWMTSLayer(WMTSLayerInfo wms) throws IOException {
        LOGGER.fine("Persisting wmts layer " + wms.getName());
        Resource xml = dd.config(wms);
        persist(wms, xml);
    }

    private void modifyWMTSLayer(WMTSLayerInfo wms) throws IOException {
        LOGGER.fine("Persisting wmts layer" + wms.getName());
        Resource xml = dd.config(wms);
        persist(wms, xml);
    }

    private void removeWMTSLayer(WMTSLayerInfo wms) throws IOException {
        LOGGER.fine("Removing wms layer " + wms.getName());
        Resource directory = dd.get(wms);
        rmRes(directory);
    }
    // layers
    private void addLayer(LayerInfo l) throws IOException {
        LOGGER.fine("Persisting layer " + l.getName());
        Resource xml = dd.config(l);
        persist(l, xml);
    }

    private void modifyLayer(LayerInfo l) throws IOException {
        LOGGER.fine("Persisting layer " + l.getName());
        Resource xml = dd.config(l);
        persist(l, xml);
    }

    private void removeLayer(LayerInfo l) throws IOException {
        LOGGER.fine("Removing layer " + l.getName());
        Resource directory = dd.get(l);
        rmRes(directory);
    }

    // styles
    private void addStyle(StyleInfo s) throws IOException {
        LOGGER.fine("Persisting style " + s.getName());
        Resource xml = dd.config(s);
        persist(s, xml);
    }

    private void renameStyle(StyleInfo s, String newName) throws IOException {
        LOGGER.fine("Renaming style " + s.getName() + " to " + newName);

        // rename xml configuration file
        Resource xml = dd.config(s);
        renameRes(xml, newName + ".xml");
    }

    private void modifyStyle(StyleInfo s) throws IOException {
        LOGGER.fine("Persisting style " + s.getName());
        Resource xml = dd.config(s);
        persist(s, xml);
        /*
        //save out sld
        File f = file(s);
        BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( f ) );
        SLDTransformer tx = new SLDTransformer();
        try {
            tx.transform( s.getSLD(),out );
            out.flush();
        }
        catch (TransformerException e) {
            throw (IOException) new IOException().initCause( e );
        }
        finally {
            out.close();
        }
        */
    }

    private void removeStyle(StyleInfo s) throws IOException {
        LOGGER.fine("Removing style " + s.getName());
        Resource xml = dd.config(s);
        rmRes(xml);
    }

    // layer groups
    private void addLayerGroup(LayerGroupInfo lg) throws IOException {
        LOGGER.fine("Persisting layer group " + lg.getName());
        Resource xml = dd.config(lg);
        persist(lg, xml);
    }

    private void renameLayerGroup(LayerGroupInfo lg, String newName) throws IOException {
        LOGGER.fine("Renaming layer group " + lg.getName() + " to " + newName);
        Resource xml = dd.config(lg);
        renameRes(xml, String.format("%s.xml", newName));
    }

    private void modifyLayerGroup(LayerGroupInfo lg) throws IOException {
        LOGGER.fine("Persisting layer group " + lg.getName());
        Resource xml = dd.config(lg);
        persist(lg, xml);
    }

    private void removeLayerGroup(LayerGroupInfo lg) throws IOException {
        LOGGER.fine("Removing layer group " + lg.getName());
        Resource xml = dd.config(lg);
        rmRes(xml);
    }

    private void persist(Object o, Resource r) throws IOException {
        try {
            synchronized (xp) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                xp.save(o, bos);
                r.setContents(bos.toByteArray());
            }
            LOGGER.fine("Persisted " + o.getClass().getName() + " to " + r.path());
        } catch (Exception e) {
            // catch any exceptions and send them back as CatalogExeptions
            String msg = "Error persisting " + o + " to " + r.path();
            throw new CatalogException(msg, e);
        }
    }

    private void rmRes(Resource r) {
        try {
            rl.remove(r.path());
        } catch (Exception e) {
            throw new CatalogException(e);
        }
    }

    private void renameRes(Resource r, String newName) {
        try {
            rl.move(r.path(), r.parent().get(newName).path());
        } catch (Exception e) {
            throw new CatalogException(e);
        }
    }

    private void moveResToDir(Resource r, Resource newDir) {
        try {
            rl.move(r.path(), newDir.get(r.name()).path());
        } catch (Exception e) {
            throw new CatalogException(e);
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
