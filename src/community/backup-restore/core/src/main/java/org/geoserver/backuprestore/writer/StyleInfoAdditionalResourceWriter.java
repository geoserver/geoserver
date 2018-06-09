/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.io.IOException;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/**
 * On a {@link CatalogFireWriter} firePostWrite event call, dumps styles main resources into the
 * backup target folder. <br>
 * Notice that this one does check actually for icons and everything <b>different</b> from "sld",
 * "ysld", "xml" or "css" (depending on which kind of style the resource is). Those are managed
 * separately by the {@link AbstractCatalogBackupRestoreTasklet}.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class StyleInfoAdditionalResourceWriter
        implements CatalogAdditionalResourcesWriter<StyleInfo> {

    @Override
    public boolean canHandle(Object item) {
        return (item instanceof StyleInfo);
    }

    @Override
    public void writeAdditionalResources(Backup backupFacade, Resource base, StyleInfo item)
            throws IOException {
        Resource styleFile = backupFacade.getGeoServerDataDirectory().get(item, item.getFilename());

        if (styleFile != null && Resources.exists(styleFile)) {
            Resources.copy(styleFile.file(), BackupUtils.dir(base.parent(), "styles"));
        }
    }
}
