/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer.backup;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.tasklet.CatalogBackupRestoreTasklet;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.*;
import org.geoserver.config.*;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geoserver.gwc.layer.DefaultTileLayerCatalog;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.*;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.XMLConfiguration;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.util.Assert;

public class BackupRunner {

    protected static Logger LOGGER = Logging.getLogger(BackupRunner.class);

    CatalogBackupRestoreTasklet catalogBackupRestoreTasklet;

    /** @param catalogBackupRestoreTasklet */
    public BackupRunner(CatalogBackupRestoreTasklet catalogBackupRestoreTasklet) {
        this.catalogBackupRestoreTasklet = catalogBackupRestoreTasklet;
    }

    /**
     * Perform Backup *
     *
     * @param jobExecution
     * @param geoserver
     * @param dd
     * @param resourceStore
     * @throws Exception
     */
    public void doBackup(
            JobExecution jobExecution,
            final GeoServer geoserver,
            final GeoServerDataDirectory dd,
            final ResourceStore resourceStore)
            throws Exception {
        try {
            final String outputFolderURL =
                    jobExecution
                            .getJobParameters()
                            .getString(org.geoserver.backuprestore.Backup.PARAM_OUTPUT_FILE_PATH);
            Resource targetBackupFolder = Resources.fromURL(outputFolderURL);

            this.catalogBackupRestoreTasklet.authenticate();

            if (!this.catalogBackupRestoreTasklet.isSkipSettings()) {
                // Store GeoServer Global Info
                this.catalogBackupRestoreTasklet.doWrite(
                        geoserver.getGlobal(), targetBackupFolder, "global.xml");

                // Store GeoServer Global Settings
                this.catalogBackupRestoreTasklet.doWrite(
                        geoserver.getSettings(), targetBackupFolder, "settings.xml");

                // Store GeoServer Global Logging Settings
                this.catalogBackupRestoreTasklet.doWrite(
                        geoserver.getLogging(), targetBackupFolder, "logging.xml");

                // Store GeoServer Global Services
                for (ServiceInfo service : geoserver.getServices()) {
                    // Local Services will be saved later on ...
                    if (service.getWorkspace() == null) {
                        this.catalogBackupRestoreTasklet.doWrite(
                                service, targetBackupFolder, "services");
                    }
                }
            }

            // Save Workspace specific settings
            Resource targetWorkspacesFolder = BackupUtils.dir(targetBackupFolder, "workspaces");

            // Store Default Workspace
            if (!this.catalogBackupRestoreTasklet.filteredResource(
                    this.catalogBackupRestoreTasklet.getCatalog().getDefaultWorkspace(), true)) {
                this.catalogBackupRestoreTasklet.doWrite(
                        this.catalogBackupRestoreTasklet.getCatalog().getDefaultNamespace(),
                        targetWorkspacesFolder,
                        "defaultnamespace.xml");
                this.catalogBackupRestoreTasklet.doWrite(
                        this.catalogBackupRestoreTasklet.getCatalog().getDefaultWorkspace(),
                        targetWorkspacesFolder,
                        "default.xml");
            }

            // Store Workspace Specific Settings and Services
            for (WorkspaceInfo ws : this.catalogBackupRestoreTasklet.getCatalog().getWorkspaces()) {
                if (!this.catalogBackupRestoreTasklet.filteredResource(ws, true)) {
                    if (geoserver.getSettings(ws) != null) {
                        this.catalogBackupRestoreTasklet.doWrite(
                                geoserver.getSettings(ws),
                                BackupUtils.dir(targetWorkspacesFolder, ws.getName()),
                                "settings.xml");
                    }

                    if (geoserver.getServices(ws) != null) {
                        for (ServiceInfo service : geoserver.getServices(ws)) {
                            this.catalogBackupRestoreTasklet.doWrite(
                                    service, targetWorkspacesFolder, ws.getName());
                        }
                    }

                    // Backup other configuration bits, like images, palettes, user projections and
                    // so on...
                    GeoServerDataDirectory wsDd =
                            new GeoServerDataDirectory(
                                    dd.get(Paths.path("workspaces", ws.getName())).dir());
                    this.catalogBackupRestoreTasklet.backupRestoreAdditionalResources(
                            wsDd.getResourceStore(), targetWorkspacesFolder.get(ws.getName()));

                    // Backup Style SLDs
                    for (StyleInfo sty :
                            this.catalogBackupRestoreTasklet
                                    .getCatalog()
                                    .getStylesByWorkspace(ws)) {
                        Resource styResource = wsDd.get(Paths.path("styles", sty.getFilename()));
                        if (Resources.exists(styResource)) {
                            Resources.copy(
                                    styResource.file(),
                                    BackupUtils.dir(
                                            targetWorkspacesFolder.get(ws.getName()), "styles"));
                        }
                    }
                }
            }

            if (!this.catalogBackupRestoreTasklet.filterIsValid()) {
                // Backup additional stuff only when performing a FULL backup
                backupFullAdditionals(dd, resourceStore, targetBackupFolder);
            } else {
                // backup selected GWC artifacts
                backupFilteredGwc(dd, resourceStore, targetBackupFolder);
            }
        } catch (Exception e) {
            this.catalogBackupRestoreTasklet.logValidationExceptions(
                    (ValidationResult) null,
                    new UnexpectedJobExecutionException(
                            "Exception occurred while storing GeoServer globals and services settings!",
                            e));
        }
    }

    /**
     * @param dd
     * @param resourceStore
     * @param targetBackupFolder
     * @throws IOException
     * @throws Exception
     */
    public void backupFilteredGwc(
            final GeoServerDataDirectory dd,
            final ResourceStore resourceStore,
            Resource targetBackupFolder)
            throws IOException, Exception {
        if (!this.catalogBackupRestoreTasklet.isSkipGWC()) {
            try {
                if (GeoServerExtensions.bean(
                                this.catalogBackupRestoreTasklet.GWC_GEOSERVER_CONFIG_PERSISTER)
                        != null) {
                    backupGWCLayers(targetBackupFolder);
                }
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
            }
        }
    }

    /**
     * @param targetBackupFolder
     * @throws Exception
     */
    public void backupGWCSettings(Resource targetBackupFolder) throws Exception {
        GWCConfigPersister gwcGeoServerConfigPersister =
                (GWCConfigPersister)
                        GeoServerExtensions.bean(
                                this.catalogBackupRestoreTasklet.GWC_GEOSERVER_CONFIG_PERSISTER);

        GWCConfigPersister testGWCCP =
                new GWCConfigPersister(
                        this.catalogBackupRestoreTasklet.getxStreamPersisterFactory(),
                        new GeoServerResourceLoader(targetBackupFolder.dir()));

        // Test that everything went well
        try {
            testGWCCP.save(gwcGeoServerConfigPersister.getConfig());

            GWCConfig gwcConfig = testGWCCP.getConfig();

            Assert.notNull(gwcConfig, "gwcConfig is NULL");

            // TODO: perform more tests and integrity checks on reloaded configuration

            // Store GWC Providers Configurations
            Resource targetGWCProviderBackupDir =
                    BackupUtils.dir(
                            targetBackupFolder,
                            GeoserverXMLResourceProvider.DEFAULT_CONFIGURATION_DIR_NAME);

            for (GeoserverXMLResourceProvider gwcProvider :
                    GeoServerExtensions.extensions(GeoserverXMLResourceProvider.class)) {
                Resource providerConfigFile = Resources.fromPath(gwcProvider.getLocation());
                if (Resources.exists(providerConfigFile)
                        && FileUtils.sizeOf(providerConfigFile.file()) > 0) {
                    Resources.copy(
                            gwcProvider.in(),
                            targetGWCProviderBackupDir,
                            providerConfigFile.name());
                }
            }

            // Store GWC Layers Configurations
            // TODO: This should be done using the spring-batch item reader/writer, since it is not
            // safe to save tons of single XML files.
            //       Nonetheless, given the default implementation of GWC Catalog does not have much
            // sense to refactor this code now.
            final TileLayerCatalog gwcCatalog =
                    (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");

            if (gwcCatalog != null) {
                final XMLConfiguration gwcXmlPersisterFactory =
                        (XMLConfiguration) GeoServerExtensions.bean("gwcXmlConfig");
                final GeoServerResourceLoader resourceLoader =
                        new GeoServerResourceLoader(targetBackupFolder.dir());

                final DefaultTileLayerCatalog gwcBackupCatalog =
                        new DefaultTileLayerCatalog(resourceLoader, gwcXmlPersisterFactory);
                gwcBackupCatalog.initialize();

                for (String layerName : gwcCatalog.getLayerNames()) {
                    backupGwcLayer(gwcCatalog, gwcBackupCatalog, layerName);
                }
            }

        } catch (Exception e) {
            this.catalogBackupRestoreTasklet.logValidationExceptions(null, e);
        }
    }

    /**
     * @param targetBackupFolder
     * @throws Exception
     */
    public void backupGWCLayers(Resource targetBackupFolder) throws Exception {
        GWCConfigPersister gwcGeoServerConfigPersister =
                (GWCConfigPersister)
                        GeoServerExtensions.bean(
                                this.catalogBackupRestoreTasklet.GWC_GEOSERVER_CONFIG_PERSISTER);

        GWCConfigPersister testGWCCP =
                new GWCConfigPersister(
                        this.catalogBackupRestoreTasklet.getxStreamPersisterFactory(),
                        new GeoServerResourceLoader(targetBackupFolder.dir()));

        // Test that everything went well
        try {
            testGWCCP.save(gwcGeoServerConfigPersister.getConfig());

            GWCConfig gwcConfig = testGWCCP.getConfig();

            Assert.notNull(gwcConfig, "gwcConfig is NULL");

            // Store GWC Layers Configurations
            final TileLayerCatalog gwcCatalog =
                    (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");

            if (gwcCatalog != null) {
                final XMLConfiguration gwcXmlPersisterFactory =
                        (XMLConfiguration) GeoServerExtensions.bean("gwcXmlConfig");
                final GeoServerResourceLoader resourceLoader =
                        new GeoServerResourceLoader(targetBackupFolder.dir());

                final DefaultTileLayerCatalog gwcBackupCatalog =
                        new DefaultTileLayerCatalog(resourceLoader, gwcXmlPersisterFactory);
                gwcBackupCatalog.initialize();

                for (String layerName : gwcCatalog.getLayerNames()) {
                    backupGwcLayer(gwcCatalog, gwcBackupCatalog, layerName);
                }
            }

        } catch (Exception e) {
            this.catalogBackupRestoreTasklet.logValidationExceptions(null, e);
        }
    }

    /**
     * @param gwcCatalog
     * @param gwcBackupCatalog
     * @param layerName
     */
    public void backupGwcLayer(
            final TileLayerCatalog gwcCatalog,
            final DefaultTileLayerCatalog gwcBackupCatalog,
            String layerName) {
        GeoServerTileLayerInfo gwcLayerInfo = gwcCatalog.getLayerByName(layerName);

        // Persist the GWC Layer Info into the backup folder
        boolean persistResource = false;

        LayerInfo layerInfo =
                this.catalogBackupRestoreTasklet.getCatalog().getLayerByName(layerName);

        if (layerInfo != null) {
            WorkspaceInfo ws = this.catalogBackupRestoreTasklet.getLayerWorkspace(layerInfo);

            if (!this.catalogBackupRestoreTasklet.filteredResource(
                    layerInfo, ws, true, LayerInfo.class)) {
                persistResource = true;
            }
        } else {
            try {
                LayerGroupInfo layerGroupInfo =
                        this.catalogBackupRestoreTasklet
                                .getCatalog()
                                .getLayerGroupByName(layerName);
                if (layerGroupInfo != null) {
                    WorkspaceInfo ws =
                            this.catalogBackupRestoreTasklet.getLayerGroupWorkspace(layerGroupInfo);

                    if (!this.catalogBackupRestoreTasklet.filteredResource(ws, false)) {
                        persistResource = true;
                    }
                }
            } catch (NullPointerException e) {
                if (this.catalogBackupRestoreTasklet.getCurrentJobExecution() != null) {
                    this.catalogBackupRestoreTasklet
                            .getCurrentJobExecution()
                            .addWarningExceptions(Arrays.asList(e));
                }
            }
        }

        if (persistResource) {
            gwcBackupCatalog.save(gwcLayerInfo);
        }
    }

    /**
     * Backup additional stuff only when performing a FULL backup.
     *
     * @param dd
     * @param resourceStore
     * @param targetBackupFolder
     * @throws IOException
     * @throws Exception
     */
    public void backupFullAdditionals(
            final GeoServerDataDirectory dd,
            final ResourceStore resourceStore,
            Resource targetBackupFolder)
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

        for (GeoServerPropertyConfigurer props :
                GeoServerExtensions.extensions(GeoServerPropertyConfigurer.class)) {
            // On restore invoke 'props.reload();' after having replaced the properties
            // files.
            Resource configFile = props.getConfigFile();

            if (configFile != null && Resources.exists(configFile)) {
                Resource targetDir =
                        Files.asResource(
                                targetGeoServerResourceLoader.findOrCreateDirectory(
                                        Paths.convert(
                                                dd.getResourceLoader().getBaseDirectory(),
                                                configFile.parent().dir())));

                Resources.copy(configFile.file(), targetDir);
            }
        }

        // Backup other configuration bits, like images, palettes, user projections and so
        // on...
        this.catalogBackupRestoreTasklet.backupRestoreAdditionalResources(
                resourceStore, targetBackupFolder);

        // Backup GWC Configuration bits
        if (!this.catalogBackupRestoreTasklet.isSkipGWC()) {
            try {
                if (GeoServerExtensions.bean(
                                this.catalogBackupRestoreTasklet.GWC_GEOSERVER_CONFIG_PERSISTER)
                        != null) {
                    backupGWCSettings(targetBackupFolder);
                }
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.log(Level.WARNING, "Skipped GWC GeoServer Config Persister: ", e);
            }
        }
    }
}
