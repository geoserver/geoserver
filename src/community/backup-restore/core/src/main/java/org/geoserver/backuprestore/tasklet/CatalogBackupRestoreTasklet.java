/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerPluginConfigurator;
import org.geoserver.config.GeoServerPropertyConfigurer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
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
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geowebcache.config.XMLConfiguration;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.util.Assert;

/**
 * Concrete implementation of the {@link AbstractCatalogBackupRestoreTasklet}. <br>
 * Actually takes care of dumping/restoring everything is not a {@link ResourceInfo}, like the GeoServer settings,
 * logging and global/local (workspaces) infos.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogBackupRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    private static final String GWC_GEOSERVER_CONFIG_PERSISTER = "gwcGeoServervConfigPersister";

    // whether existing resources should be deleted
    private boolean purge = true;

    // whether global settings should be skipped
    private boolean skipSettings = true;

    // whether GWC should be skipped
    private boolean skipGWC = false;

    public CatalogBackupRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        this.skipSettings =
                Boolean.parseBoolean(stepExecution.getJobParameters().getString(Backup.PARAM_SKIP_SETTINGS, "true"));

        this.skipGWC = Boolean.parseBoolean(stepExecution.getJobParameters().getString(Backup.PARAM_SKIP_GWC, "false"));

        this.purge =
                Boolean.parseBoolean(stepExecution.getJobParameters().getString(Backup.PARAM_PURGE_RESOURCES, "false"));
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        final GeoServer geoserver = backupFacade.getGeoServer();
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();
        final ResourceStore resourceStore = dd.getResourceStore();

        try {
            if (!isNew()) {
                doBackup(jobExecution, geoserver, dd, resourceStore);
            } else {
                doRestore(jobExecution, geoserver, dd);
            }
        } catch (Exception e) {
            logValidationExceptions(
                    (ValidationResult) null,
                    new UnexpectedJobExecutionException(
                            "Exception occurred while storing GeoServer globals and services settings!", e));
        }

        return RepeatStatus.FINISHED;
    }

    /** Perform Backup */
    private void doBackup(
            JobExecution jobExecution,
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            final ResourceStore resourceStore)
            throws Exception {
        try {
            final String outputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_OUTPUT_FILE_PATH);
            Resource targetBackupFolder = Resources.fromURL(outputFolderURL);

            authenticate();

            if (!skipSettings) {
                // Store GeoServer Global Info
                doWrite(geoserver.getGlobal(), targetBackupFolder, "global.xml");

                // Store GeoServer Global Settings
                doWrite(geoserver.getSettings(), targetBackupFolder, "settings.xml");

                // Store GeoServer Global Logging Settings
                doWrite(geoserver.getLogging(), targetBackupFolder, "logging.xml");

                // Store GeoServer Global Services
                for (ServiceInfo service : geoserver.getServices()) {
                    // Local Services will be saved later on ...
                    if (service.getWorkspace() == null) {
                        doWrite(service, targetBackupFolder, "services");
                    }
                }
            }

            // Save Workspace specific settings
            Resource targetWorkspacesFolder = BackupUtils.dir(targetBackupFolder, "workspaces");

            // Store Default Workspace
            if (!filteredResource(getCatalog().getDefaultWorkspace(), true)) {
                doWrite(getCatalog().getDefaultNamespace(), targetWorkspacesFolder, "defaultnamespace.xml");
                doWrite(getCatalog().getDefaultWorkspace(), targetWorkspacesFolder, "default.xml");
            }

            // Store Workspace Specific Settings and Services
            for (WorkspaceInfo ws : getCatalog().getWorkspaces()) {
                if (!filteredResource(ws, true)) {
                    if (geoserver.getSettings(ws) != null) {
                        doWrite(
                                geoserver.getSettings(ws),
                                BackupUtils.dir(targetWorkspacesFolder, ws.getName()),
                                "settings.xml");
                    }

                    if (geoserver.getServices(ws) != null) {
                        for (ServiceInfo service : geoserver.getServices(ws)) {
                            doWrite(service, targetWorkspacesFolder, ws.getName());
                        }
                    }

                    // Backup other configuration bits, like images, palettes, user projections and
                    // so on...
                    GeoServerDataDirectory wsDd = new GeoServerDataDirectory(
                            dd.get(Paths.path("workspaces", ws.getName())).dir());
                    backupRestoreAdditionalResources(wsDd.getResourceStore(), targetWorkspacesFolder.get(ws.getName()));

                    // Backup Style SLDs
                    for (StyleInfo sty : getCatalog().getStylesByWorkspace(ws)) {
                        Resource styResource = wsDd.get(Paths.path("styles", sty.getFilename()));
                        if (Resources.exists(styResource)) {
                            Resources.copy(
                                    styResource.file(),
                                    BackupUtils.dir(targetWorkspacesFolder.get(ws.getName()), "styles"));
                        }
                    }
                }
            }

            if (!filterIsValid()) {
                // Backup additional stuff only when performing a FULL backup
                backupFullAdditionals(dd, resourceStore, targetBackupFolder);
            } else {
                // backup selected GWC artifacts
                backupFilteredGwc(dd, resourceStore, targetBackupFolder);
            }
        } catch (Exception e) {
            logValidationExceptions(
                    (ValidationResult) null,
                    new UnexpectedJobExecutionException(
                            "Exception occurred while storing GeoServer globals and services settings!", e));
        }
    }

    private void backupFilteredGwc(
            final GeoServerDataDirectory dd, final ResourceStore resourceStore, Resource targetBackupFolder)
            throws IOException, Exception {
        if (!skipGWC) {
            try {
                if (GeoServerExtensions.bean(GWC_GEOSERVER_CONFIG_PERSISTER) != null) {
                    backupGWCLayers(targetBackupFolder);
                }
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
            }
        }
    }

    private void backupGWCLayers(Resource targetBackupFolder) throws Exception {
        GWCConfigPersister gwcGeoServerConfigPersister =
                (GWCConfigPersister) GeoServerExtensions.bean(GWC_GEOSERVER_CONFIG_PERSISTER);

        GWCConfigPersister testGWCCP = new GWCConfigPersister(
                getxStreamPersisterFactory(), new GeoServerResourceLoader(targetBackupFolder.dir()));

        // Test that everything went well
        try {
            testGWCCP.save(gwcGeoServerConfigPersister.getConfig());

            GWCConfig gwcConfig = testGWCCP.getConfig();

            Assert.notNull(gwcConfig, "gwcConfig is NULL");

            // Store GWC Layers Configurations
            final TileLayerCatalog gwcCatalog = (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");

            if (gwcCatalog != null) {
                final XMLConfiguration gwcXmlPersisterFactory =
                        (XMLConfiguration) GeoServerExtensions.bean("gwcXmlConfig");
                final GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(targetBackupFolder.dir());

                final DefaultTileLayerCatalog gwcBackupCatalog =
                        new DefaultTileLayerCatalog(resourceLoader, gwcXmlPersisterFactory);
                gwcBackupCatalog.initialize();

                for (String layerName : gwcCatalog.getLayerNames()) {
                    backupGwcLayer(gwcCatalog, gwcBackupCatalog, layerName);
                }
            }

        } catch (Exception e) {
            logValidationExceptions(null, e);
        }
    }

    /** Backup additional stuff only when performing a FULL backup. */
    private void backupFullAdditionals(
            final GeoServerDataDirectory dd, final ResourceStore resourceStore, Resource targetBackupFolder)
            throws IOException, Exception {
        // Backup GeoServer Plugins
        final GeoServerResourceLoader targetGeoServerResourceLoader =
                new GeoServerResourceLoader(targetBackupFolder.dir());
        for (GeoServerPluginConfigurator pluginConfig :
                GeoServerExtensions.extensions(GeoServerPluginConfigurator.class)) {
            // On restore invoke 'pluginConfig.loadConfiguration(resourceLoader);' after
            // having
            // replaced the config files.
            pluginConfig.saveConfiguration(targetGeoServerResourceLoader);
        }

        for (GeoServerPropertyConfigurer props : GeoServerExtensions.extensions(GeoServerPropertyConfigurer.class)) {
            // On restore invoke 'props.reload();' after having replaced the properties
            // files.
            Resource configFile = props.getConfigFile();

            if (configFile != null && Resources.exists(configFile)) {
                Resource targetDir = Files.asResource(targetGeoServerResourceLoader.findOrCreateDirectory(Paths.convert(
                        dd.getResourceLoader().getBaseDirectory(),
                        configFile.parent().dir())));

                Resources.copy(configFile.file(), targetDir);
            }
        }

        // Backup other configuration bits, like images, palettes, user projections and so
        // on...
        backupRestoreAdditionalResources(resourceStore, targetBackupFolder);

        // Backup GWC Configuration bits
        if (!skipGWC) {
            try {
                if (GeoServerExtensions.bean(GWC_GEOSERVER_CONFIG_PERSISTER) != null) {
                    backupGWCSettings(targetBackupFolder);
                }
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
            }
        }
    }

    /** */
    @SuppressWarnings("unused")
    private void doRestore(JobExecution jobExecution, final GeoServer geoserver, final GeoServerDataDirectory dd)
            throws Exception {
        final String inputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_INPUT_FILE_PATH);
        Resource sourceRestoreFolder = Resources.fromURL(inputFolderURL);
        Resource sourceWorkspacesFolder = null;

        authenticate();

        // Try first to load all the settings available into the source restore folder
        GeoServerInfo newGeoServerInfo = null;
        SettingsInfo newSettings = null;
        LoggingInfo newLoggingInfo = null;
        try {
            newGeoServerInfo = (GeoServerInfo) doRead(sourceRestoreFolder, "global.xml");
            newLoggingInfo = (LoggingInfo) doRead(sourceRestoreFolder, "logging.xml");
        } catch (Exception e) {
            logValidationExceptions(
                    (ValidationResult) null,
                    new UnexpectedJobExecutionException(
                            "Exception occurred while storing GeoServer globals and services settings!", e));
        }

        // Save Workspace specific settings
        try {
            sourceWorkspacesFolder = BackupUtils.dir(sourceRestoreFolder, "workspaces");

            // Set Default Namespace and Workspace
            if (!filterIsValid() && Resources.exists(sourceWorkspacesFolder.get("default.xml"))) {
                NamespaceInfo newDefaultNamespace =
                        (NamespaceInfo) doRead(sourceWorkspacesFolder, "defaultnamespace.xml");
                WorkspaceInfo newDefaultWorkspace = (WorkspaceInfo) doRead(sourceWorkspacesFolder, "default.xml");
                getCatalog().setDefaultNamespace(newDefaultNamespace);
                getCatalog().setDefaultWorkspace(newDefaultWorkspace);
            }
        } catch (Exception e) {
            if (filterIsValid()) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer globals and services settings!", e));
            } else {
                LOGGER.log(Level.WARNING, "Error occurred while trying to Restore the Default Workspace!", e);
                if (getCurrentJobExecution() != null) {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
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
        if (!isDryRun()) {
            try {
                hardRestore(
                        geoserver, dd, sourceRestoreFolder, sourceWorkspacesFolder, newGeoServerInfo, newLoggingInfo);
            } catch (Exception e) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer globals and services settings!", e));
            } finally {
                /*
                 * TODO: - Handle Revert ??
                 */
            }
        } else {
            // DRY-RUN-MODE ON: Try to check backup files consistency as much as possible
            try {
                // Temporary GeoServer Data Dir just for testing
                GeoServerDataDirectory td =
                        new GeoServerDataDirectory(BackupUtils.tmpDir().dir());
                softRestore(
                        geoserver, td, sourceRestoreFolder, sourceWorkspacesFolder, newGeoServerInfo, newLoggingInfo);
            } catch (Exception e) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer globals and services settings!", e));
            } finally {
            }
        }
    }

    /** */
    private void hardRestore(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder,
            GeoServerInfo newGeoServerInfo,
            LoggingInfo newLoggingInfo)
            throws IOException, Exception, IllegalArgumentException {

        final boolean purgeResources = purge;
        final boolean filterIsValid = filterIsValid();

        if (!skipSettings && !filterIsValid) {
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
            syncTo(geoserver.getCatalog());
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
        if (purgeResources || !skipGWC) {
            restoreGwc(geoserver, dd, sourceRestoreFolder);
        }
    }

    /** Restore Workspace Specific Settings and Services. */
    private void restoreWorkspaceSpecifics(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder)
            throws Exception, IOException {
        restoreLocalWorkspaceSettingsAndServices(geoserver, sourceRestoreFolder, sourceWorkspacesFolder, dd);

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

        for (GeoServerPropertyConfigurer props : GeoServerExtensions.extensions(GeoServerPropertyConfigurer.class)) {
            // On restore invoke 'props.reload();' after having replaced the properties files.
            Resource configFile = props.getConfigFile();
            replaceConfigFile(sourceGeoServerResourceLoader, configFile);

            // - Invoke 'props.reload()' from the GOSERVER_DATA_DIR
            props.reload();
        }

        // Restore other configuration bits, like images, palettes, user projections and so
        // on...
        backupRestoreAdditionalResources(sourceGeoServerResourceLoader, dd.get(Paths.BASE));
    }

    private void restoreGlobals(
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            Resource sourceRestoreFolder,
            GeoServerInfo newGeoServerInfo,
            LoggingInfo newLoggingInfo)
            throws Exception {
        // Restore GeoServer Global Info
        Files.delete(dd.get("global.xml").file());
        doWrite(newGeoServerInfo, dd.get(Paths.BASE), "global.xml");
        geoserver.setGlobal(newGeoServerInfo);

        // Restore GeoServer Global Logging Settings
        Files.delete(dd.get("logging.xml").file());
        doWrite(newLoggingInfo, dd.get(Paths.BASE), "logging.xml");
        geoserver.setLogging(newLoggingInfo);

        restoreGlobalServices(sourceRestoreFolder, dd);
    }

    private void restoreGwc(final GeoServer geoserver, final GeoServerDataDirectory dd, Resource sourceRestoreFolder)
            throws Exception {
        try {
            if (GeoServerExtensions.bean(GWC_GEOSERVER_CONFIG_PERSISTER) != null) {
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

    /** */
    private void softRestore(
            final GeoServer geoserver,
            GeoServerDataDirectory td,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder,
            GeoServerInfo newGeoServerInfo,
            LoggingInfo newLoggingInfo)
            throws Exception {
        // Restore GeoServer Global Info
        doWrite(newGeoServerInfo, td.get(Paths.BASE), "global.xml");

        // Restore GeoServer Global Logging Settings
        doWrite(newLoggingInfo, td.get(Paths.BASE), "logging.xml");

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
        restoreLocalWorkspaceSettingsAndServices(geoserver, sourceRestoreFolder, sourceWorkspacesFolder, td);

        // Restore GeoServer Plugins
        final GeoServerResourceLoader sourceGeoServerResourceLoader =
                new GeoServerResourceLoader(sourceRestoreFolder.dir());

        // Restore other configuration bits, like images, palettes, user projections and so on...
        backupRestoreAdditionalResources(sourceGeoServerResourceLoader, td.get(Paths.BASE));

        // Restore GWC Configuration bits
        try {
            if (GeoServerExtensions.bean(GWC_GEOSERVER_CONFIG_PERSISTER) != null) {
                restoreGWCSettings(sourceRestoreFolder, td.get(Paths.BASE));
            }
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
        }

        // Cleanup Temp Folder
        Files.delete(td.get(Paths.BASE).dir());
    }

    /** */
    private void replaceConfigFile(final GeoServerResourceLoader resourceLoader, Resource configFile)
            throws IOException {
        // - Check of the resource exists on the restore folder
        final File destinationResource = resourceLoader.find(Paths.path(
                configFile.file().getParentFile().getName(), configFile.file().getName()));
        if (destinationResource != null) {
            Resource rstConfigFile = Files.asResource(destinationResource);

            // - Copy the resource into the GOSERVER_DATA_DIR (overwriting the old one if exists)
            if (Resources.exists(rstConfigFile)) {
                Resources.copy(rstConfigFile.file(), configFile.parent());
            }
        }
    }

    /** */
    private void restoreLocalWorkspaceSettingsAndServices(
            final GeoServer geoserver,
            Resource sourceRestoreFolder,
            Resource sourceWorkspacesFolder,
            GeoServerDataDirectory dd)
            throws Exception {
        for (WorkspaceInfo ws : geoserver.getCatalog().getWorkspaces()) {
            if (!filteredResource(ws, true)) {
                Resource wsFolder = BackupUtils.dir(sourceWorkspacesFolder, ws.getName());
                SettingsInfo wsSettings = null;
                if (Resources.exists(wsFolder.get("settings.xml"))) {
                    wsSettings = (SettingsInfo) doRead(wsFolder, "settings.xml");
                }

                if (wsSettings != null) {
                    wsSettings.setWorkspace(ws);
                    if (!isDryRun()) {
                        geoserver.add(wsSettings);
                        doWrite(
                                geoserver.getSettings(ws),
                                dd.get(Paths.path("workspaces", ws.getName())),
                                "settings.xml");
                    } else {
                        doWrite(wsSettings, dd.get(Paths.path("workspaces", ws.getName())), "settings.xml");
                    }

                    NamespaceInfo wsNameSpace = null;
                    if (Resources.exists(wsFolder.get("namespace.xml"))) {
                        wsNameSpace = (NamespaceInfo) doRead(wsFolder, "namespace.xml");
                    }

                    if (wsNameSpace != null) {
                        if (!isDryRun()) {
                            geoserver.add(wsSettings);
                            doWrite(
                                    geoserver.getSettings(ws),
                                    dd.get(Paths.path("workspaces", ws.getName())),
                                    "namespace.xml");
                        } else {
                            doWrite(wsSettings, dd.get(Paths.path("workspaces", ws.getName())), "namespace.xml");
                        }
                    }

                    WorkspaceInfo wsInfo = null;
                    if (Resources.exists(wsFolder.get("workspace.xml"))) {
                        wsInfo = (WorkspaceInfo) doRead(wsFolder, "workspace.xml");
                    }

                    if (wsInfo != null) {
                        if (!isDryRun()) {
                            geoserver.add(wsSettings);
                            doWrite(
                                    geoserver.getSettings(ws),
                                    dd.get(Paths.path("workspaces", ws.getName())),
                                    "workspace.xml");
                        } else {
                            doWrite(wsSettings, dd.get(Paths.path("workspaces", ws.getName())), "workspace.xml");
                        }
                    }
                }

                // Restore Workspace Local Services
                List<Resource> serviceResources = Resources.list(wsFolder, new Filter<Resource>() {

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
                    ServiceInfo localService = (ServiceInfo) doRead(wsFolder, serviceResource.name());
                    if (localService != null) {
                        localService.setWorkspace(ws);
                        if (!isDryRun()) {
                            if (geoserver.getServiceByName(ws, serviceResource.name(), ServiceInfo.class) == null) {
                                geoserver.add(localService);
                            }
                        }
                        doWrite(localService, dd.get(Paths.path("workspaces", ws.getName())), "");
                    }
                }

                // Restore Local Styles
                for (StyleInfo sty : getCatalog().getStylesByWorkspace(ws.getName())) {
                    // Only Local Services here.
                    sty.setWorkspace(ws);
                    Resource wsLocalStyleFolder =
                            BackupUtils.dir(dd.get(Paths.path("workspaces", ws.getName())), "styles");
                    doWrite(sty, wsLocalStyleFolder, sty.getName() + ".xml");

                    Resource styResource = sourceRestoreFolder.get(
                            Paths.path("workspaces", ws.getName(), "styles", sty.getFilename()));
                    if (Resources.exists(styResource)) {
                        Resources.copy(styResource.file(), wsLocalStyleFolder);
                    }
                }
            }
        }
    }

    /** */
    private void restoreGlobalStyles(Resource sourceRestoreFolder, Resource styles) throws Exception {
        for (StyleInfo sty : getCatalog().getStyles()) {
            // Only Global Styles here. Local ones will be restored later on
            if (sty.getWorkspace() == null) {
                doWrite(sty, styles, sty.getName() + ".xml");

                Resource styResource = sourceRestoreFolder.get(Paths.path("styles", sty.getFilename()));
                if (Resources.exists(styResource)) {
                    Resources.copy(styResource.file(), styles);
                }
            }
        }
    }

    /** */
    private void restoreGlobalServices(Resource sourceRestoreFolder, GeoServerDataDirectory td) throws Exception {
        for (Resource serviceResource : sourceRestoreFolder.get("services").list()) {
            // Local Services will be saved later on ...
            ServiceInfo service = (ServiceInfo) doRead(sourceRestoreFolder.get("services"), serviceResource.name());
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

    /** */
    private void restoreWorkSpacesAndLayers(Resource sourceRestoreFodler, Resource workspaces) throws Exception {
        // - Restore Default Workspace
        if (!filterIsValid() || !filteredResource(getCatalog().getDefaultWorkspace(), true)) {
            Files.delete(workspaces.get("default.xml").file());
            doWrite(getCatalog().getDefaultWorkspace(), workspaces, "default.xml");
        }

        // - Restore Workspaces/Namespaces definitions and settings
        for (WorkspaceInfo ws : getCatalog().getWorkspaces()) {
            if (!filteredResource(ws, true)) {
                // Restore Workspace and Namespace confifuration
                // - Prepare Folder
                Resource wsFolder = BackupUtils.dir(workspaces, ws.getName());
                if (getFilters().length == 1 || getFilters()[1] == null) {
                    Files.delete(workspaces.get(ws.getName()).dir());

                    doWrite(getCatalog().getNamespaceByPrefix(ws.getName()), wsFolder, "namespace.xml");
                    doWrite(ws, wsFolder, "workspace.xml");
                }

                // Restore DataStores/CoverageStores
                for (DataStoreInfo ds : getCatalog().getStoresByWorkspace(ws.getName(), DataStoreInfo.class)) {
                    if (!filteredResource(ds, ws, true, StoreInfo.class)) {
                        // - Prepare Folder
                        Resource dsFolder = BackupUtils.dir(wsFolder, ds.getName());

                        if (getFilters().length == 3 && getFilters()[2] == null) {
                            Files.delete(dsFolder.dir());
                            ds.setWorkspace(ws);
                            doWrite(ds, dsFolder, "datastore.xml");
                        }

                        // Restore Resources
                        for (FeatureTypeInfo ft : getCatalog().getFeatureTypesByDataStore(ds)) {
                            if (!filteredResource(ft, ws, true, ResourceInfo.class)) {
                                // - Prepare Folder
                                Files.delete(dsFolder.get(ft.getName()).dir());
                                Resource ftFolder = BackupUtils.dir(dsFolder, ft.getName());

                                doWrite(ft, ftFolder, "featuretype.xml");

                                // Restore Layers
                                for (LayerInfo ly : getCatalog().getLayers(ft)) {
                                    if (!filteredResource(ly, ws, true, LayerInfo.class)) {
                                        doWrite(ly, ftFolder, "layer.xml");

                                        Resource ftResource = sourceRestoreFodler.get(Paths.path(
                                                "workspaces/" + ws.getName() + "/" + ds.getName(), ft.getName()));
                                        List<Resource> resources = Resources.list(
                                                ftResource,
                                                new Filter<Resource>() {
                                                    @Override
                                                    public boolean accept(Resource res) {
                                                        if (res.getType() == Type.RESOURCE
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
                                }
                            }
                        }
                    }
                }

                for (CoverageStoreInfo cs : getCatalog().getStoresByWorkspace(ws.getName(), CoverageStoreInfo.class)) {
                    if (!filteredResource(cs, ws, true, StoreInfo.class)) {
                        // - Prepare Folder
                        Resource csFolder = BackupUtils.dir(wsFolder, cs.getName());

                        cs.setWorkspace(ws);

                        doWrite(cs, csFolder, "coveragestore.xml");

                        // Restore Resources
                        for (CoverageInfo ci : getCatalog().getCoveragesByCoverageStore(cs)) {
                            if (!filteredResource(ci, ws, true, ResourceInfo.class)) {
                                // - Prepare Folder
                                Files.delete(csFolder.get(ci.getName()).dir());
                                Resource ciFolder = BackupUtils.dir(csFolder, ci.getName());

                                doWrite(ci, ciFolder, "coverage.xml");

                                // Restore Layers
                                for (LayerInfo ly : getCatalog().getLayers(ci)) {
                                    if (!filteredResource(ly, ws, true, LayerInfo.class)) {
                                        doWrite(ly, ciFolder, "layer.xml");

                                        Resource ftResource = sourceRestoreFodler.get(Paths.path(
                                                "workspaces/" + ws.getName() + "/" + cs.getName(), ci.getName()));
                                        List<Resource> resources = Resources.list(
                                                ftResource,
                                                new Filter<Resource>() {
                                                    @Override
                                                    public boolean accept(Resource res) {
                                                        if (res.getType() == Type.RESOURCE
                                                                && !res.name().endsWith(".xml")) {
                                                            return true;
                                                        }
                                                        return false;
                                                    }
                                                },
                                                true);

                                        for (Resource resource : resources) {
                                            Resources.copy(resource.in(), ciFolder, resource.name());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (WMSStoreInfo wms : getCatalog().getStoresByWorkspace(ws.getName(), WMSStoreInfo.class)) {
                    if (!filteredResource(wms, ws, true, StoreInfo.class)) {
                        restoreWMSStoreInfo(sourceRestoreFodler, ws, wsFolder, wms);
                    }
                }

                for (WMTSStoreInfo wmts : getCatalog().getStoresByWorkspace(ws.getName(), WMTSStoreInfo.class)) {
                    if (!filteredResource(wmts, ws, true, StoreInfo.class)) {
                        restoreWMTSStoreInfo(sourceRestoreFodler, ws, wsFolder, wmts);
                    }
                }
            }
        }
    }

    private void restoreWMTSStoreInfo(
            Resource sourceRestoreFodler, WorkspaceInfo ws, Resource wsFolder, WMTSStoreInfo wmts) throws Exception {
        // - Prepare Folder
        Resource wmtsFolder = BackupUtils.dir(wsFolder, wmts.getName());

        wmts.setWorkspace(ws);

        doWrite(wmts, wmtsFolder, "wmtsstore.xml");
        // Restore Resources
        List<WMTSLayerInfo> wmtsLayers = getCatalog().getResourcesByStore(wmts, WMTSLayerInfo.class);
        for (WMTSLayerInfo wl : wmtsLayers) {
            if (!filteredResource(wl, ws, true, ResourceInfo.class)) {
                restoreWTMSLayer(sourceRestoreFodler, ws, wmts, wmtsFolder, wl);
            }
        }
    }

    private void restoreWMSStoreInfo(
            Resource sourceRestoreFodler, WorkspaceInfo ws, Resource wsFolder, WMSStoreInfo wms) throws Exception {
        // - Prepare Folder
        Resource wmsFolder = BackupUtils.dir(wsFolder, wms.getName());

        wms.setWorkspace(ws);

        doWrite(wms, wmsFolder, "wmsstore.xml");
        // Restore Resources
        List<WMSLayerInfo> wmsLayerInfoList = getCatalog().getResourcesByStore(wms, WMSLayerInfo.class);
        for (WMSLayerInfo wl : wmsLayerInfoList) {
            if (!filteredResource(wl, ws, true, ResourceInfo.class)) {
                restoreWMSLayer(sourceRestoreFodler, ws, wms, wmsFolder, wl);
            }
        }
    }

    private void restoreWTMSLayer(
            Resource sourceRestoreFodler, WorkspaceInfo ws, WMTSStoreInfo wms, Resource wmsFolder, WMTSLayerInfo wl)
            throws Exception {
        // - Prepare Folder
        Files.delete(wmsFolder.get(wl.getName()).dir());
        Resource ftFolder = BackupUtils.dir(wmsFolder, wl.getName());

        doWrite(wl, ftFolder, "wmtslayer.xml");

        // Restore Layers
        for (LayerInfo ly : getCatalog().getLayers(wl)) {
            if (!filteredResource(ly, ws, true, LayerInfo.class)) {
                String wmtsLayerInfoName = wl.getName();
                restoreLayerResources(sourceRestoreFodler, ws, wms, ftFolder, ly, wmtsLayerInfoName);
            }
        }
    }

    private void restoreWMSLayer(
            Resource sourceRestoreFodler, WorkspaceInfo ws, WMSStoreInfo wms, Resource wmsFolder, WMSLayerInfo wl)
            throws Exception {
        // - Prepare Folder
        Files.delete(wmsFolder.get(wl.getName()).dir());
        Resource ftFolder = BackupUtils.dir(wmsFolder, wl.getName());

        doWrite(wl, ftFolder, "wmslayer.xml");

        // Restore Layers
        for (LayerInfo ly : getCatalog().getLayers(wl)) {
            if (!filteredResource(ly, ws, true, LayerInfo.class)) {
                String wmsLayerInfoName = wl.getName();
                restoreLayerResources(sourceRestoreFodler, ws, wms, ftFolder, ly, wmsLayerInfoName);
            }
        }
    }

    private void restoreLayerResources(
            Resource sourceRestoreFodler,
            WorkspaceInfo ws,
            HTTPStoreInfo httpStoreInfo,
            Resource ftFolder,
            LayerInfo ly,
            String wmsLayerInfoName)
            throws Exception {
        doWrite(ly, ftFolder, "layer.xml");

        Resource ftResource = sourceRestoreFodler.get(
                Paths.path("workspaces/" + ws.getName() + "/" + httpStoreInfo.getName(), wmsLayerInfoName));
        List<Resource> resources = Resources.list(
                ftResource,
                new Filter<Resource>() {
                    @Override
                    public boolean accept(Resource res) {
                        if (res.getType() == Type.RESOURCE && !res.name().endsWith(".xml")) {
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

    // ////////////////////////////////////////////////////////////////////////////////////// //
    //
    // GWC Stuff
    // ////////////////////////////////////////////////////////////////////////////////////// //
    /** */
    private void backupGWCSettings(Resource targetBackupFolder) throws Exception {
        GWCConfigPersister gwcGeoServerConfigPersister =
                (GWCConfigPersister) GeoServerExtensions.bean(GWC_GEOSERVER_CONFIG_PERSISTER);

        GWCConfigPersister testGWCCP = new GWCConfigPersister(
                getxStreamPersisterFactory(), new GeoServerResourceLoader(targetBackupFolder.dir()));

        // Test that everything went well
        try {
            testGWCCP.save(gwcGeoServerConfigPersister.getConfig());

            GWCConfig gwcConfig = testGWCCP.getConfig();

            Assert.notNull(gwcConfig, "gwcConfig is NULL");

            // TODO: perform more tests and integrity checks on reloaded configuration

            // Store GWC Providers Configurations
            Resource targetGWCProviderBackupDir =
                    BackupUtils.dir(targetBackupFolder, GeoserverXMLResourceProvider.DEFAULT_CONFIGURATION_DIR_NAME);

            for (GeoserverXMLResourceProvider gwcProvider :
                    GeoServerExtensions.extensions(GeoserverXMLResourceProvider.class)) {
                Resource providerConfigFile = Resources.fromPath(gwcProvider.getLocation());
                if (Resources.exists(providerConfigFile) && FileUtils.sizeOf(providerConfigFile.file()) > 0) {
                    Resources.copy(gwcProvider.in(), targetGWCProviderBackupDir, providerConfigFile.name());
                }
            }

            // Store GWC Layers Configurations
            // TODO: This should be done using the spring-batch item reader/writer, since it is not
            // safe to save tons of single XML files.
            //       Nonetheless, given the default implementation of GWC Catalog does not have much
            // sense to refactor this code now.
            final TileLayerCatalog gwcCatalog = (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");

            if (gwcCatalog != null) {
                final XMLConfiguration gwcXmlPersisterFactory =
                        (XMLConfiguration) GeoServerExtensions.bean("gwcXmlConfig");
                final GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(targetBackupFolder.dir());

                final DefaultTileLayerCatalog gwcBackupCatalog =
                        new DefaultTileLayerCatalog(resourceLoader, gwcXmlPersisterFactory);
                gwcBackupCatalog.initialize();

                for (String layerName : gwcCatalog.getLayerNames()) {
                    backupGwcLayer(gwcCatalog, gwcBackupCatalog, layerName);
                }
            }

        } catch (Exception e) {
            logValidationExceptions(null, e);
        }
    }

    private void backupGwcLayer(
            final TileLayerCatalog gwcCatalog, final DefaultTileLayerCatalog gwcBackupCatalog, String layerName) {
        GeoServerTileLayerInfo gwcLayerInfo = gwcCatalog.getLayerByName(layerName);

        // Persist the GWC Layer Info into the backup folder
        boolean persistResource = false;

        LayerInfo layerInfo = getCatalog().getLayerByName(layerName);

        if (layerInfo != null) {
            WorkspaceInfo ws = getLayerWorkspace(layerInfo);

            if (!filteredResource(layerInfo, ws, true, LayerInfo.class)) {
                persistResource = true;
            }
        } else {
            try {
                LayerGroupInfo layerGroupInfo = getCatalog().getLayerGroupByName(layerName);
                if (layerGroupInfo != null) {
                    WorkspaceInfo ws = getLayerGroupWorkspace(layerGroupInfo);

                    if (!filteredResource(ws, false)) {
                        persistResource = true;
                    }
                }
            } catch (NullPointerException e) {
                if (getCurrentJobExecution() != null) {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
            }
        }

        if (persistResource) {
            gwcBackupCatalog.save(gwcLayerInfo);
        }
    }

    private WorkspaceInfo getLayerGroupWorkspace(LayerGroupInfo layerGroupInfo) {
        WorkspaceInfo ws = layerGroupInfo.getWorkspace() != null
                ? getCatalog().getWorkspaceByName(layerGroupInfo.getWorkspace().getName())
                : null;
        return ws;
    }

    private WorkspaceInfo getLayerWorkspace(LayerInfo layerInfo) {
        WorkspaceInfo ws = layerInfo.getResource() != null
                        && layerInfo.getResource().getStore() != null
                        && layerInfo.getResource().getStore().getWorkspace() != null
                ? getCatalog()
                        .getWorkspaceByName(layerInfo
                                .getResource()
                                .getStore()
                                .getWorkspace()
                                .getName())
                : null;
        return ws;
    }

    /**
     * TODO: When Restoring
     *
     * <p>1. the securityManager should issue the listeners 2. the GWCInitializer should be re-initialized
     */
    private void restoreGWCSettings(Resource sourceRestoreFolder, Resource baseDir) throws Exception {
        // Restore configuration files form source and Test that everything went well
        try {
            // - Prepare folder
            GeoserverXMLResourceProvider gwcConfigProvider =
                    (GeoserverXMLResourceProvider) GeoServerExtensions.bean("gwcXmlConfigResourceProvider");
            Resource targetGWCProviderRestoreDir = gwcConfigProvider.getConfigDirectory();
            if (!filterIsValid()) {
                Files.delete(targetGWCProviderRestoreDir.dir());

                // Restore GWC Providers Configurations
                for (GeoserverXMLResourceProvider gwcProvider :
                        GeoServerExtensions.extensions(GeoserverXMLResourceProvider.class)) {
                    Resource providerConfigFile = sourceRestoreFolder.get(Paths.path(
                            GeoserverXMLResourceProvider.DEFAULT_CONFIGURATION_DIR_NAME,
                            gwcProvider.getConfigFileName()));
                    if (Resources.exists(providerConfigFile) && FileUtils.sizeOf(providerConfigFile.file()) > 0) {
                        Resources.copy(providerConfigFile.in(), targetGWCProviderRestoreDir, providerConfigFile.name());
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
            if (getCurrentJobExecution() != null) {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    /** Restores GWC Layers Configurations. */
    private void restoreGwcLayers(Resource sourceRestoreFolder, Resource targetGWCProviderRestoreDir)
            throws IOException {
        final TileLayerCatalog gwcCatalog = (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
        if (gwcCatalog == null) return;
        BiMap<String, String> layersByName = null;

        if (isDryRun()) {
            BiMap<String, String> baseBiMap = HashBiMap.create();
            layersByName = Maps.synchronizedBiMap(baseBiMap);
        }

        final XMLConfiguration gwcXmlPersisterFactory = (XMLConfiguration) GeoServerExtensions.bean("gwcXmlConfig");
        final GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(sourceRestoreFolder.dir());

        final DefaultTileLayerCatalog gwcRestoreCatalog =
                new DefaultTileLayerCatalog(resourceLoader, gwcXmlPersisterFactory);
        gwcRestoreCatalog.initialize();
        // only delete gwc layers directory if its a full restore
        if (!filterIsValid()) {
            Resource gwcCatalogPersistenceLocation =
                    targetGWCProviderRestoreDir.parent().get(gwcCatalog.getPersistenceLocation());
            Files.delete(gwcCatalogPersistenceLocation.dir());
        }
        // restore tile layers
        restoreGWCTileLayersInfos(gwcCatalog, layersByName, gwcRestoreCatalog);
    }

    /** */
    private void restoreGWCTileLayersInfos(
            final TileLayerCatalog gwcCatalog,
            BiMap<String, String> layersByName,
            final TileLayerCatalog gwcRestoreCatalog) {
        for (String layerName : gwcRestoreCatalog.getLayerNames()) {
            GeoServerTileLayerInfo gwcLayerInfo = gwcRestoreCatalog.getLayerByName(layerName);

            LayerInfo layerInfo = getCatalog().getLayerByName(layerName);

            if (layerInfo != null) {
                WorkspaceInfo ws = getLayerWorkspace(layerInfo);

                restoreGWCTileLayerInfo(gwcCatalog, layersByName, layerName, gwcLayerInfo, layerInfo.getId());
            } else {
                LayerGroupInfo layerGroupInfo = getCatalog().getLayerGroupByName(layerName);

                if (layerGroupInfo != null) {
                    WorkspaceInfo ws = getLayerGroupWorkspace(layerGroupInfo);

                    restoreGWCTileLayerInfo(gwcCatalog, layersByName, layerName, gwcLayerInfo, layerGroupInfo.getId());
                }
            }
        }
    }

    /** */
    private void restoreGWCTileLayerInfo(
            final TileLayerCatalog gwcCatalog,
            BiMap<String, String> layersByName,
            String layerName,
            GeoServerTileLayerInfo gwcLayerInfo,
            String layerID)
            throws IllegalArgumentException {
        if (filterIsValid() && gwcCatalog.getLayerByName(layerName) == null) {
            return;
        }
        if (!isDryRun()) {
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
                        "TileLayer with same name already exists: " + layerName + ": <" + layerID + ">");
            }
        }
    }
}
