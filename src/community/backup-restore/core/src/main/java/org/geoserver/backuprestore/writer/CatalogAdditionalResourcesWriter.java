/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.io.IOException;
import org.geoserver.backuprestore.Backup;
import org.geoserver.platform.resource.Resource;

/**
 * Extension point allowing {@link CatalogItemWriter} to dump additional resources. <br>
 * The concrete classes can be external beans defined as GeoServer Extensions implementing the
 * {@link CatalogAdditionalResourcesWriter} interface.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface CatalogAdditionalResourcesWriter<T> {

    public boolean canHandle(Object item);

    public void writeAdditionalResources(Backup backupFacade, Resource base, T item)
            throws IOException;
}
