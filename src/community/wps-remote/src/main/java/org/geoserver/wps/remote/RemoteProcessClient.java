/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.SpatialFile;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.remote.plugin.XMPPClient;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;

/**
 * Base class for the remote clients implementations. Those implementations will be plugged into
 * GeoServer through the Spring app-context.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class RemoteProcessClient implements DisposableBean, ExtensionPriority {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPClient.class.getPackage().getName());

    /** Whether this client is enabled or not from configuration */
    private boolean enabled;

    /**
     * Whenever more instances of the client are available, they should be ordered by ascending
     * priority
     */
    private int priority;

    /** The {@link RemoteProcessFactoryConfigurationWatcher} implementation */
    private final RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher;

    /** The registered {@link RemoteProcessFactoryListener} */
    private Set<RemoteProcessFactoryListener> remoteFactoryListeners =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<RemoteProcessFactoryListener, Boolean>());

    /** The registered {@link RemoteProcessClientListener} */
    private Set<RemoteProcessClientListener> remoteClientListeners =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<RemoteProcessClientListener, Boolean>());

    /** The available Registered Processing Machines */
    protected List<RemoteMachineDescriptor> registeredProcessingMachines =
            Collections.synchronizedList(new ArrayList<RemoteMachineDescriptor>());

    /** */
    protected List<RemoteRequestDescriptor> pendingRequests =
            Collections.synchronizedList(new LinkedList<RemoteRequestDescriptor>());

    /** */
    protected File certificateFile = null;

    /** */
    protected String certificatePassword = null;

    /**
     * The default Cosntructor
     *
     * @param remoteProcessFactory
     */
    public RemoteProcessClient(
            RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher,
            boolean enabled,
            int priority) {
        this.remoteProcessFactoryConfigurationWatcher = remoteProcessFactoryConfigurationWatcher;
        this.enabled = enabled;
        this.priority = priority;
    }

    /** @return the {@link RemoteProcessFactoryConfiguration} object */
    public RemoteProcessFactoryConfiguration getConfiguration() {
        return this.remoteProcessFactoryConfigurationWatcher.getConfiguration();
    }

    /** Initialization method */
    public abstract void init() throws Exception;

    /** Destroy method */
    public abstract void destroy() throws Exception;

    /** @return the remoteFactoryListeners */
    public Set<RemoteProcessFactoryListener> getRemoteFactoryListeners() {
        return remoteFactoryListeners;
    }

    /** @return the remoteClientListeners */
    public Set<RemoteProcessClientListener> getRemoteClientListeners() {
        return remoteClientListeners;
    }

    /** @param enabled the enabled to set */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the registeredProcessingMachines */
    public List<RemoteMachineDescriptor> getRegisteredProcessingMachines() {
        return registeredProcessingMachines;
    }

    /** @param registeredProcessingMachines the registeredProcessingMachines to set */
    public void setRegisteredProcessingMachines(
            List<RemoteMachineDescriptor> registeredProcessingMachines) {
        this.registeredProcessingMachines = registeredProcessingMachines;
    }

    /** Whether the plugin is enabled or not. */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** @return the priority */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the KeyStore Certificate Path
     *
     * @param certificateFile
     * @throws IOException
     */
    public void setCertificateFile(Resource certificateFile) throws IOException {
        this.certificateFile = certificateFile.getFile();
    }

    /**
     * Set the KeyStore Certificate Password
     *
     * @param certificatePassword
     */
    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    /** @param priority the priority to set */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Registers the {@link RemoteProcessFactoryListener} remoteClientListeners
     *
     * @param listener
     */
    public void registerProcessFactoryListener(RemoteProcessFactoryListener listener) {
        remoteFactoryListeners.add(listener);
    }

    /**
     * De-registers the {@link RemoteProcessFactoryListener} remoteClientListeners
     *
     * @param listener
     */
    public void deregisterProcessFactoryListener(RemoteProcessFactoryListener listener) {
        remoteFactoryListeners.remove(listener);
    }

    /**
     * Registers the {@link RemoteProcessClientListener} remoteClientListeners
     *
     * @param listener
     */
    public void registerProcessClientListener(RemoteProcessClientListener listener) {
        remoteClientListeners.add(listener);
    }

    /**
     * De-registers the {@link RemoteProcessClientListener} remoteClientListeners
     *
     * @param listener
     */
    public void deregisterProcessClientListener(RemoteProcessClientListener listener) {
        remoteClientListeners.remove(listener);
    }

    /**
     * Invoke the {@link RemoteProcessClient} execution
     *
     * @param name
     * @param input
     * @param metadata
     * @param monitor
     */
    public abstract String execute(
            Name name,
            Map<String, Object> input,
            Map<String, Object> metadata,
            ProgressListener monitor)
            throws Exception;

    /** Accessor for global geoserver instance from the test application context. */
    public GeoServer getGeoServer() {
        return (GeoServer) GeoServerExtensions.bean("geoServer");
    }

    /** Accessor for global geoserver instance from the test application context. */
    public Importer getImporter() {
        return (Importer) GeoServerExtensions.bean("importer");
    }

    /**
     * @param wsName
     * @param dsName
     */
    public DataStoreInfo createH2DataStore(String wsName, String dsName) {
        // create a datastore to import into
        Catalog cat = getGeoServer().getCatalog();

        WorkspaceInfo ws =
                wsName != null ? cat.getWorkspaceByName(wsName) : cat.getDefaultWorkspace();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setWorkspace(ws);
        ds.setName(dsName);
        ds.setType("H2");

        GeoServerResourceLoader loader = cat.getResourceLoader();
        final String dataDir = loader.getBaseDirectory().getAbsolutePath();

        Map params = new HashMap();
        params.put("database", dataDir + "/" + dsName);
        params.put("dbtype", "h2");
        params.put("namespace", cat.getNamespaceByPrefix(ws.getName()).getURI());
        ds.getConnectionParameters().putAll(params);
        ds.setEnabled(true);
        cat.add(ds);

        return ds;
    }

    /**
     * @param metadata
     * @param value
     * @throws IOException
     */
    public LayerInfo importLayer(
            File file,
            String type,
            DataStoreInfo store,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws Exception {
        Importer importer = getImporter();

        LOGGER.fine(
                " - [Remote Process Client - importLayer] Importer Context from Spatial File:"
                        + file.getAbsolutePath());

        ImportContext context =
                (store != null
                        ? importer.createContext(new SpatialFile(file), store)
                        : importer.createContext(new SpatialFile(file)));

        if (context.getTasks() != null && context.getTasks().size() > 0) {
            WorkspaceInfo ws = null;
            ImportTask task = context.getTasks().get(0);

            if (targetWorkspace != null) {
                LOGGER.fine(
                        " - [Remote Process Client - importLayer] Looking for Workspace in the catalog:"
                                + targetWorkspace);

                ws = importer.getCatalog().getWorkspaceByName(targetWorkspace);
                if (ws != null) {

                    LOGGER.fine(" - [Remote Process Client - importLayer] Workspace found:" + ws);

                    context.setTargetWorkspace(ws);
                } else {
                    LOGGER.fine(
                            " - [Remote Process Client - importLayer] Workspace *NOT* found - using the Default one:"
                                    + importer.getCatalog().getDefaultWorkspace());

                    context.setTargetWorkspace(importer.getCatalog().getDefaultWorkspace());
                }
            }

            if (defaultStyle != null) {
                StyleInfo style = importer.getCatalog().getStyleByName(defaultStyle);
                if (style == null && targetWorkspace != null) {
                    style = importer.getCatalog().getStyleByName(targetWorkspace, defaultStyle);
                }

                if (style != null) {
                    task.getLayer().setDefaultStyle(style);
                }
            }

            if (name != null) {
                task.getLayer().setName(name);
            }
            if (title != null) {
                task.getLayer().setTitle(title);
            }
            if (description != null) {
                task.getLayer().setAbstract(description);
            }
            if (metadata != null) {
                task.getLayer().getMetadata().put("owc_properties", metadata);
            }
            // AF: Importer ISSUE -> The target workspace is not honored
            /*if (task.getLayer().getResource() != null) {
                if (ws != null) {
                    task.getLayer().getResource().getStore().setWorkspace(ws);
                }

                if (task.getLayer().getResource() instanceof CoverageInfo) {
                    ((CoverageInfoImpl) task.getLayer().getResource()).setNativeCoverageName(name);
                }
            }*/

            importer.run(context);

            for (int importChecks = 0; importChecks < 10; importChecks++) {
                if (context.getState() == ImportContext.State.COMPLETE) {
                    if (context.getTasks() != null && context.getTasks().size() > 0) {
                        // ImportTask task = context.getTasks().get(0);
                        // assertEquals(ImportTask.State.READY, task.getState());

                        // assertEquals("the layer name", task.getLayer().getResource().getName());

                        task = context.getTasks().get(0);

                        // WARNING: The Importer Configures Just The First Layer
                        if (task.getLayer().getResource() instanceof CoverageInfo) {
                            CoverageInfo ci = ((CoverageInfo) task.getLayer().getResource());

                            GridCoverageReader reader = null;
                            try {
                                reader = ci.getGridCoverageReader(null, null);

                                String[] cvNames = reader.getGridCoverageNames();

                                if (cvNames != null && cvNames.length > 0) {
                                    final String nativeCoverageName = cvNames[0];

                                    ci.setNativeCoverageName(nativeCoverageName);

                                    // if(type.equals("application/x-netcdf")) Set Dimensions
                                    if (reader instanceof StructuredGridCoverage2DReader) {
                                        StructuredGridCoverage2DReader structuredReader =
                                                ((StructuredGridCoverage2DReader) reader);

                                        // Getting dimension descriptors
                                        final List<DimensionDescriptor> dimensionDescriptors =
                                                structuredReader.getDimensionDescriptors(
                                                        nativeCoverageName);
                                        DimensionDescriptor timeDimension = null;
                                        DimensionDescriptor elevationDimension = null;
                                        final List<DimensionDescriptor> customDimensions =
                                                new ArrayList<DimensionDescriptor>();

                                        // Collect dimension Descriptor info
                                        for (DimensionDescriptor dimensionDescriptor :
                                                dimensionDescriptors) {
                                            if (dimensionDescriptor
                                                    .getName()
                                                    .equalsIgnoreCase(ResourceInfo.TIME)) {
                                                timeDimension = dimensionDescriptor;
                                            } else if (dimensionDescriptor
                                                    .getName()
                                                    .equalsIgnoreCase(ResourceInfo.ELEVATION)) {
                                                elevationDimension = dimensionDescriptor;
                                            } else {
                                                customDimensions.add(dimensionDescriptor);
                                            }
                                        }

                                        final boolean defaultTimeNeeded = timeDimension != null;
                                        final boolean defaultElevationNeeded =
                                                elevationDimension != null;

                                        // Create Default Time Dimension If Needed
                                        if (defaultTimeNeeded) {
                                            DimensionInfo di = new DimensionInfoImpl();
                                            di.setEnabled(true);
                                            di.setPresentation(DimensionPresentation.LIST);
                                            di.setAttribute(timeDimension.getStartAttribute());
                                            ci.getMetadata().put(ResourceInfo.TIME, di);
                                        }

                                        // Create Default Elevation Dimension If Needed
                                        if (defaultElevationNeeded) {
                                            DimensionInfo di = new DimensionInfoImpl();
                                            di.setEnabled(true);
                                            di.setPresentation(DimensionPresentation.LIST);
                                            di.setUnits("EPSG:5030");
                                            di.setUnitSymbol("m");
                                            di.setAttribute(elevationDimension.getStartAttribute());
                                            ci.getMetadata().put(ResourceInfo.ELEVATION, di);
                                        }
                                    }
                                }
                            } finally {
                                // WARNING: Disposing The Reader Causes The Catalog To Fail
                                /*if (reader != null) {
                                    reader.dispose();
                                }*/
                            }
                        }

                        LOGGER.fine(
                                " - [Remote Process Client - importLayer] The Importer has finished correctly for Spatial File:"
                                        + file.getAbsolutePath());

                        return task.getLayer();
                    } else {
                        break;
                    }
                } else {
                    Thread.sleep(1500);
                }
            }
        }

        LOGGER.warning(
                " - [Remote Process Client - importLayer] The Importer has finished *BUT* did not returned any layer for Spatial File:"
                        + file.getAbsolutePath());

        return null;
    }
}
