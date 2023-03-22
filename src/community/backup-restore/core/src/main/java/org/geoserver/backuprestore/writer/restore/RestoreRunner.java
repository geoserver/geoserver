/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer.restore;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.tasklet.CatalogBackupRestoreTasklet;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.*;
import org.geoserver.config.*;
import org.geoserver.gwc.config.GWCInitializer;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geoserver.gwc.layer.DefaultTileLayerCatalog;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.XMLConfiguration;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class RestoreRunner {
    protected static Logger LOGGER = Logging.getLogger(RestoreRunner.class);

    CatalogBackupRestoreTasklet catalogBackupRestoreTasklet;

    /**
     *
     * @param catalogBackupRestoreTasklet
     */
    public RestoreRunner(CatalogBackupRestoreTasklet catalogBackupRestoreTasklet) {
        this.catalogBackupRestoreTasklet = catalogBackupRestoreTasklet;
    }

    /**
     *  Hard Restore of GeoServer Configuration and Data (Catalog) from a Backup Archive (ZIP File) to a GeoServer Instance (Catalog)
     *
     *  @param geoserver (can be null)
     *  @param dd (can be null)
     *  @param sourceRestoreFolder (can be null)
     *  @param sourceWorkspacesFolder (can be null)
     *  @param newGeoServerInfo (can be null)
     * @throws Exception
     */
    public void doRestore(
            JobExecution jobExecution, final GeoServer geoserver, final GeoServerDataDirectory dd)
            throws Exception {
        final String inputFolderURL =
                jobExecution.getJobParameters().getString(Backup.PARAM_INPUT_FILE_PATH);
        Resource sourceRestoreFolder = Resources.fromURL(inputFolderURL);
        Resource sourceWorkspacesFolder = null;

        this.catalogBackupRestoreTasklet.authenticate();

        // Try first to load all the settings available into the source restore folder
        GeoServerInfo newGeoServerInfo = null;
        SettingsInfo newSettings = null;
        LoggingInfo newLoggingInfo = null;
        try {
            newGeoServerInfo =
                    (GeoServerInfo)
                            this.catalogBackupRestoreTasklet.doRead(
                                    sourceRestoreFolder, "global.xml");
            newLoggingInfo =
                    (LoggingInfo)
                            this.catalogBackupRestoreTasklet.doRead(
                                    sourceRestoreFolder, "logging.xml");
        } catch (Exception e) {
            this.catalogBackupRestoreTasklet.logValidationExceptions(
                    (ValidationResult) null,
                    new UnexpectedJobExecutionException(
                            "Exception occurred while storing GeoServer globals and services settings!",
                            e));
        }

        // Save Workspace specific settings
        try {
            sourceWorkspacesFolder = BackupUtils.dir(sourceRestoreFolder, "workspaces");

            // Set Default Namespace and Workspace
            if (!this.catalogBackupRestoreTasklet.filterIsValid()
                    && Resources.exists(sourceWorkspacesFolder.get("default.xml"))) {
                NamespaceInfo newDefaultNamespace =
                        (NamespaceInfo)
                                this.catalogBackupRestoreTasklet.doRead(
                                        sourceWorkspacesFolder, "defaultnamespace.xml");
                WorkspaceInfo newDefaultWorkspace =
                        (WorkspaceInfo)
                                this.catalogBackupRestoreTasklet.doRead(
                                        sourceWorkspacesFolder, "default.xml");
                this.catalogBackupRestoreTasklet
                        .getCatalog()
                        .setDefaultNamespace(newDefaultNamespace);
                this.catalogBackupRestoreTasklet
                        .getCatalog()
                        .setDefaultWorkspace(newDefaultWorkspace);
            }
        } catch (Exception e) {
            if (this.catalogBackupRestoreTasklet.filterIsValid()) {
                this.catalogBackupRestoreTasklet.logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer globals and services settings!",
                                e));
            } else {
                LOGGER.log(
                        Level.WARNING,
                        "Error occurred while trying to Restore the Default Workspace!",
                        e);
                if (this.catalogBackupRestoreTasklet.getCurrentJobExecution() != null) {
                    this.catalogBackupRestoreTasklet
                            .getCurrentJobExecution()
                            .addWarningExceptions(Arrays.asList(e));
                }
            }
        }

        // RESTORE
        // TODO: Save old settings
        /*
         * GeoServerInfo oldGeoServerInfo = geoserver.getGlobal(); SettingsInfo oldSettings = geoserver.getSettings(); LoggingInfo oldLoggingInfo =
         * geoserver.getLogging(); WorkspaceInfo oldDefaultWorkspace = geoserver.getCatalog().getDefaultWorkspace(); NamespaceInfo oldDefaultNamespace
         * = geoserver.getCatalog().getDefaultNamespace();
         */

        // Do this *ONLY* when DRY-RUN-MODE == OFF
        if (!this.catalogBackupRestoreTasklet.isDryRun()) {
            try {
                hardRestore(
                        geoserver,
                        dd,
                        sourceRestoreFolder,
                        sourceWorkspacesFolder,
                        newGeoServerInfo,
                        newLoggingInfo);
            } catch (Exception e) {
                this.catalogBackupRestoreTasklet.logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer globals and services settings!",
                                e));
            } finally {
                /*
                 * TODO: - Handle Revert ??
                 */
            }
        } else {
            // DRY-RUN-MODE ON: Try to check backup files consistency as much as possible
            try {
                // Temporary GeoServer Data Dir just for testing
                GeoServerDataDirectory td = new GeoServerDataDirectory(BackupUtils.tmpDir().dir());
                softRestore(
                        geoserver,
                        td,
                        sourceRestoreFolder,
                        sourceWorkspacesFolder,
                        newGeoServerInfo,
                        newLoggingInfo);
            } catch (Exception e) {
                this.catalogBackupRestoreTasklet.logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer globals and services settings!",
                                e));
            } finally {
            }
        }
    }

    /**
     * Performs a "Hard Restore". Repleaces the whole Catalog.
     *
     * @param geoserver
     * @param dd
     * @param sourceRestoreFolder
     * @param sourceWorkspacesFolder
     * @param newGeoServerInfo
     * @param newLoggingInfo
     * @throws IOException
     * @throws Exception
     * @throws IllegalArgumentException
     */
    public void hardRestore(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder,
            GeoServerInfo newGeoServerInfo,
            LoggingInfo newLoggingInfo)
            throws IOException, Exception, IllegalArgumentException {

        final boolean purgeResources = this.catalogBackupRestoreTasklet.isPurge();
        final boolean filterIsValid = this.catalogBackupRestoreTasklet.filterIsValid();

        if (!this.catalogBackupRestoreTasklet.isSkipSettings() && !filterIsValid) {
            restoreGlobals(geoserver, dd, sourceRestoreFolder, newGeoServerInfo, newLoggingInfo);
        }

        // Restore Workspaces
        // - Prepare folder
        Resource workspaces = dd.get("workspaces");
        if (purgeResources) {
            if (!filterIsValid) {
                Files.delete(workspaces.dir());
            }
            workspaces = BackupUtils.dir(dd.get(Paths.BASE), "workspaces");
        }
        restoreWorkSpacesAndLayers(sourceRestoreFolder, workspaces);

        // - GeoServer Catalog Alignment
        // Align missing Resources (in case of filtering) from the original catalog
        if (!purgeResources) {
            LOGGER.log(Level.INFO, "Sync catalogs in execution due to no purge flag activated.");
            this.catalogBackupRestoreTasklet.syncTo(geoserver.getCatalog());
        }

        // Restore Styles
        // - Prepare folder
        Resource styles = dd.get("styles");

        if (purgeResources && !filterIsValid) {
            Files.delete(styles.dir());
            styles = BackupUtils.dir(dd.get(Paths.BASE), "styles");
            restoreGlobalStyles(sourceRestoreFolder, styles);
        }

        // Restore Workspace Specific Settings and Services
        if (purgeResources) {
            restoreWorkspaceSpecifics(geoserver, dd, sourceRestoreFolder, sourceWorkspacesFolder);
        }

        // Restore GWC Configuration bits
        if (purgeResources || !this.catalogBackupRestoreTasklet.isSkipGWC()) {
            restoreGwc(geoserver, dd, sourceRestoreFolder);
        }
    }

    /**
     * Restore Workspace Specific Settings and Services.
     *
     * @param geoserver
     * @param dd
     * @param sourceRestoreFolder
     * @param sourceWorkspacesFolder
     * @throws Exception
     * @throws IOException
     */
    public void restoreWorkspaceSpecifics(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder)
            throws Exception, IOException {
        restoreLocalWorkspaceSettingsAndServices(
                geoserver, sourceRestoreFolder, sourceWorkspacesFolder, dd);

        // Restore GeoServer Plugins
        final GeoServerResourceLoader sourceGeoServerResourceLoader =
                new GeoServerResourceLoader(sourceRestoreFolder.dir());
        for (GeoServerPluginConfigurator pluginConfig :
                GeoServerExtensions.extensions(GeoServerPluginConfigurator.class)) {
            // On restore invoke 'pluginConfig.loadConfiguration(resourceLoader);'. Replace
            // 'properties' files first.
            for (Resource configFile : pluginConfig.getFileLocations()) {
                replaceConfigFile(sourceGeoServerResourceLoader, configFile);
            }

            // - Invoke 'pluginConfig.loadConfiguration' from the GOSERVER_DATA_DIR
            pluginConfig.loadConfiguration(dd.getResourceLoader());
        }

        for (GeoServerPropertyConfigurer props :
                GeoServerExtensions.extensions(GeoServerPropertyConfigurer.class)) {
            // On restore invoke 'props.reload();' after having replaced the properties files.
            Resource configFile = props.getConfigFile();
            replaceConfigFile(sourceGeoServerResourceLoader, configFile);

            // - Invoke 'props.reload()' from the GOSERVER_DATA_DIR
            props.reload();
        }

        // Restore other configuration bits, like images, palettes, user projections and so
        // on...
        this.catalogBackupRestoreTasklet.backupRestoreAdditionalResources(
                sourceGeoServerResourceLoader, dd.get(Paths.BASE));
    }

    /**
     * @param geoserver
     * @param dd
     * @param sourceRestoreFolder
     * @param newGeoServerInfo
     * @param newLoggingInfo
     * @throws Exception
     */
    public void restoreGlobals(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder,
            GeoServerInfo newGeoServerInfo,
            LoggingInfo newLoggingInfo)
            throws Exception {
        // Restore GeoServer Global Info
        Files.delete(dd.get("global.xml").file());
        this.catalogBackupRestoreTasklet.doWrite(
                newGeoServerInfo, dd.get(Paths.BASE), "global.xml");
        geoserver.setGlobal(newGeoServerInfo);

        // Restore GeoServer Global Logging Settings
        Files.delete(dd.get("logging.xml").file());
        this.catalogBackupRestoreTasklet.doWrite(newLoggingInfo, dd.get(Paths.BASE), "logging.xml");
        geoserver.setLogging(newLoggingInfo);

        restoreGlobalServices(sourceRestoreFolder, dd);
    }

    /**
     * Restore GWC Configurations.
     *
     * @param geoserver
     * @param dd
     * @param sourceRestoreFolder
     * @throws Exception
     */
    public void restoreGwc(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder)
            throws Exception {
        try {
            if (GeoServerExtensions.bean(
                            this.catalogBackupRestoreTasklet.GWC_GEOSERVER_CONFIG_PERSISTER)
                    != null) {
                restoreGWCSettings(sourceRestoreFolder, dd.get(Paths.BASE));

                // Initialize GWC with the new settings
                GWCInitializer gwcInitializer = GeoServerExtensions.bean(GWCInitializer.class);
                if (gwcInitializer != null) {
                    gwcInitializer.initialize(geoserver);
                }
            }
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
        }
    }

    /**
     * Performs a "Soft Restore", adds Resources to the current Catalog, if possible.
     *
     * @param geoserver
     * @param td
     * @param sourceRestoreFolder
     * @param sourceWorkspacesFolder
     * @param newGeoServerInfo
     * @param newLoggingInfo
     * @throws Exception
     */
    public void softRestore(
            final GeoServer geoserver,
            GeoServerDataDirectory td,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder,
            GeoServerInfo newGeoServerInfo,
            LoggingInfo newLoggingInfo)
            throws Exception {
        // Restore GeoServer Global Info
        this.catalogBackupRestoreTasklet.doWrite(
                newGeoServerInfo, td.get(Paths.BASE), "global.xml");

        // Restore GeoServer Global Logging Settings
        this.catalogBackupRestoreTasklet.doWrite(newLoggingInfo, td.get(Paths.BASE), "logging.xml");

        // Restore GeoServer Global Services
        restoreGlobalServices(sourceRestoreFolder, td);

        // Restore Workspaces
        // - Prepare folder
        BackupUtils.dir(td.get(Paths.BASE), "workspaces");
        Resource workspaces = td.get("workspaces");

        restoreWorkSpacesAndLayers(sourceRestoreFolder, workspaces);

        // Restore Styles
        // - Prepare folder
        BackupUtils.dir(td.get(Paths.BASE), "styles");
        Resource styles = td.get("styles");

        restoreGlobalStyles(sourceRestoreFolder, styles);

        // Restore LayerGroups
        // - Prepare folder
        BackupUtils.dir(td.get(Paths.BASE), "layergroups");
        Resource layerGroups = td.get("layergroups");

        // Restore Workspace Specific Settings and Services
        restoreLocalWorkspaceSettingsAndServices(
                geoserver, sourceRestoreFolder, sourceWorkspacesFolder, td);

        // Restore GeoServer Plugins
        final GeoServerResourceLoader sourceGeoServerResourceLoader =
                new GeoServerResourceLoader(sourceRestoreFolder.dir());

        // Restore other configuration bits, like images, palettes, user projections and so on...
        this.catalogBackupRestoreTasklet.backupRestoreAdditionalResources(
                sourceGeoServerResourceLoader, td.get(Paths.BASE));

        // Restore GWC Configuration bits
        try {
            if (GeoServerExtensions.bean(
                            this.catalogBackupRestoreTasklet.GWC_GEOSERVER_CONFIG_PERSISTER)
                    != null) {
                restoreGWCSettings(sourceRestoreFolder, td.get(Paths.BASE));
            }
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
        }

        // Cleanup Temp Folder
        Files.delete(td.get(Paths.BASE).dir());
    }

    /**
     * @param resourceLoader
     * @param configFile
     * @throws IOException
     */
    public void replaceConfigFile(final GeoServerResourceLoader resourceLoader, Resource configFile)
            throws IOException {
        // - Check of the resource exists on the restore folder
        final File destinationResource =
                resourceLoader.find(
                        Paths.path(
                                configFile.file().getParentFile().getName(),
                                configFile.file().getName()));
        if (destinationResource != null) {
            Resource rstConfigFile = Files.asResource(destinationResource);

            // - Copy the resource into the GOSERVER_DATA_DIR (overwriting the old one if exists)
            if (Resources.exists(rstConfigFile)) {
                Resources.copy(rstConfigFile.file(), configFile.parent());
            }
        }
    }

    /**
     * @param geoserver
     * @param sourceRestoreFolder
     * @param sourceWorkspacesFolder
     * @param dd
     * @throws Exception
     */
    public void restoreLocalWorkspaceSettingsAndServices(
            final GeoServer geoserver,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder,
            GeoServerDataDirectory dd)
            throws Exception {
        for (WorkspaceInfo ws : geoserver.getCatalog().getWorkspaces()) {
            if (!this.catalogBackupRestoreTasklet.filteredResource(ws, true)) {
                Resource wsFolder = BackupUtils.dir(sourceWorkspacesFolder, ws.getName());
                SettingsInfo wsSettings = null;
                if (Resources.exists(wsFolder.get("settings.xml"))) {
                    wsSettings =
                            (SettingsInfo)
                                    this.catalogBackupRestoreTasklet.doRead(
                                            wsFolder, "settings.xml");
                }

                if (wsSettings != null) {
                    wsSettings.setWorkspace(ws);
                    if (!this.catalogBackupRestoreTasklet.isDryRun()) {
                        geoserver.add(wsSettings);
                        this.catalogBackupRestoreTasklet.doWrite(
                                geoserver.getSettings(ws),
                                dd.get(Paths.path("workspaces", ws.getName())),
                                "settings.xml");
                    } else {
                        this.catalogBackupRestoreTasklet.doWrite(
                                wsSettings,
                                dd.get(Paths.path("workspaces", ws.getName())),
                                "settings.xml");
                    }

                    NamespaceInfo wsNameSpace = null;
                    if (Resources.exists(wsFolder.get("namespace.xml"))) {
                        wsNameSpace =
                                (NamespaceInfo)
                                        this.catalogBackupRestoreTasklet.doRead(
                                                wsFolder, "namespace.xml");
                    }

                    if (wsNameSpace != null) {
                        if (!this.catalogBackupRestoreTasklet.isDryRun()) {
                            geoserver.add(wsSettings);
                            this.catalogBackupRestoreTasklet.doWrite(
                                    geoserver.getSettings(ws),
                                    dd.get(Paths.path("workspaces", ws.getName())),
                                    "namespace.xml");
                        } else {
                            this.catalogBackupRestoreTasklet.doWrite(
                                    wsSettings,
                                    dd.get(Paths.path("workspaces", ws.getName())),
                                    "namespace.xml");
                        }
                    }

                    WorkspaceInfo wsInfo = null;
                    if (Resources.exists(wsFolder.get("workspace.xml"))) {
                        wsInfo =
                                (WorkspaceInfo)
                                        this.catalogBackupRestoreTasklet.doRead(
                                                wsFolder, "workspace.xml");
                    }

                    if (wsInfo != null) {
                        if (!this.catalogBackupRestoreTasklet.isDryRun()) {
                            geoserver.add(wsSettings);
                            this.catalogBackupRestoreTasklet.doWrite(
                                    geoserver.getSettings(ws),
                                    dd.get(Paths.path("workspaces", ws.getName())),
                                    "workspace.xml");
                        } else {
                            this.catalogBackupRestoreTasklet.doWrite(
                                    wsSettings,
                                    dd.get(Paths.path("workspaces", ws.getName())),
                                    "workspace.xml");
                        }
                    }
                }

                // Restore Workspace Local Services
                List<Resource> serviceResources =
                        Resources.list(
                                wsFolder,
                                new Filter<Resource>() {

                                    @Override
                                    public boolean accept(Resource res) {
                                        if (!"settings.xml".equals(res.name())
                                                && !"namespace.xml".equals(res.name())
                                                && !"workspace.xml".equals(res.name())
                                                && res.name().endsWith(".xml")) {
                                            return true;
                                        }
                                        return false;
                                    }
                                });
                for (Resource serviceResource : serviceResources) {
                    ServiceInfo localService =
                            (ServiceInfo)
                                    this.catalogBackupRestoreTasklet.doRead(
                                            wsFolder, serviceResource.name());
                    if (localService != null) {
                        localService.setWorkspace(ws);
                        if (!this.catalogBackupRestoreTasklet.isDryRun()) {
                            if (geoserver.getServiceByName(
                                            ws, serviceResource.name(), ServiceInfo.class)
                                    == null) {
                                geoserver.add(localService);
                            }
                        }
                        this.catalogBackupRestoreTasklet.doWrite(
                                localService, dd.get(Paths.path("workspaces", ws.getName())), "");
                    }
                }

                // Restore Local Styles
                for (StyleInfo sty :
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getStylesByWorkspace(ws.getName())) {
                    // Only Local Services here.
                    sty.setWorkspace(ws);
                    Resource wsLocalStyleFolder =
                            BackupUtils.dir(
                                    dd.get(Paths.path("workspaces", ws.getName())), "styles");
                    this.catalogBackupRestoreTasklet.doWrite(
                            sty, wsLocalStyleFolder, sty.getName() + ".xml");

                    Resource styResource =
                            sourceRestoreFolder.get(
                                    Paths.path(
                                            "workspaces",
                                            ws.getName(),
                                            "styles",
                                            sty.getFilename()));
                    if (Resources.exists(styResource)) {
                        Resources.copy(styResource.file(), wsLocalStyleFolder);
                    }
                }
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param styles
     * @throws Exception
     */
    public void restoreGlobalStyles(Resource sourceRestoreFolder, Resource styles)
            throws Exception {
        for (StyleInfo sty : this.catalogBackupRestoreTasklet.getCatalog().getStyles()) {
            // Only Global Styles here. Local ones will be restored later on
            if (sty.getWorkspace() == null) {
                this.catalogBackupRestoreTasklet.doWrite(sty, styles, sty.getName() + ".xml");

                Resource styResource =
                        sourceRestoreFolder.get(Paths.path("styles", sty.getFilename()));
                if (Resources.exists(styResource)) {
                    Resources.copy(styResource.file(), styles);
                }
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param td
     * @throws Exception
     */
    public void restoreGlobalServices(Resource sourceRestoreFolder, GeoServerDataDirectory td)
            throws Exception {
        for (Resource serviceResource : sourceRestoreFolder.get("services").list()) {
            // Local Services will be saved later on ...
            ServiceInfo service =
                    (ServiceInfo)
                            this.catalogBackupRestoreTasklet.doRead(
                                    sourceRestoreFolder.get("services"), serviceResource.name());
            if (service != null && service.getWorkspace() == null) {
                Files.delete(td.get(serviceResource.name()).file());
                Resources.copy(
                        sourceRestoreFolder
                                .get(Paths.path("services", serviceResource.name()))
                                .file(),
                        td.get(Paths.BASE));
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param workspaces
     * @throws Exception
     */
    public void restoreWorkSpacesAndLayers(Resource sourceRestoreFolder, Resource workspaces)
            throws Exception {
        // - Restore Default Workspace
        if (!this.catalogBackupRestoreTasklet.filterIsValid()
                || !this.catalogBackupRestoreTasklet.filteredResource(
                        this.catalogBackupRestoreTasklet.getCatalog().getDefaultWorkspace(),
                        true)) {
            Files.delete(workspaces.get("default.xml").file());
            this.catalogBackupRestoreTasklet.doWrite(
                    this.catalogBackupRestoreTasklet.getCatalog().getDefaultWorkspace(),
                    workspaces,
                    "default.xml");
        }

        // - Restore Workspaces/Namespaces definitions and settings
        for (WorkspaceInfo ws : this.catalogBackupRestoreTasklet.getCatalog().getWorkspaces()) {
            if (!this.catalogBackupRestoreTasklet.filteredResource(ws, true)) {
                // Restore Workspace and Namespace confifuration
                // - Prepare Folder
                Resource wsFolder = BackupUtils.dir(workspaces, ws.getName());
                if (this.catalogBackupRestoreTasklet.getFilters().length == 1
                        || this.catalogBackupRestoreTasklet.getFilters()[1] == null) {
                    Files.delete(workspaces.get(ws.getName()).dir());

                    this.catalogBackupRestoreTasklet.doWrite(
                            this.catalogBackupRestoreTasklet
                                    .getCatalog()
                                    .getNamespaceByPrefix(ws.getName()),
                            wsFolder,
                            "namespace.xml");
                    this.catalogBackupRestoreTasklet.doWrite(ws, wsFolder, "workspace.xml");
                }

                // Restore DataStores/CoverageStores
                for (DataStoreInfo ds :
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getStoresByWorkspace(ws.getName(), DataStoreInfo.class)) {
                    if (!this.catalogBackupRestoreTasklet.filteredResource(
                            ds, ws, true, StoreInfo.class)) {
                        // - Prepare Folder
                        Resource dsFolder = BackupUtils.dir(wsFolder, ds.getName());

                        if (this.catalogBackupRestoreTasklet.getFilters().length == 3
                                && this.catalogBackupRestoreTasklet.getFilters()[2] == null) {
                            Files.delete(dsFolder.dir());
                            ds.setWorkspace(ws);
                            this.catalogBackupRestoreTasklet.doWrite(ds, dsFolder, "datastore.xml");
                        }

                        // Restore Resources
                        for (FeatureTypeInfo ft :
                                this.catalogBackupRestoreTasklet
                                        .getCatalog()
                                        .getFeatureTypesByDataStore(ds)) {
                            if (!this.catalogBackupRestoreTasklet.filteredResource(
                                    ft, ws, true, ResourceInfo.class)) {
                                // - Prepare Folder
                                Files.delete(dsFolder.get(ft.getName()).dir());
                                Resource ftFolder = BackupUtils.dir(dsFolder, ft.getName());

                                this.catalogBackupRestoreTasklet.doWrite(
                                        ft, ftFolder, "featuretype.xml");

                                // Restore Layers
                                for (LayerInfo ly :
                                        this.catalogBackupRestoreTasklet
                                                .getCatalog()
                                                .getLayers(ft)) {
                                    if (!this.catalogBackupRestoreTasklet.filteredResource(
                                            ly, ws, true, LayerInfo.class)) {
                                        this.catalogBackupRestoreTasklet.doWrite(
                                                ly, ftFolder, "layer.xml");

                                        Resource ftResource =
                                                sourceRestoreFolder.get(
                                                        Paths.path(
                                                                "workspaces/"
                                                                        + ws.getName()
                                                                        + "/"
                                                                        + ds.getName(),
                                                                ft.getName()));
                                        List<Resource> resources =
                                                Resources.list(
                                                        ftResource,
                                                        new Filter<Resource>() {
                                                            @Override
                                                            public boolean accept(Resource res) {
                                                                if (res.getType()
                                                                                == Resource.Type
                                                                                        .RESOURCE
                                                                        && !res.name()
                                                                                .endsWith(".xml")) {
                                                                    return true;
                                                                }
                                                                return false;
                                                            }
                                                        },
                                                        true);

                                        for (Resource resource : resources) {
                                            Resources.copy(
                                                    resource.in(), ftFolder, resource.name());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (CoverageStoreInfo cs :
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getStoresByWorkspace(ws.getName(), CoverageStoreInfo.class)) {
                    if (!this.catalogBackupRestoreTasklet.filteredResource(
                            cs, ws, true, StoreInfo.class)) {
                        // - Prepare Folder
                        Resource csFolder = BackupUtils.dir(wsFolder, cs.getName());

                        cs.setWorkspace(ws);

                        this.catalogBackupRestoreTasklet.doWrite(cs, csFolder, "coveragestore.xml");

                        // Restore Resources
                        for (CoverageInfo ci :
                                this.catalogBackupRestoreTasklet
                                        .getCatalog()
                                        .getCoveragesByCoverageStore(cs)) {
                            if (!this.catalogBackupRestoreTasklet.filteredResource(
                                    ci, ws, true, ResourceInfo.class)) {
                                // - Prepare Folder
                                Files.delete(csFolder.get(ci.getName()).dir());
                                Resource ciFolder = BackupUtils.dir(csFolder, ci.getName());

                                this.catalogBackupRestoreTasklet.doWrite(
                                        ci, ciFolder, "coverage.xml");

                                // Restore Layers
                                for (LayerInfo ly :
                                        this.catalogBackupRestoreTasklet
                                                .getCatalog()
                                                .getLayers(ci)) {
                                    if (!this.catalogBackupRestoreTasklet.filteredResource(
                                            ly, ws, true, LayerInfo.class)) {
                                        this.catalogBackupRestoreTasklet.doWrite(
                                                ly, ciFolder, "layer.xml");

                                        Resource ftResource =
                                                sourceRestoreFolder.get(
                                                        Paths.path(
                                                                "workspaces/"
                                                                        + ws.getName()
                                                                        + "/"
                                                                        + cs.getName(),
                                                                ci.getName()));
                                        List<Resource> resources =
                                                Resources.list(
                                                        ftResource,
                                                        new Filter<Resource>() {
                                                            @Override
                                                            public boolean accept(Resource res) {
                                                                if (res.getType()
                                                                                == Resource.Type
                                                                                        .RESOURCE
                                                                        && !res.name()
                                                                                .endsWith(".xml")) {
                                                                    return true;
                                                                }
                                                                return false;
                                                            }
                                                        },
                                                        true);

                                        for (Resource resource : resources) {
                                            Resources.copy(
                                                    resource.in(), ciFolder, resource.name());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (WMSStoreInfo wms :
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getStoresByWorkspace(ws.getName(), WMSStoreInfo.class)) {
                    if (!this.catalogBackupRestoreTasklet.filteredResource(
                            wms, ws, true, StoreInfo.class)) {
                        restoreWMSStoreInfo(sourceRestoreFolder, ws, wsFolder, wms);
                    }
                }

                for (WMTSStoreInfo wmts :
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getStoresByWorkspace(ws.getName(), WMTSStoreInfo.class)) {
                    if (!this.catalogBackupRestoreTasklet.filteredResource(
                            wmts, ws, true, StoreInfo.class)) {
                        restoreWMTSStoreInfo(sourceRestoreFolder, ws, wsFolder, wmts);
                    }
                }
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param ws
     * @param wsFolder
     * @param wmts
     * @throws Exception
     */
    public void restoreWMTSStoreInfo(
            Resource sourceRestoreFolder, WorkspaceInfo ws, Resource wsFolder, WMTSStoreInfo wmts)
            throws Exception {
        // - Prepare Folder
        Resource wmtsFolder = BackupUtils.dir(wsFolder, wmts.getName());

        wmts.setWorkspace(ws);

        this.catalogBackupRestoreTasklet.doWrite(wmts, wmtsFolder, "wmtsstore.xml");

        // Restore Resources
        List<WMTSLayerInfo> wmtsLayers =
                this.catalogBackupRestoreTasklet
                        .getCatalog()
                        .getResourcesByStore(wmts, WMTSLayerInfo.class);
        for (WMTSLayerInfo wl : wmtsLayers) {
            if (!this.catalogBackupRestoreTasklet.filteredResource(
                    wl, ws, true, ResourceInfo.class)) {
                restoreWTMSLayer(sourceRestoreFolder, ws, wmts, wmtsFolder, wl);
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param ws
     * @param wsFolder
     * @param wms
     * @throws Exception
     */
    public void restoreWMSStoreInfo(
            Resource sourceRestoreFolder, WorkspaceInfo ws, Resource wsFolder, WMSStoreInfo wms)
            throws Exception {
        // - Prepare Folder
        Resource wmsFolder = BackupUtils.dir(wsFolder, wms.getName());

        wms.setWorkspace(ws);

        this.catalogBackupRestoreTasklet.doWrite(wms, wmsFolder, "wmsstore.xml");

        // Restore Resources
        List<WMSLayerInfo> wmsLayerInfoList =
                this.catalogBackupRestoreTasklet
                        .getCatalog()
                        .getResourcesByStore(wms, WMSLayerInfo.class);
        for (WMSLayerInfo wl : wmsLayerInfoList) {
            if (!this.catalogBackupRestoreTasklet.filteredResource(
                    wl, ws, true, ResourceInfo.class)) {
                restoreWMSLayer(sourceRestoreFolder, ws, wms, wmsFolder, wl);
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param ws
     * @param wms
     * @param wmsFolder
     * @param wl
     * @throws Exception
     */
    public void restoreWTMSLayer(
            Resource sourceRestoreFolder,
            WorkspaceInfo ws,
            WMTSStoreInfo wms,
            Resource wmsFolder,
            WMTSLayerInfo wl)
            throws Exception {
        // - Prepare Folder
        Files.delete(wmsFolder.get(wl.getName()).dir());
        Resource ftFolder = BackupUtils.dir(wmsFolder, wl.getName());

        this.catalogBackupRestoreTasklet.doWrite(wl, ftFolder, "wmtslayer.xml");

        // Restore Layers
        for (LayerInfo ly : this.catalogBackupRestoreTasklet.getCatalog().getLayers(wl)) {
            if (!this.catalogBackupRestoreTasklet.filteredResource(ly, ws, true, LayerInfo.class)) {
                String wmtsLayerInfoName = wl.getName();
                restoreLayerResources(
                        sourceRestoreFolder, ws, wms, ftFolder, ly, wmtsLayerInfoName);
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param ws
     * @param wms
     * @param wmsFolder
     * @param wl
     * @throws Exception
     */
    public void restoreWMSLayer(
            Resource sourceRestoreFolder,
            WorkspaceInfo ws,
            WMSStoreInfo wms,
            Resource wmsFolder,
            WMSLayerInfo wl)
            throws Exception {
        // - Prepare Folder
        Files.delete(wmsFolder.get(wl.getName()).dir());
        Resource ftFolder = BackupUtils.dir(wmsFolder, wl.getName());

        this.catalogBackupRestoreTasklet.doWrite(wl, ftFolder, "wmslayer.xml");

        // Restore Layers
        for (LayerInfo ly : this.catalogBackupRestoreTasklet.getCatalog().getLayers(wl)) {
            if (!this.catalogBackupRestoreTasklet.filteredResource(ly, ws, true, LayerInfo.class)) {
                String wmsLayerInfoName = wl.getName();
                restoreLayerResources(sourceRestoreFolder, ws, wms, ftFolder, ly, wmsLayerInfoName);
            }
        }
    }

    /**
     * @param sourceRestoreFolder
     * @param ws
     * @param httpStoreInfo
     * @param ftFolder
     * @param ly
     * @param wmsLayerInfoName
     * @throws Exception
     */
    public void restoreLayerResources(
            Resource sourceRestoreFolder,
            WorkspaceInfo ws,
            HTTPStoreInfo httpStoreInfo,
            Resource ftFolder,
            LayerInfo ly,
            String wmsLayerInfoName)
            throws Exception {
        this.catalogBackupRestoreTasklet.doWrite(ly, ftFolder, "layer.xml");

        Resource ftResource =
                sourceRestoreFolder.get(
                        Paths.path(
                                "workspaces/" + ws.getName() + "/" + httpStoreInfo.getName(),
                                wmsLayerInfoName));
        List<Resource> resources =
                Resources.list(
                        ftResource,
                        new Filter<Resource>() {
                            @Override
                            public boolean accept(Resource res) {
                                if (res.getType() == Resource.Type.RESOURCE
                                        && !res.name().endsWith(".xml")) {
                                    return true;
                                }
                                return false;
                            }
                        },
                        true);

        for (Resource resource : resources) {
            Resources.copy(resource.in(), ftFolder, resource.name());
        }
    }

    /**
     * TODO: When Restoring
     *
     * <p>1. the securityManager should issue the listeners 2. the GWCInitializer should be
     * re-initialized
     *
     * @param sourceRestoreFolder
     * @param baseDir
     * @throws Exception
     */
    public void restoreGWCSettings(Resource sourceRestoreFolder, Resource baseDir)
            throws Exception {
        // Restore configuration files form source and Test that everything went well
        try {
            // - Prepare folder
            GeoserverXMLResourceProvider gwcConfigProvider =
                    (GeoserverXMLResourceProvider)
                            GeoServerExtensions.bean("gwcXmlConfigResourceProvider");
            Resource targetGWCProviderRestoreDir = gwcConfigProvider.getConfigDirectory();
            if (!this.catalogBackupRestoreTasklet.filterIsValid()) {
                Files.delete(targetGWCProviderRestoreDir.dir());

                // Restore GWC Providers Configurations
                for (GeoserverXMLResourceProvider gwcProvider :
                        GeoServerExtensions.extensions(GeoserverXMLResourceProvider.class)) {
                    Resource providerConfigFile =
                            sourceRestoreFolder.get(
                                    Paths.path(
                                            GeoserverXMLResourceProvider
                                                    .DEFAULT_CONFIGURATION_DIR_NAME,
                                            gwcProvider.getConfigFileName()));
                    if (Resources.exists(providerConfigFile)
                            && FileUtils.sizeOf(providerConfigFile.file()) > 0) {
                        Resources.copy(
                                providerConfigFile.in(),
                                targetGWCProviderRestoreDir,
                                providerConfigFile.name());
                    }
                }
            }

            // Restore GWC Layers Configurations
            // TODO: This should be done using the spring-batch item reader/writer, since it is not
            // safe to save tons of single XML files.
            //       Nonetheless, given the default implementation of GWC Catalog does not have much
            // sense to refactor this code now.
            restoreGwcLayers(sourceRestoreFolder, targetGWCProviderRestoreDir);

        } catch (Exception e) {
            if (this.catalogBackupRestoreTasklet.getCurrentJobExecution() != null) {
                this.catalogBackupRestoreTasklet
                        .getCurrentJobExecution()
                        .addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    /**
     * Restores GWC Layers Configurations.
     *
     * @param sourceRestoreFolder
     * @param targetGWCProviderRestoreDir
     * @throws IOException
     */
    public void restoreGwcLayers(Resource sourceRestoreFolder, Resource targetGWCProviderRestoreDir)
            throws IOException {
        final TileLayerCatalog gwcCatalog =
                (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
        if (gwcCatalog == null) return;
        BiMap<String, String> layersByName = null;

        if (this.catalogBackupRestoreTasklet.isDryRun()) {
            BiMap<String, String> baseBiMap = HashBiMap.create();
            layersByName = Maps.synchronizedBiMap(baseBiMap);
        }

        final XMLConfiguration gwcXmlPersisterFactory =
                (XMLConfiguration) GeoServerExtensions.bean("gwcXmlConfig");
        final GeoServerResourceLoader resourceLoader =
                new GeoServerResourceLoader(sourceRestoreFolder.dir());

        final DefaultTileLayerCatalog gwcRestoreCatalog =
                new DefaultTileLayerCatalog(resourceLoader, gwcXmlPersisterFactory);
        gwcRestoreCatalog.initialize();
        // only delete gwc layers directory if its a full restore
        if (!this.catalogBackupRestoreTasklet.filterIsValid()) {
            Resource gwcCatalogPersistenceLocation =
                    targetGWCProviderRestoreDir.parent().get(gwcCatalog.getPersistenceLocation());
            Files.delete(gwcCatalogPersistenceLocation.dir());
        }
        // restore tile layers
        restoreGWCTileLayersInfos(gwcCatalog, layersByName, gwcRestoreCatalog);
    }

    /**
     * @param gwcCatalog
     * @param layersByName
     * @param gwcRestoreCatalog
     */
    public void restoreGWCTileLayersInfos(
            final TileLayerCatalog gwcCatalog,
            BiMap<String, String> layersByName,
            final TileLayerCatalog gwcRestoreCatalog) {
        for (String layerName : gwcRestoreCatalog.getLayerNames()) {
            GeoServerTileLayerInfo gwcLayerInfo = gwcRestoreCatalog.getLayerByName(layerName);

            LayerInfo layerInfo =
                    this.catalogBackupRestoreTasklet.getCatalog().getLayerByName(layerName);

            if (layerInfo != null) {
                WorkspaceInfo ws = this.catalogBackupRestoreTasklet.getLayerWorkspace(layerInfo);

                restoreGWCTileLayerInfo(
                        gwcCatalog, layersByName, layerName, gwcLayerInfo, layerInfo.getId());
            } else {
                LayerGroupInfo layerGroupInfo =
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getLayerGroupByName(layerName);

                if (layerGroupInfo != null) {
                    WorkspaceInfo ws =
                            this.catalogBackupRestoreTasklet.getLayerGroupWorkspace(layerGroupInfo);

                    restoreGWCTileLayerInfo(
                            gwcCatalog,
                            layersByName,
                            layerName,
                            gwcLayerInfo,
                            layerGroupInfo.getId());
                }
            }
        }
    }

    /**
     * @param gwcCatalog
     * @param layersByName
     * @param layerName
     * @param gwcLayerInfo
     * @param layerID
     * @throws IllegalArgumentException
     */
    public void restoreGWCTileLayerInfo(
            final TileLayerCatalog gwcCatalog,
            BiMap<String, String> layersByName,
            String layerName,
            GeoServerTileLayerInfo gwcLayerInfo,
            String layerID)
            throws IllegalArgumentException {
        if (this.catalogBackupRestoreTasklet.filterIsValid()
                && gwcCatalog.getLayerByName(layerName) == null) {
            return;
        }
        if (!this.catalogBackupRestoreTasklet.isDryRun()) {
            // - Depersist the GWC Layer Info into the restore folder
            GeoServerTileLayerInfo oldValue = gwcCatalog.getLayerByName(layerName);
            if (oldValue != null) {
                gwcCatalog.delete(oldValue.getId());
            }
            // - Update the ID
            gwcLayerInfo.setId(layerID);
            gwcCatalog.save(gwcLayerInfo);
        } else {
            if (layersByName.get(layerName) == null) {
                layersByName.put(layerName, layerID);
            } else {
                // - Warning or Exception
                throw new IllegalArgumentException(
                        "TileLayer with same name already exists: "
                                + layerName
                                + ": <"
                                + layerID
                                + ">");
            }
        }
    }
}
