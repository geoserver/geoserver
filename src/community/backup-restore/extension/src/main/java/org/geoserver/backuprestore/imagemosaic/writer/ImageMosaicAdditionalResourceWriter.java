/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.imagemosaic.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.imagemosaic.ImageMosaicAdditionalResource;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.backuprestore.writer.CatalogAdditionalResourcesWriter;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;

/** @author Alessio Fabiani, GeoSolutions */
public class ImageMosaicAdditionalResourceWriter extends ImageMosaicAdditionalResource
        implements CatalogAdditionalResourcesWriter<StoreInfo> {

    @Override
    public boolean canHandle(Object item) {
        if (item instanceof CoverageStoreInfo
                && ((CoverageStoreInfo) item).getType().equals(COVERAGE_TYPE)) {
            return true;
        }
        return false;
    }

    @Override
    public void writeAdditionalResources(Backup backupFacade, Resource base, StoreInfo item)
            throws IOException {

        final Resource targetBackupFolder =
                BackupUtils.dir(base.parent(), IMAGEMOSAIC_INDEXES_FOLDER);

        // Create folder if not exists
        Resources.directory(targetBackupFolder, !Resources.exists(targetBackupFolder));

        final CoverageStoreInfo mosaicCoverageStore =
                backupFacade.getCatalog().getResourcePool().clone((CoverageStoreInfo) item, true);
        final String mosaicName = mosaicCoverageStore.getName();
        final String mosaicUrlBase = mosaicCoverageStore.getURL();

        final Resource mosaicIndexBase = Resources.fromURL(mosaicUrlBase);

        final Resource mosaicBaseFolder =
                Files.asResource(
                        (Resources.directory(mosaicIndexBase) != null
                                ? Resources.directory(mosaicIndexBase)
                                : Resources.directory(mosaicIndexBase.parent())));

        // Create the target mosaic folder
        Resource targetMosaicBaseFolder =
                BackupUtils.dir(targetBackupFolder, mosaicBaseFolder.name());

        if (Resources.exists(mosaicIndexBase)) {
            for (Entry<String, Filter<Resource>> entry : resources.entrySet()) {
                List<Resource> mosaicIndexerResources =
                        Resources.list(mosaicIndexBase, entry.getValue(), true);

                for (Resource res : mosaicIndexerResources) {
                    if (!FilenameUtils.getBaseName(res.name()).equals(mosaicName)
                            && !FilenameUtils.getBaseName(res.name())
                                    .equals(mosaicBaseFolder.name())
                            && Resources.exists(res)
                            && Resources.canRead(res)) {
                        final String relative =
                                mosaicIndexBase
                                        .parent()
                                        .dir()
                                        .toURI()
                                        .relativize(res.file().toURI())
                                        .getPath();

                        Resource targetFtl = Resources.fromPath(relative, targetBackupFolder);

                        if (!targetFtl.parent().dir().exists()) {
                            targetFtl.parent().dir().mkdirs();
                        }

                        Resources.copy(res.file(), targetFtl.parent());
                    }
                }
            }
        }

        // Populate "Name=<mosaicName>" property into the indexer
        final File indexerFile = new File(targetMosaicBaseFolder.dir(), "indexer.properties");

        Properties indexerProperties = new Properties();

        if (indexerFile.exists() && indexerFile.canRead()) {
            indexerProperties.load(new FileInputStream(indexerFile));
        }

        indexerProperties.setProperty("Name", mosaicName);

        indexerProperties.store(new FileOutputStream(indexerFile), null);
    }
}
