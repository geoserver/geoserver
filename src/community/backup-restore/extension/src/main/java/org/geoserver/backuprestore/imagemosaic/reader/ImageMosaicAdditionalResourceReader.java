/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.imagemosaic.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.imagemosaic.ImageMosaicAdditionalResource;
import org.geoserver.backuprestore.reader.CatalogAdditionalResourcesReader;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/** @author Alessio Fabiani, GeoSolutions */
public class ImageMosaicAdditionalResourceReader extends ImageMosaicAdditionalResource
        implements CatalogAdditionalResourcesReader<StoreInfo> {

    final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

    @Override
    public boolean canHandle(Object item) {
        if (item instanceof CoverageStoreInfo
                && ((CoverageStoreInfo) item).getType().equals(COVERAGE_TYPE)) {
            return true;
        }
        return false;
    }

    @Override
    public void readAdditionalResources(Backup backupFacade, Resource base, StoreInfo item)
            throws IOException {

        final Resource sourceBackupFolder =
                BackupUtils.dir(base.parent(), IMAGEMOSAIC_INDEXES_FOLDER);

        final CoverageStoreInfo mosaicCoverageStore =
                backupFacade.getCatalog().getResourcePool().clone((CoverageStoreInfo) item, true);
        final String mosaicName = mosaicCoverageStore.getName();
        final String mosaicUrlBase = mosaicCoverageStore.getURL();

        final Resource mosaicIndexBase = Resources.fromURL(mosaicUrlBase);

        List<Resource> mosaicIndexerResources =
                Resources.list(sourceBackupFolder, resources.get("properties"), true);

        mosaicIndexerResources.addAll(
                Resources.list(sourceBackupFolder, resources.get("info"), true));

        boolean datastoreAlreadyPresent = true;
        for (Resource res : mosaicIndexerResources) {
            if (!FilenameUtils.getBaseName(res.name()).equals(mosaicName)
                    && Resources.exists(res)
                    && Resources.canRead(res)) {
                boolean result = copyFile(sourceBackupFolder, mosaicIndexBase, res, false);

                if (result && FilenameUtils.getBaseName(res.name()).equals("datastore")) {
                    // The copy of the new "datastore.properties" was successful, meaning that
                    // there wasn't an other copy of that file on the target folder.
                    datastoreAlreadyPresent = false;
                }
            }
        }

        List<Resource> mosaicIndexerTemplateResources =
                Resources.list(sourceBackupFolder, resources.get("templates"), true);

        for (Resource res : mosaicIndexerTemplateResources) {
            if (Resources.exists(res) && Resources.canRead(res)) {
                boolean result = copyFile(sourceBackupFolder, mosaicIndexBase, res, true);

                if (result) {
                    resolveTemplate(sourceBackupFolder, mosaicIndexBase, res);
                }
            }
        }

        if (!datastoreAlreadyPresent) {
            // Sine there wasn't already a "datasotre.properties" on the target folder
            // we assume this is a new mosaic.
            // We need to be sure the property "CanBeEmpty=true" is present on the
            // "indexer.properties"
            final File indexerFile = new File(mosaicIndexBase.dir(), "indexer.properties");

            Properties indexerProperties = new Properties();

            if (indexerFile.exists() && indexerFile.canRead()) {
                indexerProperties.load(new FileInputStream(indexerFile));
            }

            indexerProperties.setProperty("CanBeEmpty", "true");

            indexerProperties.store(new FileOutputStream(indexerFile), null);
        }
    }

    /** */
    private void resolveTemplate(
            final Resource sourceBackupFolder, final Resource mosaicIndexBase, Resource res)
            throws IOException, FileNotFoundException {
        // Overwrite target .properties file by resolving template placeholders
        Properties templateProperties = new Properties();
        templateProperties.load(res.in());

        Properties resolvedProperties = new Properties();
        for (Entry<Object, Object> propEntry : templateProperties.entrySet()) {
            String value = (String) propEntry.getValue();

            if (GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
                value = (String) gsEnvironment.resolveValue(value);
            }

            resolvedProperties.setProperty((String) propEntry.getKey(), value);
        }

        final String relative =
                sourceBackupFolder.dir().toURI().relativize(res.file().toURI()).getPath();

        final String targetPropertyFileName =
                relative.substring(0, relative.length() - ".template".length());

        final File targetFile = new File(mosaicIndexBase.parent().dir(), targetPropertyFileName);

        resolvedProperties.store(new FileOutputStream(targetFile), null);
    }

    /** */
    private boolean copyFile(
            final Resource sourceBackupFolder,
            final Resource mosaicIndexBase,
            Resource res,
            boolean overwrite)
            throws IOException {
        final String relative =
                sourceBackupFolder.dir().toURI().relativize(res.file().toURI()).getPath();

        Resource targetFtl = Resources.fromPath(relative, mosaicIndexBase.parent());

        if (!Resources.exists(targetFtl) || overwrite) {
            if (!targetFtl.parent().dir().exists()) {
                targetFtl.parent().dir().mkdirs();
            }

            Resources.copy(res.file(), targetFtl.parent());

            return true;
        }

        return false;
    }
}
